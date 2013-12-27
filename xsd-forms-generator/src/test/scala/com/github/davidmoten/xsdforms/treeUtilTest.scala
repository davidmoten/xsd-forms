package com.github.davidmoten.xsdforms {

  import org.junit.Test

  @Test
  class TreeUtilTest {
    import org.junit.Assert._

    import tree.TreeUtil._

    @Test
    def testParseSingle {
      assertEquals(
        Map("true" -> 1),
        parseMakeVisibleMap(Some("true->1")))
    }

    @Test
    def testParseThree {
      assertEquals(
        Map("true" -> 1, "yes" -> 2, "boo" -> 3),
        parseMakeVisibleMap(Some("true->1,yes->2,boo->3")))
    }

    @Test
    def testParseNone {
      assertTrue(parseMakeVisibleMap(None).isEmpty)
    }

    @Test
    def testParseBlankReturnsEmptyMap {
      try {
        assertTrue(parseMakeVisibleMap(Some("")).isEmpty)
        fail
      } catch {
        case _: Throwable =>
      }
    }

  }
}