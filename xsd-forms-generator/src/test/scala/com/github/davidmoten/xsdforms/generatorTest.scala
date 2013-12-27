package com.github.davidmoten.xsdforms {

  import org.junit.Test
  import org.junit.Before
  import xsd.Schema
  import org.junit.runners.BlockJUnit4ClassRunner
  import org.junit.runner.notification.Failure
  import java.io.File
  import java.io.InputStream
  import tree._

  @Test
  class GeneratorTest {

    import org.apache.commons.io._

    import TstUtil._

    import java.io._

    import org.junit.Assert._

    @Before
    def testSetupWebapp() {
      copyHtmlJs
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
    def generatePolrepForm {
      println("generating polrep form")
      generate(
        idPrefix = "b-",
        schemaInputStream = TstUtil.getClass().getResourceAsStream("/polrep.xsd"),
        rootElement = "polrep",
        outputFile = new File("target/generated-webapp/polrep-form.html"))
    }

    @Test
    def generateTheAnnotationsDemoForm {
      println("generating the annotations demo form")
      generate(
        idPrefix = "b-",
        schemaInputStream = TstUtil.getClass().getResourceAsStream("/annotations-demo.xsd"),
        rootElement = "person",
        outputFile = new File("target/generated-webapp/annotations-demo.html"))
    }

    @Test
    def generateTheDemoForm {
      println("generating the demo form")
      generate(
        idPrefix = "b-",
        schemaInputStream = TstUtil.getClass().getResourceAsStream("/demo.xsd"),
        rootElement = "main",
        outputFile = new File("target/generated-webapp/demo-form.html"))
    }

    @Test
    def testGenerateZip() {
      val out = new File("target/out.zip")
      new FileOutputStream(out)
      Generator.generateZip(getClass.getResourceAsStream("/demo.xsd"), new FileOutputStream(out))
      assertTrue(out.exists)

      import java.util.zip._
      val zipFile = new ZipFile(out)

      import scala.collection.JavaConversions._
      val names = enumerationAsScalaIterator(zipFile.entries()).map(_.getName).toSet
      println(names.mkString("\n"))
      assertTrue(names.contains("form.html"))
      assertTrue(names.contains("css/xsd-forms-style.css"))
      assertTrue(names.contains("js/xsd-forms-override.js"))

      Option.empty
    }

    @Test
    def testGenerateDirectory() {
      val out = new File("target/out.zip")
      val directory = new File("target/testGenerateDirectory")
      Generator.generateDirectory(getClass.getResourceAsStream("/demo.xsd"), directory)
      assertTrue(directory.exists)
      Option.empty
    }

  }

}
