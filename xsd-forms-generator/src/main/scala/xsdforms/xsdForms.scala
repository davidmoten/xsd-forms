package xsdforms {

  import xsd._
  import xsd.ComplexTypeModelSequence1
  import javax.xml.namespace.QName
  import scalaxb._

  /**
   * **************************************************************
   *
   *   Util
   *
   *
   * **************************************************************
   */

  object Util {
    def unexpected(s: String) = throw new RuntimeException(s)
    def unexpected() = throw new RuntimeException()
  }

  object XsdUtil {
    val Xsd = "http://www.w3.org/2001/XMLSchema"
    val AppInfoSchema = "http://moten.david.org/xsd-forms"
    def qn(namespaceUri: String, localPart: String) = new QName(namespaceUri, localPart)
    def qn(localPart: String): QName = new QName(Xsd, localPart)
    val XsdDateTime = "dateTime"
    val XsdDate = "date"
    val XsdTime = "time"
    val XsdInteger = "integer"
    val XsdDecimal = "decimal"
    val XsdBoolean = "boolean"
    val XsdString = "string"
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
  case class BaseType(qName: QName);

  /**
   * **************************************************************
   *
   *   Tree
   *
   *
   * **************************************************************
   */

  import scala.collection.mutable.MutableList

  trait Node {
    val element: ElementWrapper
  }
  trait NodeGroup extends Node {
    val children: MutableList[Node] = MutableList()
  }

  // immutable would be preferrable but should be safe because not changed after tree created
  case class NodeSequence(element: ElementWrapper, override val children: MutableList[Node]) extends NodeGroup
  case class NodeChoice(element: ElementWrapper, choice: Choice, override val children: MutableList[Node]) extends NodeGroup
  trait NodeBasic extends Node
  case class NodeSimpleType(element: ElementWrapper, typ: SimpleType) extends NodeBasic
  case class NodeBaseType(element: ElementWrapper, typ: BaseType) extends NodeBasic

  /**
   * **************************************************************
   *
   *   TreeCreatingVisitor
   *
   *
   * **************************************************************
   */

  object ElementWrapper {
    implicit def unwrap(wrapped: ElementWrapper): Element = wrapped.element
  }

  case class ElementWrapper(element: Element, uniqueId: String)

  class TreeCreatingVisitor extends Visitor {

    import Util._
    import java.util.UUID
    private var tree: Option[Node] = None
    private val stack = new scala.collection.mutable.Stack[Node]

    private implicit def wrap(e: Element): ElementWrapper = ElementWrapper(e, UUID.randomUUID.toString)

    override def startSequence(e: Element) {
      val seq = NodeSequence(e, MutableList())
      addChild(seq)
      stack.push(seq)
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
      addChild(chc);
      stack.push(chc);
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

  case class Instances(heirarchy: Seq[Int] = List()) {
    def add(instance: Int): Instances = Instances(heirarchy :+ instance)
    override def toString = heirarchy.mkString("_")
    def last = heirarchy.last
    def size = heirarchy.size
  }

  case class XsdFormsAnnotation(name: String)

  /**
   * **************************************************************
   *
   *   TreeToHtmlConverter object
   *
   *
   * **************************************************************
   */

  object TreeToHtmlConverter {
    val InstanceDelimiter = "-instance-"
    val ChoiceIndexDelimiter = "-choice-"

    def getItemId(idPrefix: String, number: String, instances: Instances) =
      idPrefix + "item-" + number + InstanceDelimiter + instances

    def getItemErrorId(idPrefix: String, number: String, instances: Instances) =
      idPrefix + "item-error-" + number + InstanceDelimiter + instances

    def getChoiceItemId(idPrefix: String, number: String, index: Int, instances: Instances): String =
      getItemId(idPrefix, number, instances) + ChoiceIndexDelimiter + index

    def getChoiceItemName(idPrefix: String, number: String, instances: Instances) =
      idPrefix + "item-input-" + number + InstanceDelimiter + instances

    def getRepeatButtonId(idPrefix: String, number: String, instances: Instances) =
      idPrefix + "repeat-button-" + number + InstanceDelimiter + instances

    def getRepeatingEnclosingId(idPrefix: String, number: String, instances: Instances): String =
      idPrefix + "repeating-enclosing-" + number + InstanceDelimiter + instances

    val ClassInvisible = "invisible"
    val ClassSequence = "sequence"
    val ClassSequenceLabel = "sequence-label"
    val ClassSequenceContent = "sequence-content"
    val ClassFieldset = "fieldset"
    val ClassChoiceLabel = "choice-label"
    val ClassDivChoiceItem = "div-choice-item"
    val ClassItemNumber = "item-number"
    val ClassItemTitle = "item-title"
    val ClassItemLabel = "item-label"
    val ClassItemInput = "item-input"
    val ClassItemHelp = "item-help"
    val ClassItemBefore = "item-before"
    val ClassItemAfter = "item-after"
    val ClassItemPath = "item-path"
    val ClassItemEnclosing = "item-enclosing"
    val ClassItemError = "item-error"
    val ClassChoiceItem = "choice-item"
    val ClassNonRepeatingTitle = "non-repeating-title"
    val ClassRepeatButton = "repeat-button"
    val ClassRepeatingEnclosing = "repeating-enclosing"
    val ClassItemInputTextarea = "item-input-textarea"
    val ClassItemInputText = "item-input-text"
    val ClassSelect = "select"
    val ClassChoice = "choice"
    val ClassWhite = "white"
    val ClassSmall = "small"
    val ClassItemDescription = "item-description"

  }

  object Annotation {
    //TODO document each of the annotations in scaladoc
    val Label = XsdFormsAnnotation("label")
    val Choice = XsdFormsAnnotation("choice")
    val ChoiceLabel = XsdFormsAnnotation("choiceLabel")
    val Legend = XsdFormsAnnotation("legend")
    val RepeatLabel = XsdFormsAnnotation("repeatLabel")
    val Title = XsdFormsAnnotation("title")
    val Before = XsdFormsAnnotation("before")
    val After = XsdFormsAnnotation("after")
    val Text = XsdFormsAnnotation("text")
    val Width = XsdFormsAnnotation("width")
    val Selector = XsdFormsAnnotation("selector")
    val AddBlank = XsdFormsAnnotation("addBlank")
    val Css = XsdFormsAnnotation("css")
    val Validation = XsdFormsAnnotation("validation")
    val Help = XsdFormsAnnotation("help")
    val MakeVisible = XsdFormsAnnotation("makeVisible")
    val NonRepeatingTitle = XsdFormsAnnotation("nonRepeatingTitle")
    val Description = XsdFormsAnnotation("description")
    val Visible = XsdFormsAnnotation("visible")
  }

  case class JS() {
    val b = new StringBuffer()

    def lines(s: String): JS = {
      b append s
      this
    }

    def line: JS = line("")

    def line(s: String, params: Object*): JS = {
      b append "\n"
      b append String.format(s, params: _*)
      this
    }

    override def toString = b.toString
  }

  /**
   * **************************************************************
   *
   *   TreeToHtmlConverter class
   *
   *
   * **************************************************************
   */

  class TreeToHtmlConverter(targetNamespace: String, idPrefix: String, extraScript: Option[String], tree: Node) {
    import TreeToHtmlConverter._
    import XsdUtil._
    import Util._
    private val script = new StringBuilder

    private var number = 0
    val margin = "  "
    private val Plus = " + "

    private sealed trait Entry
    private sealed trait StackEntry
    private val html = new Html

    private val NumInstancesForMultiple = 3

    import scala.collection.mutable.HashMap
    private val elementNumbers = new HashMap[ElementWrapper, String]()

    //assign element numbers so that order of display on page 
    //will match order of element numbers. To do this must 
    //traverse children before siblings
    assignElementNumbers(tree)

    //process the abstract syntax tree
    doNode(tree, new Instances)

    addXmlExtractScriptlet(tree, new Instances)

    /**
     * Traverse children before siblings to provide element
     * numbers matching page display order.
     * @param node
     */
    private def assignElementNumbers(node: Node) {
      elementNumber(node)
      node match {
        case n: NodeGroup => n.children.foreach { assignElementNumbers(_) }
        case _ => ;
      }
    }

    private def doNode(node: Node, instances: Instances) {
      node match {
        case n: NodeSimpleType => doNode(n, instances)
        case n: NodeBaseType => doNode(n, instances)
        case n: NodeSequence => doNode(n, instances)
        case n: NodeChoice => doNode(n, instances)
        case _ => Util.unexpected
      }
    }

    private def addXmlExtractScriptlet(node: Node, instances: Instances) {
      node match {
        case n: NodeSimpleType => addXmlExtractScriptlet(n, instances)
        case n: NodeBaseType => addXmlExtractScriptlet(n, instances)
        case n: NodeSequence => addXmlExtractScriptlet(n, instances)
        case n: NodeChoice => addXmlExtractScriptlet(n, instances)
        case _ => Util.unexpected
      }
    }

    private def doNode(node: NodeSequence, instances: Instances) {
      val e = node.element
      val number = elementNumber(node)
      val legend = getAnnotation(e, Annotation.Legend)
      val usesFieldset = legend.isDefined

      val label = getAnnotation(e, Annotation.Label).mkString

      html
        .div(classes = List(ClassSequence))
      nonRepeatingTitle(e, e.minOccurs.intValue() == 0 || e.maxOccurs != "1", instances)
      for (instanceNo <- repeats(e)) {
        val instNos = instances add instanceNo
        repeatingEnclosing(e, instNos)
        html.div(classes = List(ClassSequenceLabel), content = Some(label))
          .closeTag
          .div(id = Some(idPrefix + "sequence-" + number + InstanceDelimiter + instanceNo),
            classes = List(ClassSequenceContent))
        if (usesFieldset)
          html.fieldset(legend = legend, classes = List(ClassFieldset), id = Some(idPrefix + "fieldset-" + number + InstanceDelimiter + instanceNo))

        doNodes(node.children, instNos)

        if (usesFieldset)
          html.closeTag
        html.closeTag
      }
      html
        .closeTag(2)
      addMaxOccursScriptlet(e, instances)

    }

    private def doNode(node: NodeChoice, instances: Instances) {
      val choice = node.choice
      val e = node.element
      val choiceInline = displayChoiceInline(choice)

      val number = elementNumber(e)

      html.div(id = Some(getItemEnclosingId(number, instances add 1)), classes = List(ClassChoice))
      nonRepeatingTitle(e, e.minOccurs.intValue() == 0 || e.maxOccurs != "1", instances)
      for (instanceNo <- repeats(e)) {
        val instNos = instances add instanceNo
        repeatingEnclosing(e, instNos)
        val particles = choice.group.particleOption3.map(_.value)
        addChoiceHideOnStartScriptlet(particles, number, instNos)
        addChoiceShowHideOnSelectionScriptlet(particles, number, instNos)

        html.div(
          classes = List(ClassChoiceLabel),
          content = Some(getAnnotation(choice.group, Annotation.Label).mkString))
          .closeTag

        val forEachParticle = particles.zipWithIndex.foreach _

        forEachParticle(x => {
          val particle = x._1
          val index = x._2 + 1
          html.div(
            id = Some(idPrefix + "div-choice-item-" + number + InstanceDelimiter + instanceNo + ChoiceIndexDelimiter + index),
            classes = List(ClassDivChoiceItem))
          html.input(
            id = Some(getChoiceItemId(number, index, instNos)),
            name = getChoiceItemName(number, instNos),
            classes = List(ClassChoiceItem),
            typ = Some("radio"),
            value = Some("number"),
            content = Some(getChoiceLabel(e, particle)),
            number = Some(number))
          html.closeTag(2)
        })

        node.children.zipWithIndex.foreach {
          case (n, index) => {
            html.div(id = Some(choiceContentId(idPrefix, number, (index + 1), instNos)), classes = List(ClassInvisible))
            doNode(n, instNos)
            html.closeTag
          }
        }

        html.closeTag
      }
      html.closeTag

      addMaxOccursScriptlet(e, instances)

    }

    private def doNode(node: NodeSimpleType, instances: Instances) {
      val e = node.element
      val typ = node.typ

      nonRepeatingSimpleType(e, instances)
      val t = Some(typ)
      val number = elementNumber(e)
      for (instanceNo <- repeats(e)) {
        val instNos = instances add instanceNo
        repeatingEnclosing(e, instNos)
        itemTitle(e)
        itemBefore(e)
        html.div(classes = List(ClassItemNumber), content = Some(number)).closeTag
          .label(forInputName = getItemName(number, instNos),
            classes = List(ClassItemLabel), content = Some(getLabel(e, t))).closeTag
          .div(classes = List(ClassItemInput))

        simpleType(node, instNos)

        html
          .closeTag(3)
      }
      addMaxOccursScriptlet(e, instances)
    }

    private def doNode(node: NodeBaseType, instances: Instances) {
      val e = node.element
      val typ = node.typ
      nonRepeatingSimpleType(e, instances)
      val t = None
      val number = elementNumber(e)
      for (instanceNo <- repeats(e)) {
        val instNos = instances add instanceNo
        repeatingEnclosing(e, instNos)
        itemTitle(e)
        itemBefore(e)
        html.div(classes = List(ClassItemNumber), content = Some(number)).closeTag
          .label(forInputName = getItemName(number, instNos),
            classes = List(ClassItemLabel), content = Some(getLabel(e, t))).closeTag
          .div(classes = List(ClassItemInput))
        simpleType(node, instNos)
        html
          .closeTag(2)
      }
      html.closeTag
      addMaxOccursScriptlet(e, instances)
    }

    private def doNodes(nodes: MutableList[Node], instances: Instances) {
      nodes.foreach(doNode(_, instances))
    }

    private def addXmlExtractScriptlet(node: NodeSequence, instances: Instances) {
      {
        val number = elementNumber(node)
        val js = JS()
          .line("    var xml = %s%s;", spaces(instances add 1), xmlStart(node))
          .line("    //now add sequence children for each instanceNo")

        for (instanceNo <- repeats(node)) {
          val instNos = instances add instanceNo
          node.children.foreach { n =>
            js.line("    if (idVisible('%s'))", getRepeatingEnclosingId(number, instNos))
            js.line("    xml += %s();", xmlFunctionName(n, instNos))
            addXmlExtractScriptlet(n, instNos)
          }
        }
        js.line("    xml += %s%s;", spaces(instances add 1), xmlEnd(node))
        js.line("    return xml;")

        addXmlExtractScriptlet(node, js.toString, instances);
      }
    }

    private def addXmlExtractScriptlet(node: NodeChoice, instances: Instances) {

      val js = JS()
        .line("    var xml = %s%s;", spaces(instances add 1), xmlStart(node))
        .line("    //now optionally add selected child if any")
      for (instanceNo <- repeats(node)) {
        val instNos = instances add instanceNo
        js.line("    var checked = $(':input[name=%s]:checked').attr('id');", getChoiceItemName(node, instNos))

        node.children.zipWithIndex.foreach {
          case (n, index) =>
            js.line("    if (checked == \"%s\")", getChoiceItemId(node, index + 1, instNos))
              .line("    xml += %s();", xmlFunctionName(n, instNos))
            addXmlExtractScriptlet(n, instNos)
        }
        js.line("    xml += %s%s;", spaces(instances add 1), xmlEnd(node))
          .line("    return xml;")
        addXmlExtractScriptlet(node, js.toString(), instances);
      }
    }

    private def addXmlExtractScriptlet(node: NodeSimpleType, instances: Instances) {
      addXmlExtractScriptletForSimpleOrBase(node, instances)
    }

    private def addXmlExtractScriptlet(node: NodeBaseType, instances: Instances) {
      addXmlExtractScriptletForSimpleOrBase(node, instances)
    }

    private def addXmlExtractScriptletForSimpleOrBase(node: NodeBasic, instances: Instances) {
      val number = elementNumber(node)
      val js = JS().line("  var xml=\"\";")
      for (instanceNo <- repeats(node)) {
        val instNos = instances add instanceNo
        js.line("  if (idVisible('%s'))", getRepeatingEnclosingId(number, instNos))
        val valueById =
          if (isRadio(node.element)) "$('input[name=" + getItemName(number, instNos) + "]:radio:checked').val()"
          else valById(getItemId(node, instNos))
        js.line("    xml+= %s%s;", spaces(instNos), xml(node, transformToXmlValue(node, valueById)))
      }
      js.line("   return xml;")
      addXmlExtractScriptlet(node, js.toString, instances);
    }

    private def transformToXmlValue(node: NodeBasic, value: String): String =
      node match {
        case n: NodeSimpleType => transformToXmlValue(restriction(n), value)
        case n: NodeBaseType => transformToXmlValue(restriction(n), value)
      }

    private def transformToXmlValue(r: Restriction, value: String): String = {
      val qn = toQN(r)
      qn match {
        case QN(xs, XsdDate) => "toXmlDate(" + value + ")"
        case QN(xs, XsdDateTime) => "toXmlDateTime(" + value + ")"
        case QN(xs, XsdTime) => "toXmlTime(" + value + ")"
        case QN(xs, XsdBoolean) => "toBoolean(" + value + ")"
        case _ => value
      }
    }

    private def xmlFunctionName(node: Node, instances: Instances) = {
      val number = elementNumber(node.element)
      "getXml" + number + "instance" + instances
    }

    private def addXmlExtractScriptlet(node: Node, functionBody: String, instances: Instances) {
      val functionName = xmlFunctionName(node, instances)
      addScript(
        JS().line("//extract xml from element <%s>", node.element.name.getOrElse("?"))
          .line("function %s() {", functionName)
          .line(functionBody)
          .line("}")
          .line("").toString)
    }

    private def refById(id: String) = "$(\"#" + id + "\")"
    private def valById(id: String) = "encodedValueById(\"" + id + "\")"
    private def namespace(node: Node) =
      if (elementNumber(node.element).equals("1"))
        " xmlns=\"" + targetNamespace + "\""
      else
        ""
    private def xmlStart(node: Node) =
      "'<" + node.element.name.getOrElse("?") + namespace(node) + ">'"

    private def xmlEnd(node: Node) =
      "'</" + node.element.name.getOrElse("?") + ">'"

    private def xml(node: Node, value: String) =
      xmlStart(node) + Plus + value + Plus + xmlEnd(node)

    private def spaces(instances: Instances) =
      if (instances.size == 0) "'\\n' + "
      else "'\\n' + spaces(" + ((instances.size - 1) * 2) + ") + "

    override def toString = text

    private def addScript(s: String) {
      script.append(s)
      script.append("\n")
    }

    private def addScript(js: JS) {
      addScript(js.toString)
    }

    private def displayChoiceInline(choice: Choice) =
      "inline" == getAnnotation(choice.group, Annotation.Choice).mkString

    private def addChoiceHideOnStartScriptlet(
      particles: Seq[ParticleOption], number: String, instances: Instances) {

      val forEachParticle = particles.zipWithIndex.foreach _

      forEachParticle(x => {
        val index = x._2 + 1
        addScript(
          JS().line("$('#%s').hide();", choiceContentId(idPrefix, number, index, instances)))
      })
    }

    private def addChoiceShowHideOnSelectionScriptlet(
      particles: Seq[ParticleOption], number: String, instances: Instances) {

      val forEachParticle = particles.zipWithIndex.foreach _

      val choiceChangeFunction = "choiceChange" + number + "instance" + instances;

      val js = JS()
      js.line("var %s = function addChoiceChange%sinstance%s() {", choiceChangeFunction, number, instances)
        .line("  $(':input[@name=%s]').change(function() {", getChoiceItemName(number, instances))
        .line("    var checked = $(':input[name=%s]:checked').attr('id');", getChoiceItemName(number, instances))

      forEachParticle(x => {
        val index = x._2 + 1
        val ccId =
          choiceContentId(idPrefix, number, index, instances)
        js.line("    if (checked == '%s') {", getChoiceItemId(number, index, instances))
          .line("      $('#%s').show();", ccId)
          .line("      $('#%s').find('.item-path').attr('enabled','true');", ccId)
          .line("    }")
          .line("    else {")
          .line("      $('#%s').hide();", ccId)
          .line("      $('#%s').find('.item-path').attr('enabled','false');", ccId)
          .line("    }")

      })
      js.line("  });")
        .line("}")
        .line
        .line("%s();", choiceChangeFunction)

      addScript(js)

    }

    private def getChoiceLabel(e: Element, p: ParticleOption): String = {
      val labels =
        p match {
          case x: Element => {
            getAnnotation(x, Annotation.ChoiceLabel) ++ getAnnotation(x, Annotation.Label) ++ Some(getLabel(x, None))
          }
          case _ => unexpected
        }
      labels.head
    }

    private class MyRestriction(qName: QName)
      extends Restriction(None, SimpleRestrictionModelSequence(), None, Some(qName), Map())

    private def getVisibility(e: Element) =
      getAnnotation(e, Annotation.Visible) match {
        case Some("false") => Some(ClassInvisible)
        case _ => None
      }

    private def nonRepeatingTitle(e: ElementWrapper, hasButton: Boolean, instances: Instances) {
      //there's only one of these so use instanceNo = 1
      val number = elementNumber(e)
      html.div(
        classes = List(ClassNonRepeatingTitle),
        content = getAnnotation(e, Annotation.NonRepeatingTitle)).closeTag
      if (hasButton)
        html.div(
          id = Some(getRepeatButtonId(number, instances)),
          classes = List(ClassRepeatButton, ClassWhite, ClassSmall),
          content = Some(getAnnotation(e, Annotation.RepeatLabel).getOrElse("+"))).closeTag
    }

    private def repeatingEnclosing(e: ElementWrapper, instances: Instances) {
      val number = elementNumber(e)
      val id = getRepeatingEnclosingId(number, instances)
      html.div(
        id = Some(id),
        classes = List(ClassRepeatingEnclosing))
      if (instances.last != 1 || e.minOccurs == 0)
        addScript(JS().line("$('#%s').hide();", id))
    }

    private def nonRepeatingSimpleType(e: ElementWrapper, instances: Instances) {
      val number = elementNumber(e)
      html
        .div(
          classes = List(ClassItemEnclosing) ++ getVisibility(e),
          id = Some(getItemEnclosingId(number, instances add 1)))
      nonRepeatingTitle(e, e.maxOccurs != "0" && e.maxOccurs != "1", instances)
    }

    private def itemTitle(e: Element) {
      getAnnotation(e, Annotation.Title) match {
        case Some(x) => html.div(classes = List(ClassItemTitle), content = Some(x)).closeTag
        case _ =>
      }
    }

    private def itemBefore(e: Element) {
      getAnnotation(e, Annotation.Before) match {
        case Some(x) => html.div(classes = List(ClassItemBefore), content = Some(x)).closeTag
        case _ =>
      }
    }

    private def elementNumber(node: Node): String = elementNumber(node.element)

    private def elementNumber(e: ElementWrapper): String = {
      val n = elementNumbers.get(e);
      if (n.isDefined)
        n.get
      else {
        val newNumber = nextNumber
        elementNumbers(e) = newNumber
        newNumber
      }
    }

    private def getTextType(e: Element) =
      getAnnotation(e, Annotation.Text)

    private def simpleType(node: NodeBasic, instances: Instances) {
      val e = node.element

      val r = restriction(node)

      val qn = toQN(r)

      addInput(e, qn, r, instances)

      addDescription(e)

      addPath(e, instances)

      addError(e, instances)

      addHelp(e)

      addAfter(e)

      val statements = List(
        createDeclarationScriptlet(e, qn, instances),
        createMandatoryTestScriptlet(node),
        createPatternsTestScriptlet(getPatterns(node)),
        createEnumerationTestScriptlet(node, instances),
        createBasePatternTestScriptlet(qn),
        createFacetTestScriptlet(r),
        createLengthTestScriptlet(r),
        createCanExcludeScriptlet(e),
        createClosingScriptlet(e, qn, instances))

      statements
        .map(stripMargin(_))
        .foreach(x => if (x.length > 0) addScript(x))

    }

    private def addInput(e: ElementWrapper, qn: QN, r: Restriction, instances: Instances) {

      val number = elementNumber(e)

      if (isEnumeration(r))
        addEnumeration(e, r, instances)
      else
        addTextField(e, r, getExtraClasses(qn), instances)

      addWidthScript(e, instances)

      addCssScript(e, instances)
    }

    private def getExtraClasses(qn: QN) = qn match {
      case QN(xs, XsdDate) => "datepickerclass "
      case QN(xs, XsdDateTime) => "datetimepickerclass "
      case QN(xs, XsdTime) => "timepickerclass "
      case _ => ""
    }

    private def addTextField(
      e: ElementWrapper, r: Restriction,
      extraClasses: String, instances: Instances) {
      val number = elementNumber(e)
      val inputType = getInputType(r)
      val itemId = getItemId(number, instances)
      getTextType(e) match {
        case Some("textarea") =>
          html.textarea(
            id = Some(itemId),
            name = getItemName(number, instances),
            classes = List(extraClasses, ClassItemInputTextarea),
            content = Some(e.default.mkString),
            number = Some(number))
            .closeTag
        case _ =>
          //text or boolean
          val checked = e.default match {
            case Some("true") => Some(true)
            case _ => None
          }
          val v = defaultValue(e.default, r)
          html.input(
            id = Some(itemId),
            name = getItemName(number, instances),
            classes = List(extraClasses, ClassItemInputText),
            typ = Some(inputType),
            checked = checked,
            value = v,
            number = Some(number))
            .closeTag
      }
    }

    private def defaultValue(value: Option[String], r: Restriction): Option[String] = {
      value match {
        case Some(v) => {
          toQN(r) match {
            // drop the seconds off the time so js timepicker is happy
            case QN(xs, XsdTime) => Some(v.substring(0, 5))
            case QN(xs, XsdDateTime) => Some(v.substring(0, 16))
            case _ => value
          }
        }
        case None => value
      }
    }

    private def addWidthScript(e: ElementWrapper, instances: Instances) {
      val itemId = getItemId(e, instances)
      getAnnotation(e, Annotation.Width) match {
        case Some(x) =>
          addScript(JS().line("  $('#%s').width('%s');", itemId, x).toString)
        case None =>
      }
    }

    private def addCssScript(e: ElementWrapper, instances: Instances) {
      val itemId = getItemId(e, instances)
      getAnnotation(e, Annotation.Css) match {
        case Some(x) => {
          val items = x.split(';')
            .foreach(
              y => {
                val pair = y.split(':')
                if (pair.size != 2)
                  unexpected("css properties incorrect syntax\n" + pair)
                addScript(JS().line("  $('#%s').css('%s','%s');", itemId, pair(0), pair(1)))
              })
        }
        case None =>
      }
    }

    private def isEnumeration(r: Restriction) =
      !getEnumeration(r).isEmpty

    private def addEnumeration(e: ElementWrapper, r: Restriction, instances: Instances) {
      val number = elementNumber(e)
      val en = getEnumeration(r)

      val initializeBlank = getAnnotation(e, Annotation.AddBlank) match {
        case Some("true") => true
        case _ => false
      }
      enumeration(en, number, isRadio(e), initializeBlank, instances)
    }

    private def isRadio(e: ElementWrapper) =
      //TODO add check is enumeration as well  
      getAnnotation(e, Annotation.Selector) match {
        case Some("radio") => true
        case _ => false
      }

    private def getEnumeration(r: Restriction): Seq[(String, NoFixedFacet)] =
      r.simpleRestrictionModelSequence3.facetsOption2.seq.map(
        _.value match {
          case y: NoFixedFacet => {
            val label = getAnnotation(y, Annotation.Label) match {
              case Some(x) => x
              case None => y.valueAttribute
            }
            Some((label, y))
          }
          case _ => None
        }).flatten

    private def enumeration(en: Seq[(String, NoFixedFacet)],
      number: String, isRadio: Boolean, initializeBlank: Boolean, instances: Instances) {
      if (isRadio) {
        en.zipWithIndex.foreach(x => {
          html.input(
            id = Some(getItemId(number, x._2, instances)),
            name = getItemName(number, instances),
            classes = List(ClassSelect),
            typ = Some("radio"),
            value = Some(x._1._2.valueAttribute),
            content = Some(x._1._1),
            number = Some(number)).closeTag
        })
      } else {
        html.select(
          id = Some(getItemId(number, instances)),
          name = getItemName(number, instances),
          classes = List(ClassSelect),
          number = Some(number))
        if (initializeBlank)
          html.option(content = Some("Select one..."), value = "").closeTag
        en.foreach { x =>
          html.option(content = Some(x._1), value = x._2.valueAttribute).closeTag
          getAnnotation(x._2, Annotation.MakeVisible) match {
            case Some(y: String) => {
              val refersTo = number.toInt + y.toInt
              val js = JS().line("$('#%s').change( function() {", getItemId(number, instances))
                .line("  var v = $('#%s');", getItemId(number, instances))
                .line("  var refersTo = $('#%s');", getItemEnclosingId(refersTo + "", instances))
                .line("  if ('%s' == v.val())", x._2.valueAttribute)
                .line("    refersTo.show();")
                .line("  else")
                .line("    refersTo.hide();")
                .line("})")
                .line
              addScript(js)
            }
            case _ =>
          }
        }
        html.closeTag
      }
    }

    private def addDescription(e: Element) {
      getAnnotation(e, Annotation.Description) match {
        case Some(x) =>
          html.div(
            classes = List(ClassItemDescription),
            content = Some(x))
            .closeTag
        case None =>
      }
    }

    private def addError(e: ElementWrapper, instances: Instances) {
      val itemErrorId = getItemErrorId(elementNumber(e), instances)
      html.div(
        id = Some(itemErrorId),
        classes = List(ClassItemError),
        content = Some(getAnnotation(e, Annotation.Validation).getOrElse("Invalid")))
        .closeTag

    }

    private def addPath(e: ElementWrapper, instances: Instances) {
      html.div(
        classes = List("item-path"),
        id = Some(getPathId(elementNumber(e), instances)),
        enabledAttr = Some("true"),
        content = Some(""))
        .closeTag
    }

    private def addHelp(e: Element) {
      getAnnotation(e, Annotation.Help) match {
        case Some(x) =>
          html.div(classes = List(ClassItemHelp), content = Some(x)).closeTag
        case None =>
      }
    }

    private def addAfter(e: Element) {
      getAnnotation(e, Annotation.After) match {
        case Some(x) =>
          html.div(classes = List(ClassItemAfter), content = Some(x)).closeTag
        case None =>
      }
    }

    private def getBasePattern(qn: QN) = {
      qn match {
        case QN(xs, XsdDecimal) => Some("\\d+(\\.\\d*)?")
        case QN(xs, XsdInteger) => Some("\\d+")
        case _ => None
      }
    }

    private def createDeclarationScriptlet(e: ElementWrapper, qn: QN, instances: Instances) = {
      val number = elementNumber(e)
      val itemId = getItemId(number, instances)
      JS()
        .line("// %s", e.name.get)
        .line("var validate%sinstance%s = function () {", number, instances)
        .line("  var ok = true;")
        .line("  var v = $('#%s');", itemId)
        .line("  var pathDiv = $('#%s');", getPathId(number, instances))
        .toString
    }

    private def createMandatoryTestScriptlet(node: NodeBasic) = {
      if (isMandatory(node.element, restriction(node)))
        JS().line("  // mandatory test")
          .line("  if ((v.val() == null) || (v.val().length==0))")
          .line("    ok=false;")
          .toString
      else ""
    }

    private def createLengthTestScriptlet(r: Restriction) = {
      r.simpleRestrictionModelSequence3.facetsOption2.seq.flatMap(f => {
        val start = """
|  //length test
|  if (v.val().length """
        val finish = """)
|    ok = false;"""
        f match {
          case DataRecord(xs, Some("minLength"), x: NumFacet) =>
            Some(start + "<" + x.valueAttribute + finish)
          case DataRecord(xs, Some("maxLength"), x: NumFacet) =>
            Some(start + ">" + x.valueAttribute + finish)
          case DataRecord(xs, Some("length"), x: NumFacet) =>
            Some(start + "!=" + x.valueAttribute + finish)
          case _ => None
        }
      }).mkString("")
    }

    private def createCanExcludeScriptlet(e: Element) =
      if (e.minOccurs > 0) ""
      else {
        """
|  // minOccurs=0, disable if blank
|  var includeInXml  = !((v.val() == null) || (v.val().length==0));
|  pathDiv.attr('enabled','' + includeInXml);"""
      }

    private def createFacetTestScriptlet(r: Restriction) = {
      r.simpleRestrictionModelSequence3.facetsOption2.seq.flatMap(f => {
        val start = "\n|  //facet test\n|  if ((+(v.val())) "
        val finish = ")\n|    ok = false;"

        f match {
          case DataRecord(xs, Some("minInclusive"), x: Facet) =>
            Some(start + "< " + x.valueAttribute + finish)
          case DataRecord(xs, Some("maxInclusive"), x: Facet) =>
            Some(start + "> " + x.valueAttribute + finish)
          case DataRecord(xs, Some("minExclusive"), x: Facet) =>
            Some(start + "<=" + x.valueAttribute + finish)
          case DataRecord(xs, Some("maxExclusive"), x: Facet) =>
            Some(start + ">= " + x.valueAttribute + finish)
          case _ => None
        }
      }).mkString("")
    }

    private def createPatternsTestScriptlet(patterns: Seq[String]) =
      if (patterns.size > 0)
        """|    var patternMatched =false;
""" +
          patterns.zipWithIndex.map(x => createPatternScriptlet(x)).mkString("\n") + """
|  if (!(patternMatched))
|    ok = false;"""
      else ""

    private def createEnumerationTestScriptlet(node: NodeBasic, instances: Instances) = {
      if (isEnumeration(restriction(node))) {
        "\n|  //enumeration test" +
          (if (isRadio(node.element))
            "\n|  var radioInput=$('input:radio[name=\"" + getItemName(elementNumber(node), instances) + "\"]');" +
            "\n|  if (! radioInput.is(':checked')) ok = false;"
          else
            "\n|  if ($.trim(v.val()).length ==0) ok = false;")
      } else
        ""
    }

    private def createPatternScriptlet(x: (String, Int)) =
      """|
|  // pattern test
|  var regex""" + x._2 + """ = /^""" + x._1 + """$/ ;
|  if (regex""" + x._2 + """.test(v.val())) 
|    patternMatched = true;"""

    private def createBasePatternTestScriptlet(qn: QN) = {
      val basePattern = getBasePattern(qn)
      if (basePattern.size > 0)
        """    	  
|  // base pattern test
|  var regex = /^""" + basePattern.head + """$/ ;
|  if (!(regex.test(v.val()))) 
|    ok = false;"""
      else ""
    }

    private def changeReference(e: ElementWrapper, instances: Instances) =
      if (isRadio(e))
        "input:radio[name='" + getItemName(elementNumber(e), instances) + "']"
      else
        "#" + getItemId(elementNumber(e), instances)

    private def createClosingScriptlet(e: ElementWrapper, qn: QN, instances: Instances) = {
      val number = elementNumber(e)
      val onChange = "change("
      val changeMethod = qn match {
        case QN(xs, XsdDate) => onChange
        case QN(xs, XsdDateTime) => onChange
        case QN(xs, XsdTime) => onChange
        case _ => onChange
      };
      """
|  return ok;
|}
|      
|$("""" + changeReference(e, instances) + """").""" + changeMethod + """ function() {
|  var ok = validate""" + number + "instance" + instances + """();
|  var error= $("#""" + getItemErrorId(number, instances) + """");
|  if (!(ok)) 
|    error.show();
|  else 
|    error.hide();
|})
""" + (if (e.minOccurs == 0 && e.default.isEmpty)
        """
|//disable item-path due to minOccurs=0 and default is empty  
|$("#""" + getPathId(number, instances) + """").attr('enabled','false');"""
      else "")
    }

    private def addScriptWithMargin(s: String) = addScript(stripMargin(s))

    private def getPatterns(r: Restriction): Seq[String] =
      r.simpleRestrictionModelSequence3.facetsOption2.seq.flatMap(f => {
        f match {
          case DataRecord(xs, Some("pattern"), x: Pattern) => Some(x.valueAttribute)
          case _ => None
        }
      })

    private def getPatterns(node: NodeBasic): Seq[String] =
      {
        val r = restriction(node)

        val explicitPatterns = getPatterns(r)

        val qn = toQN(r)

        //calculate implicit patterns for dates, times, and datetimes
        val implicitPatterns =
          qn match {
            case QN(xs, XsdDate) => Some("\\d\\d\\d\\d-\\d\\d-\\d\\d")
            //TODO why spaces on end of time?
            case QN(xs, XsdTime) => Some("\\d\\d:\\d\\d *")
            case QN(xs, XsdDateTime) => Some("\\d\\d\\d\\d-\\d\\d-\\d\\dT\\d\\d:\\d\\d")
            case _ => None
          }

        explicitPatterns ++ implicitPatterns
      }

    private def getInputType(r: Restriction) = {
      val qn = toQN(r)
      qn match {
        case QN(xs, XsdBoolean) => "checkbox"
        case _ => "text"
      }
    }

    private def stripMargin(s: String) =
      s.stripMargin.replaceAll("\n", "\n" + margin)

    private def isMultiple(node: Node): Boolean =
      isMultiple(node.element)

    private def isMultiple(e: ElementWrapper): Boolean =
      return (e.maxOccurs == "unbounded" || e.maxOccurs.toInt > 1)

    private def repeatingEnclosingIds(e: ElementWrapper, instances: Instances) =
      repeats(e).map(instances.add(_)).map(getRepeatingEnclosingId(e, _))

    private def addMaxOccursScriptlet(e: ElementWrapper, instances: Instances) {
      val number = elementNumber(e)
      if (isMultiple(e)) {
        val repeatButtonId = getRepeatButtonId(number, instances)
        addScriptWithMargin("""
|$("#""" + repeatButtonId + """").click(function() {
|   // loop through all repeats until find first nonInvisible repeat and make it visible
|  var elem;
""" + repeatingEnclosingIds(e, instances)
          .map(id => { "|  elem = $('#" + id + "');\n|  if (!elemVisible(elem))\n|    { elem.show(); return; }\n" })
          .mkString("") +
          """|})
""")
      }
    }

    private def getAnnotation(e: Annotatedable, key: XsdFormsAnnotation): Option[String] =
      e.annotation match {
        case Some(x) =>
          x.attributes.get("@{" + AppInfoSchema + "}" + key.name) match {
            case Some(y) => Some(y.value.toString)
            case None => None
          }
        case None => None
      }

    private def getLabel(e: Element, typ: Option[SimpleType]) =
      {
        val name = getLabelFromName(e)
        val label = getAnnotation(e, Annotation.Label) match {
          case Some(x) => x
          case _ => name
        }

        val mustOccur = e.minOccurs.intValue > 0
        val isText = e.typeValue match {
          case Some(x: QName) => x == qn("string")
          case _ => false
        }
        val mandatory = typ match {
          case None => mustOccur && !isText
          case Some(x) => (
            x.simpleDerivationOption3.value match {
              case y: Restriction =>
                (getInputType(y) != "text") || isMandatory(e, y)
              case _ =>
                !isText
            })
        }
        if (mandatory) label + "<em>*</em>"
        else label
      }

    private def getLabelFromName(e: Element) =
      e.name.get
        .replaceAll("-", " ")
        .replaceAll("_", " ")
        .split(" ")
        .map(s =>
          Character.toUpperCase(s.charAt(0))
            + s.substring(1, s.length))
        .mkString(" ")

    private def isMandatory(e: Element, r: Restriction): Boolean = {
      val patterns = getPatterns(r)
      getInputType(r) == "text" &&
        e.minOccurs.intValue() == 1 &&
        patterns.size > 0 &&
        !patterns.exists(java.util.regex.Pattern.matches(_, ""))
    }

    private def restriction(node: NodeBasic): Restriction =
      node match {
        case n: NodeSimpleType => restriction(n)
        case n: NodeBaseType => restriction(n)
      }

    private def restriction(node: NodeSimpleType) =
      node.typ.simpleDerivationOption3.value match {
        case x: Restriction => x
        case _ => Util.unexpected
      }

    private def restriction(node: NodeBaseType) =
      new MyRestriction(node.typ.qName)

    private def numInstances(e: ElementWrapper): Int =
      if (isMultiple(e)) NumInstancesForMultiple
      else 1

    private def repeats(node: Node): Range = repeats(node.element)

    private def repeats(e: ElementWrapper): Range = 1 to numInstances(e)

    private def choiceContentId(idPrefix: String, number: String, index: Int, instances: Instances) =
      idPrefix + "choice-content-" + number + InstanceDelimiter + instances + ChoiceIndexDelimiter + index
    private def getRepeatButtonId(number: String, instances: Instances) =
      TreeToHtmlConverter.getRepeatButtonId(idPrefix, number, instances)
    private def getRepeatingEnclosingId(element: ElementWrapper, instances: Instances): String =
      TreeToHtmlConverter.getRepeatingEnclosingId(idPrefix, elementNumber(element), instances)
    private def getRepeatingEnclosingId(number: String, instances: Instances) =
      TreeToHtmlConverter.getRepeatingEnclosingId(idPrefix, number, instances)
    private def getChoiceItemName(node: Node, instances: Instances): String =
      getChoiceItemName(elementNumber(node.element), instances)
    private def getChoiceItemName(number: String, instances: Instances): String =
      TreeToHtmlConverter.getChoiceItemName(idPrefix, number, instances)
    private def getChoiceItemId(node: Node, index: Int, instances: Instances): String =
      getChoiceItemId(elementNumber(node.element), index, instances)
    private def getChoiceItemId(number: String, index: Int, instances: Instances): String =
      TreeToHtmlConverter.getChoiceItemId(idPrefix, number, index, instances)
    private def getItemId(node: Node, instances: Instances): String =
      getItemId(elementNumber(node.element), instances)
    private def getItemId(element: ElementWrapper, instances: Instances): String =
      getItemId(elementNumber(element), instances)
    private def getItemId(number: String, instances: Instances): String =
      TreeToHtmlConverter.getItemId(idPrefix, number, instances)
    private def getItemId(number: String, enumeration: Integer, instances: Instances): String =
      getItemId(number, instances) + "-" + enumeration
    private def getItemName(number: String, instances: Instances) =
      idPrefix + "item-input-" + number + InstanceDelimiter + instances;
    private def getItemEnclosingId(number: String, instances: Instances) =
      idPrefix + "item-enclosing-" + number + InstanceDelimiter + instances
    private def getItemErrorId(number: String, instances: Instances) =
      TreeToHtmlConverter.getItemErrorId(idPrefix, number, instances)
    private def getPathId(number: String, instances: Instances) =
      idPrefix + "item-path-" + number + InstanceDelimiter + instances

    private def nextNumber: String = {
      number += 1
      number + ""
    }

    private case class QN(namespace: String, localPart: String)

    private def toQN(r: Restriction): QN = toQN(r.base.get)

    private implicit def toQN(qName: QName): QN =
      QN(qName.getNamespaceURI(), qName.getLocalPart())

    def text =
      header +
        html.toString() + footer

    private def header = {
      val s = new StringBuilder
      s.append(
        """
<html>
<head>
<link rel="stylesheet" href="css/xsd-forms-style.css" type="text/css"/>
<link rel="stylesheet" href="css/xsd-forms-style-override.css" type="text/css"/>
<link type="text/css" href="css/smoothness/jquery-ui-1.8.16.custom.css" rel="stylesheet" />	
<link type="text/css" href="css/timepicker.css" rel="stylesheet" />	
<script type="text/javascript" src="js/jquery-1.6.2.min.js"></script>
<script type="text/javascript" src="js/jquery-ui-1.8.16.custom.min.js"></script>
<script type="text/javascript" src="js/jquery-ui-timepicker-addon.js"></script>
<script type="text/javascript">

function encodeHTML(s) {
    if (typeof(myVariable) != "undefined")
	    return s.replace(/&/g, '&amp;')
	               .replace(/</g, '&lt;')
	               .replace(/>/g, '&gt;')
	               .replace(/"/g, '&quot;');
     else 
          return s; 
}
          
function encodedValueById(id) {
    return encodeHTML($("#"+id).val());
}

function spaces(n) {
    var s = "";
    for (var i=0;i<n;i++)
      s = s + " ";
    return s;
}

function cloneAndReplaceIds(element, suffix){
  var clone = element.clone();
  clone.find("*[id]").andSelf().each(function() { 
    var previousId = $(this).attr("id");
    var newId = previousId.replace(/(-[0-9][0-9]*)$/,"$1" + suffix);
    $(this).attr("id", newId); 
  });
  return clone;
}
    
function idVisible(id) {
    return elemVisible($("#"+id));
}
    
function elemVisible(elem) {
    return elem.is(":visible");    
}
          
function toXmlDate(s) {
  return s;
}
          
function toXmlDateTime(s) {
  return $.trim(s) +":00";
}
          
function toXmlTime(s) {
  return $.trim(s) +":00";
}
          
function toBoolean(s) {
  if (s == "on" || s == "true")
    return "true";
  else 
    return "false";
}
          
$(function() {
  $('input').filter('.datepickerclass').datepicker();
  //now a workaround because datepicker does not use the initial value with the required format but expects mm/dd/yyyy
  $('input').filter('.datepickerclass').each(function() {
    var elem = $(this);
    var val = elem.attr('value');
    elem.datepicker( "option", "dateFormat","yy-mm-dd");
    if (typeof(val) != 'undefined') {
      console.log("val="+val);
      elem.datepicker('setDate',val);
    }
  });
  $('input').filter('.datetimepickerclass').datetimepicker({ dateFormat: 'yy-mm-dd', timeFormat: 'hh:mm',separator: 'T'});
  $('input').filter('.timepickerclass').timepicker({});

  function callMethod(methodName, argument) {
    var method = eval('(' + methodName + ')');
    return method(argument);
  }

  $('#pre-submit').click( function () {
    var previousItems = null;
    $('*[number]').each( function(index) {
      var thisId = this.id
      var elem = $('#' + thisId)
      // will do validations here
      //if elem visible then do the validation for that element
      if (elemVisible(elem)) 
        elem.change();
    });
    $('input:radio').each( function (index) {
      console.log("radio.id="+ this.id);
      var elem = $('#' + this.id)
      if (elemVisible(elem))
        elem.change();
    });
    var count = $('.item-error').filter(":visible").length
    if (count>0) {
      $('#validation-errors').show();
      return;
    }
    else 
      $('#validation-errors').hide();
    var s = getXml1instance();
    s = "<?xml version=\"1.0\" encoding=\"utf-8\"?>" + s;
    s = s.replace(/</g,"&lt;").replace(/>/g,"&gt;");
    s = "<pre>" + s + "</pre>";
          
    $('#submit-comments').html(s);
  });
""" + script + """
  $("#form").submit(function () { return false; }); // so it won't submit
""" + extraScript.mkString + """
    });
</script>
<script type="text/javascript" src="js/xsd-forms-override.js"></script>
</head>
<body>
<div class="form">
<form method="POST" action="form.html" name="form">
""")
      s.toString
      //TODO action form parameter should be a constructor parameter for HtmlVisitor
    }

    private def footer =
      """
  <!--<input id="submit" class="submit" type="submit"></input>-->
      <div id="validation-errors" class="validationErrors">The form is not yet complete. Check through the form for error messages</div>
  <div id="pre-submit" class="pre-submit">Submit</div>
    		<p><div id="submit-comments"></div></p>
</form>
</div>
</body>
</html>"""

  }

  /**
   * **************************************************************
   *
   *   SchemaTraversor
   *
   *
   * **************************************************************
   */

  class SchemaTraversor(s: Schema, rootElement: String, visitor: Visitor) {
    import Util._
    import XsdUtil._

    private val topLevelElements =
      s.schemasequence1.flatMap(_.schemaTopOption1.value match {
        case y: TopLevelElement => Some(y)
        case _ => None
      })

    private val topLevelComplexTypes = s.schemasequence1.flatMap(_.schemaTopOption1.value match {
      case y: TopLevelComplexType => Some(y)
      case _ => None
    })

    private val topLevelSimpleTypes = s.schemasequence1.flatMap(_.schemaTopOption1.value match {
      case y: TopLevelSimpleType => Some(y)
      case _ => None
    })

    private val targetNs = s.targetNamespace.getOrElse(
      unexpected("schema must have targetNamespace attribute")).toString

    private val schemaTypes =
      (topLevelComplexTypes.map(x => (qn(targetNs, x.name.get), x))
        ++ (topLevelSimpleTypes.map(x => (qn(targetNs, x.name.get), x)))).toMap;

    private val baseTypes =
      Set(XsdDecimal, XsdString, XsdInteger, XsdDate, XsdDateTime, XsdTime, XsdBoolean)
        .map(qn(_))

    private def getType(q: QName): AnyRef = {
      schemaTypes.get(q) match {
        case Some(x: Annotatedable) => return x
        case _ =>
          if (baseTypes contains q) return BaseType(q)
          else unexpected("unrecognized type: " + q)
      }
    }

    private def toQName[T](d: DataRecord[T]) =
      new QName(d.namespace.getOrElse(null), d.key.getOrElse(null))

    private def matches[T](d: DataRecord[T], q: QName) =
      toQName(d).equals(q)

    private case class MyType(typeValue: AnyRef)

    /**
     * Visits the element definition tree.
     */
    def traverse {

      val element = topLevelElements.find(
        _.name match {
          case Some(y) => y equals rootElement
          case None => false
        }).getOrElse(unexpected("did not find element " + rootElement))

      process(element)

    }

    private def process(e: Element) {
      def exception = unexpected("type of element " + e + " is missing")
      e.typeValue match {
        case Some(x: QName) => process(e, MyType(getType(x)))
        case None => {
          e.elementoption match {
            case Some(x: DataRecord[ElementOption]) => {
              x.value match {
                case y: LocalComplexType => process(e, y)
                case y: LocalSimpleType => process(e, y)
                case _ => unexpected
              }
            }
            case _ => unexpected
          }
        }
        case _ => unexpected
      }
    }

    private def process(e: Element, typeValue: MyType) {
      typeValue.typeValue match {
        case x: TopLevelSimpleType => process(e, x)
        case x: TopLevelComplexType => process(e, x)
        case x: BaseType => process(e, x)
      }
    }

    private def process(e: Element, x: SimpleType) {
      visitor.simpleType(e, x)
    }

    private def process(e: Element, x: ComplexType) {
      x.complexTypeModelOption3.value match {
        case x: ComplexContent =>
          unexpected
        case x: SimpleContent =>
          unexpected
        case x: ComplexTypeModelSequence1 =>
          x.typeDefParticleOption1.getOrElse(unexpected).value match {
            case y: GroupRef =>
              unexpected
            case y: ExplicitGroupable =>
              if (matches(x.typeDefParticleOption1.get, qn("sequence")))
                process(e, Sequence(y))
              else if (matches(x.typeDefParticleOption1.get, qn("choice")))
                process(e, Choice(y))
              else unexpected
            case _ => unexpected
          }
      }
    }

    private def process(e: Element, x: BaseType) {
      visitor.baseType(e, x)
    }

    private def process(e: Element, x: Sequence) {
      visitor.startSequence(e)
      x.group.particleOption3.foreach(y => process(e, toQName(y), y.value))
      visitor.endSequence
    }

    private def process(e: Element, q: QName, x: ParticleOption) {
      if (q == qn("element")) {
        x match {
          case y: LocalElementable => process(y)
          case _ => unexpected
        }
      } else if (q == qn("choice")) {
        x match {
          case y: ExplicitGroupable => process(e, Choice(y))
          case _ => unexpected
        }
      } else unexpected(q + x.toString)
    }

    private def process(e: Element, x: Choice) {
      visitor.startChoice(e, x)
      var index = 0
      x.group.particleOption3.foreach(y => {
        index = index + 1
        visitor.startChoiceItem(e, y.value, index)
        process(e, toQName(y), y.value)
        visitor.endChoiceItem
      })
      visitor.endChoice
    }
  }

  /**
   * **************************************************************
   *
   *   Html
   *
   *
   * **************************************************************
   */

  private object Html {
    val Legend = "legend"
    val Div = "div"
    val Select = "select"
    val Option = "option"
    val Label = "label"
    val Fieldset = "fieldset"
    val Textarea = "textarea"
    val Input = "input"
    val Id = "id"
    val Class = "class"
    val Value = "value"
    val Enabled = "enabled"
    val Checked = "checked"
    val Name = "name"
  }

  private class Html {
    import Html._
    private case class HtmlElement(name: String, hasContent: Boolean)
    private val stack = new scala.collection.mutable.Stack[HtmlElement]
    private var s = new StringBuffer

    private def indent {
      val indent = "  " * stack.size
      append("\n")
      append(indent)
    }

    private def append(str: String) = {
      s.append(str)
      this
    }

    def div(id: Option[String] = None,
      classes: List[String] = List(), enabledAttr: Option[String] = None,
      content: Option[String] = None) =
      element(name = Div, id = id, classes = classes, enabledAttr = enabledAttr,
        content = content)

    def select(id: Option[String] = None, name: String,
      classes: List[String] = List(), content: Option[String] = None, number: Option[String] = None) =
      element(name = Select, id = id, classes = classes, nameAttr = Some(name), numberAttr = number,
        content = content)

    def option(id: Option[String] = None,
      classes: List[String] = List(),
      value: String,
      content: Option[String] = None) =
      element(name = Html.Option, id = id, classes = classes,
        content = content, value = Some(value))

    def label(forInputName: String, id: Option[String] = None,
      classes: List[String] = List(), content: Option[String] = None) =
      element(
        name = Label,
        id = id,
        forAttr = Some(forInputName),
        classes = classes,
        content = content)

    def fieldset(
      legend: Option[String] = None,
      classes: List[String] = List(),
      id: Option[String]) = {
      element(name = Fieldset, classes = classes, id = id)
      legend match {
        case Some(x) => element(name = Legend, content = Some(x)).closeTag
        case None =>
      }
    }

    def textarea(id: Option[String] = None,
      classes: List[String] = List(),
      content: Option[String] = None,
      value: Option[String] = None,
      name: String, number: Option[String] = None,
      closed: Boolean = false) =
      element(name = Textarea,
        id = id,
        classes = classes,
        content = content,
        value = value,
        nameAttr = Some(name),
        numberAttr = number)

    def input(id: Option[String] = None,
      classes: List[String] = List(),
      content: Option[String] = None,
      name: String, value: Option[String] = None,
      checked: Option[Boolean] = None,
      number: Option[String] = None,
      typ: Option[String]) =
      element(name = Input, id = id, classes = classes, checked = checked,
        content = content, value = value, nameAttr = Some(name), typ = typ, numberAttr = number)

    private def classNames(classes: List[String]) =
      if (classes.length == 0)
        None
      else
        Some(classes.mkString(" "))

    def element(name: String,
      id: Option[String] = None,
      classes: List[String] = List(),
      content: Option[String] = None,
      value: Option[String] = None,
      checked: Option[Boolean] = None,
      nameAttr: Option[String] = None,
      enabledAttr: Option[String] = None,
      forAttr: Option[String] = None,
      numberAttr: Option[String] = None,
      typ: Option[String] = None): Html = {
      val attributes =
        id.map((Id, _)) ++
          classNames(classes).map((Class -> _)) ++
          value.map((Value, _)) ++
          nameAttr.map((Name, _)) ++
          enabledAttr.map((Enabled, _)) ++
          forAttr.map(("for", _)) ++
          typ.map(("type", _)) ++
          checked.map(x => (Checked, x.toString)) ++
          numberAttr.map(("number", _))
      elementBase(name, attributes.toMap, content)
      this
    }

    def elementBase(name: String, attributes: Map[String, String],
      content: Option[String]): Html = {
      stack.push(HtmlElement(name, content.isDefined))
      indent
      val attributesClause =
        attributes.map(x => x._1 + "=\"" + x._2 + "\"").mkString(" ")
      append("<" + name + " "
        + attributesClause + ">" + content.mkString)
      this
    }

    def closeTag: Html = {
      if (stack.isEmpty) throw new RuntimeException("closeTag called on empty html stack!")
      val element = stack.head
      if (!element.hasContent) {
        indent
      }
      append("</" + element.name + ">");
      stack.pop
      this
    }

    def closeTag(n: Int): Html = {
      for (i <- 1 to n)
        closeTag
      this
    }

    override def toString = s.toString()

  }
}

