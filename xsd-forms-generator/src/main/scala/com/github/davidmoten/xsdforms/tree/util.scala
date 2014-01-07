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
private[xsdforms] object Util {
  def unexpected(s: String) = throw new RuntimeException(s)
  def unexpected = throw new RuntimeException
}

private[tree] case class XsdDatatype(name: String, pattern: Option[String] = None)
case class XsdElement(name: String)


/**
 * **************************************************************
 *
 *   Instances
 *
 *
 * **************************************************************
 */

private[xsdforms] case class Instances(heirarchy: Seq[Int] = List(), indentsDelta: Int = 0) {
  def add(instance: Int): Instances = Instances(heirarchy :+ instance, indentsDelta)
  def add(instance: Int, suppressIndent: Boolean) =
    Instances(heirarchy :+ instance, indentsDelta - (if (suppressIndent) 1 else 0))
  override def toString = heirarchy.mkString("_")
  def last = heirarchy.last
  def indentCount = heirarchy.size + indentsDelta
}


