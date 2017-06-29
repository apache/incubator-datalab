package com.epam.dlab.utils;

import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epam.dlab.constants.ServiceConsts;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;

public class ServiceUtils {
	private static final Logger LOGGER = LoggerFactory.getLogger(ServiceUtils.class);
	
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
	
	
	
	private static Manifest getManifestForClass(Class<?> clazz) throws IOException {
		LOGGER.trace("Find mainfest in default class loader for class {}", clazz);
		URL url = clazz.getClassLoader().getResource(JarFile.MANIFEST_NAME);
		InputStream is = url.openStream();
        return new Manifest(is);
	}

	private static Manifest getManifestFromJar(String classPath) throws IOException {
		LOGGER.trace("Find mainfest for path {}", classPath);
		URL url = new URL(classPath);
		JarURLConnection jarConnection = (JarURLConnection) url.openConnection();
		return jarConnection.getManifest();
	}
	
	public static Map<String, String> getManifest(Class<?> clazz) {
		LOGGER.trace("Find mainfest for class {}", clazz);
		String className = "/" + clazz.getName().replace('.', '/') + ".class";
		LOGGER.trace("Resource name for class is {}", className);
		String classPath = clazz.getResource(className).toString();
		LOGGER.trace("Found for class {} resource path {}", className, classPath);

		Map<String, String> map = new HashMap<>();
		try {
			Manifest manifest = (classPath.startsWith("jar:file:") ? getManifestFromJar(classPath) : getManifestForClass(clazz));
			Attributes attributes = manifest.getMainAttributes();
			for (Object key : attributes.keySet()) {
				map.put(key.toString(), (String) attributes.get(key));
			}
			LOGGER.trace("Manifest properties {}", map);
		} catch (IOException e) {
			LOGGER.warn("Cannot found or open manifest for class {}", className);
		}
		
		return map;
	}
	
	
	/** Print to standard output the manifest info about application. If parameter <b>args</b> is not
	 * <b>null</b> and one or more arguments have value -v or --version then print version and return <b>true<b/>
	 * otherwise <b>false</b>.
	 * @param mainClass the main class of application.
	 * @param args the arguments of main class function or null.
	 * @return if parameter <b>args</b> is not null and one or more arguments have value -v or --version
	 * then return <b>true<b/> otherwise <b>false</b>.
	 */
	public static boolean printAppVersion(Class<?> mainClass, String ... args) {
		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		context.getLogger(LOGGER.getName()).setLevel(Level.TRACE);
		
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
		
		Map<String, String> manifest = getManifest(mainClass);
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
