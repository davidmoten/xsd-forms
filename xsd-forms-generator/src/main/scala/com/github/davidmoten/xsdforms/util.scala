package com.github.davidmoten.xsdforms {

  import xsd._
  import xsd.ComplexTypeModelSequence1

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
  case class XsdElement(name:String) 
 
  
  /**
   * Utility methods and constants for XML Schemas (XSD).
   */
  object XsdUtil {
    val Xsd = "http://www.w3.org/2001/XMLSchema"
    val AppInfoSchema = "http://moten.david.org/xsd-forms"
    def toQName[T](d: DataRecord[T]) =
      new QName(d.namespace.getOrElse(null), d.key.getOrElse(null))
    def qn(namespaceUri: String, localPart: String) = new QName(namespaceUri, localPart)
    def qn(localPart: String): QName = new QName(Xsd, localPart)
    def qn(datatype: XsdDatatype): QName = qn(Xsd, datatype.name)

    val QnXsdExtension = qn("extension")
    val QnXsdSequence = qn("sequence")
    val QnXsdChoice= qn("choice")
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

  /**
   * **************************************************************
   *
   *   Visitor
   *
   *
   * **************************************************************
   */

  trait Visitor {
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

  //every element is either a sequence, choice or simpleType
  // simpleTypes may be based on string, decimal, boolean, date, datetime
  // and may be restricted to a regex pattern, have min, max ranges
  // or be an enumeration. all elements may have  minOccurs and maxOccurs
  //attributes.

  case class Sequence(group: ExplicitGroupable)
  case class Choice(group: ExplicitGroupable)
  case class BaseType(qName: QName)

  /**
   * **************************************************************
   *
   *   Tree
   *
   *
   * **************************************************************
   */

  import scala.collection.mutable.MutableList

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

  /**
   * **************************************************************
   *
   *   ElementWrapper
   *
   *
   * **************************************************************
   */

  object ElementWrapper {
    implicit def unwrap(wrapped: ElementWrapper): Element = wrapped.element
  }

  case class ElementWrapper(element: Element,
    uniqueId: String = java.util.UUID.randomUUID.toString)

  case class Configuration(header: Option[String], footer: Option[String],
    extraImports: Option[String], extraScript: Option[String], extraCss: Option[String])

  /**
   * **************************************************************
   *
   *   TreeCreatingVisitor
   *
   *
   * **************************************************************
   */

  class TreeCreatingVisitor extends Visitor {

    import Util._

    private var tree: Option[Node] = None
    var configuration: Option[Configuration] = None
    private val stack = new scala.collection.mutable.Stack[Node]
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

  case class Options(targetNamespace:String, idPrefix:String)
  
}