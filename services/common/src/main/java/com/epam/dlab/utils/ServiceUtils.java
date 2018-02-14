package com.epam.dlab.utils;

import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.exceptions.DlabException;

import java.io.IOException;
import java.net.JarURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class ServiceUtils {

	private static String includePath = null;

	private ServiceUtils() {
	}

	static {
		includePath = System.getenv(ServiceConsts.DLAB_CONF_DIR_NAME);
		if (includePath == null || includePath.isEmpty()) {
			includePath = getUserDir();
		}
	}

	/* Return working directory.
	 */
	public static String getUserDir() {
		return System.getProperty("user.dir");
	}

	/**
	 * Return path to DLab configuration directory.
	 *
	 * @return
	 */
	public static String getConfPath() {
		return includePath;
	}


	/**
	 * Return manifest for given class or empty manifest if {@link JarFile#MANIFEST_NAME} not found.
	 *
	 * @param clazz class.
	 * @throws IOException
	 */
	private static Manifest getManifestForClass(Class<?> clazz) throws IOException {
		URL url = clazz.getClassLoader().getResource(JarFile.MANIFEST_NAME);
		return (url == null ? new Manifest() : new Manifest(url.openStream()));
	}

	/**
	 * Return manifest from JAR file.
	 *
	 * @param classPath path to class in JAR file.
	 * @throws IOException
	 */
	private static Manifest getManifestFromJar(String classPath) throws IOException {
		URL url = new URL(classPath);
		JarURLConnection jarConnection = (JarURLConnection) url.openConnection();
		return jarConnection.getManifest();
	}

	/**
	 * Return manifest map for given class or empty map if manifest not found or cannot be read.
	 *
	 * @param clazz class.
	 */
	public static Map<String, String> getManifest(Class<?> clazz) {
		String className = "/" + clazz.getName().replace('.', '/') + ".class";
		String classPath = clazz.getResource(className).toString();

		Map<String, String> map = new HashMap<>();
		try {
			Manifest manifest = (classPath.startsWith("jar:file:") ? getManifestFromJar(classPath) : getManifestForClass(clazz));
			Attributes attributes = manifest.getMainAttributes();
			attributes.forEach((key, value) -> map.put(key.toString(), (String) value));
		} catch (IOException e) {
			System.err.println("Cannot found or open manifest for class " + className);
			throw new DlabException("Cannot read manifest file", e);
		}

		return map;
	}

	/**
	 * Print to standard output the manifest info about application. If parameter <b>args</b> is not
	 * <b>null</b> and one or more arguments have value -v or --version then print version and return <b>true<b/>
	 * otherwise <b>false</b>.
	 *
	 * @param mainClass the main class of application.
	 * @param args      the arguments of main class function or null.
	 * @return if parameter <b>args</b> is not null and one or more arguments have value -v or --version
	 * then return <b>true<b/> otherwise <b>false</b>.
	 */
	public static boolean printAppVersion(Class<?> mainClass, String... args) {
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
