package com.github.davidmoten.xsdforms

case class Configuration(header: Option[String], footer: Option[String],
  extraImports: Option[String], extraScript: Option[String], extraCss: Option[String])
  
case class Options(targetNamespace: String, idPrefix: Prefix)

case class Prefix(value: String) {
	override def toString = value
}