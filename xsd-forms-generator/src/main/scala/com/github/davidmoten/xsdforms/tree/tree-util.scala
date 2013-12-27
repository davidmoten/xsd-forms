package com.github.davidmoten.xsdforms.tree {

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
    def addHtml(html2: String) = HtmlJs(html.+:( html2), js)
    def addJs(js2: String) = HtmlJs(html, js.+:(js2))
  }

  object Ids {

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
  }

}