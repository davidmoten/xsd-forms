package com.github.davidmoten.xsdforms.tree

import com.github.davidmoten.xsdforms.Configuration

/**
 * **************************************************************
 *
 *   Visitor
 *
 *
 * **************************************************************
 */

private[xsdforms] trait Visitor {
  import xsd._
    
  def configuration(configuration: Configuration)
  def startSequence(e: Element)
  def endSequence
  def startChoice(e: Element, choice: Choice)
  def startChoiceItem(e: Element, p: ParticleOption, index: Int)
  def endChoiceItem
  def endChoice
  def simpleType(e: Element, typ: SimpleType)
  def baseType(e: Element, typ: BaseType)
}

/**
 * **************************************************************
 *
 *   TreeCreatingVisitor
 *
 *
 * **************************************************************
 */

private[xsdforms] class TreeCreatingVisitor extends Visitor {
  import xsd._
  import Util._

  private var tree: Option[Node] = None
  var configuration: Option[Configuration] = None
  private val stack = new scala.collection.mutable.Stack[Node]

  import scala.collection.mutable.MutableList
  import scala.collection.mutable.HashMap
  
  private val nodes = new HashMap[Element, Node]()

  private implicit def wrap(e: Element): ElementWrapper = ElementWrapper(e)

  override def configuration(config: Configuration) {
    configuration = Some(config)
  }

  override def startSequence(e: Element) {
    val seq = NodeSequence(e, MutableList())
    addChild(seq)
    stack.push(seq)
    nodes.put(e, seq)
  }

  /**
   * If tree is empty then sets tree to existing node. If not empty then adds node
   * to children of top of stack.
   *
   * @param node
   */
  private def addChild(node: Node) {
    if (tree.isEmpty) tree = Some(node);
    else
      stack.top match {
        case g: NodeGroup => g.children += node
        case _ => unexpected
      }
  }

  override def endSequence {
    stack.pop
  }

  override def startChoice(e: Element, choice: Choice) {
    val chc = NodeChoice(e, choice, MutableList())
    addChild(chc)
    stack.push(chc)
    nodes.put(e, chc)
  }

  override def startChoiceItem(e: Element, p: ParticleOption, index: Int) {
    // do nothing
  }

  override def endChoiceItem {
    //do nothing
  }

  override def endChoice {
    stack.pop
  }

  override def simpleType(e: Element, typ: SimpleType) {
    val s = NodeSimpleType(e, typ)
    addChild(s)
    nodes.put(e, s)
  }

  override def baseType(e: Element, typ: BaseType) {
    val s = NodeBaseType(e, typ)
    addChild(s)
  }

  private def toString(node: Node, margin: String): String = {
    node match {
      case NodeBaseType(e, typ) => margin + "NodeBaseType=" + e.name.get
      case NodeSimpleType(e, typ) => margin + "NodeSimpleType=" + e.name.get
      case n: NodeGroup => margin + n.getClass.getSimpleName + "=\n" +
        n.children.map(c => toString(c, margin + "  ")).mkString("\n")
      case _ => unexpected
    }
  }

  override def toString: String = {
    if (tree.isEmpty)
      ""
    else
      toString(tree.get, "")
  }

  def rootNode: Node = tree.get;

}