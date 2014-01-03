package com.github.davidmoten.xsdforms.tree

import xsd._

import javax.xml.namespace.QName

import scalaxb._

/**
 * ***************************************************************
 *
 *   Util
 *
 *
 * **************************************************************
 */

/**
 * Utility methods.
 *
 */
object Util {
  def unexpected(s: String) = throw new RuntimeException(s)
  def unexpected = throw new RuntimeException
}

case class XsdDatatype(name: String, pattern: Option[String] = None)
case class XsdElement(name: String)



case class Configuration(header: Option[String], footer: Option[String],
  extraImports: Option[String], extraScript: Option[String], extraCss: Option[String])



/**
 * **************************************************************
 *
 *   Instances
 *
 *
 * **************************************************************
 */

case class Instances(heirarchy: Seq[Int] = List(), indentsDelta: Int = 0) {
  def add(instance: Int): Instances = Instances(heirarchy :+ instance, indentsDelta)
  def add(instance: Int, suppressIndent: Boolean) =
    Instances(heirarchy :+ instance, indentsDelta - (if (suppressIndent) 1 else 0))
  override def toString = heirarchy.mkString("_")
  def last = heirarchy.last
  def indentCount = heirarchy.size + indentsDelta
}

/**
 * **************************************************************
 *
 *   JS
 *
 *
 * **************************************************************
 */

/**
 * Utility class for building javascript statements.
 */
case class JS() {
  val b = new StringBuffer()

  def line: JS = line("")

  def line(s: String, params: Object*): JS = {
    b append "\n"
    b append String.format(s, params: _*)
    this
  }

  def append(s: String, params: Object*): JS = {
    b append String.format(s, params: _*)
    this
  }

  override def toString = b.toString
}

case class Prefix(value: String) {
  override def toString = value
}
case class Options(targetNamespace: String, idPrefix: Prefix)
