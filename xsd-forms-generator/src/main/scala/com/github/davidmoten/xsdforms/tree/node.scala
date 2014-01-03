package com.github.davidmoten.xsdforms.tree

import scala.collection.mutable.MutableList
import javax.xml.namespace.QName
import xsd._

//every element is either a sequence, choice or simpleType
// simpleTypes may be based on string, decimal, boolean, date, datetime
// and may be restricted to a regex pattern, have min, max ranges
// or be an enumeration. all elements may have  minOccurs and maxOccurs
//attributes.

case class Sequence(group: ExplicitGroupable)
case class Choice(group: ExplicitGroupable)
case class BaseType(qName: QName)

sealed trait Node {
  val element: ElementWrapper
  def isAnonymous = element.name.isEmpty
}

sealed trait NodeGroup extends Node {
  val children: MutableList[Node] = MutableList()
}

// immutable would be preferrable but should be safe because not changed after tree created
sealed trait NodeBasic extends Node

sealed trait BasicType
case class BasicTypeSimple(typ: SimpleType) extends BasicType
case class BasicTypeBase(typ: BaseType) extends BasicType

//TODO stop using mutable types
case class NodeSequence(element: ElementWrapper,
  override val children: MutableList[Node]) extends NodeGroup
case class NodeChoice(element: ElementWrapper, choice: Choice,
  override val children: MutableList[Node]) extends NodeGroup
case class NodeSimpleType(element: ElementWrapper, typ: SimpleType) extends NodeBasic
case class NodeBaseType(element: ElementWrapper, typ: BaseType) extends NodeBasic
