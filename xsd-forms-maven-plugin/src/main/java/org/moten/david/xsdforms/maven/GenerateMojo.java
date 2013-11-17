package org.moten.david.xsdforms.maven;

/*
 * Copyright 2001-2005 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import scala.Option;
import xsdforms.Generator;

/**
 * Goal which generates form and dependent files.
 * 
 * @goal generate
 * 
 * @phase generate-sources
 */
@Mojo(name="generate")
public class GenerateMojo extends AbstractMojo {
	/**
	 * The directory to write the files to.
	 * 
	 */
	@Parameter(defaultValue="target/xsd-forms")
	private File outputDirectory;
	

	/**
	 * The schema path (on classpath or as file)
	 * 
	 */
	@Parameter
	private String schema;

	/**
	 * The id prefix in generated html.
	 * 
	 */
	@Parameter
	private String idPrefix;

	/**
	 * Top level element from schema to use as root level element in xml.
	 * 
	 */
	@Parameter
	private String rootElement;

	/**
	 * Extra script to include in jquery document body.
	 * 
	 */
	@Parameter
	private String extraScript;

	@Override
	public void execute() throws MojoExecutionException {
		//look first on classpath
		InputStream schemaIn = getClass().getResourceAsStream(schema);
		//then on file system
		if (schemaIn == null)
			try {
				schemaIn = new FileInputStream(schema);
			} catch (FileNotFoundException e) {
				throw new MojoExecutionException(e.getMessage(), e);
			}
		Generator.generateDirectory(schemaIn, outputDirectory, idPrefix,
				Option.apply(rootElement), Option.apply(extraScript));
	}
}
