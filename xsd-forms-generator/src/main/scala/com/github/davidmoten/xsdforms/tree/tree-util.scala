package com.github.davidmoten.xsdforms.tree

object TreeUtil {
  import xsd.Annotatedable
  import XsdUtil.AppInfoSchema

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

//Use Vector because has O(1) append
protected case class HtmlJs(html: Vector[String], js: Vector[String]) {
  def addHtml(html2: String) = HtmlJs(html.+:(html2), js)
  def addJs(js2: String) = HtmlJs(html, js.+:(js2))
}

object Ids {

  val InstanceDelimiter = "-instance-"
  val ChoiceIndexDelimiter = "-choice-"

  def getItemId(number: Int, instances: Instances)(implicit idPrefix: Prefix) =
    idPrefix + "item-" + number + InstanceDelimiter + instances

  def getItemName(number: Int, instances: Instances)(implicit idPrefix: Prefix): String =
    idPrefix + "item-input-" + number + InstanceDelimiter + instances;

  def getItemErrorId(number: Int, instances: Instances)(implicit idPrefix: Prefix): String =
    idPrefix + "item-error-" + number + InstanceDelimiter + instances

  def getChoiceItemId(number: Int, index: Int,
    instances: Instances)(implicit idPrefix: Prefix): String =
    getItemId(number, instances)(idPrefix) + ChoiceIndexDelimiter + index

  def getChoiceItemName(number: Int, instances: Instances)(implicit idPrefix: Prefix): String =
    idPrefix + "item-input-" + number + InstanceDelimiter + instances

  def choiceContentId(number: Int, index: Int,
    instances: Instances)(implicit idPrefix: Prefix): String =
    idPrefix + "choice-content-" + number + InstanceDelimiter +
      instances + ChoiceIndexDelimiter + index

  def getRepeatButtonId(number: Int, instances: Instances)(implicit idPrefix: Prefix): String =
    idPrefix + "repeat-button-" + number + InstanceDelimiter + instances

  def getRemoveButtonId(number: Int, instances: Instances)(implicit idPrefix: Prefix): String =
    idPrefix + "remove-button-" + number + InstanceDelimiter + instances

  def getRepeatingEnclosingId(number: Int,
    instances: Instances)(implicit idPrefix: Prefix): String =
    idPrefix + "repeating-enclosing-" + number + InstanceDelimiter + instances

  def getMinOccursZeroId(number: Int, instances: Instances)(implicit idPrefix: Prefix): String =
    idPrefix + "min-occurs-zero-" + number + InstanceDelimiter + instances

  def getMinOccursZeroName(number: Int,
    instances: Instances)(implicit idPrefix: Prefix): String =
    idPrefix + "min-occurs-zero-name" + number + InstanceDelimiter + instances

  def getItemEnclosingId(number: Int, instances: Instances)(implicit idPrefix: Prefix) =
    idPrefix + "item-enclosing-" + number + InstanceDelimiter + instances

  def getPathId(number: Int, instances: Instances)(implicit idPrefix: Prefix) =
    idPrefix + "item-path-" + number + InstanceDelimiter + instances
}

protected trait HasOptions {
  val options: Options
  implicit val idPrefix = options.idPrefix
}

protected trait HasHtml {
  import com.github.davidmoten.xsdforms.html.Html
  val html: Html
}