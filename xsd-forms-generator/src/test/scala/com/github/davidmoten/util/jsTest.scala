package com.github.davidmoten.util

import org.junit.Test
import org.junit.Assert.assertEquals

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