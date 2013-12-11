package com.github.davidmoten.xsdforms.maven;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;

public class GeneratorMojoTest {

	@Test
	public void testFilesWereGenerated() {
		assertTrue(new File("target/generated-resources/form.html").exists());
	}
	
}
