package com.epam.dlab;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class Test {

	private static void printVersion(Class<?> clazz) {
		InputStream in = clazz.getResourceAsStream("/" + JarFile.MANIFEST_NAME);
		Manifest manifest;
		try {
			manifest = new Manifest(in);
		} catch (IOException e) {
			System.err.println("Cannot open mainfest: " + e.getLocalizedMessage());
			e.printStackTrace();
			return;
		}
		
		Attributes attr = manifest.getMainAttributes();
		System.out.println("Title       " + attr.getValue(java.util.jar.Attributes.Name.IMPLEMENTATION_TITLE));
		System.out.println("Version     " + attr.getValue(java.util.jar.Attributes.Name.IMPLEMENTATION_VERSION));
		System.out.println("Created By  " + attr.getValue("Created-By"));
		System.out.println("Vendor      " + attr.getValue(java.util.jar.Attributes.Name.IMPLEMENTATION_VENDOR));
		System.out.println("GIT-Branch  " + attr.getValue("GIT-Branch"));
		System.out.println("GIT-Commit  " + attr.getValue("GIT-Commit"));
		System.out.println("Build JDK   " + attr.getValue("Build-JDK"));
		System.out.println("Build OS    " + attr.getValue("Build-OS"));
		System.out.println("Built Time  " + attr.getValue("Build-Time"));
		System.out.println("Built By    " + attr.getValue("Built-By"));
	}

	public static void main(String[] args) {
		printVersion(Test.class);
	}

}
