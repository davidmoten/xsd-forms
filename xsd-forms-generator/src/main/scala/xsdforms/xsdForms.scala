package xsdforms {

  import xsd._
  import xsd.ComplexTypeModelSequence1
  import javax.xml.namespace.QName
  import scalaxb._

  private case class BaseType(qName: QName)

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

  case class Sequence(group: ExplicitGroupable)
  case class Choice(group: ExplicitGroupable)

  //every element is either a sequence, choice or simpleType
  // simpleTypes may be based on string, decimal, boolean, date, datetime
  // and may be restricted to a regex pattern, have min, max ranges
  // or be an enumeration. all elements may have  minOccurs and maxOccurs
  //attributes.

  /**
   * **************************************************************
   *
   *   Visitor
   *
   *
   * **************************************************************
   */

  trait Visitor {
    def startSequence(e: Element, sequence: Sequence)
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
   *   HtmlVisitor
   *
   *
   * **************************************************************
   */

  class HtmlVisitor(targetNamespace: String, idPrefix: String, extraScript: Option[String]) extends Visitor {
    import XsdUtil._
    import Util._
    private val script = new StringBuilder

    private var number = 0
    val margin = "  "

    private sealed trait StackEntry
    private val stack = new scala.collection.mutable.Stack[StackEntry]
    private val path = new scala.collection.mutable.Stack[String]
    private val html = new Html

    override def toString = html.toString

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
    var s = "";
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
    $('div[enabled="true"]').each( function(index) { 
      var inputId = this.id.replace("path-","");
      var value = $('#'+ inputId).val()
     
      var items = $(this).text().split('|');
      var startAt = getStartAt(previousItems, items);
      
      s = closePreviousItems(previousItems, startAt,s);
      
      for (var i=startAt; i<items.length; i++) {
          var tag;
          if (i==0)
            tag = openTagWithNs(items[i],"ns");
          else
            tag = openTag(items[i]);
    	  s = s + "\n" + spaces(i*2) + tag;
      }
      s = s + value 
      s = s + closeTag(items[items.length-1]);
      previousItems = items;
    });
    //TODO handle closing multiple repeated elements see https://github.com/davidmoten/xsd-forms/issues/1
    s = closePreviousItems(previousItems, 0,s);
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

    private case class SequenceEntry(
      element: Element, sequence: Sequence, number: String,
      usesFieldset: Boolean) extends StackEntry
    private case class ChoiceEntry(
      element: Element, choice: Choice, number: String) extends StackEntry
    private case class ChoiceItemEntry(
      element: Element, p: ParticleOption, number: String) extends StackEntry

    private def currentNumber = number + ""

    def startSequence(e: Element, sequence: Sequence) {
      path.push(e.name.get)

      val number = nextNumber
      val legend = getAnnotation(e, "legend")
      val usesFieldset = legend.isDefined

      stack.push(SequenceEntry(e, sequence, number, usesFieldset))

      val label = getAnnotation(e, "label").mkString

      html
        .div(classes = List("sequence"))
      repeatingTitle(e, number, e.minOccurs.intValue() == 0 || e.maxOccurs != "1")
      repeatingEnclosing(e, number)
      addMaxOccurs(e, number)
      html.div(classes = List("sequence-label"), content = Some(label))
        .closeTag
        .div(id = Some(idPrefix + "sequence-" + number),
          classes = List("sequence-content"))
      if (usesFieldset)
        html.fieldset(legend = legend, classes = List("fieldset"), id = Some(idPrefix + "fieldset-" + number))
    }

    def endSequence {
      stack.pop match {
        case e: SequenceEntry => {
          html.closeTag
          if (e.usesFieldset)
            html.closeTag
          html
            .closeTag
            .closeTag
        }
        case _ => unexpected
      }
      path.pop
    }

    private def getChoiceItemName(number: String) = idPrefix + "item-input-" + number
    private def getChoiceItemId(number: String, index: Int) = getItemId(number) + choiceIndexDelimiter + index

    def startChoice(e: Element, choice: Choice) {
      path.push(e.name.get)
      val choiceInline = displayChoiceInline(choice)

      val number = nextNumber
      stack.push(ChoiceEntry(e, choice, number))

      html.div(id = Some(getItemEnclosingId(number)), classes = List("choice"))
      repeatingTitle(e, number, e.minOccurs.intValue() == 0 || e.maxOccurs != "1")
      repeatingEnclosing(e, number)
      addMaxOccurs(e, number)
      val particles = choice.group.arg1.map(_.value)
      addChoiceHideOnStart(particles, number)
      addChoiceShowHideScriptOnSelection(particles, number)

      html.div(
        classes = List("choice-label"),
        content = Some(getAnnotation(choice.group, "label").mkString))
        .closeTag

      val forEachParticle = particles.zipWithIndex.foreach _

      forEachParticle(x => {
        val particle = x._1
        val index = x._2 + 1
        html.div(
          id = Some(idPrefix + "div-choice-item-" + number + choiceIndexDelimiter + index),
          classes = List("div-choice-item"))
        html.input(
          id = Some(getChoiceItemId(number, index)),
          name = getChoiceItemName(number),
          classes = List("choice-item"),
          typ = Some("radio"),
          value = Some("number"),
          content = Some(getChoiceLabel(e, particle)),
          number = Some(number))
        html.closeTag
        html.closeTag
      })

    }

    private def displayChoiceInline(choice: Choice) =
      "inline" == getAnnotation(choice.group, "choice").mkString

    private def choiceIndexDelimiter = "-"
      
    private def addChoiceHideOnStart(
      particles: Seq[ParticleOption], number: String) {

      val forEachParticle = particles.zipWithIndex.foreach _

      forEachParticle(x => {
        val index = x._2 + 1
        addScriptWithMargin("""
|$("#""" + idPrefix + """choice-content-""" + number + choiceIndexDelimiter + index + """").hide();""")
      })
    }

    private def addChoiceShowHideScriptOnSelection(
      particles: Seq[ParticleOption], number: String) {

      val forEachParticle = particles.zipWithIndex.foreach _
      
      addScriptWithMargin(
        """
|var choiceChange"""+number+""" = function addChoiceChange"""+number+"""(suffix) {
|  $(":input[@name='""" + getChoiceItemName(number) + """" + suffix + "']").change(function() {
|    var checked = $(':input[name=""" + getChoiceItemName(number) + """' + suffix + ']:checked').attr("id");""")

      forEachParticle(x => {
        val index = x._2 + 1
        val choiceContentId =
          idPrefix + """choice-content-""" + number + choiceIndexDelimiter  + index
        addScriptWithMargin(
          """
|    if (checked == """" + getChoiceItemId(number, index) + """" + suffix) {
|      $("#""" + choiceContentId + """" + suffix).show();
|      $("#""" + choiceContentId + """" + suffix).find('.item-path').attr('enabled','true');
|    }
|    else {
|      $("#""" + choiceContentId + """" + suffix).hide();
|      $("#""" + choiceContentId + """" + suffix).find('.item-path').attr('enabled','false');
|    }"""
      )})
addScriptWithMargin(
"""
|  })
|}
|
|choiceChange"""+number+"""("");""")

    }

    def startChoiceItem(e: Element, p: ParticleOption, index: Int) {
      val number =
        stack.head match {
          case ChoiceEntry(_, _, id) => id
          case _ => unexpected
        }
      stack.push(ChoiceItemEntry(e, p, number))

      html.div(id = Some(idPrefix + "choice-content-" + number + choiceIndexDelimiter + index), classes=List("invisible"))
    }

    def getChoiceLabel(e: Element, p: ParticleOption): String = {
      val labels =
        p match {
          case x: Element => {
            getAnnotation(x, "choiceLabel") ++ getAnnotation(x, "label") ++ Some(getLabel(x, None))
          }
          case _ => unexpected
        }
      labels.head
    }

    def getLabel(e: Element, p: ParticleOption): String =
      p match {
        case x: Element => getLabel(x, None)
        case _ => getLabel(e, None)
      }

    def endChoiceItem {
      stack.pop
      html.closeTag
    }

    def endChoice {
      path.pop
      stack.pop match {
        case e: ChoiceEntry =>
          html
            .closeTag
            .closeTag
        case _ => unexpected
      }
    }

    private class MyRestriction(qName: QName)
      extends Restriction(None, SimpleRestrictionModelSequence(), None, Some(qName), Map())

    private def getVisibility(e: Element) =
      getAnnotation(e, "visible") match {
        case Some("false") => Some("invisible")
        case _ => None
      }

    private def repeatingTitle(e: Element, number: String, hasButton: Boolean) {
      html.div(
        classes = List("repeating-title"),
        content = getAnnotation(e, "repeatingTitle")).closeTag
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

    private def repeatingEnclosing(e: Element, number: String) {
      html.div(
        id = Some(getRepeatingEnclosingId(number)),
        classes = List("repeating-enclosing"))
    }

    def addItemHtmlOpening(e: Element, number: String, typ: Option[SimpleType]) {
      html
        .div(
          classes = List("item-enclosing") ++ getVisibility(e),
          id = Some(getItemEnclosingId(number)))
      repeatingTitle(e, number, e.maxOccurs != "0" && e.maxOccurs != "1")
      repeatingEnclosing(e, number)
      getAnnotation(e, "title") match {
        case Some(x) => html.div(classes = List("item-title"), content = Some(x)).closeTag
        case _ =>
      }
      getAnnotation(e, "before") match {
        case Some(x) => html.div(classes = List("item-before"), content = Some(x)).closeTag
        case _ =>
      }
      html.div(classes = List("item-number"), content = Some(number)).closeTag
        .label(forInputName = getItemName(number),
          classes = List("item-label"), content = Some(getLabel(e, typ))).closeTag
        .div(classes = List("item-input"))
    }

    def simpleType(e: Element, typ: SimpleType) {
      path.push(e.name.get)
      val number = nextNumber

      addItemHtmlOpening(e, number, Some(typ))
      typ.arg1.value match {
        case x: Restriction => simpleType(e, x, number)
        case _ => unexpected
      }
      html
        .closeTag
        .closeTag
        .closeTag
      path.pop
    }

    private def getTextType(e: Element) =
      getAnnotation(e, "text")

    private def nextNumber: String = {
      number += 1
      number + ""
    }

    def baseType(e: Element, typ: BaseType) {
      path.push(e.name.get)
      val number = nextNumber
      addItemHtmlOpening(e, number, None)
      simpleType(e, new MyRestriction(typ.qName), number + "")
      html
        .closeTag
        .closeTag
        .closeTag
      path.pop
    }

    case class QN(namespace: String, localPart: String)

    implicit def toQN(qName: QName) =
      QN(qName.getNamespaceURI(), qName.getLocalPart())

    def getItemId(number: String) = idPrefix + "item-" + number

    def getItemEnclosingId(number: String) =
      idPrefix + "item-enclosing-" + number

    def getItemErrorId(number: String) =
      idPrefix + "item-error-" + number

    def getPathId(number: String) = idPrefix + "item-path-" + number

    def simpleType(e: Element, r: Restriction, number: String) {
      val qn = toQN(r.base.get)

      addInput(e, qn, r, number)

      addMaxOccurs(e, number)

      addDescription(e)

      addError(e, getItemErrorId(number))

      addPath(number)

      addHelp(e)

      addAfter(e)

      val statements = List(
        createDeclarationScriptlet(e, qn, number),
        createMandatoryTestScriptlet(e, r),
        createPatternsTestScriptlet(getPatterns(r)),
        createBasePatternTestScriptlet(qn),
        createFacetTestScriptlet(r),
        createLengthTestScriptlet(r),
        createCanExcludeScriptlet(e),
        createClosingScriptlet(e, qn, number))

      statements
        .map(stripMargin(_))
        .foreach(x => if (x.length > 0) addScript(x))

    }

    private def getItemName(number: String) =
      idPrefix + "item-input-" + number;

    private def addInput(e: Element, qn: QN, r: Restriction, number: String) {

      val itemId = getItemId(number)

      if (isEnumeration(r))
        addEnumeration(e, r, number)
      else
        addTextField(e, r, itemId, number, getExtraClasses(qn))

      addWidthScript(e, itemId)

      addCssScript(e, itemId)
    }

    private def getExtraClasses(qn: QN) = qn match {
      case QN(xs, "date") => "datepickerclass "
      case QN(xs, "datetime") => "datetimepickerclass "
      case QN(xs, "time") => "timepickerclass "
      case _ => ""
    }

    private def addTextField(
      e: Element, r: Restriction, itemId: String,
      number: String, extraClasses: String) {
      val inputType = getInputType(r)
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

    private def addWidthScript(e: Element, itemId: String) {
      getAnnotation(e, "width") match {
        case Some(x) =>
          addScriptWithMargin("|  $('#" + itemId + "').width('" + x + "');")
        case None =>
      }
    }

    private def addCssScript(e: Element, itemId: String) {
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

    private def addEnumeration(e: Element, r: Restriction, number: String) {
      val en = getEnumeration(r)
      val isRadio = getAnnotation(e, "selector") match {
        case Some("radio") => true
        case None => false
      }

      val initializeBlank = getAnnotation(e, "addBlank") match {
        case Some("true") => true
        case _ => false
      }
      enumeration(en, number, isRadio, initializeBlank)
    }

    private def getEnumeration(typ: SimpleType): Seq[(String, NoFixedFacet)] =
      typ.arg1.value match {
        case x: Restriction =>
          getEnumeration(x)
        case _ => unexpected
      }

    private def getEnumeration(r: Restriction): Seq[(String, NoFixedFacet)] =
      r.arg1.arg2.seq.map(
        _.value match {
          case y: NoFixedFacet => {
            val label = getAnnotation(y, "label") match {
              case Some(x) => x
              case None => y.value
            }
            Some((label, y))
          }
          case _ => None
        }).flatten

    private def enumeration(en: Seq[(String, NoFixedFacet)],
      number: String, isRadio: Boolean, initializeBlank: Boolean) {
      if (isRadio) {
        en.zipWithIndex.foreach(x => {
          html.input(
            id = Some(idPrefix + "item-" + number + "-" + x._2),
            name = getItemName(number),
            classes = List("select"),
            typ = Some("radio"),
            value = Some(x._1._1),
            content = Some(x._1._1),
            number = Some(number)).closeTag
        })
      } else {
        html.select(
          id = Some(idPrefix + "item-" + number),
          name = getItemName(number),
          classes = List("select"),
          number = Some(number))
        if (initializeBlank)
          html.option(content = Some("Select one..."), value = "").closeTag
        en.foreach { x =>
          html.option(content = Some(x._1), value = x._2.value).closeTag
          getAnnotation(x._2, "makeVisible") match {
            case Some(y: String) => {
              val refersTo = number.toInt + y.toInt
              addScriptWithMargin("""
|  $("#""" + idPrefix + """item-""" + number + """").change( function() {
|    var v = $("#""" + idPrefix + """item-""" + number + """");
|    var refersTo = $("#""" + getItemEnclosingId(refersTo + "") + """") 
|    if ("""" + x._2.value + """" == v.val()) 
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

    private def addError(e: Element, itemErrorId: String) {
      html.div(
        id = Some(itemErrorId),
        classes = List("item-error"),
        content = Some(getAnnotation(e, "validation").getOrElse("Invalid")))
        .closeTag

    }

    private def addPath(number: String) {
      html.div(
        classes = List("item-path"),
        id = Some(getPathId(number)),
        enabledAttr = Some("true"),
        content = Some(path.reverse.mkString("|")))
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

    private def createDeclarationScriptlet(e: Element, qn: QN, number: String) = {
      val itemId = getItemId(number)
      """
|// """ + e.name.get + """
|var validate""" + number + """ = function() {
|  return validate""" + number + """WithSuffix("");  
|}
|
|var validate""" + number + """WithSuffix = function (suffix) {
|  var ok = true;
|  var v = $("#""" + itemId + """" + suffix);
|  var pathDiv = $("#""" + getPathId(number) + """");"""
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
      r.arg1.arg2.seq.flatMap(f => {
        val start = """
|  //length test
|  if (v.val().length """
        val finish = """)
|    ok = false;"""
        f match {
          case DataRecord(xs, Some("minLength"), x: NumFacet) =>
            Some(start + "<" + x.value + finish)
          case DataRecord(xs, Some("maxLength"), x: NumFacet) =>
            Some(start + ">" + x.value + finish)
          case DataRecord(xs, Some("length"), x: NumFacet) =>
            Some(start + "!=" + x.value + finish)
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
      r.arg1.arg2.seq.flatMap(f => {
        val start = "\n|  //facet test\n|  if ((+(v.val())) "
        val finish = ")\n|    ok = false;"

        f match {
          case DataRecord(xs, Some("minInclusive"), x: Facet) =>
            Some(start + "< " + x.value + finish)
          case DataRecord(xs, Some("maxInclusive"), x: Facet) =>
            Some(start + "> " + x.value + finish)
          case DataRecord(xs, Some("minExclusive"), x: Facet) =>
            Some(start + "<=" + x.value + finish)
          case DataRecord(xs, Some("maxExclusive"), x: Facet) =>
            Some(start + ">= " + x.value + finish)
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

    private def createClosingScriptlet(e: Element, qn: QN, number: String) = {
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
|$("#""" + getItemId(number) + """").""" + changeMethod + """ function() {
|  var ok = validate""" + number + """();
|  var error= $("#""" + getItemErrorId(number) + """");
|  if (!(ok)) 
|    error.show();
|  else 
|    error.hide();
|})
""" + (if (e.minOccurs == 0 && e.default.isEmpty)
        """
|//disable item-path due to minOccurs=0 and default is empty  
|$("#""" + getPathId(number) + """").attr('enabled','false');"""
      else "")
    }

    private def addScriptWithMargin(s: String) = addScript(stripMargin(s))

    private def getPatterns(r: Restriction) =
      r.arg1.arg2.seq.flatMap(f => {
        f match {
          case DataRecord(xs, Some("pattern"), x: Pattern) => Some(x.value)
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

    private def isMultiple(e: Element) =
      (e.maxOccurs == "unbounded" || e.maxOccurs.toInt > 1)

    private def addMaxOccurs(e: Element, number: String) {

      if (isMultiple(e)) {
        val repeatButtonId = getRepeatButtonId(number)
        val enclosingId = idPrefix + "repeating-enclosing-" + number

        addScriptWithMargin("""

|            
|//make a hidden copy of the enclosing item
|var enclosing""" + number + """ = $("#""" + enclosingId + """").clone();
|""" + (if (e.minOccurs == 0) """$("#""" + enclosingId + """").hide();""" else "") + """ 
|var lastRepeat""" + number + """="""" + enclosingId + """";
|$("#""" + repeatButtonId + """").click(function() {
|  var clone = enclosing""" + number + """.clone();
|  clone.insertAfter("#"+lastRepeat""" + number + """);
|  var map = {};
|  clone.find('*').andSelf().each( function(index) {  
|    var id = $(this).attr("id");
|    if (typeof id === "undefined") 
|      return;
|    if (id.match(/^.*-\d+$/)) {
|      //extract the number
|      //extract the number from a choice id pattern first
|      var number = id.replace(/^.*-(\d+)(-\d+)$/,"$1");
|      //if not found then extract from a standard id pattern
|      if (number == id)
|        number = id.replace(/^.*-(\d+)$/,"$1");
|      if (map[number] == null) {
|        repeatCount++;
|        map[number]=repeatCount;
|      }
|      var suffix = "-" + map[number];
|      var newId = id + suffix;
|      $(this).attr("id",newId);
|      //TODO fix up label for attribute (points to name attribute of input element)
|      if (id.match(/^""" + idPrefix + """item(-\d+)?-\d+$/)) {
|        var input = $('#'+newId);  
|        var numberFromId = id.replace(/^.*-(\d+)(-\d+)$/,"$1");
|        if (numberFromId == id)
|          numberFromId = id.replace(/^.*-(\d+)$/,"$1");
|         
|        input.attr("name", """" + idPrefix + """item-input-" + numberFromId + "-" + map[numberFromId]);
|        input.change( function() {
|          console.log("changed " + newId);
|          var ok = callMethod("validate" + numberFromId +"WithSuffix",suffix);
|          var error= $("#""" + idPrefix + """item-error-" + numberFromId  + "-" + map[numberFromId]);
|          if (!(ok)) 
|            error.show();
|          else 
|            error.hide();
|        })
|      }
|    }
|  })
|  var nextId = clone.attr("id");
|  lastRepeat""" + number + """=nextId;
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
            x.arg1.value match {
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

  class Traversor(s: Schema, rootElement: String, visitor: Visitor) {
    import Util._
    import XsdUtil._

    private val topLevelElements =
      s.schemasequence1.flatMap(_.arg1.value match {
        case y: TopLevelElement => Some(y)
        case _ => None
      })

    private val topLevelComplexTypes = s.schemasequence1.flatMap(_.arg1.value match {
      case y: TopLevelComplexType => Some(y)
      case _ => None
    })

    private val topLevelSimpleTypes = s.schemasequence1.flatMap(_.arg1.value match {
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
    def process {

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
      x.arg1.value match {
        case x: ComplexContent =>
          unexpected
        case x: SimpleContent =>
          unexpected
        case x: ComplexTypeModelSequence1 =>
          x.arg1.getOrElse(unexpected).value match {
            case y: GroupRef =>
              unexpected
            case y: ExplicitGroupable =>
              if (matches(x.arg1.get, qn("sequence")))
                process(e, Sequence(y))
              else if (matches(x.arg1.get, qn("choice")))
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
      visitor.startSequence(e, x)
      x.group.arg1.foreach(y => process(e, toQName(y), y.value))
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
      x.group.arg1.foreach(y => {
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

  /**
   * @author dave
   *
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

    def closeTag = {
      val element = stack.head
      if (!element.hasContent) {
        indent
      }
      append("</" + element.name + ">");
      stack.pop
      this
    }

    override def toString = s.toString()

  }
}
