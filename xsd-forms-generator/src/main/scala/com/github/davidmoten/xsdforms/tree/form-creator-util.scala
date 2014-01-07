package com.github.davidmoten.xsdforms.tree

private[tree] object TreeUtil {
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

private[tree] case class ElementWithNumber(element: ElementWrapper, number: Int)

//Use Vector because has O(1) append
private[tree] case class HtmlJs(html: Vector[String], js: Vector[String]) {
  def addHtml(html2: String) = HtmlJs(html.+:(html2), js)
  def addJs(js2: String) = HtmlJs(html, js.+:(js2))
}

private class ElementNumbersAssigner(node: Node) {

  private def assign(node: Node, number: Int): Map[ElementWrapper, Int] = {
    val m = Map(node.element -> (number + 1))
    node match {
      case n: NodeGroup =>
        n.children.foldLeft(m)(
          (map, nd) => map ++ assign(nd, number + map.size))
      case _ =>
        m
    }
  }

  val assignments = assign(node, 0)

}

private[tree] trait FormCreatorState {
  import com.github.davidmoten.xsdforms.tree.html.Html
  import com.github.davidmoten.xsdforms.Options

  val options: Options

  val tree: Node

  implicit val idPrefix = options.idPrefix

  val html: Html

  //assign element numbers so that order of display on page 
  //will match order of element numbers. To do this must 
  //traverse children left to right before siblings
  private val elementNumbers = new ElementNumbersAssigner(tree).assignments

  implicit def toElementWithNumber(element: ElementWrapper): ElementWithNumber =
    ElementWithNumber(element, elementNumber(element))

  private def elementNumber(e: ElementWrapper): Int =
    elementNumbers.get(e).get;
}

  
