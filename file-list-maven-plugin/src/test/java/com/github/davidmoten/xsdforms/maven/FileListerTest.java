package com.github.davidmoten.xsdforms.maven;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.junit.Test;

public class FileListerTest {

	@Test
	public void testWithPrefix() {
		String s = FileLister.listFiles("boo", new File("src/test/resources"));
		System.out.println(s);
		assertEquals("boo/a\nboo/b\nboo/c/d\nboo/c/e/g\nboo/c/e/h",s);
	}
	
	@Test
	public void testWithoutPrefix() {
		String s = FileLister.listFiles("", new File("src/test/resources"));
		System.out.println(s);
		assertEquals("a\nb\nc/d\nc/e/g\nc/e/h",s);
	}
	
}
