package com.github.davidmoten.xsdforms {

  import org.junit.Test
  import org.junit.Before
  import xsd.Schema
  import org.junit.runners.BlockJUnit4ClassRunner
  import org.junit.runner.notification.Failure
  import java.io.File
  import java.io.InputStream
  import tree._

  object TstUtil {

    import org.apache.commons.io._

    implicit val idPrefix = Prefix("c-")

    def generate(
      idPrefix: String,
      schemaInputStream: InputStream,
      rootElement: String,
      outputFile: File,
      extraScript: Option[String] = None) {

      //write results to a file
      outputFile.getParentFile().mkdirs
      val fos = new java.io.FileOutputStream(outputFile);
      Generator.generateHtml(schemaInputStream, fos, idPrefix, Some(rootElement))
      fos.close
    }

    def generateDemoForm(file: File) {
      println("generating demo form")
      generate(
        idPrefix = idPrefix.toString,
        schemaInputStream = TstUtil.getClass().getResourceAsStream("/demo.xsd"),
        rootElement = "main",
        outputFile = file)
    }

    def generateDemoForm {
      val file = new File("target/generated-webapp/demo-form.html")
      generateDemoForm(file)
    }

    def copyHtmlJs() {
      val directory = new File("target/generated-webapp")
      FileUtils.deleteDirectory(new File(directory, "css"))
      FileUtils.deleteDirectory(new File(directory, "js"))
      FileUtils.copyDirectory(new File("../xsd-forms-html-js/src/main/resources"), directory)
    }

  }
}