package com.github.davidmoten.xsdforms {

  import org.junit.Test
  import tree.JS

  @Test
  class JSTest {

    import org.junit.Assert._

    @Test
    def testJS {
      val js = JS()
        .line("function %s(doc,%s) {", "logit", "name")
        .line("  console.log(doc);")
        .line("}")
      println(js)
      val expected = """
function logit(doc,name) {
  console.log(doc);
}"""
      assertEquals(expected, js.toString)
    }
  }
}