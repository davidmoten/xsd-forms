package com.github.davidmoten.xsdforms.maven;

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
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

/**
 * Goal which generates file list.
 * 
 */
@Mojo(name = "list", defaultPhase = LifecyclePhase.GENERATE_RESOURCES, threadSafe = true)
public class FileListMojo extends AbstractMojo {
	/**
	 * The file to contain the file list.
	 * 
	 */
	@Parameter(property = "output", defaultValue = "${project.build.directory}/generated-resources/file-list.txt")
	private File output;

	/**
	 * The directory to list.
	 * 
	 */
	@Parameter(property = "directory", defaultValue = "${basedir}/src/main/resources")
	private File directory;

	@Parameter(property = "prefix", defaultValue = "")
	private String prefix;

	@Override
	public void execute() throws MojoExecutionException {
		if (prefix == null)
			prefix = "";
		getLog().info(
				"writing list of " + directory + " to " + output
						+ " with prefix " + prefix);
		String list = FileLister.listFiles(prefix, directory);
		getLog().info("list=\n"+list);
		output.getParentFile().mkdirs();
		try {
			FileOutputStream fos = new FileOutputStream(output);
			fos.write(list.getBytes(Charset.forName("UTF-8")));
			fos.close();
		} catch (IOException e) {
			throw new MojoExecutionException(e.getMessage(), e);
		}
		getLog().info("written to " + output);
	}
}
