package com.github.davidmoten.xsdforms.tree {

//  import xsd._
  import javax.xml.namespace.QName
  import scalaxb._
  import com.github.davidmoten.xsdforms.presentation._
  import Css._
  import com.github.davidmoten.xsdforms.html._

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

    def getChoiceItemId(idPrefix: String, number: Int, index: Int,
      instances: Instances): String =
      getItemId(idPrefix, number, instances) + ChoiceIndexDelimiter + index

    def getChoiceItemName(idPrefix: String, number: Int, instances: Instances) =
      idPrefix + "item-input-" + number + InstanceDelimiter + instances

    def getRepeatButtonId(idPrefix: String, number: Int, instances: Instances) =
      idPrefix + "repeat-button-" + number + InstanceDelimiter + instances

    def getRemoveButtonId(idPrefix: String, number: Int, instances: Instances) =
      idPrefix + "remove-button-" + number + InstanceDelimiter + instances

    def getRepeatingEnclosingId(idPrefix: String, number: Int,
      instances: Instances): String =
      idPrefix + "repeating-enclosing-" + number + InstanceDelimiter + instances

    def getMinOccursZeroId(idPrefix: String, number: Int, instances: Instances): String =
      idPrefix + "min-occurs-zero-" + number + InstanceDelimiter + instances

    def getMinOccursZeroName(idPrefix: String, number: Int,
      instances: Instances): String =
      idPrefix + "min-occurs-zero-name" + number + InstanceDelimiter + instances

    def parseMakeVisibleMap(value: Option[String]): Map[String, Int] = {
      import Util._

      val Problem = "could not parse makeVisible, expecting 'value1->1,value2->2' (pairs delimited by comma and key value delimited by '->'"
      value match {
        case Some(s) =>
          s.split(",")
            .toList
            .map(
              x => {
                val items = x.split("->")
                if (items.length < 2)
                  unexpected(Problem)
                (items(0), items(1).toInt)
              }).toMap
        case None => Map()
      }

    }
  }
  
  protected case class ElementWithNumber(element: ElementWrapper, number: Int)

  
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
    
    import TreeToHtmlConverter._
    import XsdUtil._
    import Util._
    private val script = new StringBuilder
    private val idPrefix = options.idPrefix

    private val margin = "  "
    private val Plus = " + "

    private sealed trait Entry
    private sealed trait StackEntry
    private val html = new Html

    private val NumInstancesForMultiple = 5




    //assign element numbers so that order of display on page 
    //will match order of element numbers. To do this must 
    //traverse children left to right before siblings
    val elementNumbers = new ElementNumbersAssigner(tree).assignments

    implicit def toElementWithNumber(element: ElementWrapper): ElementWithNumber = ElementWithNumber(element, elementNumber(element))
    implicit def toElementWithNumber(node: Node): ElementWithNumber = toElementWithNumber(node.element)

    //process the abstract syntax tree
    doNode(tree, new Instances)

    addXmlExtractScriptlet(tree, new Instances)

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

    private def hasButton(e: Element) =
      e.maxOccurs != "1" && e.minOccurs.toString != e.maxOccurs

    private def doNode(node: NodeSequence, instances: Instances) {
      val e = node.element
      val number = node.number
      val legend = getAnnotation(e, Annotation.Legend)
      val usesFieldset = legend.isDefined

      val label = getAnnotation(e, Annotation.Label)

      html
        .div(id = Some(getItemEnclosingId(number, instances add 1)),
          classes = List(ClassSequence) ++ getVisibility(e))
      nonRepeatingTitle(e, instances)
      minOccursZeroCheckbox(e, instances)
      repeatButton(e, instances)
      for (instanceNo <- repeats(e)) {
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
        classes = List(ClassChoice) ++ getVisibility(e))
      nonRepeatingTitle(e, instances)
      minOccursZeroCheckbox(e, instances)
      repeatButton(e, instances)
      for (instanceNo <- repeats(e)) {
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
            html.div(id = Some(choiceContentId(idPrefix, number, (index + 1), instNos)),
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

    private def doNode(node: NodeSimpleType, instances: Instances) {
      val e = node.element
      val typ = node.typ

      nonRepeatingSimpleType(e, instances)
      repeatButton(e, instances)
      val t = Some(typ)
      val number = e.number
      for (instanceNo <- repeats(e)) {
        val instNos = instances add instanceNo
        repeatingEnclosing(e, instNos)
        itemTitle(e)
        addRemoveButton(e, instNos)
        itemBefore(e)
        html.div(classes = List(ClassItemNumber),
          content = Some(number.toString)).closeTag
          .label(forInputName = getItemName(number, instNos),
            classes = List(ClassItemLabel), content = Some(getLabel(node, t))).closeTag
          .div(classes = List(ClassItemInput))

        simpleType(node, instNos)
        html.closeTag
        addError(e, instNos)
        html closeTag
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
      val number = e.number
      for (instanceNo <- repeats(e)) {
        val instNos = instances add instanceNo
        repeatingEnclosing(e, instNos)
        itemTitle(e)
        addRemoveButton(e, instNos)
        itemBefore(e)
        html.div(classes = List(ClassItemNumber),
          content = Some(number.toString)).closeTag
          .label(forInputName = getItemName(number, instNos),
            classes = List(ClassItemLabel), content = Some(getLabel(node, t))).closeTag
          .div(classes = List(ClassItemInput))
        simpleType(node, instNos)
        html
          .closeTag
        addError(e, instNos)
        html.closeTag
      }
      html.closeTag
      addMaxOccursScriptlet(e, instances)
    }

    import scala.collection.mutable.MutableList

    private def addXmlExtractScriptlet(node: NodeSequence, instances: Instances) {
      {
        val js = JS()
        val number = node.number
        if (isMinOccursZero(node.element)) {
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
          js.line("    if (idVisible('%s')) {", getRepeatingEnclosingId(number, instNos))
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
          getChoiceItemName(node, instNos))

        node.children.zipWithIndex.foreach {
          case (n, index) =>
            js.line("    if (checked == \"%s\")", getChoiceItemId(node, index + 1, instNos))
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
          .line("  if (idVisible('%s')) {", getRepeatingEnclosingId(number, instNos))
        if (isRadio(node.element))
          js.line("    var v = encodeHTML($('input[name=%s]:radio:checked').val());",
            getItemName(number, instNos))
        else if (isCheckbox(node))
          js.line("    var v = $('#%s').is(':checked');", getItemId(node, instNos))
        else
          js.line("    var v = %s;", valById(getItemId(node, instNos)))

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
      script.append(s)
    }

    private def addScript(js: JS) {
      addScript(js.toString)
    }

    private def minOccursZeroCheckbox(e: ElementWrapper, instances: Instances) {
      val number = e.number
      if (isMinOccursZero(e)) {
        html.div(classes = List(ClassMinOccursZeroContainer))
        html
          .div(
            classes = List(ClassMinOccursZeroLabel),
            content = Some(getAnnotation(e, Annotation.MinOccursZeroLabel)
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
        for (instanceNo <- repeats(e)) {
          js
            .line("  changeMinOccursZeroCheckbox($(this),$('#%s'));",
              getRepeatingEnclosingId(number, instances add instanceNo))
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
      val content = getAnnotation(e, Annotation.NonRepeatingTitle)
      if (content.isDefined)
        html
          .div(
            classes = List(ClassNonRepeatingTitle),
            content = content)
          .closeTag
    }

    private def repeatButton(e: ElementWrapper, instances: Instances) {
      val number = e.number
      if (hasButton(e)) {
        html.div(
          id = Some(getRepeatButtonId(number, instances)),
          classes = List(ClassRepeatButton, ClassWhite, ClassSmall),
          content = Some(getAnnotation(e, Annotation.RepeatLabel)
            .getOrElse("+"))).closeTag
        html.div(classes = List(ClassClear)).closeTag
      }
    }

    private def repeatingEnclosing(e: ElementWrapper, instances: Instances) {
      val number = e.number
      val id = getRepeatingEnclosingId(number, instances)
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
          classes = List(ClassItemEnclosing) ++ getVisibility(e),
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

      //addError(e, instances)

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

      val number = e.number

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
      val number = e.number
      val inputType = getInputType(r)
      val itemId = getItemId(number, instances)
      if (isTextArea(e)) {
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
      val makeVisibleString = getAnnotation(e, Annotation.MakeVisible);
      val makeVisibleMapOnElement = parseMakeVisibleMap(makeVisibleString)
      println(e.name + ",makeVisibleMap=" + makeVisibleMapOnElement)
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
                addScript(JS().line("  $('#%s').css('%s','%s');",
                  itemId, pair(0), pair(1)))
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
            content = Some(getAnnotation(e, Annotation.RemoveLabel)
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
            choiceContentId(idPrefix, number, index, instances)))
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
          choiceContentId(idPrefix, number, index, instances)
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
          html.option(content = Some("Select one..."), value = "")
            .closeTag
        val makeVisibleString = getAnnotation(e, Annotation.MakeVisible);
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

      val initializeBlank = getAnnotation(e, Annotation.AddBlank) match {
        case Some("true") => true
        case _ => false
      }
      enumeration(e, en, number, isRadio(e), initializeBlank, instances)
    }

    private def addError(e: ElementWrapper, instances: Instances) {
      val itemErrorId = getItemErrorId(e.number, instances)
      html.div(classes = List(ClassClear)).closeTag
      html.div(
        id = Some(itemErrorId),
        classes = List(ClassItemError),
        content = Some(getAnnotation(e, Annotation.Validation)
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

    private def addHelp(e: Element) {
      getAnnotation(e, Annotation.Help) match {
        case Some(x) =>
          html.div(classes = List(ClassItemHelp),
            content = Some(x)).closeTag
        case None =>
      }
    }

    private def addAfter(e: Element) {
      getAnnotation(e, Annotation.After) match {
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
        if (isRadio(node.element))
          js.line("  var radioInput=$('input:radio[name=%s]');",
            getItemName(node.number, instances))
            .line("  if (! radioInput.is(':checked')) ok = false;")
        else
          js.line("  if ($.trim(v.val()).length ==0) ok = false;")
      }
      js.toString
    }

    private def changeReference(e: ElementWrapper, instances: Instances) =
      if (isRadio(e))
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
        .line("  showError('%s',ok);", getItemErrorId(number, instances))
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

    def text =
      template
        .replace("//GENERATED_SCRIPT", script.toString)
        .replace("//EXTRA_SCRIPT", configuration.flatMap(_.extraScript).mkString(""))
        .replace("<!--HEADER-->", configuration.flatMap(_.header).mkString(""))
        .replace("<!--FOOTER-->", configuration.flatMap(_.footer).mkString(""))
        .replace("<!--EXTRA_IMPORTS-->", configuration.flatMap(_.extraImports).mkString(""))
        .replace("/* EXTRA_CSS */", configuration.flatMap(_.extraCss).mkString(""))
        .replace("<!--GENERATED_HTML-->", html.toString)

    <!-- move to object -->

    private def valById(id: String) = "encodedValueById(\"" + id + "\")"

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

    private def isMinOccursZero(e: ElementWrapper) =
      e.minOccurs.intValue == 0 && getAnnotation(e, Annotation.Visible) != Some("false")

    private def displayChoiceInline(choice: Choice) =
      "inline" == getAnnotation(choice.group, Annotation.Choice).mkString

    private def getChoiceLabel(p: ParticleOption): String = {

      val labels =
        p match {
          case x: Element => {
            getAnnotation(x, Annotation.ChoiceLabel) ++
              getAnnotation(x, Annotation.Label) ++
              Some(getLabelFromNameOrAnnotation(x))
          }
          case _ => unexpected
        }
      labels.head
    }

    private class MyRestriction(qName: QName)
      extends Restriction(None, SimpleRestrictionModelSequence(),
        None, Some(qName), Map())

    private def getVisibility(e: Element) =
      getAnnotation(e, Annotation.Visible) match {
        case Some("false") => Some(ClassInvisible)
        case _ => None
      }

    private def itemTitle(e: Element) {
      getAnnotation(e, Annotation.Title) match {
        case Some(x) =>
          html.div(classes = List(ClassItemTitle),
            content = Some(x)).closeTag
        case _ =>
      }
    }

    private def itemBefore(e: Element) {
      getAnnotation(e, Annotation.Before) match {
        case Some(x) =>
          html.div(classes = List(ClassItemBefore),
            content = Some(x)).closeTag
        case _ =>
      }
    }

    private def isTextArea(e: Element) =
      getAnnotation(e, Annotation.Text) match {
        case Some(t) if (t == "textarea") => true
        case _ => false
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

    private def isEnumeration(r: Restriction) =
      !getEnumeration(r).isEmpty

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

    private def getPatterns(r: Restriction): Seq[String] =
      r.simpleRestrictionModelSequence3.facetsOption2.seq.flatMap(f => {
        f match {
          case DataRecord(xs, Some("pattern"), x: Pattern) =>
            Some(x.valueAttribute)
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
            case QN(xs, XsdDate.name) =>
              Some("\\d\\d\\d\\d-\\d\\d-\\d\\d")
            //TODO why spaces on end of time?
            case QN(xs, XsdTime.name) =>
              Some("\\d\\d:\\d\\d *")
            case QN(xs, XsdDateTime.name) =>
              Some("\\d\\d\\d\\d-\\d\\d-\\d\\dT\\d\\d:\\d\\d")
            case _ => None
          }

        explicitPatterns ++ implicitPatterns
      }

    private sealed trait InputType {
      val name: String;
    }
    private case object Checkbox extends InputType {
      val name = "checkbox";
    }
    private case object TextBox extends InputType {
      val name = "text"
    }

    private def getInputType(r: Restriction): InputType = {
      val qn = toQN(r)
      qn match {
        case QN(xs, XsdBoolean.name) => Checkbox
        case _ => TextBox
      }
    }

    private def isCheckbox(node: NodeBasic) =
      Checkbox == getInputType(restriction(node))

    private def insertMargin(s: String) =
      s.stripMargin.replaceAll("\n", "\n" + margin)

    private def isMultiple(node: Node): Boolean =
      isMultiple(node.element)

    private def isMultiple(e: ElementWrapper): Boolean =
      return (e.maxOccurs == "unbounded" || e.maxOccurs.toInt > 1)

    private def repeatingEnclosingIds(e: ElementWrapper,
      instances: Instances) =
      repeats(e).map(instances.add(_)).map(getRepeatingEnclosingId(e, _))

    private def getAnnotation(e: Annotatedable,
      key: XsdFormsAnnotation): Option[String] =
      e.annotation match {
        case Some(x) =>
          x.attributes.get("@{" + AppInfoSchema + "}" + key.name) match {
            case Some(y) => Some(y.value.toString)
            case None => None
          }
        case None => None
      }

    private def getLabelFromNameOrAnnotation(e: Element): String = {
      val name = getLabelFromName(e)
      getAnnotation(e, Annotation.Label) match {
        case Some(x: String) => x
        case _ => name
      }
    }

    private def getLabel(node: Node, typ: Option[SimpleType]) =
      {
        val e = node.element
        val label = getLabelFromNameOrAnnotation(e)

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
                (getInputType(y) match {
                  case Checkbox => true
                  case TextBox => isMandatory(node, y)
                })
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

    private def isMandatory(node: Node, r: Restriction): Boolean = {
      val e = node.element
      val patterns =
        node match {
          case b: NodeBasic => getPatterns(b)
          case _ => getPatterns(r)
        }
      getInputType(r) == TextBox &&
        e.minOccurs.intValue == 1 &&
        ((patterns.size > 0 &&
          !patterns.exists(java.util.regex.Pattern.matches(_, ""))) || isEnum(r))
    }

    private def isEnum(r: Restriction) =
      r.simpleRestrictionModelSequence3
        .facetsOption2
        .filter(x => toQName(x) == qn("enumeration"))
        .size > 0

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

    private def getRepeatingEnclosingId(element: ElementWrapper,
      instances: Instances): String =
      TreeToHtmlConverter.getRepeatingEnclosingId(
        idPrefix, element.number, instances)
    private def getChoiceItemName(node: Node, instances: Instances): String =
      getChoiceItemName(node.element.number, instances)
    private def getChoiceItemId(node: Node, index: Int,
      instances: Instances): String =
      getChoiceItemId(node.element.number, index, instances)
    private def getItemId(node: Node, instances: Instances): String =
      getItemId(node.element.number, instances)
    private def getItemId(element: ElementWrapper, instances: Instances): String =
      getItemId(element.number, instances)

    private def choiceContentId(idPrefix: String, number: Int, index: Int,
      instances: Instances) =
      idPrefix + "choice-content-" + number + InstanceDelimiter +
        instances + ChoiceIndexDelimiter + index
    private def getMinOccursZeroId(number: Int, instances: Instances) =
      TreeToHtmlConverter.getMinOccursZeroId(idPrefix, number, instances)
    private def getMinOccursZeroName(number: Int, instances: Instances) =
      TreeToHtmlConverter.getMinOccursZeroName(idPrefix, number, instances)
    private def getRepeatButtonId(number: Int, instances: Instances) =
      TreeToHtmlConverter.getRepeatButtonId(idPrefix, number, instances)
    private def getRemoveButtonId(number: Int, instances: Instances) =
      TreeToHtmlConverter.getRemoveButtonId(idPrefix, number, instances)
    private def getRepeatingEnclosingId(number: Int, instances: Instances) =
      TreeToHtmlConverter.getRepeatingEnclosingId(idPrefix, number, instances)
    private def getChoiceItemName(number: Int, instances: Instances): String =
      TreeToHtmlConverter.getChoiceItemName(idPrefix, number, instances)
    private def getChoiceItemId(number: Int, index: Int,
      instances: Instances): String =
      TreeToHtmlConverter.getChoiceItemId(idPrefix, number, index, instances)
    private def getItemId(number: Int, instances: Instances): String =
      TreeToHtmlConverter.getItemId(idPrefix, number, instances)
    private def getItemId(number: Int, enumeration: Integer,
      instances: Instances): String =
      getItemId(number, instances) + "-" + enumeration
    private def getItemName(number: Int, instances: Instances) =
      TreeToHtmlConverter.getItemName(idPrefix, number, instances)
    private def getItemEnclosingId(number: Int, instances: Instances) =
      idPrefix + "item-enclosing-" + number + InstanceDelimiter + instances
    private def getItemErrorId(number: Int, instances: Instances) =
      TreeToHtmlConverter.getItemErrorId(idPrefix, number, instances)
    private def getPathId(number: Int, instances: Instances) =
      idPrefix + "item-path-" + number + InstanceDelimiter + instances

    private case class QN(namespace: String, localPart: String)

    private def toQN(r: Restriction): QN = toQN(r.base.get)

    private def toQN(qName: QName): QN =
      QN(qName.getNamespaceURI(), qName.getLocalPart())

    def template = io.Source.fromInputStream(
      getClass.getResourceAsStream("/template.html")).mkString

  }

}

