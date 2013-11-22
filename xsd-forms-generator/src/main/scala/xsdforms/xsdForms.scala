package xsdforms {

  import xsd._
  import xsd.ComplexTypeModelSequence1

  import javax.xml.namespace.QName

  import scalaxb._

  object Annotation {
    //TODO document each of the annotations in scaladoc

    /**
     * Annotation 'label' is the label to be used for the element
     * input field. Usually located just left of the input field
     * associated with the element with this annotation. If omitted
     * the default value is the element name with dashes and underscores
     * replaced with spaces then camel cased.
     */
    val Label = XsdFormsAnnotation("label")

    /**
     * Annotation 'choice' If choice=inline then choice is inline
     * with radio selector otherwise appears after the group of
     * radio selectors.
     */
    val Choice = XsdFormsAnnotation("choice")
    val ChoiceLabel = XsdFormsAnnotation("choiceLabel")

    /**
     * If legend is set then is used as label for a fieldset block.
     */
    val Legend = XsdFormsAnnotation("legend")
    val RepeatLabel = XsdFormsAnnotation("repeatLabel")
    val MinOccursZeroLabel = XsdFormsAnnotation("minOccursZeroLabel")
    val RemoveLabel = XsdFormsAnnotation("removeLabel")
    val Title = XsdFormsAnnotation("title")
    val Before = XsdFormsAnnotation("before")
    val After = XsdFormsAnnotation("after")

    /**
     * if text=textarea then html textarea used as input. Otherwise normal
     * short text input used.
     */
    val Text = XsdFormsAnnotation("text")

    /**
     * Specific css for width to be applied to the input element.
     */
    val Width = XsdFormsAnnotation("width")

    /**
     * If selector=radio then radio control used for choice instead
     * of drop down.
     */
    val Selector = XsdFormsAnnotation("selector")

    /**
     * If addBlank=true and an enumeration is being displayed (in a
     * drop-down) then a blank option will be added to the drop-down
     *  representing no selection.
     */
    val AddBlank = XsdFormsAnnotation("addBlank")
    val Css = XsdFormsAnnotation("css")

    /**
     * Validation message to be displayed if the input is assessed
     *  as invalid.
     */
    val Validation = XsdFormsAnnotation("validation")

    /**
     * Help to display as tooltip if user clicks/hovers input.
     */
    val Help = XsdFormsAnnotation("help")

    /**
     * makeVisible=n, n integer, means that on selection of this choice the
     * element with number n greater than the current element will be made visible.
     */
    val MakeVisible = XsdFormsAnnotation("makeVisible")
    val NonRepeatingTitle = XsdFormsAnnotation("nonRepeatingTitle")
    /**
     * Annotation 'description' appears just below the input box.
     */
    val Description = XsdFormsAnnotation("description")
    val Visible = XsdFormsAnnotation("visible")

    /**
     * maxRepeats should be an integer value >0 for an element and is the
     * maximum number of repeats generated in html of the element (all
     * be them hidden).
     */
    val MaxRepeats = XsdFormsAnnotation("maxRepeats")
  }

  /**
   * **************************************************************
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
    def unexpected() = throw new RuntimeException()
  }

  case class XsdDatatype(name: String, pattern: Option[String] = None)

  /**
   * Utility methods and constants for XML Schemas (XSD).
   */
  object XsdUtil {
    val Xsd = "http://www.w3.org/2001/XMLSchema"
    val AppInfoSchema = "http://moten.david.org/xsd-forms"
    def qn(namespaceUri: String, localPart: String) = new QName(namespaceUri, localPart)
    def qn(localPart: String): QName = new QName(Xsd, localPart)
    def qn(datatype: XsdDatatype): QName = qn(Xsd, datatype.name)

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
   *   ElementWrapper
   *
   *
   * **************************************************************
   */

  object ElementWrapper {
    implicit def unwrap(wrapped: ElementWrapper): Element = wrapped.element
  }

  case class ElementWrapper(element: Element, uniqueId: String)

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

    def getItemId(idPrefix: String, number: Int, instances: Instances) =
      idPrefix + "item-" + number + InstanceDelimiter + instances

    def getItemName(idPrefix: String, number: Int, instances: Instances) =
      idPrefix + "item-input-" + number + InstanceDelimiter + instances;

    def getItemErrorId(idPrefix: String, number: Int, instances: Instances) =
      idPrefix + "item-error-" + number + InstanceDelimiter + instances

    def getChoiceItemId(idPrefix: String, number: Int, index: Int, instances: Instances): String =
      getItemId(idPrefix, number, instances) + ChoiceIndexDelimiter + index

    def getChoiceItemName(idPrefix: String, number: Int, instances: Instances) =
      idPrefix + "item-input-" + number + InstanceDelimiter + instances

    def getRepeatButtonId(idPrefix: String, number: Int, instances: Instances) =
      idPrefix + "repeat-button-" + number + InstanceDelimiter + instances

    def getRemoveButtonId(idPrefix: String, number: Int, instances: Instances) =
      idPrefix + "remove-button-" + number + InstanceDelimiter + instances

    def getRepeatingEnclosingId(idPrefix: String, number: Int, instances: Instances): String =
      idPrefix + "repeating-enclosing-" + number + InstanceDelimiter + instances

    def getMinOccursZeroId(idPrefix: String, number: Int, instances: Instances): String =
      idPrefix + "min-occurs-zero-" + number + InstanceDelimiter + instances

    def getMinOccursZeroName(idPrefix: String, number: Int, instances: Instances): String =
      idPrefix + "min-occurs-zero-name" + number + InstanceDelimiter + instances

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
    val ClassRemoveButton = "remove-button"
    val ClassRemoveButtonContainer = "remove-button-container"
    val ClassRepeatingEnclosing = "repeating-enclosing"
    val ClassItemInputTextarea = "item-input-textarea"
    val ClassItemInputText = "item-input-text"
    val ClassSelect = "select"
    val ClassChoice = "choice"
    val ClassWhite = "white"
    val ClassSmall = "small"
    val ClassItemDescription = "item-description"
    val ClassTimePicker = "timepickerclass"
    val ClassDatePicker = "datepickerclass"
    val ClassDateTimePicker = "datetimepickerclass"
    val ClassMinOccursZero = "min-occurs-zero"
    val ClassMinOccursZeroContainer = "min-occurs-zero-container"
    val ClassMinOccursZeroLabel = "min-occurs-zero-label"
  }

  case class XsdFormsAnnotation(name: String)

  /**
   * **************************************************************
   *
   *   JS
   *
   *
   * **************************************************************
   */

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

    def append(s: String, params: Object*): JS = {
      b append String.format(s, params: _*)
      this
    }

    override def toString = b.toString
  }

  /**
   * **************************************************************
   *
   *   Generator
   *
   *
   * **************************************************************
   */

  object Generator {
    import java.io._
    import java.util.UUID
    import java.util.zip._
    import org.apache.commons.io._

    def generateZip(
      schema: InputStream,
      zip: OutputStream,
      idPrefix: String = "a-",
      rootElement: Option[String] = None,
      extraScript: Option[String] = None) {

      val zipOut = new ZipOutputStream(zip)

      def action(bytes: Array[Byte], name: String, isDirectory: Boolean) {
        zipOut putNextEntry new ZipEntry(name)
        zipOut write bytes
      }

      copyJsCssAndGeneratedForm(schema, action, idPrefix, rootElement, extraScript)

      zipOut.close
    }

    def generateDirectory(
      schema: InputStream,
      directory: File,
      idPrefix: String = "a-",
      rootElement: Option[String] = None,
      extraScript: Option[String] = None) {

      def action(bytes: Array[Byte], name: String, isDirectory: Boolean) {
        import org.apache.commons.io.FileUtils
        val path = directory.getPath + File.separator + name
        val file = new File(path)
        new File(file.getParent).mkdirs
        if (isDirectory)
          file.mkdir
        else {
          val fos = new FileOutputStream(file)
          fos write bytes
          fos.close
        }
      }

      copyJsCssAndGeneratedForm(schema, action, idPrefix, rootElement, extraScript)
    }

    private def copyJsCssAndGeneratedForm(
      schema: InputStream,
      action: (Array[Byte], String, Boolean) => Unit,
      idPrefix: String = "a-",
      rootElement: Option[String] = None,
      extraScript: Option[String] = None) {
      val text = generateHtmlAsString(schema, idPrefix, rootElement, extraScript)

      val zipIn = new ZipInputStream(Generator.getClass().getResourceAsStream("/xsd-forms-js-css.zip"))

      val iterator = Iterator.continually(zipIn.getNextEntry).takeWhile(_ != null)
      iterator.foreach { zipEntry =>
        val bytes = IOUtils.toByteArray(zipIn)
        val name = zipEntry.getName
        action(bytes, name, zipEntry.isDirectory)
      }
      zipIn.close

      val name = "form.html"
      action(text.getBytes, name, false)
    }

    def generateHtml(schema: InputStream,
      html: OutputStream,
      idPrefix: String = "a-",
      rootElement: Option[String] = None,
      extraScript: Option[String] = None) {

      val text = generateHtmlAsString(schema, idPrefix, rootElement, extraScript)
      html write text.getBytes
    }

    def generateHtmlAsString(schema: InputStream,
      idPrefix: String = "a-",
      rootElement: Option[String] = None,
      extraScript: Option[String] = None): String = {

      import scala.xml._

      val schemaXb = scalaxb.fromXML[Schema](
        XML.load(schema))

      val ns = schemaXb.targetNamespace.get.toString

      val visitor = new TreeCreatingVisitor()

      new SchemaTraversor(schemaXb, rootElement, visitor).traverse
      println("tree:\n" + visitor)

      new TreeToHtmlConverter(ns, idPrefix, extraScript, visitor.rootNode).text
    }

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

    private val NumInstancesForMultiple = 5

    import scala.collection.mutable.HashMap
    private val elementNumbers = new HashMap[ElementWrapper, Int]()

    //assign element numbers so that order of display on page 
    //will match order of element numbers. To do this must 
    //traverse children left to right before siblings
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

    private def hasButton(e: Element) = e.maxOccurs != "1" && e.minOccurs.toString != e.maxOccurs

    private def doNode(node: NodeSequence, instances: Instances) {
      val e = node.element
      val number = elementNumber(node)
      val legend = getAnnotation(e, Annotation.Legend)
      val usesFieldset = legend.isDefined

      val label = getAnnotation(e, Annotation.Label).mkString

      html
        .div(classes = List(ClassSequence))
      nonRepeatingTitle(e, instances)
      minOccursZeroCheckbox(e, instances)
      repeatButton(e, instances)
      for (instanceNo <- repeats(e)) {
        val instNos = instances add instanceNo
        repeatingEnclosing(e, instNos)

        html
          .div(classes = List(ClassSequenceLabel), content = Some(label)).closeTag
          .div(id = Some(idPrefix + "sequence-" + number + InstanceDelimiter + instanceNo),
            classes = List(ClassSequenceContent))
        addRemoveButton(e, instNos)
        if (usesFieldset)
          html.fieldset(legend = legend, classes = List(ClassFieldset), id = Some(idPrefix + "fieldset-" + number + InstanceDelimiter + instanceNo))

        doNodes(node.children, instNos)

        if (usesFieldset)
          html closeTag

        html closeTag 2
      }
      html closeTag

      addMaxOccursScriptlet(e, instances)
    }

    private def doNode(node: NodeChoice, instances: Instances) {
      val choice = node.choice
      val e = node.element
      val choiceInline = displayChoiceInline(choice)

      val number = elementNumber(e)

      html.div(id = Some(getItemEnclosingId(number, instances add 1)), classes = List(ClassChoice))
      nonRepeatingTitle(e, instances)
      minOccursZeroCheckbox(e, instances)
      repeatButton(e, instances)
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
        addRemoveButton(e, instNos)

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
      repeatButton(e, instances)
      val t = Some(typ)
      val number = elementNumber(e)
      for (instanceNo <- repeats(e)) {
        val instNos = instances add instanceNo
        repeatingEnclosing(e, instNos)
        itemTitle(e)
        addRemoveButton(e, instNos)
        itemBefore(e)
        html.div(classes = List(ClassItemNumber), content = Some(number.toString)).closeTag
          .label(forInputName = getItemName(number, instNos),
            classes = List(ClassItemLabel), content = Some(getLabel(e, t))).closeTag
          .div(classes = List(ClassItemInput))

        simpleType(node, instNos)

        html closeTag 2
      }
      html closeTag;
      addMaxOccursScriptlet(e, instances)
    }

    private def doNode(node: NodeBaseType, instances: Instances) {
      val e = node.element
      val typ = node.typ
      nonRepeatingSimpleType(e, instances)
      repeatButton(e, instances)
      val t = None
      val number = elementNumber(e)
      for (instanceNo <- repeats(e)) {
        val instNos = instances add instanceNo
        repeatingEnclosing(e, instNos)
        itemTitle(e)
        addRemoveButton(e, instNos)
        itemBefore(e)
        html.div(classes = List(ClassItemNumber), content = Some(number.toString)).closeTag
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
        val js = JS()
        val number = elementNumber(node)
        if (isMinOccursZero(node.element)) {
          js.line("if (!$('#%s').is(':checked')) return '';", getMinOccursZeroId(number, instances))
        }
        js
          .line("    var xml = %s%s;", spaces(instances add 1), xmlStart(node))
          .line("    //now add sequence children for each instanceNo")

        for (instanceNo <- repeats(node)) {
          val instNos = instances add instanceNo
          js.line("    if (idVisible('%s')) {", getRepeatingEnclosingId(number, instNos))
          node.children.foreach { n =>
            js.line("      xml += %s();", xmlFunctionName(n, instNos))
            addXmlExtractScriptlet(n, instNos)
          }
          js.line("    }")
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
      val js = JS().line("  var xml='';")
      for (instanceNo <- repeats(node)) {
        val instNos = instances add instanceNo
        js
          .line("  if (idVisible('%s')) {", getRepeatingEnclosingId(number, instNos))
        if (isRadio(node.element))
          js.line("    var v = encodeHTML($('input[name=%s]:radio:checked').val());", getItemName(number, instNos))
        else
          js.line("    var v = %s;", valById(getItemId(node, instNos)))

        val extraIndent =
          if (node.element.minOccurs.intValue == 0) {
            js
              .line("    if (v.length>0)")
            "  "
          } else ""
        js.line("    %sxml += %s%s;", extraIndent, spaces(instNos), xml(node, transformToXmlValue(node, "v")))

          .line("  }")
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
        case QN(xs, XsdDate.name) => "toXmlDate(" + value + ")"
        case QN(xs, XsdDateTime.name) => "toXmlDateTime(" + value + ")"
        case QN(xs, XsdTime.name) => "toXmlTime(" + value + ")"
        case QN(xs, XsdBoolean.name) => "toBoolean(" + value + ")"
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
        JS().line("  //extract xml from element <%s>", node.element.name.getOrElse("?"))
          .line("  function %s() {", functionName)
          .line(functionBody)
          .line("  }")
          .line)
    }

    private def refById(id: String) = "$(\"#" + id + "\")"
    private def valById(id: String) = "encodedValueById(\"" + id + "\")"
    private def namespace(node: Node) =
      if (elementNumber(node.element) == 1)
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
    }

    private def addScript(js: JS) {
      addScript(js.toString)
    }

    private def isMinOccursZero(e: ElementWrapper) = e.minOccurs.intValue == 0

    private def minOccursZeroCheckbox(e: ElementWrapper, instances: Instances) {
      val number = elementNumber(e)
      if (isMinOccursZero(e)) {
        html.div(classes = List(ClassMinOccursZeroContainer))
        html
          .div(
            classes = List(ClassMinOccursZeroLabel),
            content = Some(getAnnotation(e, Annotation.MinOccursZeroLabel).getOrElse("Click to enable")))
          .closeTag
        html.input(
          id = Some(getMinOccursZeroId(number, instances)),
          name = getMinOccursZeroName(number, instances),
          classes = List(ClassMinOccursZero),
          typ = Some("checkbox"),
          checked = Some(false),
          value = None,
          number = Some(number))
          .closeTag
        html.closeTag
        val js = JS()
          .line("$('#%s').change( function () {", getMinOccursZeroId(number, instances))
        for (instanceNo <- repeats(e)) {
          js
            .line("  changeMinOccursZeroCheckbox($(this),$('#%s'));", getRepeatingEnclosingId(number, instances add instanceNo))
        }

        js.line("})")
          .line
          .line("$('#%s').change();", getMinOccursZeroId(number, instances))

        addScript(js)
      }
    }

    private def displayChoiceInline(choice: Choice) =
      "inline" == getAnnotation(choice.group, Annotation.Choice).mkString

    private def addChoiceHideOnStartScriptlet(
      particles: Seq[ParticleOption], number: Int, instances: Instances) {

      val forEachParticle = particles.zipWithIndex.foreach _

      forEachParticle(x => {
        val index = x._2 + 1
        addScript(
          JS().line("  $('#%s').hide();", choiceContentId(idPrefix, number, index, instances)))
      })
    }

    private def addChoiceShowHideOnSelectionScriptlet(
      particles: Seq[ParticleOption], number: Int, instances: Instances) {

      val forEachParticle = particles.zipWithIndex.foreach _

      val choiceChangeFunction = "choiceChange" + number + "instance" + instances;

      val js = JS()
      js.line("  var %s = function addChoiceChange%sinstance%s() {", choiceChangeFunction, number.toString, instances)
        .line("    $(':input[@name=%s]').change(function() {", getChoiceItemName(number, instances))
        .line("      var checked = $(':input[name=%s]:checked').attr('id');", getChoiceItemName(number, instances))

      forEachParticle(x => {
        val index = x._2 + 1
        val ccId =
          choiceContentId(idPrefix, number, index, instances)
        js.line("      if (checked == '%s') {", getChoiceItemId(number, index, instances))
          .line("        $('#%s').show();", ccId)
          .line("        $('#%s').find('.item-path').attr('enabled','true');", ccId)
          .line("      }")
          .line("      else {")
          .line("        $('#%s').hide();", ccId)
          .line("        $('#%s').find('.item-path').attr('enabled','false');", ccId)
          .line("      }")

      })
      js.line("    });")
        .line("  }")
        .line
        .line("  %s();", choiceChangeFunction)

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

    private def nonRepeatingTitle(e: ElementWrapper, instances: Instances) {
      //there's only one of these so use instanceNo = 1
      val number = elementNumber(e)
      html
        .div(
          classes = List(ClassNonRepeatingTitle),
          content = getAnnotation(e, Annotation.NonRepeatingTitle))
        .closeTag
    }

    private def repeatButton(e: ElementWrapper, instances: Instances) {
      val number = elementNumber(e)
      if (hasButton(e))
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
      if (e.minOccurs.intValue > 0 && instances.last > e.minOccurs.intValue)
        addScript(JS().line("  $('#%s').hide();", id))
    }

    private def nonRepeatingSimpleType(e: ElementWrapper, instances: Instances) {
      val number = elementNumber(e)
      html
        .div(
          classes = List(ClassItemEnclosing) ++ getVisibility(e),
          id = Some(getItemEnclosingId(number, instances add 1)))
      nonRepeatingTitle(e, instances)
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

    private def elementNumber(node: Node): Int = elementNumber(node.element)

    private def elementNumber(e: ElementWrapper): Int = {
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
        .map(insertMargin(_))
        .filter(_.length > 0)
        .foreach(addScript(_))

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
      case QN(xs, XsdDate.name) => ClassDatePicker + " "
      case QN(xs, XsdDateTime.name) => ClassDateTimePicker + " "
      case QN(xs, XsdTime.name) => ClassTimePicker + " "
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
            case QN(xs, XsdTime.name) => Some(v.substring(0, 5))
            case QN(xs, XsdDateTime.name) => Some(v.substring(0, 16))
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
          addScript(JS().line("  $('#%s').width('%s');", itemId, x))
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

    private def addRemoveButton(e: ElementWrapper, instances: Instances) {
      val number = elementNumber(e)
      val removeButtonId = getRemoveButtonId(number, instances)
      val canRemove =
        (instances.last != 1 && e.maxOccurs != e.minOccurs.toString)
      if (canRemove)
        html
          .div(classes = List(ClassRemoveButtonContainer))
          .div(
            id = Some(getRemoveButtonId(number, instances)),
            classes = List(ClassRemoveButton, ClassWhite, ClassSmall),
            content = Some(getAnnotation(e, Annotation.RemoveLabel).getOrElse("-")))
          .closeTag
          .closeTag

      val repeatingEncId = getRepeatingEnclosingId(e, instances)
      val js = JS()
        .line("  $('#%s').click(function() {", removeButtonId)
        .line("    $('#%s').hide();", repeatingEncId)
        .line("  });")
        .line
      addScript(js)
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
      number: Int, isRadio: Boolean, initializeBlank: Boolean, instances: Instances) {
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
              val refersTo = number + y.toInt
              val js = JS()
                .line("  $('#%s').change( function() {", getItemId(number, instances))
                .line("    var v = $('#%s');", getItemId(number, instances))
                .line("    var refersTo = $('#%s');", getItemEnclosingId(refersTo, instances))
                .line("    if ('%s' == v.val())", x._2.valueAttribute)
                .line("      refersTo.show();")
                .line("    else")
                .line("      refersTo.hide();")
                .line("  })")
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
        case QN(xs, XsdDecimal.name) => XsdDecimal.pattern
        case QN(xs, XsdInteger.name) => XsdInteger.pattern
        case QN(xs, XsdInt.name) => XsdInt.pattern
        case QN(xs, XsdLong.name) => XsdLong.pattern
        case QN(xs, XsdShort.name) => XsdShort.pattern
        case QN(xs, XsdPositiveInteger.name) => XsdPositiveInteger.pattern
        case QN(xs, XsdNonPositiveInteger.name) => XsdNonPositiveInteger.pattern
        case QN(xs, XsdNegativeInteger.name) => XsdNegativeInteger.pattern
        case QN(xs, XsdNonNegativeInteger.name) => XsdNonNegativeInteger.pattern
        case QN(xs, XsdDouble.name) => XsdDouble.pattern
        case QN(xs, XsdFloat.name) => XsdFloat.pattern
        case _ => None
      }
    }

    private def createDeclarationScriptlet(e: ElementWrapper, qn: QN, instances: Instances) = {
      val number = elementNumber(e)
      val itemId = getItemId(number, instances)
      JS()
        .line("// %s", e.name.get)
        .line("var validate%sinstance%s = function () {", number.toString, instances)
        .line("  var ok = true;")
        .line("  var v = $('#%s');", itemId)
        .line("  var pathDiv = $('#%s');", getPathId(number, instances))
        .toString
    }

    private def createMandatoryTestScriptlet(node: NodeBasic) = {
      if (isMandatory(node.element, restriction(node)))
        JS()
          .line("  // mandatory test")
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
      if (e.minOccurs.intValue > 0) ""
      else {
        JS()
          .line("  // minOccurs=0, ok if blank")
          .line("  var isBlank  = (v.val() == null) || (v.val().length==0);")
          .line("  if (isBlank) ok = true;")
          .toString
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
        JS().line("  var patternMatched =false;")
          .append(patterns.zipWithIndex.map(x => createPatternScriptlet(x)).mkString(""))
          .line("  if (!(patternMatched))")
          .line("    ok = false;")
          .toString
      else ""

    private def createEnumerationTestScriptlet(node: NodeBasic, instances: Instances) = {
      val js = JS()
      if (isEnumeration(restriction(node))) {
        js.line("  //enumeration test")
        if (isRadio(node.element))
          js.line("  var radioInput=$('input:radio[name=%s]');", getItemName(elementNumber(node), instances))
            .line("  if (! radioInput.is(':checked')) ok = false;")
        else
          js.line("  if ($.trim(v.val()).length ==0) ok = false;")
      }
      js.toString
    }

    private def createPatternScriptlet(x: (String, Int)) =
      JS()
        .line("  patternMatched |= matchesPattern(v,/^%s$/);", x._1)
        .toString

    private def createBasePatternTestScriptlet(qn: QN) = {
      val js = JS()
      val basePattern = getBasePattern(qn)
      basePattern match {
        case Some(pattern) => js.line("  ok &= matchesPattern(v,/^%s$/);", pattern)
        case _ =>
      }
      js.toString
    }

    private def changeReference(e: ElementWrapper, instances: Instances) =
      if (isRadio(e))
        "input:radio[name=" + getItemName(elementNumber(e), instances) + "]"
      else
        "#" + getItemId(elementNumber(e), instances)

    private def createClosingScriptlet(e: ElementWrapper, qn: QN, instances: Instances) = {
      val number = elementNumber(e)
      val changeMethod = "change("
      val js = JS()
        .line("  return ok;")
        .line("}")
        .line
        .line("$('%s').change( function() {", changeReference(e, instances))
        .line("  var ok = validate%sinstance%s();", number.toString, instances)
        .line("  showError('%s',ok);", getItemErrorId(number, instances))
        .line("});")
        .line
      if (e.minOccurs.intValue == 0 && e.default.isEmpty)
        js.line("//disable item-path due to minOccurs=0 and default is empty")
          .line("$('#%s').attr('enabled','false');", getPathId(number, instances))
      js.toString
    }

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
            case QN(xs, XsdDate.name) => Some("\\d\\d\\d\\d-\\d\\d-\\d\\d")
            //TODO why spaces on end of time?
            case QN(xs, XsdTime.name) => Some("\\d\\d:\\d\\d *")
            case QN(xs, XsdDateTime.name) => Some("\\d\\d\\d\\d-\\d\\d-\\d\\dT\\d\\d:\\d\\d")
            case _ => None
          }

        explicitPatterns ++ implicitPatterns
      }

    private def getInputType(r: Restriction) = {
      val qn = toQN(r)
      qn match {
        case QN(xs, XsdBoolean.name) => "checkbox"
        case _ => "text"
      }
    }

    private def insertMargin(s: String) =
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
        val js = JS()
          .line("  $('#%s').click( function() {", repeatButtonId)
          .line("    // loop through all repeats until find first nonInvisible repeat and make it visible")
          .line("    var elem;")
        repeatingEnclosingIds(e, instances)
          .foreach(id => {
            js.line("    elem = $('#%s');", id)
              .line("    if (!elemVisible(elem))")
              .line("      { elem.show(); return; }")
          })
        js
          .line("  })")
          .line

        addScript(js)
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
        e.minOccurs.intValue == 1 &&
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

    private def numInstances(e: ElementWrapper): Int = {

      val n = if (e.maxOccurs == "unbounded")
        NumInstancesForMultiple
      else
        e.maxOccurs.toInt

      getAnnotation(e, Annotation.MaxRepeats) match {
        case Some(m) => Math.min(n, m.toInt)
        case _ => n
      }
    }

    private def repeats(node: Node): Range = repeats(node.element)

    private def repeats(e: ElementWrapper): Range = 1 to numInstances(e)

    private def choiceContentId(idPrefix: String, number: Int, index: Int, instances: Instances) =
      idPrefix + "choice-content-" + number + InstanceDelimiter + instances + ChoiceIndexDelimiter + index
    private def getMinOccursZeroId(number: Int, instances: Instances) =
      TreeToHtmlConverter.getMinOccursZeroId(idPrefix, number, instances)
    private def getMinOccursZeroName(number: Int, instances: Instances) =
      TreeToHtmlConverter.getMinOccursZeroName(idPrefix, number, instances)
    private def getRepeatButtonId(number: Int, instances: Instances) =
      TreeToHtmlConverter.getRepeatButtonId(idPrefix, number, instances)
    private def getRemoveButtonId(number: Int, instances: Instances) =
      TreeToHtmlConverter.getRemoveButtonId(idPrefix, number, instances)
    private def getRepeatingEnclosingId(element: ElementWrapper, instances: Instances): String =
      TreeToHtmlConverter.getRepeatingEnclosingId(idPrefix, elementNumber(element), instances)
    private def getRepeatingEnclosingId(number: Int, instances: Instances) =
      TreeToHtmlConverter.getRepeatingEnclosingId(idPrefix, number, instances)
    private def getChoiceItemName(node: Node, instances: Instances): String =
      getChoiceItemName(elementNumber(node.element), instances)
    private def getChoiceItemName(number: Int, instances: Instances): String =
      TreeToHtmlConverter.getChoiceItemName(idPrefix, number, instances)
    private def getChoiceItemId(node: Node, index: Int, instances: Instances): String =
      getChoiceItemId(elementNumber(node.element), index, instances)
    private def getChoiceItemId(number: Int, index: Int, instances: Instances): String =
      TreeToHtmlConverter.getChoiceItemId(idPrefix, number, index, instances)
    private def getItemId(node: Node, instances: Instances): String =
      getItemId(elementNumber(node.element), instances)
    private def getItemId(element: ElementWrapper, instances: Instances): String =
      getItemId(elementNumber(element), instances)
    private def getItemId(number: Int, instances: Instances): String =
      TreeToHtmlConverter.getItemId(idPrefix, number, instances)
    private def getItemId(number: Int, enumeration: Integer, instances: Instances): String =
      getItemId(number, instances) + "-" + enumeration
    private def getItemName(number: Int, instances: Instances) =
      TreeToHtmlConverter.getItemName(idPrefix, number, instances)
    private def getItemEnclosingId(number: Int, instances: Instances) =
      idPrefix + "item-enclosing-" + number + InstanceDelimiter + instances
    private def getItemErrorId(number: Int, instances: Instances) =
      TreeToHtmlConverter.getItemErrorId(idPrefix, number, instances)
    private def getPathId(number: Int, instances: Instances) =
      idPrefix + "item-path-" + number + InstanceDelimiter + instances

    private def nextNumber: Int = {
      number += 1
      number
    }

    private case class QN(namespace: String, localPart: String)

    private def toQN(r: Restriction): QN = toQN(r.base.get)

    private implicit def toQN(qName: QName): QN =
      QN(qName.getNamespaceURI(), qName.getLocalPart())

    def template = io.Source.fromInputStream(getClass.getResourceAsStream("/template.html")).mkString

    def text =
      template
        .replace("//GENERATED_SCRIPT", script.toString)
        .replace("//EXTRA_SCRIPT", extraScript.mkString)
        .replace("<!--GENERATED_HTML-->", html.toString)

  }

  /**
   * **************************************************************
   *
   *   SchemaTraversor
   *
   *
   * **************************************************************
   */

  class SchemaTraversor(s: Schema, rootElement: Option[String], visitor: Visitor) {
    import Util._
    import XsdUtil._

    val extensionStack = new scala.collection.mutable.Stack[DataRecord[TypeDefParticleOption]]

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
      Set(XsdDecimal, XsdString, XsdInteger, XsdDate, XsdDateTime, XsdTime,
        XsdBoolean, XsdInt, XsdLong, XsdShort, XsdPositiveInteger,
        XsdNegativeInteger, XsdNonPositiveInteger, XsdNonNegativeInteger,
        XsdDouble, XsdFloat)
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
      val element =
        if (rootElement.isDefined)

          topLevelElements.find(
            _.name match {
              case Some(y) => y equals rootElement.get
              case None => false
            }).getOrElse(unexpected("did not find element " + rootElement.get))
        else if (topLevelElements.length == 0)
          unexpected("no top level elements specified in schema!")
        else
          topLevelElements(0)

      process(element)

    }

    private def process(e: Element) {
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

    private def process(e: Element, c: ComplexType) {
      c.complexTypeModelOption3.value match {
        case x: ComplexContent =>
          process(e, x)
        case x: SimpleContent =>
          unexpected
        case x: ComplexTypeModelSequence1 =>
          //sequence or choice
          process(e, x.typeDefParticleOption1.getOrElse(unexpected))
      }
    }

    private def process(e: Element, x: DataRecord[TypeDefParticleOption]) {
      x.value match {
        case y: GroupRef =>
          unexpected
        case y: ExplicitGroupable =>
          if (matches(x, qn("sequence")))
            process(e, Sequence(y))
          else if (matches(x, qn("choice")))
            process(e, Choice(y))
          else unexpected
        case _ => unexpected
      }
    }

    private def process(e: Element, cc: ComplexContent) {
      val q = toQName(cc.complexcontentoption)

      val value = cc.complexcontentoption.value
      println("cc " + q + "=" + value)
      if (qn("extension") == q)
        value match {
          case et: ExtensionType => {
            process(e, et)
          }
          case _ => unexpected()
        }
      else unexpected
    }

    private def process(e: Element, et: ExtensionType) {
      println("startExtension")
      //TODO 
      //resultant type is the eventual content type of the base (which could be a number of nested extensions)
      //if resultant type of et.base is sequence that add typeDefs into the sequence
      //if resultant type of et.base is choice then create sequence of et.base followed by typedefs
      //either way if will start with a sequence
      //if resultant type of et.base is simpleType then ?

      //the extension of the base type
      et.typeDefParticleOption3 match {
        case Some(typeDefParticleOption) => {
          extensionStack.push(typeDefParticleOption)
        }
        case _ => //do nothing
      }

      process(e, MyType(getType(et.base)))

      println("stopExtension")
    }

    private def process(e: Element, x: BaseType) {
      visitor.baseType(e, x)
    }

    private def process(e: Element, x: Sequence) {
      visitor.startSequence(e)
      val extensions = extensionStack.toList
      extensionStack.clear
      extensions.foreach(y => process(e, y))
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

    def div(id: Option[String] = None,
      classes: List[String] = List(), enabledAttr: Option[String] = None,
      content: Option[String] = None) =
      element(name = Div, id = id, classes = classes, enabledAttr = enabledAttr,
        content = content)

    def select(id: Option[String] = None, name: String,
      classes: List[String] = List(), content: Option[String] = None, number: Option[Int] = None) =
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
      name: String, number: Option[Int] = None,
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
      number: Option[Int] = None,
      typ: Option[String]) =
      element(name = Input, id = id, classes = classes, checked = checked,
        content = content, value = value, nameAttr = Some(name), typ = typ, numberAttr = number)

    private def classNames(classes: List[String]) =
      if (classes.length == 0)
        None
      else
        Some(classes.mkString(" "))

    private def element(name: String,
      id: Option[String] = None,
      classes: List[String] = List(),
      content: Option[String] = None,
      value: Option[String] = None,
      checked: Option[Boolean] = None,
      nameAttr: Option[String] = None,
      enabledAttr: Option[String] = None,
      forAttr: Option[String] = None,
      numberAttr: Option[Int] = None,
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
          numberAttr.map(x => ("number", x.toString))
      elementBase(name, attributes.toMap, content)
      this
    }

    private def elementBase(name: String, attributes: Map[String, String],
      content: Option[String]): Html = {
      stack.push(HtmlElement(name, content.isDefined))
      indent
      val attributesClause =
        attributes.map(x => x._1 + "=\"" + x._2 + "\"").mkString(" ")
      append("<" + name + " "
        + attributesClause + ">" + content.mkString)
      this
    }

    private def indent {
      val indent = "  " * stack.size
      append("\n")
      append(indent)
    }

    private def append(str: String) = {
      s.append(str)
      this
    }

    def closeTag: Html = {
      if (stack.isEmpty) throw new RuntimeException("closeTag called on empty html stack! html so far=\n" + s.toString)
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

    override def toString = s.toString

  }
}

