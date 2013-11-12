package xsdforms {
  import org.junit.Test
  import org.junit.Before
  import xsd.Schema
  import org.junit.runners.BlockJUnit4ClassRunner
  import org.junit.runner.notification.Failure

  import java.io.File
  import java.io.InputStream

  object TstUtil {

    import org.apache.commons.io._

    val idPrefix = "c-"

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

      val visitor = new TreeCreatingVisitor()

      new SchemaTraversor(schema, rootElement, visitor).traverse
      println("tree:\n" + visitor)

      val text = new TreeToHtmlConverter(ns, idPrefix, extraScript, visitor.rootNode).text
      outputFile.getParentFile().mkdirs
      val fos = new java.io.FileOutputStream(outputFile);
      fos.write(text.getBytes)
      fos.close

      //println(visitor)
      println("generated")
    }

    def generateDemoForm(file: File) {
      println("generating demo form")
      generate(
        idPrefix = idPrefix,
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

    @Before
    def setupCssJsFiles {
      {
        val directory = new File("target/generated-webapp/css")
        directory.mkdirs
        FileUtils.deleteDirectory(directory)
        FileUtils.copyDirectory(new File("src/main/webapp/css"), directory)
      }
      {
        val directory = new File("target/generated-webapp/js")
        directory.mkdirs
        FileUtils.deleteDirectory(directory)
        FileUtils.copyDirectory(new File("src/main/webapp/js"), directory)
      }
      println("copied css+js directories to target/generated-webapp")
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
    def generateSimpleForm() {
      println("generating simple form")
      generate(
        idPrefix = "a-",
        schemaInputStream = TstUtil.getClass().getResourceAsStream("/simple.xsd"),
        rootElement = "person",
        outputFile = new File("target/generated-webapp/simple-form.html"))
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
    import org.openqa.selenium.support.ui.Select
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
    import TreeToHtmlConverter._

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
      //TODO enable this
      //      testDateDefaultSet(driver, 11) 
      testMakeVisible(driver, 24)
      testPatternValidation(driver, 29)
      testMultiplePatternValidation(driver, 31)
      testStringMinLength(driver, 32)
      testStringMaxLength(driver, 35)
      testIntegerMaxLength(driver, 36)
      testDecimalMaxLength(driver, 37)
      testStringLength(driver, 38)
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
      //TODO reenable 
      //testChoiceRepeat(driver,58)
      testSubmission(driver)
      driver.close
      val log = new File("chromedriver.log")
      if (log.exists) log.delete();
    }

    private def getInput(driver: WebDriver, itemNo: Int, instanceNos: Instances = Instances(List(1, 1))) = {
      val id = getItemId(idPrefix, itemNo.toString, instanceNos)
      println("getInput: id=" + id)
      driver.findElement(By.id(id));
    }

    private def getError(driver: WebDriver, itemNo: Int, instanceNos: Instances = Instances(List(1, 1))) =
      driver.findElement(By.id(getItemErrorId(idPrefix, itemNo.toString, instanceNos)))

    private def testDateDefaultSet(driver: WebDriver, itemNo: Int) {

      val input = getInput(driver, itemNo)
      assertEquals("1973-06-12", input.getText)
    }

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
      val instanceNos = Instances(List(1, 1))
      val input = driver.findElement(By.name(getChoiceItemName(idPrefix, itemNo.toString, instanceNos)));
      val option1 = driver.findElement(By.id(getChoiceItemId(idPrefix, itemNo.toString, index = 1, instanceNos)))
      val option2 = driver.findElement(By.id(getChoiceItemId(idPrefix, itemNo.toString, index = 2, instanceNos)))
      assertFalse(input.isSelected)
      option1.click
      assertTrue(getInput(driver, itemNo + 1, instanceNos add 1).isDisplayed)
      assertFalse(getInput(driver, itemNo + 2, instanceNos add 1).isDisplayed)
      option2.click
      assertFalse(getInput(driver, itemNo + 1, instanceNos add 1).isDisplayed)
      assertTrue(getInput(driver, itemNo + 2, instanceNos add 1).isDisplayed)
    }

    private def elementById(driver: WebDriver, id: String) = {
      println("finding id " + id)
      driver.findElement(By.id(id))
    }

    private def testRepeat(driver: WebDriver, itemNo: Int) {
      val instanceNos = Instances(List(1))
      val button = elementById(driver, getRepeatButtonId(idPrefix, itemNo.toString, instanceNos))
      checkDisplayedById(driver, getRepeatingEnclosingId(idPrefix, itemNo.toString, instanceNos add 1))
      checkNotDisplayedById(driver, getRepeatingEnclosingId(idPrefix, itemNo.toString, instanceNos add 2))
      checkNotDisplayedById(driver, getRepeatingEnclosingId(idPrefix, itemNo.toString, instanceNos add 3))
      button.click;
      checkDisplayedById(driver, getRepeatingEnclosingId(idPrefix, itemNo.toString, instanceNos add 1))
      checkDisplayedById(driver, getRepeatingEnclosingId(idPrefix, itemNo.toString, instanceNos add 2))
      checkNotDisplayedById(driver, getRepeatingEnclosingId(idPrefix, itemNo.toString, instanceNos add 3))
      button.click
      checkDisplayedById(driver, getRepeatingEnclosingId(idPrefix, itemNo.toString, instanceNos add 3))
      checkDisplayedById(driver, getItemId(idPrefix, itemNo.toString, instanceNos add 3))

      //clear validation for integer value
      val input = driver.findElement(By.id(getItemId(idPrefix, itemNo.toString, instanceNos add 1)))
      //put integer in so passes validation on submission
      input.sendKeys("456\n")

      val input1 = driver.findElement(By.id(getItemId(idPrefix, itemNo.toString, instanceNos add 2)))
      val error1 = driver.findElement(By.id(getItemErrorId(idPrefix, itemNo.toString, instanceNos add 2)))
      val input2 = driver.findElement(By.id(getItemId(idPrefix, itemNo.toString, instanceNos add 3)))
      val error2 = driver.findElement(By.id(getItemErrorId(idPrefix, itemNo.toString, instanceNos add 3)))

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

    private def testRepeatWhenMinOccursIsZero(driver: WebDriver, itemNo: Int) {
      val instanceNos = Instances(List(1))
      //default input should not be visible because minOccurs=0
      val input = driver.findElement(By.id(getRepeatingEnclosingId(idPrefix, itemNo.toString, instanceNos add 1)))
      assertFalse(input.isDisplayed)
      val button = driver.findElement(By.id(getRepeatButtonId(idPrefix, itemNo.toString, instanceNos)))
      button.click;
      val input1 = driver.findElement(By.id(getItemId(idPrefix, itemNo.toString, instanceNos add 1)))
      assertTrue(input1.isDisplayed)
      //make sure it validates come submission time
      input1.sendKeys("123\n")
    }

    private def testChoiceRepeat(driver: WebDriver, itemNo: Int) {
      val instanceNos = Instances(List(1))
      val input = driver.findElement(By.id(getRepeatingEnclosingId(idPrefix, itemNo.toString, instanceNos add 1)))
      assertTrue(input.isDisplayed)
      val button = driver.findElement(By.id(getRepeatButtonId(idPrefix, itemNo.toString, instanceNos)))
      button.click;

      //TODO 
      val input1 = driver.findElement(By.id(getItemId(idPrefix, itemNo.toString, instanceNos add 1)))
      assertTrue(input1.isDisplayed)
    }

    private def checkDisplayedById(driver: WebDriver, id: String) {
      val item = driver.findElement(By.id(id))
      assertTrue(id + " is not visible and should be", item.isDisplayed)
    }

    private def checkNotDisplayedById(driver: WebDriver, id: String) {
      val item = driver.findElement(By.id(id))
      assertFalse(id + " is visible and should not be", item.isDisplayed)
    }

    private def setInput(driver: WebDriver, itemNo: Int, text: String,instances:Instances=Instances(List(1,1))) {
      val in = getInput(driver, itemNo,instances)
      in.clear
      in.sendKeys(text)
    }
    
    private def checkErrorDisplayed(driver: WebDriver, itemNo:Int) {
      assertTrue(getError(driver, itemNo).isDisplayed())
    }

    private def testSubmission(driver: WebDriver) {
      val preSubmit = driver.findElement(By.id("pre-submit"))
      val errors = driver.findElement(By.id("validation-errors"))
      assertFalse(errors.isDisplayed());
      preSubmit.click
      assertTrue(errors.isDisplayed());
      checkErrorDisplayed(driver, 10)
      checkErrorDisplayed(driver, 12)
      checkErrorDisplayed(driver, 14)
      checkErrorDisplayed(driver, 19)
      checkErrorDisplayed(driver, 23)
      val xml = driver.findElement(By.id("submit-comments"))
      assertEquals("", xml.getText.trim)
      //fix errors
      setInput(driver, 6, "1")
      setInput(driver, 8, "1")
      setInput(driver, 10, "2013-12-25")
      setInput(driver, 12, "22:45")
      setInput(driver, 14, "2013-12-25T04:45")
      new Select(getInput(driver,19)).selectByIndex(1)
      driver.findElement(By.id("c-item-23-instance-1_1-0")).click
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
      setInput(driver, 51, "1901-11-30",Instances(List(1,1,1)))

      driver.findElement(By.id(getChoiceItemId(idPrefix,"58", index = 1, Instances(List(1,1))))).click
      //      driver.findElement(By.id("c-item-58-instance-1_1-choice-1")).click

      preSubmit.click
      preSubmit.click
      //TODO why twice to get chrome to work?

      println("xml=" + xml.getText)
      assertFalse(errors.isDisplayed());
      assertTrue(xml.getText.trim.contains("xmlns"))
      val expectedXml = io.Source.fromInputStream(getClass.getResourceAsStream("/demo-form-expected.xml")).mkString
      assertEquals(expectedXml.trim,xml.getText.trim )
      // fail

      // attempt unmarshal of xml
      //TODO why need to remove namespace?
      val text = xml.getText.replaceAll("xmlns=\".*\"", "")
      validateAgainstSchema(xml.getText,"/demo.xsd")
      val main = scalaxb.fromXML[demo.Main](scala.xml.XML.loadString(text))
    }
    
    private def validateAgainstSchema(xml:String,xsdPath:String) {
      import javax.xml.validation._
      import javax.xml._
      import javax.xml.transform.stream._
      import java.io._
      val factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
      val schema = factory.newSchema(new StreamSource(getClass.getResourceAsStream(xsdPath)))
      val validator = schema.newValidator
      validator.validate(new StreamSource(new StringReader(xml)))
    }
  }

  @Test
  class TreeVisitorTest {
    @Test
    def test() {
      generate(
        idPrefix = "c-",
        schemaInputStream = TstUtil.getClass().getResourceAsStream("/demo.xsd"),
        rootElement = "main",
        outputFile = new File("target/demo/demo-form-tree.html"))
    }

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
      val visitor = new TreeCreatingVisitor()

      new SchemaTraversor(schema, rootElement, visitor).traverse
      println("tree:\n" + visitor)

      val text = new TreeToHtmlConverter(ns, idPrefix, extraScript, visitor.rootNode).text
      println(text)
      println("generated")

      //write results to a file
      outputFile.getParentFile().mkdirs
      val fos = new java.io.FileOutputStream(outputFile);
      fos.write(text.getBytes)
      fos.close
    }
  }

}
