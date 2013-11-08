/**
 * **************************************************************
 *
 *   JS
 *
 *
 * **************************************************************
 */

package js {

  case class Function() {
    def apply(name: String) = NamedFunction(name)
  }

  case class NamedFunction(name: String) {
    def apply(js: JS): JS = JS("function " + name + "() {\n" + js + "\n}\n\n")
  }

  case class Expression(s: String)

  trait Statement

  case class Assignment extends Statement

  object JS {
    implicit def toJS(s: String): JS = JS(s);
    implicit def toJS(i: Int): JS = JS(i.toString)
    implicit def toJS(s: Symbol): JS = JS(s.toString)
    val function = Function()

    def test {
      val js: JS =
        function("hello")(
          'x := 'y + 'z --
            'a := 'a + 1 --
            'a := 'a + 2)
    }

  }

  case class JS(val initialValue: String = "") {

    val content: StringBuffer = new StringBuffer(initialValue)

    def fn(name: String)(body: JS): JS = {
      this
    }

    def +(js: JS): JS = {
      content append " + "
      append(js)
    }

    def append(s: String) = {
      content append s
      this
    }

    def newLine(js: JS): JS = {
      content append "\n"
      append(js)
    }

    def declare(js: JS): JS = {
      content append "\nvar "
      append(js)
    }

    def :=(js: JS): JS = {
      content append " = "
      append(js)
    }

    def --(js: JS): JS = {
      newLine(js)
    }

    def apply(s: String, js: JS) = {
      content append s
      append(js)
    }

    def append(js: JS) = {
      content append js.content.toString
      this
    }

    def apply(js: JS) = append(js)

    def start(js: JS) = {
      content append " {\n"
      this
    }

    override def toString = content.toString + "\n"

  }
}