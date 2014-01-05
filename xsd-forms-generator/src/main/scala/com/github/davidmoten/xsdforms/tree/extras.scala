package com.github.davidmoten.xsdforms.tree

private[tree] class ElementNumbersAssigner(node: Node) {

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

