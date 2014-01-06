package com.github.davidmoten.xsdforms.tree

/**
 * **************************************************************
 *
 *   ElementWrapper
 *
 *
 * **************************************************************
 */

import xsd.Element

private[tree] case class ElementWrapper(element: Element,
  uniqueId: String = java.util.UUID.randomUUID.toString) {

  import com.github.davidmoten.xsdforms.presentation._
  import com.github.davidmoten.xsdforms.presentation.Css._
  import ElementWrapper._

  private val e = element

  def hasButton =
    e.maxOccurs != "1" && e.minOccurs.toString != e.maxOccurs

  def get(key: XsdFormsAnnotation) =
    key.from(this)

  def visibility =
    get(Annotation.Visible) match {
      case Some("false") => Some(ClassInvisible)
      case _ => None
    }

  def isMinOccursZero =
    e.minOccurs.intValue == 0 && get(Annotation.Visible) != Some("false")

  def isTextArea =
    get(Annotation.Text) match {
      case Some(t) if (t == "textarea") => true
      case _ => false
    }

  def isRadio =
    //TODO add check is enumeration as well  
    get(Annotation.Selector) match {
      case Some("radio") => true
      case _ => false
    }

  def isMultiple: Boolean =
    return (e.maxOccurs == "unbounded" || e.maxOccurs.toInt > 1)

  def repeats: Range = 1 to numInstances

  private def numInstances: Int = {

    val n = if (e.maxOccurs == "unbounded")
      NumInstancesForMultiple
    else
      e.maxOccurs.toInt

    get(Annotation.MaxRepeats) match {
      case Some(m) => Math.min(n, m.toInt)
      case _ => n
    }
  }

  private def getLabelFromName =
    e.name.get
      .replaceAll("-", " ")
      .replaceAll("_", " ")
      .split(" ")
      .map(s =>
        Character.toUpperCase(s.charAt(0))
          + s.substring(1, s.length))
      .mkString(" ")

  def getLabelFromNameOrAnnotation: String = {
    val name = getLabelFromName
    get(Annotation.Label) match {
      case Some(x: String) => x
      case _ => name
    }
  }

}

private[tree] case class QN(namespace: String, localPart: String)

private[tree] sealed trait InputType {
  val name: String;
}
private[tree] case object Checkbox extends InputType {
  val name = "checkbox";
}
private[tree] case object TextBox extends InputType {
  val name = "text"
}

import xsd.Restriction
import xsd.SimpleRestrictionModelSequence
import javax.xml.namespace.QName

private[tree] class MyRestriction(qName: QName)
  extends Restriction(None, SimpleRestrictionModelSequence(),
    None, Some(qName), Map())

/**
 * **************************************************************
 *
 *   ElementWrapper object
 *
 *
 * **************************************************************
 */

private[tree] object ElementWrapper {

  import com.github.davidmoten.xsdforms.presentation._
  import Css._
  import xsd.Annotatedable
  import xsd.Restriction
  import xsd.NoFixedFacet
  import xsd.SimpleType
  import xsd.Pattern
  import xsd.ParticleOption
  import Util._
  import XsdUtil._
  import javax.xml.namespace.QName
  import scalaxb.DataRecord

  implicit def unwrap(wrapped: ElementWrapper): Element = wrapped.element

  val NumInstancesForMultiple = 5

  def valById(id: String) = "encodedValueById(\"" + id + "\")"

  def defaultValue(value: Option[String], r: Restriction): Option[String] = {
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

  def isEnumeration(r: Restriction) =
    !getEnumeration(r).isEmpty

  def getEnumeration(r: Restriction): Seq[(String, NoFixedFacet)] =
    r.simpleRestrictionModelSequence3.facetsOption2.seq.map(
      _.value match {
        case y: NoFixedFacet => {
          val label = Annotation.Label.from(y) match {
            case Some(x) => x
            case None => y.valueAttribute
          }
          Some((label, y))
        }
        case _ => None
      }).flatten

  def toQN(r: Restriction): QN = toQN(r.base.get)

  private def toQN(qName: QName): QN =
    QN(qName.getNamespaceURI(), qName.getLocalPart())

  def getBasePattern(qn: QN) = {

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

  def getLabel(node: Node, typ: Option[SimpleType]) =
    {
      val e = node.element
      val label = e.getLabelFromNameOrAnnotation

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

  def isMandatory(node: Node, r: Restriction): Boolean = {
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

  def isEnum(r: Restriction) =
    r.simpleRestrictionModelSequence3
      .facetsOption2
      .filter(x => toQName(x) == qn("enumeration"))
      .size > 0

  def restriction(node: NodeBasic): Restriction =
    node match {
      case n: NodeSimpleType => restriction(n)
      case n: NodeBaseType => restriction(n)
    }

  def restriction(node: NodeSimpleType): Restriction =
    node.typ.simpleDerivationOption3.value match {
      case x: Restriction => x
      case _ => Util.unexpected
    }

  private def restriction(node: NodeBaseType) =
    new MyRestriction(node.typ.qName)

  def getPatterns(node: NodeBasic): Seq[String] =
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

  private def getPatterns(r: Restriction): Seq[String] =
    r.simpleRestrictionModelSequence3.facetsOption2.seq.flatMap(f => {
      f match {
        case DataRecord(xs, Some("pattern"), x: Pattern) =>
          Some(x.valueAttribute)
        case _ => None
      }
    })

  def getInputType(r: Restriction): InputType = {
    val qn = toQN(r)
    qn match {
      case QN(xs, XsdBoolean.name) => Checkbox
      case _ => TextBox
    }
  }

  def isCheckbox(node: NodeBasic) =
    Checkbox == getInputType(restriction(node))

  def displayChoiceInline(choice: Choice) =
    "inline" == Annotation.Choice.from(choice.group).mkString

  def getChoiceLabel(p: ParticleOption): String = {

    val labels =
      p match {
        case x: Element => {
          Annotation.ChoiceLabel.from(x) ++
            Annotation.Label.from(x) ++
            Some(ElementWrapper(x).getLabelFromNameOrAnnotation)
        }
        case _ => unexpected
      }
    labels.head
  }

  def isMultiple(node: Node): Boolean =
    node.element.isMultiple

  def repeats(node: Node): Range = node.element.repeats

  def getExtraClasses(qn: QN) = qn match {
    case QN(xs, XsdDate.name) => ClassDatePicker + " "
    case QN(xs, XsdDateTime.name) => ClassDateTimePicker + " "
    case QN(xs, XsdTime.name) => ClassTimePicker + " "
    case _ => ""
  }

}
