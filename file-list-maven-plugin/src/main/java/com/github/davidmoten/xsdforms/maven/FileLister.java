package com.github.davidmoten.xsdforms.maven;

import java.io.File;
import java.util.Arrays;

public class FileLister {

	public static String listFiles(String prefix, File f) {
		String[] items = add(prefix, null, f).split("\n");
		Arrays.sort(items);
		StringBuilder s = new StringBuilder();
		for (String item : items) {
			if (s.length() > 0)
				s.append("\n");
			s.append(item);
		}
		return s.toString();

	}

	private static String add(String prefix, String path, File f) {
		StringBuilder s = new StringBuilder();

		String base;
		if (path == null)
			base = prefix;
		else if (path.length() == 0)
			base = f.getName();
		else
			base = path + "/" + f.getName();
		if (f.isDirectory())
			for (File file : f.listFiles()) {
				if (!file.getName().startsWith(".")) {
					if (s.length() > 0)
						s.append('\n');
					s.append(add(prefix, base, file));
				}
			}
		else if (!f.getName().startsWith("."))
			s.append(base);
		return s.toString();
	}

}
