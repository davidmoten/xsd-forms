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
    def qn(namespaceUri: String, localPart: String) = new QName(namespaceUri, localPart)
    def qn(localPart: String): QName = new QName(xs, localPart)
    val xs = "http://www.w3.org/2001/XMLSchema"
    val appInfoSchema = "http://moten.david.org/xsd-forms"
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
    val element: Element
  }
  trait NodeGroup extends Node {
    val children: MutableList[Node] = MutableList()
  }
  case class NodeSequence(element: Element, override val children: MutableList[Node]) extends NodeGroup
  case class NodeChoice(element: Element, choice: Choice, override val children: MutableList[Node]) extends NodeGroup
  case class NodeSimpleType(element: Element, typ: SimpleType) extends Node
  case class NodeBaseType(element: Element, typ: BaseType) extends Node

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
    private val stack = new scala.collection.mutable.Stack[Node]

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
   *   TreeToHtmlConverter
   *
   *
   * **************************************************************
   */

  object TreeToHtmlConverter {

    val instanceDelimiter = "-instance-"
    val choiceIndexDelimiter = "-choice-"

    def getItemId(idPrefix: String, number: String, instanceNo: Int) =
      idPrefix + "item-" + number + instanceDelimiter + instanceNo

    def getItemErrorId(idPrefix: String, number: String, instanceNo: Int) =
      idPrefix + "item-error-" + number + instanceDelimiter + instanceNo

    def getChoiceItemId(idPrefix: String, number: String, index: Int, instanceNo: Int): String = getItemId(idPrefix, number, instanceNo) + choiceIndexDelimiter + index

    def getChoiceItemName(idPrefix: String, number: String, instanceNo: Int) = idPrefix + "item-input-" + number + instanceDelimiter + instanceNo
  }

  class TreeToHtmlConverter(targetNamespace: String, idPrefix: String, extraScript: Option[String], tree: Node) {
    import TreeToHtmlConverter._
    import XsdUtil._
    import Util._
    private val script = new StringBuilder

    private var number = 0
    val margin = "  "

    private sealed trait Entry
    private sealed trait StackEntry
    private val html = new Html

    private val NumInstancesForMultiple = 3

    import scala.collection.mutable.HashMap
    private val elementNumbers = new HashMap[Element, String]()

    //assign element numbers so that order of display on page 
    //will match order of element numbers. To do this must 
    //traverse children before siblings
    assignElementNumbers(tree)

    //process the abstract syntax tree
    doNode(tree)

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

    private def doNode(node: Node) {
      node match {
        case n: NodeSimpleType => doNode(n)
        case n: NodeBaseType => doNode(n)
        case n: NodeSequence => doNode(n)
        case n: NodeChoice => doNode(n)
        case _ => Util.unexpected
      }
    }

    private def doNode(node: NodeSimpleType) {
      val e = node.element
      val typ = node.typ

      nonRepeatingSimpleType(e)
      val t = Some(typ)
      val number = elementNumber(e)
      for (instanceNo <- instances(e)) {
        repeatingEnclosing(e)
        itemTitle(e)
        itemBefore(e)
        html.div(classes = List("item-number"), content = Some(number)).closeTag
          .label(forInputName = getItemName(number),
            classes = List("item-label"), content = Some(getLabel(e, t))).closeTag
          .div(classes = List("item-input"))

        typ.simpleDerivationOption3.value match {
          case x: Restriction => simpleType(e, x, instanceNo)
          case _ => Util.unexpected
        }
        html
          .closeTag(3)
      }

      addXmlExtractScriplet(node)
    }

    private def doNode(node: NodeBaseType) {
      val e = node.element
      val typ = node.typ
      nonRepeatingSimpleType(e)
      val t = None
      val number = elementNumber(e)
      for (instanceNo <- instances(e)) {
        repeatingEnclosing(e)
        itemTitle(e)
        itemBefore(e)
        html.div(classes = List("item-number"), content = Some(number)).closeTag
          .label(forInputName = getItemName(number),
            classes = List("item-label"), content = Some(getLabel(e, t))).closeTag
          .div(classes = List("item-input"))
        simpleType(e, new MyRestriction(typ.qName), instanceNo)
        html
          .closeTag(2)
      }
      html.closeTag

      addXmlExtractScriplet(node)
    }

    private def doNodes(nodes: MutableList[Node]) {
      nodes.foreach(doNode(_))
    }

    private def numInstances(e: Element): Int =
      if (isMultiple(e)) NumInstancesForMultiple
      else 1

    private def instances(node: Node): Range = instances(node.element)

    private def instances(e: Element): Range = 1 to numInstances(e)

    private def numInstances(node: Node): Int =
      numInstances(node.element)

    private def doNode(node: NodeSequence) {
      val e = node.element
      val number = elementNumber(node)
      val legend = getAnnotation(e, "legend")
      val usesFieldset = legend.isDefined

      val label = getAnnotation(e, "label").mkString

      html
        .div(classes = List("sequence"))
      nonRepeatingTitle(e, e.minOccurs.intValue() == 0 || e.maxOccurs != "1")
      for (instanceNo <- instances(e)) {
        repeatingEnclosing(e)
        addMaxOccursScriptlet(e)
        html.div(classes = List("sequence-label"), content = Some(label))
          .closeTag
          .div(id = Some(idPrefix + "sequence-" + number + "-instance-" + instanceNo),
            classes = List("sequence-content"))
        if (usesFieldset)
          html.fieldset(legend = legend, classes = List("fieldset"), id = Some(idPrefix + "fieldset-" + number + "-instance-" + instanceNo))

        doNodes(node.children)

        if (usesFieldset)
          html.closeTag
        html.closeTag
      }
      html
        .closeTag(2)

      addXmlExtractScriplet(node)

    }

    private def doNode(node: NodeChoice) {
      val choice = node.choice
      val e = node.element
      val choiceInline = displayChoiceInline(choice)

      val number = elementNumber(e)

      html.div(id = Some(getItemEnclosingId(number)), classes = List("choice"))
      nonRepeatingTitle(e, e.minOccurs.intValue() == 0 || e.maxOccurs != "1")
      for (instanceNo <- instances(e)) {
        repeatingEnclosing(e)
        addMaxOccursScriptlet(e)
        val particles = choice.group.particleOption3.map(_.value)
        addChoiceHideOnStartScriptlet(particles, number, instanceNo)
        addChoiceShowHideOnSelectionScriptlet(particles, number, instanceNo)

        html.div(
          classes = List("choice-label"),
          content = Some(getAnnotation(choice.group, "label").mkString))
          .closeTag

        val forEachParticle = particles.zipWithIndex.foreach _

        forEachParticle(x => {
          val particle = x._1
          val index = x._2 + 1
          html.div(
            id = Some(idPrefix + "div-choice-item-" + number + instanceDelimiter + instanceNo + choiceIndexDelimiter + index),
            classes = List("div-choice-item"))
          html.input(
            id = Some(getChoiceItemId(number, index, instanceNo)),
            name = getChoiceItemName(number, instanceNo),
            classes = List("choice-item"),
            typ = Some("radio"),
            value = Some("number"),
            content = Some(getChoiceLabel(e, particle)),
            number = Some(number))
          html.closeTag(2)
        })

        node.children.zipWithIndex.foreach {
          case (n, index) => {
            html.div(id = Some(choiceContentId(idPrefix, number, (index + 1), instanceNo)), classes = List("invisible"))
            doNode(n)
            html.closeTag
          }
        }

        html.closeTag
      }
      html.closeTag

      addXmlExtractScriplet(node)

    }

    def choiceContentId(idPrefix: String, number: String, index: Int, instanceNo: Int) =
      idPrefix + "choice-content-" + number + instanceDelimiter + instanceNo + choiceIndexDelimiter + index

    private def refById(id: String) = "$(\"#" + id + "\")"
    private def valById(id: String) = "encodeHTML(" + refById(id) + ".val())"
    private def namespace(node: Node) =
      if (elementNumber(node.element).equals("1"))
        " xmlns=\"" + targetNamespace + "\""
      else
        ""
    private def xmlStart(node: Node) =
      "'<" + node.element.name.getOrElse("?") + namespace(node) + ">'"

    private def xmlEnd(node: Node) =
      "'</" + node.element.name.getOrElse("?") + ">'"
    private def xml(node: Node, value: String) = {
      xmlStart(node) + " + " +
        value +
        " + \"</" + node.element.name.getOrElse("?") + ">\""
    }

    private def addXmlExtractScriplet(node: NodeSimpleType) {
      //TODO use instanceNo
      addXmlExtractScriptlet(node, "|    return " + xml(node, valById(getItemId(node, 1))));
    }

    private def addXmlExtractScriplet(node: NodeBaseType) {
      //TODO use instanceNo
      addXmlExtractScriptlet(node, "|    return " + xml(node, valById(getItemId(node, 1))));
    }

    private def addXmlExtractScriplet(node: NodeSequence) {
      val s = new StringBuilder
      s.append("""
 |    var xml = """ + xmlStart(node) + """ + "\n"; 
 |    //now add sequence children for each instanceNo""")
      for (instanceNo <- instances(node))
        node.children.foreach { n =>
          s.append("""
 |    xml += """ + xmlFunctionName(n, Some(instanceNo)) + "() + \"\\n\";");
        }
      s.append("""
 |    xml+="""" + xmlEnd(node) + """>";
 |    return xml;""")
      addXmlExtractScriptlet(node, s.toString());
    }

    case class JSBuilder(indent: Int = 0) {
      val content = new StringBuilder

      def apply(items: String*): JSBuilder = {
        content append " " * 3
        for (item <- items) content append item
        content append "\n"
        this
      }

      def newLine(): JSBuilder = {
        content append "\n"
        this
      }
    }

    private def addXmlExtractScriplet(node: NodeSequence, instanceNo: Int) {
      val s = new StringBuilder
      s.append("""
 |    var xml = """ + xmlStart(node) + """ + "\n"; 
 |    //now add sequence children""")
      node.children.foreach { n =>
        s.append("""
 |    //TODO if instanceNo enabled
 |    xml += """ + xmlFunctionName(n, Some(instanceNo)) + "() + \"\\n\";");
      }
      s.append("""
 |    xml+="""" + xmlEnd(node) + """>";
 |""")
      addXmlExtractScriptlet(node, s.toString(), Some(instanceNo));
    }

    private def addXmlExtractScriplet(node: NodeChoice) {
      val s = new StringBuilder
      s.append("""
 |    var xml = """ + xmlStart(node) + """ + "\n"; 
 |    //now optionally add selected child if any""");
      for (instanceNo <- instances(node)) {
        s.append(""" 
 |    var checked = $(':input[name=""" + getChoiceItemName(node, instanceNo) + """]:checked').attr("id");
 """)

        node.children.zipWithIndex.foreach {
          case (n, index) =>
            s.append("""
 |    if (checked == """" + getChoiceItemId(node, index + 1, instanceNo) + """") xml += """ + xmlFunctionName(n) + "() + \"\\n\";");
        }
        s.append("""
 |    xml+="</""" + node.element.name.get + """>";
 |    return xml;""")
        addXmlExtractScriptlet(node, s.toString(), Some(instanceNo));
      }
    }

    private def xmlFunctionName(node: Node) = {
      val number = elementNumber(node.element)
      "getXml" + number
    }

    private def xmlFunctionName(node: Node, instanceNo: Option[Int] = None) = {
      val number = elementNumber(node.element)
      "getXml" + number + (if (instanceNo.isDefined) "instance" + instanceNo.get else "")
    }

    private def addXmlExtractScriptlet(node: Node, functionBody: String, instanceNo: Option[Int] = None) {
      val functionName = xmlFunctionName(node)
      addScriptWithMargin(
        """
|//extract xml from element <""" + node.element.name.getOrElse("?") + """>
|function """ + functionName + """() {
""" + functionBody + """
|}
| """)
    }

    override def toString = text

    private def addScript(s: String) {
      script.append(s)
      script.append("\n")
    }

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

function openTag(name) {
	return "<" + name + ">";
}
          
function openTagWithNs(name, namespace) {
    return "<" + name + " xmlns=\"""" + targetNamespace + """\">";
}

function closeTag(name) {
	return "</" + name + ">";
}

function getStartAt(previousItems, items) {
	if (previousItems == null) return 0;

	var startAt = 0;

        for (var i=0; i<Math.min(previousItems.length,items.length); i++) {
    		  if (!(items[i]==previousItems[i])) 
    			  return startAt;
    		  else 
    			  startAt = i+1;
    	}
	return startAt;
}

function closePreviousItems(previousItems,startAt,s) {
     if (previousItems!=null) {
          for (var i=previousItems.length-2;i>=startAt;i--) {
              s = s + "\n" + spaces(i*2) + closeTag(previousItems[i]);
          }
      }
      return s;
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
    
var repeatCount = 10000;
    
// required to hold structure with repeats
var tree = {};

$(function() {
  $('input').filter('.datepickerclass').datepicker();
  $('input').filter('.datepickerclass').datepicker( "option", "dateFormat","dd/mm/yy");
  $('input').filter('.datetimepickerclass').datetimepicker();
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
      if (elem.is(":visible")) 
        elem.change();
    });
    var count = $('.item-error').filter(":visible").length
    if (count>0) {
      $('#validation-errors').show();
      return;
    }
    else 
      $('#validation-errors').hide();
    var s = getXml1();
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

    private def getChoiceItemName(node: Node, instanceNo: Int): String = getChoiceItemName(elementNumber(node.element), instanceNo)
    private def getChoiceItemName(number: String, instanceNo: Int): String = TreeToHtmlConverter.getChoiceItemName(idPrefix, number, instanceNo)
    private def getChoiceItemId(node: Node, index: Int, instanceNo: Int): String = getChoiceItemId(elementNumber(node.element), index, instanceNo)
    private def getChoiceItemId(number: String, index: Int, instanceNo: Int): String = TreeToHtmlConverter.getChoiceItemId(idPrefix, number, index, instanceNo)

    private def displayChoiceInline(choice: Choice) =
      "inline" == getAnnotation(choice.group, "choice").mkString

    private def addChoiceHideOnStartScriptlet(
      particles: Seq[ParticleOption], number: String, instanceNo: Int) {

      val forEachParticle = particles.zipWithIndex.foreach _

      forEachParticle(x => {
        val index = x._2 + 1
        addScriptWithMargin("""
|$("#""" + choiceContentId(idPrefix, number, index, instanceNo) + """").hide();""")
      })
    }

    private def addChoiceShowHideOnSelectionScriptlet(
      particles: Seq[ParticleOption], number: String, instanceNo: Int) {

      val forEachParticle = particles.zipWithIndex.foreach _

      val choiceChangeFunction = "choiceChange" + number + "instance" + instanceNo;

      addScriptWithMargin(
        """
|var """ + choiceChangeFunction + """ = function addChoiceChange""" + number + """instance""" + instanceNo + """() {
|  $(":input[@name='""" + getChoiceItemName(number, instanceNo) + """']").change(function() {
|    var checked = $(':input[name=""" + getChoiceItemName(number, instanceNo) + """]:checked').attr("id");""")

      forEachParticle(x => {
        val index = x._2 + 1
        val ccId =
          choiceContentId(idPrefix, number, index, instanceNo)
        addScriptWithMargin(
          """
|    if (checked == """" + getChoiceItemId(number, index, instanceNo) + """") {
|      $("#""" + ccId + """").show();
|      $("#""" + ccId + """").find('.item-path').attr('enabled','true');
|    }
|    else {
|      $("#""" + ccId + """").hide();
|      $("#""" + ccId + """").find('.item-path').attr('enabled','false');
|    }""")
      })
      addScriptWithMargin(
        """
|  })
|}
|
|""" + choiceChangeFunction + """();""")

    }

    private def getChoiceLabel(e: Element, p: ParticleOption): String = {
      val labels =
        p match {
          case x: Element => {
            getAnnotation(x, "choiceLabel") ++ getAnnotation(x, "label") ++ Some(getLabel(x, None))
          }
          case _ => unexpected
        }
      labels.head
    }

    private def getLabel(e: Element, p: ParticleOption): String =
      p match {
        case x: Element => getLabel(x, None)
        case _ => getLabel(e, None)
      }

    private class MyRestriction(qName: QName)
      extends Restriction(None, SimpleRestrictionModelSequence(), None, Some(qName), Map())

    private def getVisibility(e: Element) =
      getAnnotation(e, "visible") match {
        case Some("false") => Some("invisible")
        case _ => None
      }

    private def nonRepeatingTitle(e: Element, hasButton: Boolean) {
      val number = elementNumber(e)
      html.div(
        classes = List("non-repeating-title"),
        content = getAnnotation(e, "nonRepeatingTitle")).closeTag
      if (hasButton)
        html.div(
          id = Some(getRepeatButtonId(number)),
          classes = List("repeat-button", "white", "small"),
          content = Some(getAnnotation(e, "repeatLabel").getOrElse("+"))).closeTag
    }

    private def getRepeatButtonId(number: String) =
      idPrefix + "repeat-button-" + number

    private def getRepeatingEnclosingId(number: String) =
      idPrefix + "repeating-enclosing-" + number

    private def repeatingEnclosing(e: Element) {
      val number = elementNumber(e)
      html.div(
        id = Some(getRepeatingEnclosingId(number)),
        classes = List("repeating-enclosing"))
    }

    private def nonRepeatingSimpleType(e: Element) {
      val number = elementNumber(e)
      html
        .div(
          classes = List("item-enclosing") ++ getVisibility(e),
          id = Some(getItemEnclosingId(number)))
      nonRepeatingTitle(e, e.maxOccurs != "0" && e.maxOccurs != "1")
    }

    private def itemTitle(e: Element) {
      getAnnotation(e, "title") match {
        case Some(x) => html.div(classes = List("item-title"), content = Some(x)).closeTag
        case _ =>
      }
    }

    private def itemBefore(e: Element) {
      getAnnotation(e, "before") match {
        case Some(x) => html.div(classes = List("item-before"), content = Some(x)).closeTag
        case _ =>
      }
    }

    private def elementNumber(node: Node): String = elementNumber(node.element)

    private def elementNumber(e: Element): String = {
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
      getAnnotation(e, "text")

    private def nextNumber: String = {
      number += 1
      number + ""
    }

    case class QN(namespace: String, localPart: String)

    implicit def toQN(qName: QName) =
      QN(qName.getNamespaceURI(), qName.getLocalPart())

    private def getItemId(node: Node, instanceNo: Int): String = getItemId(elementNumber(node.element), instanceNo)
    private def getItemId(element: Element, instanceNo: Int): String = getItemId(elementNumber(element), instanceNo)
    private def getItemId(number: String, instanceNo: Int): String = TreeToHtmlConverter.getItemId(idPrefix, number, instanceNo)
    private def getItemId(number: String, enumeration: Integer, instanceNo: Int): String = getItemId(number, instanceNo) + "-" + enumeration
    private def getItemName(number: String) =
      idPrefix + "item-input-" + number;

    private def getItemEnclosingId(number: String) =
      idPrefix + "item-enclosing-" + number

    private def getItemErrorId(number: String, instanceNo: Int) =
      TreeToHtmlConverter.getItemErrorId(idPrefix, number, instanceNo)

    private def getPathId(number: String, instanceNo: Int) = idPrefix + "item-path-" + number + instanceDelimiter + instanceNo

    private def simpleType(e: Element, r: Restriction, instanceNo: Int) {
      val qn = toQN(r.base.get)

      //TODO use instanceNo
      addInput(e, qn, r, instanceNo)

      addMaxOccursScriptlet(e)

      addDescription(e)

      addPath(e, instanceNo)

      addError(e, instanceNo)

      addHelp(e)

      addAfter(e)

      val statements = List(
        createDeclarationScriptlet(e, qn, instanceNo),
        createMandatoryTestScriptlet(e, r),
        createPatternsTestScriptlet(getPatterns(r)),
        createBasePatternTestScriptlet(qn),
        createFacetTestScriptlet(r),
        createLengthTestScriptlet(r),
        createCanExcludeScriptlet(e),
        createClosingScriptlet(e, qn, instanceNo))

      statements
        .map(stripMargin(_))
        .foreach(x => if (x.length > 0) addScript(x))

    }

    private def addInput(e: Element, qn: QN, r: Restriction, instanceNo: Int) {

      val number = elementNumber(e)

      if (isEnumeration(r))
        addEnumeration(e, r, instanceNo)
      else
        addTextField(e, r, getExtraClasses(qn), instanceNo)

      addWidthScript(e, instanceNo)

      addCssScript(e, instanceNo)
    }

    private def getExtraClasses(qn: QN) = qn match {
      case QN(xs, "date") => "datepickerclass "
      case QN(xs, "datetime") => "datetimepickerclass "
      case QN(xs, "time") => "timepickerclass "
      case _ => ""
    }

    private def addTextField(
      e: Element, r: Restriction,
      extraClasses: String, instanceNo: Int) {
      val number = elementNumber(e)
      val inputType = getInputType(r)
      val itemId = getItemId(number, instanceNo)
      getTextType(e) match {
        case Some("textarea") =>
          html.textarea(
            id = Some(itemId),
            name = getItemName(number),
            classes = List(extraClasses, "item-input-textarea"),
            content = Some(e.default.mkString),
            number = Some(number))
            .closeTag
        case _ =>
          //text or boolean
          val checked = e.default match {
            case Some("true") => Some(true)
            case _ => None
          }
          html.input(
            id = Some(itemId),
            name = getItemName(number),
            classes = List(extraClasses, "item-input-text"),
            typ = Some(inputType),
            checked = checked,
            value = e.default,
            number = Some(number))
            .closeTag
      }
    }

    private def addWidthScript(e: Element, instanceNo: Int) {
      val itemId = getItemId(e, instanceNo)
      getAnnotation(e, "width") match {
        case Some(x) =>
          addScriptWithMargin("|  $('#" + itemId + "').width('" + x + "');")
        case None =>
      }
    }

    private def addCssScript(e: Element, instanceNo: Int) {
      val itemId = getItemId(e, instanceNo)
      getAnnotation(e, "css") match {
        case Some(x) => {
          val items = x.split(';')
            .foreach(
              y => {
                val pair = y.split(':')
                if (pair.size != 2)
                  unexpected("css properties incorrect syntax\n" + pair)
                addScriptWithMargin(
                  "|  $('#" + itemId + "').css('" + pair(0) + "','" + pair(1) + "');")
              })
        }
        case None =>
      }
    }

    private def isEnumeration(r: Restriction) =
      !getEnumeration(r).isEmpty

    private def addEnumeration(e: Element, r: Restriction, instanceNo: Int) {
      val number = elementNumber(e)
      val en = getEnumeration(r)
      val isRadio = getAnnotation(e, "selector") match {
        case Some("radio") => true
        case _ => false
      }

      val initializeBlank = getAnnotation(e, "addBlank") match {
        case Some("true") => true
        case _ => false
      }
      enumeration(en, number, isRadio, initializeBlank, instanceNo)
    }

    private def getEnumeration(typ: SimpleType): Seq[(String, NoFixedFacet)] =
      typ.simpleDerivationOption3.value match {
        case x: Restriction =>
          getEnumeration(x)
        case _ => unexpected
      }

    private def getEnumeration(r: Restriction): Seq[(String, NoFixedFacet)] =
      r.simpleRestrictionModelSequence3.facetsOption2.seq.map(
        _.value match {
          case y: NoFixedFacet => {
            val label = getAnnotation(y, "label") match {
              case Some(x) => x
              case None => y.valueAttribute
            }
            Some((label, y))
          }
          case _ => None
        }).flatten

    private def enumeration(en: Seq[(String, NoFixedFacet)],
      number: String, isRadio: Boolean, initializeBlank: Boolean, instanceNo: Int) {
      if (isRadio) {
        en.zipWithIndex.foreach(x => {
          html.input(
            id = Some(getItemId(number, x._2, instanceNo)),
            name = getItemName(number),
            classes = List("select"),
            typ = Some("radio"),
            value = Some(x._1._1),
            content = Some(x._1._1),
            number = Some(number)).closeTag
        })
      } else {
        html.select(
          id = Some(getItemId(number, instanceNo)),
          name = getItemName(number),
          classes = List("select"),
          number = Some(number))
        if (initializeBlank)
          html.option(content = Some("Select one..."), value = "").closeTag
        en.foreach { x =>
          html.option(content = Some(x._1), value = x._2.valueAttribute).closeTag
          getAnnotation(x._2, "makeVisible") match {
            case Some(y: String) => {
              val refersTo = number.toInt + y.toInt
              addScriptWithMargin("""
|  $("#""" + getItemId(number, instanceNo) + """").change( function() {
|    var v = $("#""" + getItemId(number, instanceNo) + """");
|    var refersTo = $("#""" + getItemEnclosingId(refersTo + "") + """") 
|    if ("""" + x._2.valueAttribute + """" == v.val()) 
|      refersTo.show();
|    else
|      refersTo.hide();
|  })
""")
            }
            case _ =>
          }
        }
        html.closeTag
      }
    }

    private def addDescription(e: Element) {
      getAnnotation(e, "description") match {
        case Some(x) =>
          html.div(
            classes = List("item-description"),
            content = Some(x))
            .closeTag
        case None =>
      }
    }

    private def addError(e: Element, instanceNo: Int) {
      val itemErrorId = getItemErrorId(elementNumber(e), instanceNo)
      html.div(
        id = Some(itemErrorId),
        classes = List("item-error"),
        content = Some(getAnnotation(e, "validation").getOrElse("Invalid")))
        .closeTag

    }

    private def addPath(e: Element, instanceNo: Int) {
      html.div(
        classes = List("item-path"),
        id = Some(getPathId(elementNumber(e), instanceNo)),
        enabledAttr = Some("true"),
        content = Some(""))
        .closeTag
    }

    private def addHelp(e: Element) {
      getAnnotation(e, "help") match {
        case Some(x) =>
          html.div(classes = List("item-help"), content = Some(x)).closeTag
        case None =>
      }
    }

    private def addAfter(e: Element) {
      getAnnotation(e, "after") match {
        case Some(x) =>
          html.div(classes = List("item-after"), content = Some(x)).closeTag
        case None =>
      }
    }

    private def getBasePattern(qn: QN) = {
      qn match {
        case QN(xs, "decimal") => Some("\\d+(\\.\\d*)?")
        case QN(xs, "integer") => Some("\\d+")
        case _ => None
      }
    }

    private def createDeclarationScriptlet(e: Element, qn: QN, instanceNo: Int) = {
      val number = elementNumber(e)
      val itemId = getItemId(number, instanceNo)
      """
|// """ + e.name.get + """
|var validate""" + number + "instance" + instanceNo + """= function () {
|  var ok = true;
|  var v = $("#""" + itemId + """");
|  var pathDiv = $("#""" + getPathId(number, instanceNo) + """");"""
    }

    private def createMandatoryTestScriptlet(e: Element, r: Restriction) = {
      if (isMandatory(e, r))
        """
|  // mandatory test
|  if ((v.val() == null) || (v.val().length==0))
|    ok=false;"""
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

    private def createClosingScriptlet(e: Element, qn: QN, instanceNo: Int) = {
      val number = elementNumber(e)
      val onChange = "change("
      val changeMethod = qn match {
        case QN(xs, "date") => onChange
        case QN(xs, "datetime") => onChange
        case QN(xs, "time") => onChange
        case _ => onChange
      };
      """
|  return ok;
|}
|      
|$("#""" + getItemId(number, instanceNo) + """").""" + changeMethod + """ function() {
|  var ok = validate""" + number + "instance" + instanceNo + """();
|  var error= $("#""" + getItemErrorId(number, instanceNo) + """");
|  if (!(ok)) 
|    error.show();
|  else 
|    error.hide();
|})
""" + (if (e.minOccurs == 0 && e.default.isEmpty)
        """
|//disable item-path due to minOccurs=0 and default is empty  
|$("#""" + getPathId(number, instanceNo) + """").attr('enabled','false');"""
      else "")
    }

    private def addScriptWithMargin(s: String) = addScript(stripMargin(s))

    private def getPatterns(r: Restriction) =
      r.simpleRestrictionModelSequence3.facetsOption2.seq.flatMap(f => {
        f match {
          case DataRecord(xs, Some("pattern"), x: Pattern) => Some(x.valueAttribute)
          case _ => None
        }
      })

    private def getInputType(r: Restriction) = {
      val qn = toQN(r.base.get)
      qn match {
        case QN(xs, "boolean") => "checkbox"
        case _ => "text"
      }
    }

    private def stripMargin(s: String) =
      s.stripMargin.replaceAll("\n", "\n" + margin)

    private def isMultiple(node: Node): Boolean =
      isMultiple(node.element)

    private def isMultiple(e: Element): Boolean =
      (e.maxOccurs == "unbounded" || e.maxOccurs.toInt > 1)

    private def addMaxOccursScriptlet(e: Element) {
      val number = elementNumber(e)
      if (isMultiple(e)) {
        val repeatButtonId = getRepeatButtonId(number)
        val enclosingId = getRepeatingEnclosingId(number)

        addScriptWithMargin("""

|            
|$("#""" + repeatButtonId + """").click(function() {
|   //TODO
|})
        """)
      }
    }

    private def getAnnotation(e: Annotatedable, key: String): Option[String] =
      e.annotation match {
        case Some(x) =>
          x.attributes.get("@{" + appInfoSchema + "}" + key) match {
            case Some(y) => Some(y.value.toString)
            case None => None
          }
        case None => None
      }

    private def getLabel(e: Element, typ: Option[SimpleType]) =
      {
        val name = getLabelFromName(e)
        val label = getAnnotation(e, "label") match {
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

  }

  /**
   * **************************************************************
   *
   *   Traversor
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
      Set("decimal", "string", "integer", "date", "dateTime", "time", "boolean")
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

  private class Html {
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
      element(name = "div", id = id, classes = classes, enabledAttr = enabledAttr,
        content = content)

    def select(id: Option[String] = None, name: String,
      classes: List[String] = List(), content: Option[String] = None, number: Option[String] = None) =
      element(name = "select", id = id, classes = classes, nameAttr = Some(name), numberAttr = number,
        content = content)

    def option(id: Option[String] = None,
      classes: List[String] = List(),
      value: String,
      content: Option[String] = None) =
      element(name = "option", id = id, classes = classes,
        content = content, value = Some(value))

    def label(forInputName: String, id: Option[String] = None,
      classes: List[String] = List(), content: Option[String] = None) =
      element(
        name = "label",
        id = id,
        forAttr = Some(forInputName),
        classes = classes,
        content = content)

    def fieldset(
      legend: Option[String] = None,
      classes: List[String] = List(),
      id: Option[String]) = {
      element(name = "fieldset", classes = classes, id = id)
      legend match {
        case Some(x) => element(name = "legend", content = Some(x)).closeTag
        case None =>
      }
    }

    def textarea(id: Option[String] = None,
      classes: List[String] = List(),
      content: Option[String] = None,
      value: Option[String] = None,
      name: String, number: Option[String] = None,
      closed: Boolean = false) =
      element(name = "textarea",
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
      element(name = "input", id = id, classes = classes, checked = checked,
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
        id.map(("id", _)) ++
          classNames(classes).map(("class" -> _)) ++
          value.map(("value", _)) ++
          nameAttr.map(("name", _)) ++
          enabledAttr.map(("enabled", _)) ++
          forAttr.map(("for", _)) ++
          typ.map(("type", _)) ++
          checked.map(x => ("checked", x.toString)) ++
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

package js {

  case class Function() {
    def apply(name: String) = NamedFunction(name)
  }

  case class NamedFunction(name: String) {
    def apply(js: JS): JS = JS("function " + name + "() {\n" + js + "\n}\n\n")
  }

  case class Expression(s: String)

  trait Statement

  case class Assignment extends Statement

  object JS {
    implicit def toJS(s: String): JS = JS(s);
    implicit def toJS(i: Int): JS = JS(i.toString)
    implicit def toJS(s: Symbol): JS = JS(s.toString)
    val function = Function()

    def test {
      val js: JS =
        function("hello")(
          'x := 'y + 'z --
            'a := 'a + 1 --
            'a := 'a + 2)
    }

  }

  case class JS(val initialValue: String = "") {

    val content: StringBuffer = new StringBuffer(initialValue)

    def fn(name: String)(body: JS): JS = {
      this
    }

    def +(js: JS): JS = {
      content append " + "
      append(js)
    }

    def append(s: String) = {
      content append s
      this
    }

    def newLine(js: JS): JS = {
      content append "\n"
      append(js)
    }

    def declare(js: JS): JS = {
      content append "\nvar "
      append(js)
    }

    def :=(js: JS): JS = {
      content append " = "
      append(js)
    }

    def --(js: JS): JS = {
      newLine(js)
    }

    def apply(s: String, js: JS) = {
      content append s
      append(js)
    }

    def append(js: JS) = {
      content append js.content.toString
      this
    }

    def apply(js: JS) = append(js)

    def start(js: JS) = {
      content append " {\n"
      this
    }

    override def toString = content.toString + "\n"

  }

}
