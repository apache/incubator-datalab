package com.epam.dlab.utils;

import java.io.IOException;
import java.io.InputStream;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import com.epam.dlab.constants.ServiceConsts;

public class ServiceUtils {
	
	private static String includePath = null;
	
	static {
        includePath = System.getenv(ServiceConsts.DLAB_CONF_DIR_NAME);
        if ( includePath == null || includePath.isEmpty() ) {
        	includePath = getUserDir();
        }
	}
	
	public static String getUserDir() {
		return System.getProperty("user.dir");
	}
	
	public static String getConfPath() {
        return includePath;
	}
	
	
	/** Print to standard output the manifest info about application.
	 * @param mainClazz the main class of application.
	 * @param args the arguments of main class function or null.
	 * @return if parameter <b>args</b> is not null and one or more arguments have value -v or --version
	 * then return <b>true<b/> otherwise <b>false</b>.
	 */
	public static boolean printAppVersion(Class<?> mainClazz, String ... args) {
		boolean result = false;
		if (args != null) {
			for (String arg : args) {
	            if (arg.equals("-v") ||
	            	arg.equals("--version")) {
	            	result = true;
	            }
	        }
		}
        
		
		InputStream in = mainClazz.getResourceAsStream("/" + JarFile.MANIFEST_NAME);
		Manifest manifest;
		try {
			manifest = new Manifest(in);
		} catch (IOException e) {
			System.err.println("Cannot open mainfest: " + e.getLocalizedMessage());
			e.printStackTrace();
			return result;
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
		
		return result;
	}
}
