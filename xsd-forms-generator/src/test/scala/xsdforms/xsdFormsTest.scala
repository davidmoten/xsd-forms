package xsdforms {
  import org.junit.Test
  import xsd.Schema
  import org.junit.runners.BlockJUnit4ClassRunner
  import org.junit.runner.notification.Failure

  import java.io.File
  import java.io.InputStream

  object TstUtil {

    import org.apache.commons.io._

    def generate(
      idPrefix: String,
      schemaInputStream: InputStream,
      rootElement: String,
      outputFile: File,
      extraScript: Option[String] = None) {

      import scala.xml._

      val schema = scalaxb.fromXML[Schema](
        XML.load(schemaInputStream))
      val ns = schema.targetNamespace.get.toString
      val visitor = new HtmlVisitor(ns, idPrefix, extraScript)

      //println(schema.toString.replaceAll("\\(", "(\n"))

      new Traversor(schema, rootElement, visitor).process
      //println(visitor.text)
      outputFile.getParentFile().mkdirs
      val fos = new java.io.FileOutputStream(outputFile);
      fos.write(visitor.text.getBytes)
      fos.close

      //println(visitor)
      println("generated")
    }

    def generateDemoForm(file: File) {
      println("generating demo form")
      generate(
        idPrefix = "c-",
        schemaInputStream = TstUtil.getClass().getResourceAsStream("/demo.xsd"),
        rootElement = "main",
        outputFile = file)
    }

    def setupDemoWebapp {
      val directory = new File("target/demo")
      FileUtils.deleteDirectory(directory)
      FileUtils.copyDirectory(new File("src/main/webapp"), directory)
      generateDemoForm(new File(directory, "demo-form.html"))
      println("copied webapp directory to target/demo")
    }

  }

  @Test
  class TraversorTest {

    import org.apache.commons.io._
    import TstUtil._

    @Test
    def testSetupWebapp() {
      setupDemoWebapp
    }

    @Test
    def generatePersonForm() {
      println("generating person form")
      generate(
        idPrefix = "a-",
        schemaInputStream = TstUtil.getClass().getResourceAsStream("/test.xsd"),
        rootElement = "person",
        outputFile = new File("target/generated-webapp/person-form.html"))
    }

    @Test
    def generateCensusForm {
      println("generating census form")
      generate(
        idPrefix = "b-",
        schemaInputStream = TstUtil.getClass().getResourceAsStream("/australian-census-2011.xsd"),
        rootElement = "census",
        outputFile = new File("target/generated-webapp/census-form.html"))
    }

    @Test
    def generateTheDemoForm {
      val file = new File("target/generated-webapp/demo-form.html")
      generateDemoForm(file)
      FileUtils.copyFileToDirectory(file, new File("target/demo"));
    }

  }

  @Test
  class TraversorSeleniumTest {

    import org.openqa.selenium.By
    import org.openqa.selenium.Keys
    import org.openqa.selenium.Proxy
    import org.openqa.selenium.WebDriver
    import org.openqa.selenium.WebElement
    import org.openqa.selenium.firefox.FirefoxDriver
    import org.openqa.selenium.firefox.FirefoxProfile
    import org.openqa.selenium.chrome.ChromeDriver
    import org.openqa.selenium.htmlunit.HtmlUnitDriver
    import org.junit.Assert._
    import org.junit.matchers.JUnitMatchers._
    import org.apache.commons.io._
    import TstUtil._

    private val uri = new File("target/demo/demo-form.html").toURI().toString()
    private val WebDriverChromeDriverKey = "webdriver.chrome.driver"

    @Test
    def testOnFirefox() {
      println("firefox test")
      setupDemoWebapp
      if (!"false".equals(System.getProperty("firefox")))
        testOnWebDriver(new FirefoxDriver)
    }

    @Test
    def testOnChrome() {
      println("chrome test")
      setupDemoWebapp
      if (!"false".equals(System.getProperty("chrome")))
        testOnWebDriver(new ChromeDriver)
    }

    //    @Test
    def testOnHtmlUnit() {
      println("HtmlUnit test")
      setupDemoWebapp
      testOnWebDriver(new HtmlUnitDriver(true))
    }

    private def testOnWebDriver(driver: WebDriver) {
      println("testing web driver " + driver.getClass().getSimpleName())
      driver.get(uri)
      testMakeVisible(driver, 24)
      testPatternValidation(driver, 29)
      testMultiplePatternValidation(driver, 31)
      testStringMinLength(driver, 32)
      testIntegerMinLength(driver, 33)
      testDecimalMinLength(driver, 34)
      testStringMaxLength(driver, 35)
      testIntegerMaxLength(driver, 36)
      testDecimalMaxLength(driver, 37)
      testStringLength(driver, 38)
      testIntegerLength(driver, 39)
      testDecimalLength(driver, 40)
      testIntegerMinInclusive(driver, 41)
      testDecimalMinInclusive(driver, 42)
      testIntegerMaxInclusive(driver, 43)
      testDecimalMaxInclusive(driver, 44)
      testIntegerMinExclusive(driver, 45)
      testDecimalMinExclusive(driver, 46)
      testIntegerMaxExclusive(driver, 47)
      testDecimalMaxExclusive(driver, 48)
      testChoice(driver, 49)
      testRepeat(driver, 52)
      testRepeatWhenMinOccursIsZero(driver, 53)
      //testChoiceRepeat(driver,58)
      testSubmission(driver)
      driver.close
      val log = new File("chromedriver.log")
      if (log.exists) log.delete();
    }

    private def getInput(driver: WebDriver, itemNo: Int) =
      driver.findElement(By.id("c-item-" + itemNo));

    private def getError(driver: WebDriver, itemNo: Int) =
      driver.findElement(By.id("c-item-error-" + itemNo))

    private def testMakeVisible(driver: WebDriver, itemNo: Int) {
      val input = getInput(driver, itemNo)
      val next = getInput(driver, itemNo + 1)
      val first = getInput(driver, 2)
      assertFalse(next.isDisplayed)
      input.click
      input.sendKeys(Keys.ARROW_DOWN)
      input.sendKeys(Keys.ENTER)
      first.click
      assertTrue(next.isDisplayed())
    }

    private def testPatternValidation(driver: WebDriver, itemNo: Int) {
      val input = getInput(driver, itemNo)
      val error = getError(driver, itemNo)
      assertFalse(error.isDisplayed)
      input.sendKeys("\n")
      assertFalse(error.isDisplayed)
      input.sendKeys("a\n");
      assertTrue(error.isDisplayed)
      input.sendKeys("123\n")
      assertFalse(error.isDisplayed)
      input.sendKeys("\b\n")
      assertTrue(error.isDisplayed)
      input.sendKeys("\b\b\b\n")
      assertTrue(error.isDisplayed)
    }

    private def testMultiplePatternValidation(driver: WebDriver, itemNo: Int) {
      val input = getInput(driver, itemNo)
      val error = getError(driver, itemNo)
      assertFalse(error.isDisplayed)
      input.sendKeys("Z\n")
      assertTrue(error.isDisplayed)
      input.sendKeys("\b\b\n");
      assertTrue(error.isDisplayed)
      input.sendKeys("AB\n")
      assertFalse(error.isDisplayed)
      input.sendKeys("\b\b\n")
      assertTrue(error.isDisplayed)
      input.sendKeys("CD\n")
      assertFalse(error.isDisplayed)
    }

    private def testStringMinLength(driver: WebDriver, itemNo: Int) {
      val input = getInput(driver, itemNo)
      val error = getError(driver, itemNo)
      assertFalse(error.isDisplayed)
      input.sendKeys("Z\n")
      assertTrue(error.isDisplayed)
      input.sendKeys("\b\b\n");
      assertTrue(error.isDisplayed)
      input.sendKeys("ABC\n")
      assertFalse(error.isDisplayed)
      input.sendKeys("D");
      assertFalse(error.isDisplayed)
      input.sendKeys("\b\b\b\b\n")
      assertTrue(error.isDisplayed)
    }

    private def testIntegerMinLength(driver: WebDriver, itemNo: Int) {
      val input = getInput(driver, itemNo)
      val error = getError(driver, itemNo)
      assertFalse(error.isDisplayed)
      input.sendKeys("ABC\n")
      assertTrue(error.isDisplayed)
      input.sendKeys("\b\b\b\n");
      assertTrue(error.isDisplayed)
      input.sendKeys("12\n")
      assertTrue(error.isDisplayed)
      input.sendKeys("3\n");
      assertFalse(error.isDisplayed)
      input.sendKeys("\b\b\b\b\n")
      assertTrue(error.isDisplayed)
    }

    private def testDecimalMinLength(driver: WebDriver, itemNo: Int) {
      val input = getInput(driver, itemNo)
      val error = getError(driver, itemNo)
      assertFalse(error.isDisplayed)
      input.sendKeys("ABC\n")
      assertTrue(error.isDisplayed)
      input.sendKeys("\b\b\b\n");
      assertTrue(error.isDisplayed)
      input.sendKeys("1.\n")
      assertTrue(error.isDisplayed)
      input.sendKeys("2\n");
      assertFalse(error.isDisplayed)
      input.sendKeys("\b\b\b\b\n")
      assertTrue(error.isDisplayed)
    }

    private def testStringMaxLength(driver: WebDriver, itemNo: Int) {
      val input = getInput(driver, itemNo)
      val error = getError(driver, itemNo)
      assertFalse(error.isDisplayed)
      input.sendKeys("abcde\n")
      assertTrue(error.isDisplayed)
      input.sendKeys("\b\n");
      assertFalse(error.isDisplayed)
      input.clear;
      assertFalse(error.isDisplayed)
    }

    private def testIntegerMaxLength(driver: WebDriver, itemNo: Int) {
      val input = getInput(driver, itemNo)
      val error = getError(driver, itemNo)
      assertFalse(error.isDisplayed)
      input.sendKeys("a\n")
      assertTrue(error.isDisplayed)
      input.clear
      input.sendKeys("12345")
      assertTrue(error.isDisplayed)
      input.sendKeys("\b\n");
      assertFalse(error.isDisplayed)
      input.clear;
      assertTrue(error.isDisplayed)
    }

    private def testDecimalMaxLength(driver: WebDriver, itemNo: Int) {
      val input = getInput(driver, itemNo)
      val error = getError(driver, itemNo)
      assertFalse(error.isDisplayed)
      input.sendKeys("a\n")
      assertTrue(error.isDisplayed)
      input.clear
      input.sendKeys("1.234")
      assertTrue(error.isDisplayed)
      input.sendKeys("\b\n");
      assertFalse(error.isDisplayed)
      input.clear;
      assertTrue(error.isDisplayed)
    }

    private def testStringLength(driver: WebDriver, itemNo: Int) {
      val input = getInput(driver, itemNo)
      val error = getError(driver, itemNo)
      assertFalse(error.isDisplayed)
      input.sendKeys("abcde\n")
      assertTrue(error.isDisplayed)
      input.clear
      input.sendKeys("abc\n")
      assertTrue(error.isDisplayed)
      input.clear
      input.sendKeys("abcd\n")
      assertFalse(error.isDisplayed)
    }

    private def testIntegerLength(driver: WebDriver, itemNo: Int) {
      val input = getInput(driver, itemNo)
      val error = getError(driver, itemNo)
      assertFalse(error.isDisplayed)
      input.sendKeys("12345\n")
      assertTrue(error.isDisplayed)
      input.clear
      input.sendKeys("123\n")
      assertTrue(error.isDisplayed)
      input.clear
      input.sendKeys("1234\n")
      assertFalse(error.isDisplayed)
    }

    private def testDecimalLength(driver: WebDriver, itemNo: Int) {
      val input = getInput(driver, itemNo)
      val error = getError(driver, itemNo)
      assertFalse(error.isDisplayed)
      input.sendKeys("1.234\n")
      assertTrue(error.isDisplayed)
      input.clear
      input.sendKeys("1.2\n")
      assertTrue(error.isDisplayed)
      input.clear
      input.sendKeys("1.23\n")
      assertFalse(error.isDisplayed)
    }

    private def testIntegerMinInclusive(driver: WebDriver, itemNo: Int) {
      val input = getInput(driver, itemNo)
      val error = getError(driver, itemNo)
      assertFalse(error.isDisplayed)
      input.sendKeys("4\n")
      assertTrue(error.isDisplayed)
      input.clear
      input.sendKeys("5\n")
      assertFalse(error.isDisplayed)
    }

    private def testDecimalMinInclusive(driver: WebDriver, itemNo: Int) {
      val input = getInput(driver, itemNo)
      val error = getError(driver, itemNo)
      assertFalse(error.isDisplayed)
      input.sendKeys("4.9999\n")
      assertTrue(error.isDisplayed)
      input.clear
      input.sendKeys("5\n")
      assertFalse(error.isDisplayed)
    }

    private def testIntegerMaxInclusive(driver: WebDriver, itemNo: Int) {
      val input = getInput(driver, itemNo)
      val error = getError(driver, itemNo)
      assertFalse(error.isDisplayed)
      input.sendKeys("11\n")
      assertTrue(error.isDisplayed)
      input.clear
      input.sendKeys("10\n")
      assertFalse(error.isDisplayed)
    }

    private def testDecimalMaxInclusive(driver: WebDriver, itemNo: Int) {
      val input = getInput(driver, itemNo)
      val error = getError(driver, itemNo)
      assertFalse(error.isDisplayed)
      input.sendKeys("10.0001\n")
      assertTrue(error.isDisplayed)
      input.clear
      input.sendKeys("10\n")
      assertFalse(error.isDisplayed)
    }

    private def testIntegerMinExclusive(driver: WebDriver, itemNo: Int) {
      val input = getInput(driver, itemNo)
      val error = getError(driver, itemNo)
      assertFalse(error.isDisplayed)
      input.sendKeys("4\n")
      assertTrue(error.isDisplayed)
      input.clear
      input.sendKeys("5\n")
      assertTrue(error.isDisplayed)
      input.clear
      input.sendKeys("6\n")
      assertFalse(error.isDisplayed)
    }

    private def testDecimalMinExclusive(driver: WebDriver, itemNo: Int) {
      val input = getInput(driver, itemNo)
      val error = getError(driver, itemNo)
      assertFalse(error.isDisplayed)
      input.sendKeys("5\n")
      assertTrue(error.isDisplayed)
      input.clear
      input.sendKeys("5.0001\n")
      assertFalse(error.isDisplayed)
    }

    private def testIntegerMaxExclusive(driver: WebDriver, itemNo: Int) {
      val input = getInput(driver, itemNo)
      val error = getError(driver, itemNo)
      assertFalse(error.isDisplayed)
      input.sendKeys("10\n")
      assertTrue(error.isDisplayed)
      input.clear
      input.sendKeys("9\n")
      assertFalse(error.isDisplayed)
    }

    private def testDecimalMaxExclusive(driver: WebDriver, itemNo: Int) {
      val input = getInput(driver, itemNo)
      val error = getError(driver, itemNo)
      assertFalse(error.isDisplayed)
      input.sendKeys("10\n")
      assertTrue(error.isDisplayed)
      input.clear
      input.sendKeys("9.9999\n")
      assertFalse(error.isDisplayed)
    }

    private def testChoice(driver: WebDriver, itemNo: Int) {
      val input = driver.findElement(By.name("c-item-input-" + itemNo));
      val option1 = driver.findElement(By.id("c-item-" + itemNo + "-1"))
      val option2 = driver.findElement(By.id("c-item-" + itemNo + "-2"))
      assertFalse(input.isSelected)
      option1.click
      assertTrue(getInput(driver, itemNo + 1).isDisplayed)
      assertFalse(getInput(driver, itemNo + 2).isDisplayed)
      option2.click
      assertFalse(getInput(driver, itemNo + 1).isDisplayed)
      assertTrue(getInput(driver, itemNo + 2).isDisplayed)
    }

    private def testRepeat(driver: WebDriver, itemNo: Int) {
      val button = driver.findElement(By.id("c-repeat-button-" + itemNo))
      button.click;
      checkDisplayedById(driver, "c-repeating-enclosing-" + itemNo + "-10001")
      checkDisplayedById(driver, "c-item-" + itemNo + "-10001")
      button.click
      checkDisplayedById(driver, "c-repeating-enclosing-" + itemNo + "-10002")
      checkDisplayedById(driver, "c-item-" + itemNo + "-10002")

      //clear validation for integer value
      val input = driver.findElement(By.id("c-item-" + itemNo))
      //put integer in so passes validation on submission
      input.sendKeys("456\n")
 
      val input1 = driver.findElement(By.id("c-item-"+itemNo + "-10001"))
      val error1 = driver.findElement(By.id("c-item-error-"+itemNo + "-10001"))
      val input2 = driver.findElement(By.id("c-item-"+itemNo + "-10002"))
      val error2 = driver.findElement(By.id("c-item-error-"+itemNo + "-10002"))

      input1.sendKeys("123\n")
      assertFalse(error1.isDisplayed)
      assertFalse(error2.isDisplayed)
      input2.sendKeys("abc\n")
      assertFalse(error1.isDisplayed)
      assertTrue(error2.isDisplayed)
      input2.clear
      input2.sendKeys("123\n")
      assertFalse(error2.isDisplayed)
    }

    private def testRepeatWhenMinOccursIsZero(driver:WebDriver, itemNo:Int) {
      //default input should not be visible because minOccurs=0
      val input = driver.findElement(By.id("c-repeating-enclosing-" + itemNo))
      assertFalse(input.isDisplayed)
      val button = driver.findElement(By.id("c-repeat-button-" + itemNo))
      button.click;
      val input1 = driver.findElement(By.id("c-item-" + itemNo + "-10003"))
      assertTrue(input1.isDisplayed)
      //make sure it validates come submission time
      input1.sendKeys("123\n")
    }

    private def testChoiceRepeat(driver:WebDriver, itemNo: Int) {
      val input = driver.findElement(By.id("c-repeating-enclosing-" + itemNo))
      assertTrue(input.isDisplayed)
      val button = driver.findElement(By.id("c-repeat-button-" + itemNo))
      button.click;
      val input1 = driver.findElement(By.id("c-item-" + itemNo + "-1-10004"))
      assertTrue(input1.isDisplayed)
      assertEquals("c-item-input-" + itemNo + "-10004",input1.getAttribute("name"))
    }
    
    private def checkDisplayedById(driver: WebDriver, id: String) {
      val item = driver.findElement(By.id(id))
      assertTrue(item.isDisplayed)
    }

    private def setInput(driver: WebDriver, itemNo: Int, text: String) {
      val in = getInput(driver, itemNo)
      in.clear
      in.sendKeys(text)
    }

    private def testSubmission(driver: WebDriver) {
      val preSubmit = driver.findElement(By.id("pre-submit"))
      val errors = driver.findElement(By.id("validation-errors"))
      assertFalse(errors.isDisplayed());
      preSubmit.click
      assertTrue(errors.isDisplayed());
      val xml = driver.findElement(By.id("submit-comments"))
      assertEquals("", xml.getText.trim)
      //fix errors
      setInput(driver, 6, "1")
      setInput(driver, 8, "1")
      setInput(driver, 27, "a123")
      setInput(driver, 29, "a123")
      setInput(driver, 30, "a123")
      setInput(driver, 31, "AB")
      setInput(driver, 32, "abc")
      setInput(driver, 33, "123")
      setInput(driver, 34, "123")
      setInput(driver, 36, "123")
      setInput(driver, 37, "123")
      setInput(driver, 38, "1234")
      setInput(driver, 39, "1234")
      setInput(driver, 40, "1234")
      preSubmit.click
      preSubmit.click
      //TODO why twice to get chrome to work?
      println("xml=" + xml)
      assertFalse(errors.isDisplayed());
      assertTrue(xml.getText.trim.contains("xmlns"))

      //TODO generate objects from demo schema and attempt unmarshal of xml
      //      val text = xml.getText.replaceAll("xmlns=\".*\"", "")
      //val main = scalaxb.fromXML[demo.Main](scala.xml.XML.loadString(text))
    }
  }

}
