package com.github.davidmoten.xsdforms.tree

//  import xsd._
import javax.xml.namespace.QName
import scalaxb._
import com.github.davidmoten.xsdforms.presentation._
import Css._
import com.github.davidmoten.xsdforms.html._

/**
 * **************************************************************
 *
 *   TreeToHtmlConverter class
 *
 *
 * **************************************************************
 */

class TreeToHtmlConverter(options: Options,
  configuration: Option[Configuration], tree: Node) {

  import xsd.Element
  import xsd.Restriction
  import xsd.ParticleOption
  import xsd.NoFixedFacet
  import xsd.SimpleRestrictionModelSequence
  import xsd.NumFacet
  import xsd.Pattern
  import xsd.Annotatedable
  import xsd.SimpleType
  import xsd.Facet
  import Ids.ChoiceIndexDelimiter
  import Ids.InstanceDelimiter

  import XsdUtil._
  import Util._
  import TreeUtil._
  import ElementWrapper._
  import Ids.getPathId
  import Ids.getItemEnclosingId
  import Ids.getMinOccursZeroId
  import Ids.getMinOccursZeroName
  import Ids.getRepeatButtonId
  import Ids.getRemoveButtonId
  import Ids.{getChoiceItemName,getChoiceItemId,getItemId,getItemName,choiceContentId}

  private implicit val idPrefix = options.idPrefix
  private val html = new Html

  private val Margin = "  "
  private val Plus = " + "

  private sealed trait Entry
  private sealed trait StackEntry

  //assign element numbers so that order of display on page 
  //will match order of element numbers. To do this must 
  //traverse children left to right before siblings
  private val elementNumbers = new ElementNumbersAssigner(tree).assignments

  implicit def toElementWithNumber(element: ElementWrapper): ElementWithNumber = ElementWithNumber(element, elementNumber(element))
  implicit def toElementWithNumber(node: Node): ElementWithNumber = toElementWithNumber(node.element)

  //process the abstract syntax tree
  doNode(tree, new Instances)

  addXmlExtractScriptlet(tree, new Instances)

  private def doNode(node: Node, instances: Instances) {
    node match {
      case n: NodeBasic => doNode(n, instances)
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
    val number = node.number
    val legend = e.get(Annotation.Legend)
    val usesFieldset = legend.isDefined

    val label = e.get(Annotation.Label)

    html
      .div(id = Some(getItemEnclosingId(number, instances add 1)),
        classes = List(ClassSequence) ++ e.visibility)
    nonRepeatingTitle(e, instances)
    minOccursZeroCheckbox(e, instances)
    repeatButton(e, instances)
    for (instanceNo <- e.repeats) {
      val instNos = instances add instanceNo
      repeatingEnclosing(e, instNos)
      if (label.isDefined)
        html
          .div(classes = List(ClassSequenceLabel), content = label).closeTag
      html.div(id = Some(idPrefix + "sequence-" + number + InstanceDelimiter + instanceNo),
        classes = List(ClassSequenceContent))
      addRemoveButton(e, instNos)
      if (usesFieldset)
        html.fieldset(legend = legend, classes = List(ClassFieldset), id =
          Some(idPrefix + "fieldset-" + number + InstanceDelimiter + instanceNo))

      node.children.foreach(x => doNode(x, instNos))

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

    val number = e.number

    html.div(id = Some(getItemEnclosingId(number, instances add 1)),
      classes = List(ClassChoice) ++ e.visibility)
    nonRepeatingTitle(e, instances)
    minOccursZeroCheckbox(e, instances)
    repeatButton(e, instances)
    for (instanceNo <- e.repeats) {
      val instNos = instances add instanceNo
      repeatingEnclosing(e, instNos)
      val particles = choice.group.particleOption3.map(_.value)
      addChoiceHideOnStartScriptlet(particles, number, instNos)
      addChoiceShowHideOnSelectionScriptlet(particles, number, instNos)
      val label = getAnnotation(choice.group, Annotation.Label)
      if (label.isDefined)
        html.div(
          classes = List(ClassChoiceLabel),
          content = label)
          .closeTag
      addRemoveButton(e, instNos)

      val forEachParticle = particles.zipWithIndex.foreach _

      forEachParticle(x => {
        val particle = x._1
        val index = x._2 + 1
        html.div(
          id = Some(idPrefix + "div-choice-item-" + number + InstanceDelimiter +
            instanceNo + ChoiceIndexDelimiter + index),
          classes = List(ClassDivChoiceItem))
        html.input(
          id = Some(getChoiceItemId(number, index, instNos)),
          name = getChoiceItemName(number, instNos),
          classes = List(ClassChoiceItem),
          typ = Some("radio"),
          value = Some("number"),
          content = Some(getChoiceLabel(particle)),
          number = Some(number))
        html.closeTag(2)
      })

      node.children.zipWithIndex.foreach {
        case (n, index) => {
          html.div(id = Some(choiceContentId(number, (index + 1), instNos)),
            classes = List(ClassInvisible))
          doNode(n, instNos)
          html.closeTag
        }
      }

      html.closeTag
    }
    html.closeTag

    addMaxOccursScriptlet(e, instances)
  }

  private def doNode(node: NodeBasic, instances: Instances) {
    val e = node.element
    nonRepeatingSimpleType(e, instances)
    repeatButton(e, instances)
    val number = e.number
    for (instanceNo <- e.repeats) {
      val instNos = instances add instanceNo
      repeatingEnclosing(e, instNos)
      itemTitle(e)
      addRemoveButton(e, instNos)
      itemBefore(e)
      addNumberLabel(node, number, instNos)
      simpleType(node, instNos)
      html.closeTag
      addError(e, instNos)
      html closeTag
    }
    html.closeTag
    addMaxOccursScriptlet(e, instances)
  }

  private def addNumberLabel(node: NodeBasic, number: Int, instNos: Instances) {
    node match {
      case n: NodeBaseType => {
        val t = None
        html.div(classes = List(ClassItemNumber),
          content = Some(number.toString)).closeTag
          .label(forInputName = getItemName(number, instNos),
            classes = List(ClassItemLabel), content = Some(getLabel(node, t))).closeTag
          .div(classes = List(ClassItemInput))
      }
      case n: NodeSimpleType => {
        val t = Some(n.typ)
        html.div(classes = List(ClassItemNumber),
          content = Some(number.toString)).closeTag
          .label(forInputName = getItemName(number, instNos),
            classes = List(ClassItemLabel), content = Some(getLabel(node, t))).closeTag
          .div(classes = List(ClassItemInput))
      }
    }
  }

  private def addXmlExtractScriptlet(node: NodeSequence, instances: Instances) {
    {
      val js = JS()
      val number = node.number
      if (node.element.isMinOccursZero) {
        js.line("if (!$('#%s').is(':checked')) return '';",
          getMinOccursZeroId(number, instances))
      }
      if (node.isAnonymous)
        js.line("    var xml = '';")
      else
        js
          .line("    var xml = %s%s;", spaces(instances add 1), xmlStart(node))
      js.line("    //now add sequence children for each instanceNo")

      for (instanceNo <- repeats(node)) {
        val instNos = instances add (instanceNo, node.isAnonymous)
        js.line("    if (idVisible('%s')) {", Ids.getRepeatingEnclosingId(number, instNos))
        node.children.foreach { n =>
          js.line("      xml += %s();", xmlFunctionName(n, instNos))
          addXmlExtractScriptlet(n, instNos)
        }
        js.line("    }")
      }
      if (!node.isAnonymous)
        js.line("    xml += %s%s;", spaces(instances add 1), xmlEnd(node))
      js.line("    return xml;")

      addXmlExtractScriptlet(node, js.toString, instances);
    }
  }

  private def addXmlExtractScriptlet(node: NodeChoice, instances: Instances) {

    val js = JS()
    if (node.isAnonymous)
      js.line("    var xml = '';")
    else
      js.line("    var xml = %s%s;", spaces(instances add 1), xmlStart(node))
    js.line("    //now optionally add selected child if any")
    for (instanceNo <- repeats(node)) {
      val instNos = instances add (instanceNo, node.isAnonymous)
      js.line("    var checked = $(':input[name=%s]:checked').attr('id');",
        getChoiceItemName(node.element.number, instNos))

      node.children.zipWithIndex.foreach {
        case (n, index) =>
          js.line("    if (checked == \"%s\")", getChoiceItemId(node.element.number, index + 1, instNos))
            .line("    xml += %s();", xmlFunctionName(n, instNos))
          addXmlExtractScriptlet(n, instNos)
      }
      if (!node.isAnonymous)
        js.line("    xml += %s%s;", spaces(instances add 1), xmlEnd(node))
      js.line("    return xml;")
      addXmlExtractScriptlet(node, js.toString(), instances);
    }
  }

  private def addXmlExtractScriptlet(node: NodeSimpleType,
    instances: Instances) {
    addXmlExtractScriptletForSimpleOrBase(node, instances)
  }

  private def addXmlExtractScriptlet(node: NodeBaseType,
    instances: Instances) {
    addXmlExtractScriptletForSimpleOrBase(node, instances)
  }

  private def addXmlExtractScriptletForSimpleOrBase(node: NodeBasic,
    instances: Instances) {
    val number = node.number
    val js = JS().line("  var xml='';")
    for (instanceNo <- repeats(node)) {
      val instNos = instances add instanceNo
      js
        .line("  if (idVisible('%s')) {", Ids.getRepeatingEnclosingId(number, instNos))
      if (node.element.isRadio)
        js.line("    var v = encodeHTML($('input[name=%s]:radio:checked').val());",
          getItemName(number, instNos))
      else if (isCheckbox(node))
        js.line("    var v = $('#%s').is(':checked');", itemId(node, instNos))
      else
        js.line("    var v = %s;", valById(itemId(node, instNos)))

      val extraIndent =
        if (node.element.minOccurs.intValue == 0) {
          js
            .line("    if (v.length>0)")
          "  "
        } else ""
      js.line("    %sxml += %s%s;", extraIndent, spaces(instNos),
        xml(node, transformToXmlValue(node, "v")))

        .line("  }")
    }
    js.line("   return xml;")
    addXmlExtractScriptlet(node, js.toString, instances);
  }

  private def xmlFunctionName(node: Node, instances: Instances) = {
    val number = node.element.number
    "getXml" + number + "instance" + instances
  }

  private def addXmlExtractScriptlet(node: Node, functionBody: String,
    instances: Instances) {
    val functionName = xmlFunctionName(node, instances)
    addScript(
      JS()
        .line("  //extract xml from element <%s>", node.element.name.getOrElse("?"))
        .line("  function %s() {", functionName)
        .line(functionBody)
        .line("  }")
        .line)
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

  private def namespace(node: Node) =
    if (node.element.number == 1)
      " xmlns=\"" + options.targetNamespace + "\""
    else
      ""

  private def addScript(s: String) {
    html.appendScript(s)
  }

  private def addScript(js: JS) {
    addScript(js.toString)
  }

  private def minOccursZeroCheckbox(e: ElementWrapper, instances: Instances) {
    val number = e.number
    if (e.isMinOccursZero) {
      html.div(classes = List(ClassMinOccursZeroContainer))
      html
        .div(
          classes = List(ClassMinOccursZeroLabel),
          content = Some(e.get(Annotation.MinOccursZeroLabel)
            .getOrElse("Click to enable")))
        .closeTag
      html.input(
        id = Some(getMinOccursZeroId(number, instances)),
        name = getMinOccursZeroName(number, instances),
        classes = List(ClassMinOccursZero),
        typ = Some("checkbox"),
        checked = None,
        value = None,
        number = Some(number))
        .closeTag
      html.closeTag
      val js = JS()
        .line("$('#%s').change( function () {",
          getMinOccursZeroId(number, instances))
      for (instanceNo <- e.repeats) {
        js
          .line("  changeMinOccursZeroCheckbox($(this),$('#%s'));",
            Ids.getRepeatingEnclosingId(number, instances add instanceNo))
      }

      js.line("})")
        .line
        .line("$('#%s').change();", getMinOccursZeroId(number, instances))

      addScript(js)
    }
  }

  private def nonRepeatingTitle(e: ElementWrapper, instances: Instances) {
    //there's only one of these so use instanceNo = 1
    val number = e.number
    val content = e.get(Annotation.NonRepeatingTitle)
    if (content.isDefined)
      html
        .div(
          classes = List(ClassNonRepeatingTitle),
          content = content)
        .closeTag
  }

  private def repeatButton(e: ElementWrapper, instances: Instances) {
    val number = e.number
    if (e.hasButton) {
      html.div(
        id = Some(getRepeatButtonId(number, instances)),
        classes = List(ClassRepeatButton, ClassWhite, ClassSmall),
        content = Some(e.get(Annotation.RepeatLabel)
          .getOrElse("+"))).closeTag
      html.div(classes = List(ClassClear)).closeTag
    }
  }

  private def repeatingEnclosing(e: ElementWrapper, instances: Instances) {
    val number = e.number
    val id = Ids.getRepeatingEnclosingId(number, instances)
    html.div(
      id = Some(id),
      classes = List(ClassRepeatingEnclosing))
    if (e.minOccurs.intValue > 0 && instances.last > e.minOccurs.intValue)
      addScript(JS().line("  $('#%s').hide();", id))
  }

  private def nonRepeatingSimpleType(e: ElementWrapper, instances: Instances) {
    val number = e.number
    html
      .div(
        classes = List(ClassItemEnclosing) ++ e.visibility,
        id = Some(getItemEnclosingId(number, instances add 1)))
    nonRepeatingTitle(e, instances)
  }

  private def elementNumber(node: Node): Int = node.element.number

  private def elementNumber(e: ElementWrapper): Int = {
    elementNumbers.get(e).get;
  }

  private def simpleType(node: NodeBasic, instances: Instances) {
    val e = node.element

    val r = restriction(node)

    val qn = toQN(r)

    addInput(e, qn, r, instances)

    addDescription(e)

    addPath(e, instances)

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

  private def addInput(e: ElementWrapper, qn: QN, r: Restriction,
    instances: Instances) {

    if (isEnumeration(r))
      addEnumeration(e, r, instances)
    else
      addTextField(e, r, getExtraClasses(qn), instances)

    addWidthScript(e, instances)

    addCssScript(e, instances)
  }

  private def addTextField(
    e: ElementWrapper, r: Restriction,
    extraClasses: String, instances: Instances) {
    val number = e.number
    val inputType = getInputType(r)
    val itemId = getItemId(number, instances)
    if (e.isTextArea) {
      html.textarea(
        id = Some(itemId),
        name = getItemName(number, instances),
        classes = List(extraClasses, ClassItemInputTextarea),
        content = Some(e.default.mkString),
        number = Some(number))
        .closeTag
    } else {
      //text or boolean
      val isChecked = inputType == Checkbox &&
        e.default.isDefined && e.default.get == "true"
      val v = if (isChecked) None else defaultValue(e.default, r)
      html.input(
        id = Some(itemId),
        name = getItemName(number, instances),
        classes = List(extraClasses, ClassItemInputText),
        typ = Some(inputType.name),
        checked = None,
        value = v,
        number = Some(number))
        .closeTag
      addScript(JS().line("  $('#%s').prop('checked',%s);",
        itemId, isChecked + "").line)
      if (inputType == Checkbox) {
        addCheckboxScript(e, instances)
      }
    }

  }

  private def addCheckboxScript(e: ElementWrapper, instances: Instances) {
    val number = e.number
    val makeVisibleString = e.get(Annotation.MakeVisible);
    val makeVisibleMapOnElement = parseMakeVisibleMap(makeVisibleString)
    for (value <- List("true", "false")) {
      //get the makeVisible annotation from the named element or the
      // enumeration element in that order.
      val makeVisible = makeVisibleMapOnElement.get(value)
      makeVisible match {
        case Some(y: Int) => {
          val refersTo = number + y
          val js = JS()
            .line("  $('#%s').change( function() {",
              getItemId(number, instances))
            .line("    var v = $('#%s');", getItemId(number, instances))
            .line("    var refersTo = $('#%s');",
              getItemEnclosingId(refersTo, instances))
            .line("    if (%s v.is(':checked'))", if (value == "true") "" else "!")
            .line("      refersTo.show();")
            .line("    else")
            .line("      refersTo.hide();")
            .line("  });")
            .line
          addScript(js)
        }
        case _ =>
      }
    }
  }

  private def addWidthScript(e: ElementWrapper, instances: Instances) {
    val itmId = itemId(e, instances)
    e.get(Annotation.Width) match {
      case Some(x) =>
        addScript(JS().line("  $('#%s').width('%s');", itmId, x))
      case None =>
    }
  }

  private def addCssScript(e: ElementWrapper, instances: Instances) {
    val itmId = itemId(e, instances)
    e.get(Annotation.Css) match {
      case Some(x) => {
        val items = x.split(';')
          .foreach(
            y => {
              val pair = y.split(':')
              if (pair.size != 2)
                unexpected("css properties incorrect syntax\n" + pair)
              addScript(JS().line("  $('#%s').css('%s','%s');",
                itmId, pair(0), pair(1)))
            })
      }
      case None =>
    }
  }

  private def addRemoveButton(e: ElementWrapper, instances: Instances) {
    val number = e.number
    val removeButtonId = getRemoveButtonId(number, instances)
    val canRemove =
      (instances.last != 1 && e.maxOccurs != e.minOccurs.toString)
    if (canRemove)
      html
        .div(classes = List(ClassRemoveButtonContainer))
        .div(
          id = Some(getRemoveButtonId(number, instances)),
          classes = List(ClassRemoveButton, ClassWhite, ClassSmall),
          content = Some(e.get(Annotation.RemoveLabel)
            .getOrElse("-")))
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

  private def addChoiceHideOnStartScriptlet(
    particles: Seq[ParticleOption], number: Int, instances: Instances) {

    val forEachParticle = particles.zipWithIndex.foreach _

    forEachParticle(x => {
      val index = x._2 + 1
      addScript(
        JS().line("  $('#%s').hide();",
          choiceContentId(number, index, instances)))
    })
  }

  private def addChoiceShowHideOnSelectionScriptlet(
    particles: Seq[ParticleOption], number: Int, instances: Instances) {

    val forEachParticle = particles.zipWithIndex.foreach _

    val choiceChangeFunction = "choiceChange" + number +
      "instance" + instances;

    val js = JS()
    js.line("  var %s = function addChoiceChange%sinstance%s() {",
      choiceChangeFunction, number.toString, instances)
      .line("    $(':input[@name=%s]').change(function() {",
        getChoiceItemName(number, instances))
      .line("      var checked = $(':input[name=%s]:checked').attr('id');",
        getChoiceItemName(number, instances))

    forEachParticle(x => {
      val index = x._2 + 1
      val ccId =
        choiceContentId(number, index, instances)
      js.line("      if (checked == '%s') {",
        getChoiceItemId(number, index, instances))
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

  private def enumeration(e: ElementWrapper,
    en: Seq[(String, NoFixedFacet)],
    number: Int, isRadio: Boolean, initializeBlank: Boolean,
    instances: Instances) {
    if (isRadio) {
      //TODO add makeVisible logic for radio buttons
      en.zipWithIndex.foreach(x => {
        html.input(
          id = Some(itemId(number, x._2, instances)),
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
        html.option(content = Some("Select one..."), value = "")
          .closeTag
      val makeVisibleString = e.get(Annotation.MakeVisible);
      val makeVisibleMapOnElement = parseMakeVisibleMap(makeVisibleString)
      en.foreach { x =>
        val value = x._2.valueAttribute

        //get the makeVisible annotation from the named element or the enumeration element in that order.
        val makeVisible = makeVisibleMapOnElement.get(value) match {
          case Some(y: Int) => Some(y.toString)
          case None => getAnnotation(x._2, Annotation.MakeVisible)
        }
        html.option(content = Some(x._1), value = x._2.valueAttribute).closeTag
        makeVisible match {
          case Some(y: String) => {
            val refersTo = number + y.toInt
            val js = JS()
              .line("  $('#%s').change( function() {",
                getItemId(number, instances))
              .line("    var v = $('#%s');", getItemId(number, instances))
              .line("    var refersTo = $('#%s');",
                getItemEnclosingId(refersTo, instances))
              .line("    if ('%s' == v.val())", x._2.valueAttribute)
              .line("      refersTo.show();")
              .line("    else")
              .line("      refersTo.hide();")
              .line("  });")
              .line
            addScript(js)
          }
          case _ =>
        }
      }
      html.closeTag
    }
  }

  private def addEnumeration(e: ElementWrapper, r: Restriction,
    instances: Instances) {
    val number = e.number
    val en = getEnumeration(r)

    val initializeBlank = e.get(Annotation.AddBlank) match {
      case Some("true") => true
      case _ => false
    }
    enumeration(e, en, number, e.isRadio, initializeBlank, instances)
  }

  private def addError(e: ElementWrapper, instances: Instances) {
    val itemErrorId:String = Ids.getItemErrorId(e.number, instances)
    html.div(classes = List(ClassClear)).closeTag
    html.div(
      id = Some(itemErrorId),
      classes = List(ClassItemError),
      content = Some(e.get(Annotation.Validation)
        .getOrElse("Invalid")))
      .closeTag

  }

  private def addPath(e: ElementWrapper, instances: Instances) {
    html.div(
      classes = List("item-path"),
      id = Some(getPathId(e.number, instances)),
      enabledAttr = Some("true"),
      content = Some(""))
      .closeTag
  }

  private def addHelp(e: ElementWrapper) {
    e.get(Annotation.Help) match {
      case Some(x) =>
        html.div(classes = List(ClassItemHelp),
          content = Some(x)).closeTag
      case None =>
    }
  }

  private def addAfter(e: ElementWrapper) {
    e.get(Annotation.After) match {
      case Some(x) =>
        html.div(classes = List(ClassItemAfter),
          content = Some(x)).closeTag
      case None =>
    }
  }

  private def createDeclarationScriptlet(e: ElementWrapper, qn: QN,
    instances: Instances) = {
    val number = e.number
    val itemId = getItemId(number, instances)
    JS()
      .line("// %s", e.name.get)
      .line("var validate%sinstance%s = function () {",
        number.toString, instances)
      .line("  var ok = true;")
      .line("  var v = $('#%s');", itemId)
      .line("  var pathDiv = $('#%s');", getPathId(number, instances))
      .toString
  }

  private def createEnumerationTestScriptlet(node: NodeBasic,
    instances: Instances) = {
    val js = JS()
    if (isEnumeration(restriction(node))) {
      js.line("  //enumeration test")
      if (node.element.isRadio)
        js.line("  var radioInput=$('input:radio[name=%s]');",
          getItemName(node.number, instances))
          .line("  if (! radioInput.is(':checked')) ok = false;")
      else
        js.line("  if ($.trim(v.val()).length ==0) ok = false;")
    }
    js.toString
  }

  private def changeReference(e: ElementWrapper, instances: Instances) =
    if (e.isRadio)
      "input:radio[name=" + getItemName(e.number, instances) + "]"
    else
      "#" + getItemId(e.number, instances)

  private def createClosingScriptlet(e: ElementWrapper,
    qn: QN, instances: Instances) = {
    val number = e.number
    val changeMethod = "change("
    val js = JS()
      .line("  return ok;")
      .line("}")
      .line
      .line("$('%s').change( function() {",
        changeReference(e, instances))
      .line("  var ok = validate%sinstance%s();",
        number.toString, instances)
      .line("  showError('%s',ok);", Ids.getItemErrorId(number, instances))
      .line("});")
      .line
    if (e.minOccurs.intValue == 0 && e.default.isEmpty)
      js.line("//disable item-path due to minOccurs=0 and default is empty")
        .line("$('#%s').attr('enabled','false');",
          getPathId(number, instances))
    js.toString
  }

  private def addMaxOccursScriptlet(e: ElementWrapper,
    instances: Instances) {
    val number = e.number
    if (e.isMultiple) {
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

  def text =
    template
      .replace("//GENERATED_SCRIPT", html.script)
      .replace("//EXTRA_SCRIPT", configuration.flatMap(_.extraScript).mkString(""))
      .replace("<!--HEADER-->", configuration.flatMap(_.header).mkString(""))
      .replace("<!--FOOTER-->", configuration.flatMap(_.footer).mkString(""))
      .replace("<!--EXTRA_IMPORTS-->", configuration.flatMap(_.extraImports).mkString(""))
      .replace("/* EXTRA_CSS */", configuration.flatMap(_.extraCss).mkString(""))
      .replace("<!--GENERATED_HTML-->", html.toString)

  <!-- move to object -->

  private def xmlStart(node: Node) =
    node.element.name match {
      case Some(name) => "'<" + name + namespace(node) + ">'"
      case _ => "''"
    }

  private def xmlEnd(node: Node) =
    node.element.name match {
      case Some(name) => "'</" + name + ">'"
      case _ => "''"
    }

  private def xml(node: Node, value: String) =
    xmlStart(node) + Plus + value + Plus + xmlEnd(node)

  private def spaces(instances: Instances) =
    if (instances.indentCount == 0) "'\\n' + "
    else "'\\n' + spaces(" + ((instances.indentCount - 1) * 2) + ") + "

  private def itemTitle(e: ElementWrapper) {
    e.get(Annotation.Title) match {
      case Some(x) =>
        html.div(classes = List(ClassItemTitle),
          content = Some(x)).closeTag
      case _ =>
    }
  }

  private def itemBefore(e: ElementWrapper) {
    e.get(Annotation.Before) match {
      case Some(x) =>
        html.div(classes = List(ClassItemBefore),
          content = Some(x)).closeTag
      case _ =>
    }
  }

  private def addDescription(e: ElementWrapper) {
    e.get(Annotation.Description) match {
      case Some(x) =>
        html.div(
          classes = List(ClassItemDescription),
          content = Some(x))
          .closeTag
      case None =>
    }
  }

  private def createMandatoryTestScriptlet(node: NodeBasic) = {
    if (isMandatory(node, restriction(node)) && !isEnum(restriction(node)))
      JS()
        .line("  // mandatory test")
        .line("  if ((v.val() == null) || (v.val().length==0))")
        .line("    ok=false;")
        .toString
    else ""
  }

  private def createLengthTestScriptlet(r: Restriction) = {
    r.simpleRestrictionModelSequence3
      .facetsOption2
      .seq
      .flatMap(f => {
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
        .append(patterns
          .zipWithIndex
          .map(x => createPatternScriptlet(x)).mkString(""))
        .line("  if (!(patternMatched))")
        .line("    ok = false;")
        .toString
    else ""

  private def createPatternScriptlet(x: (String, Int)) =
    JS()
      .line("  patternMatched |= matchesPattern(v,/^%s$/);", x._1)
      .toString

  private def createBasePatternTestScriptlet(qn: QN) = {
    val js = JS()
    val basePattern = getBasePattern(qn)
    basePattern match {
      case Some(pattern) =>
        js.line("  ok &= matchesPattern(v,/^%s$/);", pattern)
      case _ =>
    }
    js.toString
  }

  private def insertMargin(s: String) =
    s.stripMargin.replaceAll("\n", "\n" + Margin)

  private def repeatingEnclosingIds(e: ElementWrapper,
    instances: Instances) =
    e.repeats.map(instances.add(_)).map(getRepeatingEnclosingId(e, _))
  
  private def itemId(node: Node, instances: Instances): String =
    getItemId(node.element.number, instances)
    
  private def itemId(element: ElementWrapper, instances: Instances): String =
    getItemId(element.number, instances)
  
  private def itemId(number: Int, enumeration: Integer,
    instances: Instances): String =
    getItemId(number, instances) + "-" + enumeration
  
  private def getRepeatingEnclosingId(e:ElementWrapper, instances:Instances):String = 
    Ids.getRepeatingEnclosingId(e.number, instances)
  
  def template = io.Source.fromInputStream(
    getClass.getResourceAsStream("/template.html")).mkString

}

