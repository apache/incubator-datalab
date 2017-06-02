package com.epam.dlab.automation.helper;

import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Paths;
import java.util.Properties;


public class PropertiesResolver {

    private static final Logger LOGGER = LogManager.getLogger(PropertiesResolver.class);
    public static final boolean DEV_MODE;
    public static final String CONFIG_FILE_NAME = "application.properties";
    public static String NOTEBOOK_FILES_LOCATION_PROPERTY_TEMPLATE = "scenario.%s.files.location";
    public static String NOTEBOOK_CONFIGURATION_FILE_TEMPLATE = "%s-notebook.json";

    //keys from application.properties(dev-application.properties)
    private final static String CONF_FILE_LOCATION_PROPERTY = "conf.file.location";
    private static String KEYS_DIRECTORY_LOCATION_PROPERTY = "keys.directory.location";
    private static String NOTEBOOK_TEST_DATA_COPY_SCRIPT = "notebook.test.data.copy.script";
    private static String JUPYTER_FILES_LOCATION_PROPERTY = "scenario.jupyter.files.location";
    private static String RSTUDIO_FILES_LOCATION_PROPERTY = "scenario.rstudio.files.location";
    private static String ZEPPELIN_FILES_LOCATION_PROPERTY = "scenario.zeppelin.files.location";
    private static String CLUSTER_CONFIG_FILE_LOCATION_PROPERTY = "ec2.config.files.location";

    private static Properties properties = new Properties();

    static {
        DEV_MODE = System.getProperty("run.mode", "remote").equalsIgnoreCase("dev");
        loadApplicationProperties();

    }

	private static String getProperty(String propertyName, boolean isOptional) {
		String s = System.getProperty(propertyName, "");
		if (s.isEmpty() && !isOptional) {
        	throw new IllegalArgumentException("Missed required JVM argument -D" + propertyName);
        }
        return s;
	}

    public static String getConfRootPath() {
    	return getProperty("conf.root.path", false);
    }

    private static void loadApplicationProperties() {

        InputStream input = null;

        try {

            input = PropertiesResolver.class.getClassLoader().getResourceAsStream(CONFIG_FILE_NAME);

            // load a properties file
            properties.load(input);
            String rootPath = getConfRootPath();
            for (String key : properties.keySet().toArray(new String[0])) {
            	String path = StringUtils.replace(properties.getProperty(key), "${CONF_ROOT_PATH}", rootPath);
            	path = Paths.get(path).toAbsolutePath().toString();
            	properties.setProperty(key, path);
            }

            // get the property value and print it out
            LOGGER.info(properties.getProperty(CONF_FILE_LOCATION_PROPERTY));
            LOGGER.info(properties.getProperty(KEYS_DIRECTORY_LOCATION_PROPERTY));
            LOGGER.info(properties.getProperty(NOTEBOOK_TEST_DATA_COPY_SCRIPT));
            LOGGER.info(properties.getProperty(JUPYTER_FILES_LOCATION_PROPERTY));
            LOGGER.info(properties.getProperty(RSTUDIO_FILES_LOCATION_PROPERTY));
            LOGGER.info(properties.getProperty(ZEPPELIN_FILES_LOCATION_PROPERTY));
            LOGGER.info(properties.getProperty(CLUSTER_CONFIG_FILE_LOCATION_PROPERTY));

        } catch (IOException ex) {
            LOGGER.error(ex);
            LOGGER.error("Application configuration file could not be found by the path: {}", CONFIG_FILE_NAME);
            System.exit(0);
        } finally {
            if (input != null) {
                try {
                    input.close();
                } catch (IOException e) {
                    LOGGER.error(e);
                    LOGGER.error("Application configuration file could not be found by the path: {}", CONFIG_FILE_NAME);
                }
            }
        }
    }


    public static String getConfFileLocation() {
        return properties.getProperty(CONF_FILE_LOCATION_PROPERTY);
    }

    public static String getKeysLocation() {
        return properties.getProperty(KEYS_DIRECTORY_LOCATION_PROPERTY);
    }

    public static String getNotebookTestDataCopyScriptLocation() {
        return properties.getProperty(NOTEBOOK_TEST_DATA_COPY_SCRIPT);
    }

    public static String getJupyterFilesLocation() {
        return properties.getProperty(JUPYTER_FILES_LOCATION_PROPERTY);
    }

    public static String getRstudioFilesLocation() {
        return properties.getProperty(RSTUDIO_FILES_LOCATION_PROPERTY);
    }

    public static String getZeppelinFilesLocation() {
        return properties.getProperty(ZEPPELIN_FILES_LOCATION_PROPERTY);
    }

    public static String getClusterConfFileLocation() {
        return properties.getProperty(CLUSTER_CONFIG_FILE_LOCATION_PROPERTY );
    }

    public static String getPropertyByName(String propertyName) {
        return properties.getProperty(propertyName);
    }

}
