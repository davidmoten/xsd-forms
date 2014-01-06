package com.github.davidmoten.util

/**
 * Utility class for building javascript statements.
 */
private[davidmoten] case class JS() {
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