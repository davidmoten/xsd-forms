package com.github.davidmoten.xsdforms.tree {

  /**
   * **************************************************************
   *
   *   ElementWrapper
   *
   *
   * **************************************************************
   */

  import xsd.Element

  protected case class ElementWrapper(element: Element,
    uniqueId: String = java.util.UUID.randomUUID.toString) {

    import com.github.davidmoten.xsdforms.presentation._
    import com.github.davidmoten.xsdforms.presentation.Css._
    import ElementWrapper._

    val e = element

    def hasButton =
      e.maxOccurs != "1" && e.minOccurs.toString != e.maxOccurs

    def get(key: XsdFormsAnnotation) =
      getAnnotation(this, key)

    def visibility =
      getAnnotation(e, Annotation.Visible) match {
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

    def numInstances: Int = {

      val n = if (e.maxOccurs == "unbounded")
        NumInstancesForMultiple
      else
        e.maxOccurs.toInt

      get(Annotation.MaxRepeats) match {
        case Some(m) => Math.min(n, m.toInt)
        case _ => n
      }
    }

    def getLabelFromName =
      e.name.get
        .replaceAll("-", " ")
        .replaceAll("_", " ")
        .split(" ")
        .map(s =>
          Character.toUpperCase(s.charAt(0))
            + s.substring(1, s.length))
        .mkString(" ")

  }
  protected case class QN(namespace: String, localPart: String)

  protected object ElementWrapper {

    import com.github.davidmoten.xsdforms.presentation._
    import xsd.Annotatedable
    import xsd.Restriction
    import xsd.NoFixedFacet
    import Util._
    import XsdUtil._
    import javax.xml.namespace.QName

    implicit def unwrap(wrapped: ElementWrapper): Element = wrapped.element

    val NumInstancesForMultiple = 5

    def valById(id: String) = "encodedValueById(\"" + id + "\")"

    def getAnnotation(e: Annotatedable, key: XsdFormsAnnotation) =
      e.annotation match {
        case Some(x) =>
          x.attributes.get("@{" + XsdUtil.AppInfoSchema + "}" + key.name) match {
            case Some(y) => Some(y.value.toString)
            case None => None
          }
        case None => None
      }

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
            val label = getAnnotation(y, Annotation.Label) match {
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

  }

}