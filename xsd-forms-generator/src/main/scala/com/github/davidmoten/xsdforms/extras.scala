package com.github.davidmoten.xsdforms {

  class ElementNumbersAssigner(node: Node) {

    private var _number = 0

    import scala.collection.mutable.HashMap
    private val elementNumbers = new HashMap[ElementWrapper, Int]()

    val map = assignElementNumbers(node)

    /**
     * Traverse children before siblings to provide element
     * numbers matching page display order.
     * @param node
     */
    private def assignElementNumbers(node: Node) {
      elementNumber(node)
      node match {
        case n: NodeGroup => n.children.foreach { assignElementNumbers(_) }
        case _ => ;
      }
    }

    private def elementNumber(node: Node): Int = elementNumber(node.element)

    private def elementNumber(e: ElementWrapper): Int = {
      val n = elementNumbers.get(e);
      if (n.isDefined)
        n.get
      else {
        val newNumber = nextNumber
        elementNumbers(e) = newNumber
        newNumber
      }
    }
    private def nextNumber: Int = {
      _number += 1
      _number
    }

    def assignments = elementNumbers.toMap
  }
  
  case class NodeSequence2(node:NodeSequence,options:Options) {
    
  }


}