package com.github.davidmoten.xsdforms

/**
 * Generates html and javascript based web forms on XML Schema (XSD).
 */
object `package` {}

/**
 * Generates html and javascript based on XML schema (XSD).
 */
object Generator {
  import java.io._
  import java.util.UUID
  import java.util.zip._
  import org.apache.commons.io._
  import xsd._
  import tree._

  /**
   *  Using the given xml schema generates html and js files and copies
   *  static js and css into a zipped file that is written to the given
   *  OutputStream. The result is a standalone site for the form.
   *
   * @param schema
   * @param zip
   * @param idPrefix
   * @param rootElement
   */
  def generateZip(
    schema: InputStream,
    zip: OutputStream,
    idPrefix: String = "a-",
    rootElement: Option[String] = None) {

    val zipOut = new ZipOutputStream(zip)

    def action(bytes: Array[Byte], name: String, isDirectory: Boolean) {
      zipOut putNextEntry new ZipEntry(name)
      zipOut write bytes
    }

    copyJsCssAndGeneratedForm(schema, action, idPrefix, rootElement)

    zipOut.close
  }

  /**
   *  Using the given xml schema generates html and js files and copies
   *  static js and css into the given directory. The result is a
   *  standalone site for the form.
   *
   * @param schema
   * @param directory
   * @param idPrefix
   * @param rootElement
   */
  def generateDirectory(
    schema: InputStream,
    directory: File,
    idPrefix: String = "a-",
    rootElement: Option[String] = None) {

    def action(bytes: Array[Byte], name: String, isDirectory: Boolean) {
      import org.apache.commons.io.FileUtils
      val path = directory.getPath + File.separator + name
      val file = new File(path)
      new File(file.getParent).mkdirs
      if (isDirectory)
        file.mkdir
      else {
        val fos = new FileOutputStream(file)
        fos write bytes
        fos.close
      }
    }

    copyJsCssAndGeneratedForm(schema, action, idPrefix, rootElement)
  }

  private def copyJsCssAndGeneratedForm(
    schema: InputStream,
    action: (Array[Byte], String, Boolean) => Unit,
    idPrefix: String = "a-",
    rootElement: Option[String] = None) {
    val text = generateHtmlAsString(schema, idPrefix, rootElement)

    val list = io.Source.fromInputStream(getClass.getResourceAsStream("/file-list.txt"))
    list.getLines.foreach { path =>
      val bytes = IOUtils.toByteArray(getClass.getResourceAsStream("/" + path))
      action(bytes, path, false)
    }

    val name = "form.html"
    action(text.getBytes, name, false)
  }

  /**
   *  Using the given xml schema generates html and js into a file that
   *  is written to the given OutputStream.
   *
   * @param schema
   * @param html
   * @param idPrefix
   * @param rootElement
   */
  def generateHtml(schema: InputStream,
    html: OutputStream,
    idPrefix: String = "a-",
    rootElement: Option[String] = None) {

    val text = generateHtmlAsString(schema, idPrefix, rootElement)
    html write text.getBytes
  }

  /**
   * Returns the text of a file containing generated html and js based on
   * the given schema.
   *
   * @param schema
   * @param idPrefix
   * @param rootElement
   * @return html text including js
   */
  def generateHtmlAsString(schema: InputStream,
    idPrefix: String = "a-",
    rootElement: Option[String] = None): String = {

    import scala.xml._

    val schemaXb = scalaxb.fromXML[Schema](
      XML.load(schema))

    val ns = schemaXb.targetNamespace.get.toString

    val visitor = new TreeCreatingVisitor()

    new SchemaTraversor(schemaXb, rootElement, visitor).traverse
    println("tree:\n" + visitor)

    new FormCreator(Options(ns, Prefix(idPrefix)),
      visitor.configuration,
      visitor.rootNode).text
  }

}
