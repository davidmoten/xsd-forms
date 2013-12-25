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
      

  }

  object ElementWrapper {

    import com.github.davidmoten.xsdforms.presentation.XsdFormsAnnotation
    import xsd.Annotatedable

    implicit def unwrap(wrapped: ElementWrapper): Element = wrapped.element

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

  }

}