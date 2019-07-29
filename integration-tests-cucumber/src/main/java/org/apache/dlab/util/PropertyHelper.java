package org.apache.dlab.util;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

public class PropertyHelper {

	private final static Properties PROPERTIES;

	static {
		PROPERTIES = new Properties();
		try (InputStream inputStream = new FileInputStream(System.getProperty("config.file"))) {
			PROPERTIES.load(inputStream);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static String read(String prop) {
		return PROPERTIES.getProperty(prop);
	}
}
