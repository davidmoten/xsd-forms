package com.github.davidmoten.xsdforms.tree

/**
 * Utility methods and constants for XML Schemas (XSD).
 */
private[xsdforms] object XsdUtil {
  
  import scalaxb.DataRecord
  import javax.xml.namespace.QName
  
  val Xsd = "http://www.w3.org/2001/XMLSchema"
  val AppInfoSchema = "http://moten.david.org/xsd-forms"
    
  def toQName[T](d: DataRecord[T]) =
    new QName(d.namespace.getOrElse(null), d.key.getOrElse(null))
  def qn(namespaceUri: String, localPart: String) = new QName(namespaceUri, localPart)
  def qn(localPart: String): QName = new QName(Xsd, localPart)
  def qn(datatype: XsdDatatype): QName = qn(Xsd, datatype.name)

  val QnXsdExtension = qn("extension")
  val QnXsdSequence = qn("sequence")
  val QnXsdChoice = qn("choice")
  val QnXsdAppInfo = qn("appinfo")
  val QnXsdElement = qn("element")

  //TODO use enumeration
  val XsdDateTime = XsdDatatype("dateTime")
  val XsdDate = XsdDatatype("date")
  val XsdTime = XsdDatatype("time")
  val XsdInteger = XsdDatatype("integer", Some("\\d+"))
  val XsdInt = XsdDatatype("int", Some("-?\\d+"))
  val XsdLong = XsdDatatype("long", Some("-?\\d+"))
  val XsdShort = XsdDatatype("short", Some("-?\\d+"))
  val XsdPositiveInteger = XsdDatatype("positiveInteger", Some("[1-9]\\d*"))
  val XsdNegativeInteger = XsdDatatype("negativeInteger", Some("-[1-9]\\d*"))
  
  //TODO allow zero for non-positive integer (adjust regex)
  val XsdNonPositiveInteger = XsdDatatype("nonPositiveInteger", Some("(-\\d+)|0"))
  val XsdNonNegativeInteger = XsdDatatype("nonNegativeInteger", Some("\\d+"))
  val XsdDecimal = XsdDatatype("decimal", Some("-?\\d+(\\.\\d*)?"))
  val XsdBoolean = XsdDatatype("boolean")
  val XsdString = XsdDatatype("string")
  val XsdDouble = XsdDatatype("double", Some("-?\\d(\\.\\d*)?([eE]-?\\d+)?"))
  val XsdFloat = XsdDatatype("float", Some("-?\\d(\\.\\d*)?([eE]-?\\d+)?"))
  val XsdAttribute = XsdDatatype("attribute", None)
  val XsdAnnotation = XsdDatatype("annotation", None)
}