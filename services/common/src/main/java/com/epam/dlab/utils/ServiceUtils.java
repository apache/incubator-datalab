package com.epam.dlab.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

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
	
	
	public static Map<String, String> getManifest(Class<?> mainClazz) {
		Map<String, String> manifest = new HashMap<>();
		
		System.out.println("MANIFEST.MF is " + mainClazz.getClassLoader().getResource("MANIFEST.MF"));
		System.out.println("META-INF/MANIFEST.MF is " + mainClazz.getClassLoader().getResource("META-INF/MANIFEST.MF"));
		System.out.println("/META-INF/MANIFEST.MF is " + mainClazz.getClassLoader().getResource("/META-INF/MANIFEST.MF"));
		
		Enumeration<URL> urls;
		try {
			urls = mainClazz.getClassLoader().getResources("META-INF/MANIFEST.MF");
			while (urls.hasMoreElements()) {
				System.out.println("  url: " + urls.nextElement());
			}
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		
		try (BufferedReader reader = new BufferedReader(
				new InputStreamReader(mainClazz.getResourceAsStream("/META-INF/MANIFEST.MF")))
				) {
			String line;
			System.out.println("    <<< Content >>    ");
			while ((line = reader.readLine()) != null) {
				int pos = line.indexOf(": ");
				if (pos > 0) {
					System.out.println(line);
					manifest.put(line.substring(0,  pos), line.substring(pos + 2));
				}
			}
			System.out.println("    <<< Content >>    ");
		} catch (IOException e) {
			System.err.println("Cannot open mainfest: " + e.getLocalizedMessage());
			e.printStackTrace(); 
		}
		
		return manifest;
	}
	
	
	/** Print to standard output the manifest info about application. If parameter <b>args</b> is not
	 * <b>null</b> and one or more arguments have value -v or --version then print version and return <b>true<b/>
	 * otherwise <b>false</b>.
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
			if (!result) {
				return result;
			}
		}
		
		Map<String, String> manifest = getManifest(mainClazz);
		if (manifest.isEmpty()) {
			return result;
		}
		
		System.out.println("Title       " + manifest.get("Implementation-Title"));
		System.out.println("Version     " + manifest.get("Implementation-Version"));
		System.out.println("Created By  " + manifest.get("Created-By"));
		System.out.println("Vendor      " + manifest.get("Implementation-Vendor"));
		System.out.println("GIT-Branch  " + manifest.get("GIT-Branch"));
		System.out.println("GIT-Commit  " + manifest.get("GIT-Commit"));
		System.out.println("Build JDK   " + manifest.get("Build-Jdk"));
		System.out.println("Build OS    " + manifest.get("Build-OS"));
		System.out.println("Built Time  " + manifest.get("Build-Time"));
		System.out.println("Built By    " + manifest.get("Built-By"));
		
		return result;
	}
}
