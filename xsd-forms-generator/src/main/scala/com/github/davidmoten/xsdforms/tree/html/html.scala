package com.github.davidmoten.xsdforms.tree.html

import scala.Option.option2Iterable

/**
 * Builds html specifically for xsd-forms.
 */
object `package` {}

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

private[tree] class Html {
  import Html._
  private case class HtmlElement(name: String, hasContent: Boolean)
  private val stack = new scala.collection.mutable.Stack[HtmlElement]
  //mutable! 
  private val s = new StringBuffer
  //mutable!
  private val scriptBuffer = new StringBuffer

  def div(id: Option[String] = None,
    classes: List[String] = List(), enabledAttr: Option[String] = None,
    content: Option[String] = None) =
    element(name = Div, id = id, classes = classes, enabledAttr = enabledAttr,
      content = content)

  def select(id: Option[String] = None, name: String,
    classes: List[String] = List(),
    content: Option[String] = None, number: Option[Int] = None) =
    element(name = Select, id = id, classes = classes,
      nameAttr = Some(name), numberAttr = number,
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
      content = content, value = value,
      nameAttr = Some(name), typ = typ, numberAttr = number)

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
    if (stack.isEmpty)
      throw new RuntimeException("closeTag called on empty html stack! html so far=\n" + s.toString)
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

  def appendScript(s: String) {
    scriptBuffer append s
  }

  def script = scriptBuffer.toString

  override def toString = s.toString

}
