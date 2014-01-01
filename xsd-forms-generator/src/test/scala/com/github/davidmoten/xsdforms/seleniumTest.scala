package com.github.davidmoten.xsdforms {

  import org.junit.Test

  @Test
  class GeneratorSeleniumTest {

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
    import tree._
    import java.io.File
    import TstUtil._
    import Ids._

    import com.gargoylesoftware.htmlunit._

    private val uri = new File("target/generated-webapp/demo-form.html").toURI().toString()
    private val WebDriverChromeDriverKey = "webdriver.chrome.driver"

    def prepareDemo {
      copyHtmlJs
      generateDemoForm
    }

    //@Test
    def testOnFirefox() {
      println("firefox test")
      prepareDemo
      if (!"false".equals(System.getProperty("firefox")))
        testOnWebDriver(new FirefoxDriver)
    }

    @Test
    def testOnChrome() {
      println("chrome test")
      prepareDemo
      if (!"false".equals(System.getProperty("chrome")))
        testOnWebDriver(new ChromeDriver)
    }

    //    @Test
    def testOnHtmlUnit() {
      println("HtmlUnit test")
      copyHtmlJs
      val driver = new HtmlUnitDriver(BrowserVersion.CHROME)
      driver.setJavascriptEnabled(true)
      testOnWebDriver(driver)
    }

    private def testOnWebDriver(driver: WebDriver) {
      try {
        println("testing web driver " + driver.getClass().getSimpleName())
        driver.get(uri)

        //TODO enable
        //testDateDefaultSet(driver, 11)
        testTextAreaUsed(driver, 4)
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
        //TODO reenable
        //testRepeatWhenMinOccursIsZero(driver, 53)
        //TODO reenable 
        //testChoiceRepeat(driver,58)

        //TODO test boolean restriction to one value means error shown if not that value
        //TODO test minOccurs=0 for missing optional element

        testBooleanMakeVisible(driver, 109)
        testSubmission(driver)
        driver.close
        //now need to ensure that any driver executable (e.g. chromedriver)
        //is stopped whether the tests fail or not.
        driver.quit
        val log = new File("chromedriver.log")
        if (log.exists) log.delete();
      } finally {
        // do nothing 
      }
    }

    private def getInput(driver: WebDriver, itemNo: Int, instanceNos: Instances = Instances(List(1, 1))) = {
      val id = getItemId(itemNo, instanceNos)
      println("getInput: id=" + id)
      driver.findElement(By.id(id));
    }

    private def getError(driver: WebDriver, itemNo: Int, instanceNos: Instances = Instances(List(1, 1))) =
      driver.findElement(By.id(getItemErrorId(itemNo, instanceNos)))

    private def testDateDefaultSet(driver: WebDriver, itemNo: Int) {
      val name = getItemName(itemNo, Instances(List(1, 1)))
      val input = driver.findElement(By.name(name))
      assertEquals("1973-06-12", input.getText)
    }

    private def testTextAreaUsed(driver: WebDriver, itemNo: Int) {
      val input = getInput(driver, itemNo)
      assertEquals("textarea", input.getTagName)
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
      val input = driver.findElement(By.name(getChoiceItemName(itemNo, instanceNos)));
      val option1 = driver.findElement(By.id(getChoiceItemId(itemNo, index = 1, instanceNos)))
      val option2 = driver.findElement(By.id(getChoiceItemId(itemNo, index = 2, instanceNos)))
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
      val button = elementById(driver, getRepeatButtonId(itemNo, instanceNos))
      checkDisplayedById(driver, getRepeatingEnclosingId(itemNo, instanceNos add 1))
      checkNotDisplayedById(driver, getRepeatingEnclosingId(itemNo, instanceNos add 2))
      checkNotDisplayedById(driver, getRepeatingEnclosingId(itemNo, instanceNos add 3))
      button.click;
      checkDisplayedById(driver, getRepeatingEnclosingId(itemNo, instanceNos add 1))
      checkDisplayedById(driver, getRepeatingEnclosingId(itemNo, instanceNos add 2))
      checkNotDisplayedById(driver, getRepeatingEnclosingId(itemNo, instanceNos add 3))
      button.click
      checkDisplayedById(driver, getRepeatingEnclosingId(itemNo, instanceNos add 3))
      checkDisplayedById(driver, getItemId(itemNo, instanceNos add 3))

      //clear validation for integer value
      val input = driver.findElement(By.id(getItemId(itemNo, instanceNos add 1)))
      //put integer in so passes validation on submission
      input.sendKeys("456\n")

      val input1 = driver.findElement(By.id(getItemId(itemNo, instanceNos add 2)))
      val error1 = driver.findElement(By.id(getItemErrorId(itemNo, instanceNos add 2)))
      val input2 = driver.findElement(By.id(getItemId(itemNo, instanceNos add 3)))
      val error2 = driver.findElement(By.id(getItemErrorId(itemNo, instanceNos add 3)))

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
      val input = driver.findElement(By.id(getRepeatingEnclosingId(itemNo, instanceNos add 1)))
      assertFalse(input.isDisplayed)
      val button = driver.findElement(By.id(getRepeatButtonId(itemNo, instanceNos)))
      button.click;
      val input1 = driver.findElement(By.id(getItemId(itemNo, instanceNos add 1)))
      assertTrue(input1.isDisplayed)
      //make sure it validates come submission time
      input1.sendKeys("123\n")
    }

    private def testChoiceRepeat(driver: WebDriver, itemNo: Int) {
      val instanceNos = Instances(List(1))
      val input = driver.findElement(By.id(getRepeatingEnclosingId(itemNo, instanceNos add 1)))
      assertTrue(input.isDisplayed)
      val button = driver.findElement(By.id(getRepeatButtonId(itemNo, instanceNos)))
      button.click;

      //TODO 
      val input1 = driver.findElement(By.id(getItemId(itemNo, instanceNos add 1)))
      assertTrue(input1.isDisplayed)
    }

    private def testBooleanMakeVisible(driver: WebDriver, itemNo: Int) {
      val instanceNos = Instances(List(1))

      //test boolean make visible on true
      val input1 = driver.findElement(By.id(getItemId(itemNo, instanceNos add 1)))
      val input1plus1 = driver.findElement(By.id(getItemId(itemNo + 1, instanceNos add 1)))
      //TODO check that input1 is not selected
      assertFalse(input1plus1.isDisplayed)
      input1.click
      assertTrue(input1plus1.isDisplayed)
      input1.click
      assertFalse(input1plus1.isDisplayed)

      //test boolean make visible on true default true
      val input2 = driver.findElement(By.id(getItemId(itemNo + 2, instanceNos add 1)))
      //TODO check that input2 is selected
      val input2plus1 = driver.findElement(By.id(getItemId(itemNo + 3, instanceNos add 1)))
      assertTrue(input2plus1.isDisplayed)
      input2.click
      assertFalse(input2plus1.isDisplayed)
      input2.click
      assertTrue(input2plus1.isDisplayed)

      //test boolean make visible on false
      val input3 = driver.findElement(By.id(getItemId(itemNo + 4, instanceNos add 1)))
      //TODO check that input3 is not selected
      val input3plus1 = driver.findElement(By.id(getItemId(itemNo + 5, instanceNos add 1)))
      assertTrue(input3plus1.isDisplayed)
      input3.click
      assertFalse(input3plus1.isDisplayed)
      input3.click
      assertTrue(input3plus1.isDisplayed)
    }

    private def checkDisplayedById(driver: WebDriver, id: String) {
      val item = driver.findElement(By.id(id))
      assertTrue(id + " is not visible and should be", item.isDisplayed)
    }

    private def checkNotDisplayedById(driver: WebDriver, id: String) {
      val item = driver.findElement(By.id(id))
      assertFalse(id + " is visible and should not be", item.isDisplayed)
    }

    private def setInput(driver: WebDriver, itemNo: Int, text: String, instances: Instances = Instances(List(1, 1))) {
      val in = getInput(driver, itemNo, instances)
      in.clear
      in.sendKeys(text)
    }

    private def checkErrorDisplayed(driver: WebDriver, itemNo: Int) {
      assertTrue(getError(driver, itemNo).isDisplayed())
    }

    private def testSubmission(driver: WebDriver) {
      val submit = driver.findElement(By.id("submit"))
      val errors = driver.findElement(By.id("validation-errors"))
      assertFalse(errors.isDisplayed());
      submit.click
      assertTrue(errors.isDisplayed());
      checkErrorDisplayed(driver, 10)
      checkErrorDisplayed(driver, 12)
      checkErrorDisplayed(driver, 14)
      checkErrorDisplayed(driver, 19)
      checkErrorDisplayed(driver, 23)
      val xml = driver.findElement(By.id("submit-comments"))
      assertEquals("", xml.getText.trim)
      //fix errors
      setInput(driver, 2, "illegal characters <>")
      setInput(driver, 6, "1")
      setInput(driver, 8, "1")
      setInput(driver, 10, "2013-12-25")
      setInput(driver, 12, "22:45")
      setInput(driver, 14, "2013-12-25T04:45")
      new Select(getInput(driver, 19)).selectByIndex(1)
      //TODO create id using method
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
      setInput(driver, 51, "1901-11-30", Instances(List(1, 1, 1)))

      val id = getChoiceItemId(58, index = 1, Instances(List(1, 1)))
      println("58 id=" + id)
      val elem = driver.findElement(By.id(id))
      elem.click
      elem.click
      assertTrue(elem.isSelected())

      driver.findElement(By.id(Ids.getMinOccursZeroId(89, Instances(List(1))))).click

      submit.click
      submit.click
      //TODO why twice to get chrome to work?

      println("xml=" + xml.getText)
      assertFalse(errors.isDisplayed());
      assertTrue(xml.getText.trim.contains("xmlns"))
      val expectedXml = io.Source.fromInputStream(getClass.getResourceAsStream("/demo-form-expected.xml")).mkString
      assertEquals(expectedXml.trim, xml.getText.trim)
      // fail

      // attempt unmarshal of xml
      validateAgainstSchema(xml.getText, "/demo.xsd")

      //TODO why need to remove namespace?
      val text = xml.getText.replaceAll("xmlns=\".*\"", "")
      //      val main = scalaxb.fromXML[demo.Main](scala.xml.XML.loadString(text))
    }

    private def validateAgainstSchema(xml: String, xsdPath: String) {

      import javax.xml.validation._
      import javax.xml._
      import javax.xml.transform.stream._
      import java.io._

      SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
        .newSchema(new StreamSource(this.getClass.getResourceAsStream(xsdPath)))
        .newValidator
        .validate(new StreamSource(new StringReader(xml)))
    }
  }

}