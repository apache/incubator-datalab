package org.apache.dlab.util;

import lombok.extern.slf4j.Slf4j;

import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;

@Slf4j
public class PropertyHelper {

	private final static Properties PROPERTIES;

	static {
		PROPERTIES = new Properties();

		try (InputStream inputStream = new FileInputStream("/Users/ofuks/work/gitwork/incubator-dlab/integration-tests-cucumber/src/test/resources/config.properties")) {
			PROPERTIES.load(inputStream);
			log.info("Configs: {}", PROPERTIES);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public static String read(String prop) {
		return PROPERTIES.getProperty(prop);
	}
}
