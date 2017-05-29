package com.epam.dlab;

public class Test {

	private static void printVersion() {
		Package pkg = Test.class.getPackage();
		System.out.println("Name    " + pkg.getName());
		System.out.println("Title   " + pkg.getImplementationTitle());
		System.out.println("Version " + pkg.getImplementationVersion());
		System.out.println("Vendor  " + pkg.getImplementationVendor());

		System.out.println("Build  ");
		System.out.println("Date   ");
		System.out.println("Commit ");
		System.out.println("Tag ");
	}

	public static void main(String[] args) {
		printVersion();
	}

}
