package com.github.davidmoten.xsdforms.tree

import scala.collection.mutable.MutableList
import javax.xml.namespace.QName
import xsd._

//every element is either a sequence, choice or simpleType
// simpleTypes may be based on string, decimal, boolean, date, datetime
// and may be restricted to a regex pattern, have min, max ranges
// or be an enumeration. all elements may have  minOccurs and maxOccurs
//attributes.

private[tree] case class Sequence(group: ExplicitGroupable)
private[tree] case class Choice(group: ExplicitGroupable)
private[tree] case class BaseType(qName: QName)

private[tree] sealed trait Node {
  val element: ElementWrapper
  def isAnonymous = element.name.isEmpty
}

private[tree] sealed trait NodeGroup extends Node {
  val children: MutableList[Node] = MutableList()
}

// immutable would be preferrable but should be safe because not changed after tree created
private[tree] sealed trait NodeBasic extends Node

private[tree] sealed trait BasicType
private[tree] case class BasicTypeSimple(typ: SimpleType) extends BasicType
private[tree] case class BasicTypeBase(typ: BaseType) extends BasicType

//TODO stop using mutable types
private[tree] case class NodeSequence(element: ElementWrapper,
  override val children: MutableList[Node]) extends NodeGroup
private[tree] case class NodeChoice(element: ElementWrapper, choice: Choice,
  override val children: MutableList[Node]) extends NodeGroup
private[tree] case class NodeSimpleType(element: ElementWrapper, typ: SimpleType) extends NodeBasic
private[tree] case class NodeBaseType(element: ElementWrapper, typ: BaseType) extends NodeBasic
