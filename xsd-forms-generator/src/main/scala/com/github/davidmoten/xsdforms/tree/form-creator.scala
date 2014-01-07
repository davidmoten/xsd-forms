package com.github.davidmoten.xsdforms.tree

import javax.xml.namespace.QName
import scalaxb._
import com.github.davidmoten.xsdforms.presentation._
import Css._
import com.github.davidmoten.xsdforms.tree.html._
import com.github.davidmoten.util.JS
import com.github.davidmoten.xsdforms.tree.html.Html
import com.github.davidmoten.xsdforms.{Configuration,Options}

/**
 * **************************************************************
 *
 *   FormCreator class
 *
 *
 * **************************************************************
 */

private[xsdforms] class FormCreator(override val options: Options,
  configuration: Option[Configuration], override val tree: Node)
  extends FormCreatorState {

  import xsd.{Element,Restriction,ParticleOption,NoFixedFacet,
    SimpleRestrictionModelSequence,NumFacet,Pattern,Annotatedable,SimpleType,Facet}

  import XsdUtil._
  import Util._
  import TreeUtil._
  import ElementWrapper._
  import Ids._

  override val html = new Html

  private val Margin = "  "
  private val Plus = " + "

  //process the abstract syntax tree
  doNode(tree, new Instances)

  addXmlExtractScriptlet(tree, new Instances)

  private def doNode(node: Node, instances: Instances) {
    node match {
      case n: NodeBasic => doNode(n, instances)
      case n: NodeSequence => doNode(n, instances)
      case n: NodeChoice => doNode(n, instances)
    }
  }

  private def addXmlExtractScriptlet(node: Node, instances: Instances) {
    node match {
      case n: NodeBasic => addXmlExtractScriptlet(n, instances)
      case n: NodeSequence => addXmlExtractScriptlet(n, instances)
      case n: NodeChoice => addXmlExtractScriptlet(n, instances)
    }
  }

  private def doNode(node: NodeSequence, instances: Instances) {
    val e = node.element
    html
      .div(id = Some(getItemEnclosingId(e.number, instances add 1)),
        classes = List(ClassSequence) ++ e.visibility)
    nonRepeatingTitle(e, instances)
    minOccursZeroCheckbox(e, instances)
    repeatButton(e, instances)
    for (instanceNo <- e.repeats) {
      doInstance(node, instances, instanceNo)
    }
    html closeTag

    addMaxOccursScriptlet(e, instances)
  }

  private def doInstance(node: NodeSequence, instances: Instances, instanceNo: Int) {
    val e = node.element
    val label = e.get(Annotation.Label)
    val legend = e.get(Annotation.Legend)
    val usesFieldset = legend.isDefined
    val instNos = instances add instanceNo
    repeatingEnclosing(e, instNos)
    if (label.isDefined)
      html
        .div(classes = List(ClassSequenceLabel), content = label).closeTag
    html.div(id = Some(idPrefix + "sequence-" + e.number + InstanceDelimiter + instanceNo),
      classes = List(ClassSequenceContent))
    addRemoveButton(e, instNos)
    if (usesFieldset)
      html.fieldset(legend = legend, classes = List(ClassFieldset), id =
        Some(idPrefix + "fieldset-" + e.number + InstanceDelimiter + instanceNo))

    node.children.foreach(x => doNode(x, instNos))

    if (usesFieldset)
      html.closeTag

    html closeTag 2
  }

  private def doNode(node: NodeChoice, instances: Instances) {
    val e = node.element
    //TODO choiceInline not used!
    val choiceInline = displayChoiceInline(node.choice)

    html.div(id = Some(getItemEnclosingId(e.number, instances add 1)),
      classes = List(ClassChoice) ++ e.visibility)
    nonRepeatingTitle(e, instances)
    minOccursZeroCheckbox(e, instances)
    repeatButton(e, instances)
    for (instanceNo <- e.repeats) {
      doInstance(node, instances, instanceNo)
    }
    html.closeTag

    addMaxOccursScriptlet(e, instances)
  }

  private def doInstance(node: NodeChoice, instances: Instances, instanceNo: Int) {
    val e = node.element
    val choice = node.choice
    val instNos = instances add instanceNo
    repeatingEnclosing(e, instNos)
    val particles = choice.group.particleOption3.map(_.value)
    addChoiceHideOnStartScriptlet(particles, e.number, instNos)
    addChoiceShowHideOnSelectionScriptlet(particles, e.number, instNos)
    val label = Annotation.Label.from(choice.group)
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
        id = Some(idPrefix + "div-choice-item-" + e.number + InstanceDelimiter +
          instanceNo + ChoiceIndexDelimiter + index),
        classes = List(ClassDivChoiceItem))
      html.input(
        id = Some(getChoiceItemId(e.number, index, instNos)),
        name = getChoiceItemName(e.number, instNos),
        classes = List(ClassChoiceItem),
        typ = Some("radio"),
        value = Some("number"),
        content = Some(getChoiceLabel(particle)),
        number = Some(e.number))
      html.closeTag(2)
    })

    node.children.zipWithIndex.foreach {
      case (n, index) => {
        html.div(id = Some(choiceContentId(e.number, (index + 1), instNos)),
          classes = List(ClassInvisible))
        doNode(n, instNos)
        html.closeTag
      }
    }

    html.closeTag
  }

  private def doNode(node: NodeBasic, instances: Instances) {
    val e = node.element
    nonRepeatingSimpleType(e, instances)
    repeatButton(e, instances)
    for (instanceNo <- e.repeats) {
      val instNos = instances add instanceNo
      repeatingEnclosing(e, instNos)
      itemTitle(e)
      addRemoveButton(e, instNos)
      itemBefore(e)
      addNumberLabel(node, e.number, instNos)
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
      val number = node.element.number
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

  private def addXmlExtractScriptlet(node: NodeBasic,
    instances: Instances) {
    val number = node.element.number
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
    if (e.isMinOccursZero) {
      html.div(classes = List(ClassMinOccursZeroContainer))
      html
        .div(
          classes = List(ClassMinOccursZeroLabel),
          content = Some(e.get(Annotation.MinOccursZeroLabel)
            .getOrElse("Click to enable")))
        .closeTag
      html.input(
        id = Some(getMinOccursZeroId(e.number, instances)),
        name = getMinOccursZeroName(e.number, instances),
        classes = List(ClassMinOccursZero),
        typ = Some("checkbox"),
        checked = None,
        value = None,
        number = Some(e.number))
        .closeTag
      html.closeTag
      val js = JS()
        .line("$('#%s').change( function () {",
          getMinOccursZeroId(e.number, instances))
      for (instanceNo <- e.repeats) {
        js
          .line("  changeMinOccursZeroCheckbox($(this),$('#%s'));",
            Ids.getRepeatingEnclosingId(e.number, instances add instanceNo))
      }

      js.line("})")
        .line
        .line("$('#%s').change();", getMinOccursZeroId(e.number, instances))

      addScript(js)
    }
  }

  private def nonRepeatingTitle(e: ElementWrapper, instances: Instances) {
    //there's only one of these so use instanceNo = 1
    val content = e.get(Annotation.NonRepeatingTitle)
    if (content.isDefined)
      html
        .div(
          classes = List(ClassNonRepeatingTitle),
          content = content)
        .closeTag
  }

  private def repeatButton(e: ElementWrapper, instances: Instances) {
    if (e.hasButton) {
      html.div(
        id = Some(getRepeatButtonId(e.number, instances)),
        classes = List(ClassRepeatButton, ClassWhite, ClassSmall),
        content = Some(e.get(Annotation.RepeatLabel)
          .getOrElse("+"))).closeTag
      html.div(classes = List(ClassClear)).closeTag
    }
  }

  private def repeatingEnclosing(e: ElementWrapper, instances: Instances) {
    val id = Ids.getRepeatingEnclosingId(e.number, instances)
    html.div(
      id = Some(id),
      classes = List(ClassRepeatingEnclosing))
    if (e.minOccurs.intValue > 0 && instances.last > e.minOccurs.intValue)
      addScript(JS().line("  $('#%s').hide();", id))
  }

  private def nonRepeatingSimpleType(e: ElementWrapper, instances: Instances) {
    html
      .div(
        classes = List(ClassItemEnclosing) ++ e.visibility,
        id = Some(getItemEnclosingId(e.number, instances add 1)))
    nonRepeatingTitle(e, instances)
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
      addTextOrCheckbox(e, r, getExtraClasses(qn), instances)

    addWidthScript(e, instances)
    addCssScript(e, instances)
  }

  private def addTextOrCheckbox(
    e: ElementWrapper, r: Restriction,
    extraClasses: String, instances: Instances) {
    if (e.isTextArea)
      addTextArea(e, extraClasses, instances)
    else
      addTextFieldOrCheckbox(e, r, extraClasses, instances)
  }

  private def addTextArea(e: ElementWrapper, extraClasses: String, instances: Instances) {
    val itemId = getItemId(e.number, instances)
    html.textarea(
      id = Some(itemId),
      name = getItemName(e.number, instances),
      classes = List(extraClasses, ClassItemInputTextarea),
      content = Some(e.default.mkString),
      number = Some(e.number))
      .closeTag
  }

  private def addTextFieldOrCheckbox(e: ElementWrapper, r: Restriction, extraClasses: String, instances: Instances) {
    val inputType = getInputType(r)
    val itemId = getItemId(e.number, instances)
    //text or checkbox
    val isChecked = inputType == Checkbox &&
      e.default.isDefined && e.default.get == "true"
    val v = if (isChecked) None else defaultValue(e.default, r)
    html.input(
      id = Some(itemId),
      name = getItemName(e.number, instances),
      classes = List(extraClasses, ClassItemInputText),
      typ = Some(inputType.name),
      checked = None,
      value = v,
      number = Some(e.number))
      .closeTag
    addScript(JS().line("  $('#%s').prop('checked',%s);",
      itemId, isChecked + "").line)
    if (inputType == Checkbox) {
      addCheckboxScript(e, instances)
    }
  }

  private def addCheckboxScript(e: ElementWrapper, instances: Instances) {
    val makeVisibleString = e.get(Annotation.MakeVisible);
    val makeVisibleMapOnElement = parseMakeVisibleMap(makeVisibleString)
    for (value <- List("true", "false")) {
      //get the makeVisible annotation from the named element or the
      // enumeration element in that order.
      val makeVisible = makeVisibleMapOnElement.get(value)
      makeVisible match {
        case Some(y: Int) => {
          val refersTo = e.number + y
          val js = JS()
            .line("  $('#%s').change( function() {",
              getItemId(e.number, instances))
            .line("    var v = $('#%s');", getItemId(e.number, instances))
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
        case None =>
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
    val removeButtonId = getRemoveButtonId(e.number, instances)
    val canRemove =
      (instances.last != 1 && e.maxOccurs != e.minOccurs.toString)
    if (canRemove)
      html
        .div(classes = List(ClassRemoveButtonContainer))
        .div(
          id = Some(getRemoveButtonId(e.number, instances)),
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
      js.line("      if (checked == '%s') ",
        getChoiceItemId(number, index, instances))
        .line("        showChoiceItem($('#%s'));", ccId)
        .line("      else")
        .line("        hideChoiceItem($('#%s'))", ccId)
    })
    js.line("    });")
      .line("  }")
      .line
      .line("  %s();", choiceChangeFunction)

    addScript(js)
  }

  private def enumeration(e: ElementWrapper,
    en: Seq[(String, NoFixedFacet)],
    isRadio: Boolean,
    instances: Instances) {
    if (isRadio)
      radio(en, e.number, instances)
    else
      dropDown(e, en, instances)
  }

  private def radio(
    en: Seq[(String, NoFixedFacet)],
    number: Int,
    instances: Instances) {
    //TODO add makeVisible logic for radio buttons
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
  }

  private def dropDown(
    e: ElementWrapper,
    en: Seq[(String, NoFixedFacet)],
    instances: Instances) {

    html.select(
      id = Some(getItemId(e.number, instances)),
      name = getItemName(e.number, instances),
      classes = List(ClassSelect),
      number = Some(e.number))

    if (initializeBlank(e))
      html.option(content = Some("Select one..."), value = "")
        .closeTag
    val makeVisibleString = e.get(Annotation.MakeVisible);
    val makeVisibleMapOnElement = parseMakeVisibleMap(makeVisibleString)
    en.foreach { x =>
      dropDownValue(x, e.number, makeVisibleMapOnElement, instances)
    }
    html.closeTag
  }

  private def dropDownValue(x: (String, NoFixedFacet), number: Int,
    makeVisibleMapOnElement: Map[String, Int], instances: Instances) {
    val value = x._2.valueAttribute

    //get the makeVisible annotation from the named element or the enumeration element in that order.
    val makeVisible = makeVisibleMapOnElement.get(value) match {
      case Some(y: Int) => Some(y.toString)
      case None => Annotation.MakeVisible.from(x._2)
    }
    html.option(content = Some(x._1), value = x._2.valueAttribute).closeTag
    makeVisible match {
      case Some(y: String) => {
        addShowHideRefersTo(x, number, y, instances)
      }
      case None =>
    }
  }

  private def addShowHideRefersTo(x: (String, NoFixedFacet), number: Int, y: String, instances: Instances) {
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

  private def addEnumeration(e: ElementWrapper, r: Restriction,
    instances: Instances) {
    val en = getEnumeration(r)

    enumeration(e, en, e.isRadio, instances)
  }

  private def addError(e: ElementWrapper, instances: Instances) {
    val itemErrorId: String = Ids.getItemErrorId(e.number, instances)
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
    val itemId = getItemId(e.number, instances)
    JS()
      .line("// %s", e.name.get)
      .line("var validate%sinstance%s = function () {",
        e.number.toString, instances)
      .line("  var ok = true;")
      .line("  var v = $('#%s');", itemId)
      .line("  var pathDiv = $('#%s');", getPathId(e.number, instances))
      .toString
  }

  private def createEnumerationTestScriptlet(node: NodeBasic,
    instances: Instances) = {
    val js = JS()
    if (isEnumeration(restriction(node))) {
      js.line("  //enumeration test")
      if (node.element.isRadio)
        js.line("  var radioInput=$('input:radio[name=%s]');",
          getItemName(node.element.number, instances))
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
    val changeMethod = "change("
    val js = JS()
      .line("  return ok;")
      .line("}")
      .line
      .line("$('%s').change( function() {",
        changeReference(e, instances))
      .line("  var ok = validate%sinstance%s();",
        e.number.toString, instances)
      .line("  showError('%s',ok);", Ids.getItemErrorId(e.number, instances))
      .line("});")
      .line
    if (e.minOccurs.intValue == 0 && e.default.isEmpty)
      js.line("//disable item-path due to minOccurs=0 and default is empty")
        .line("$('#%s').attr('enabled','false');",
          getPathId(e.number, instances))
    js.toString
  }

  private def addMaxOccursScriptlet(e: ElementWrapper,
    instances: Instances) {
    if (e.isMultiple) {
      val repeatButtonId = getRepeatButtonId(e.number, instances)
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
      case None => "''"
    }

  private def xmlEnd(node: Node) =
    node.element.name match {
      case Some(name) => "'</" + name + ">'"
      case None => "''"
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
      case None =>
    }
  }

  private def itemBefore(e: ElementWrapper) {
    e.get(Annotation.Before) match {
      case Some(x) =>
        html.div(classes = List(ClassItemBefore),
          content = Some(x)).closeTag
      case None =>
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
      case None =>
    }
    js.toString
  }

  private def initializeBlank(e: ElementWrapper) =
    e.get(Annotation.AddBlank) match {
      case Some("true") => true
      case _ => false
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

  private def getRepeatingEnclosingId(e: ElementWrapper, instances: Instances): String =
    Ids.getRepeatingEnclosingId(e.number, instances)

  def template = io.Source.fromInputStream(
    getClass.getResourceAsStream("/template.html")).mkString

}

