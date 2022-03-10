/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.epam.datalab.automation.helper;

import com.epam.datalab.automation.exceptions.LoadFailException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileReader;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Properties;

public class ConfigPropertyValue {

	private static final Logger LOGGER = LogManager.getLogger(ConfigPropertyValue.class);
	private static final String CONFIG_FILE_NAME;

	public static final String JENKINS_USERNAME = "JENKINS_USERNAME";
	public static final String JENKINS_PASS = "JENKINS_PASSWORD";
	private static final String USERNAME = "USERNAME";
	private static final String PASS = "PASSWORD";
	private static final String NOT_IAM_USERNAME = "NOT_IAM_USERNAME";
	private static final String NOT_IAM_PASS = "NOT_IAM_PASSWORD";
	private static final String NOT_DATALAB_USERNAME = "NOT_DATALAB_USERNAME";
	private static final String NOT_DATALAB_PASS = "NOT_DATALAB_PASSWORD";
	private static final String JENKINS_JOB_URL = "JENKINS_JOB_URL";
	private static final String USER_FOR_ACTIVATE_KEY = "USER_FOR_ACTIVATE_KEY";
	private static final String PASS_FOR_ACTIVATE_KEY = "PASSWORD_FOR_ACTIVATE_KEY";
	private static final String ACCESS_KEY_PRIV_FILE_NAME = "ACCESS_KEY_PRIV_FILE_NAME";
	private static final String ACCESS_KEY_PUB_FILE_NAME = "ACCESS_KEY_PUB_FILE_NAME";

	private static final String CLOUD_PROVIDER = "CLOUD_PROVIDER";

	private static final String AWS_ACCESS_KEY_ID = "AWS_ACCESS_KEY_ID";
	private static final String AWS_SECRET_ACCESS_KEY = "AWS_SECRET_ACCESS_KEY";
	private static final String AWS_REGION = "AWS_REGION";
	private static final String AWS_REQUEST_TIMEOUT = "AWS_REQUEST_TIMEOUT";

	private static final String AZURE_REGION = "AZURE_REGION";
	private static final String AZURE_REQUEST_TIMEOUT = "AZURE_REQUEST_TIMEOUT";
	private static final String AZURE_DATALAKE_ENABLED = "AZURE_DATALAKE_ENABLED";
	private static final String AZURE_DATALAKE_SHARED_ACCOUNT = "AZURE_DATALAKE_SHARED_ACCOUNT";
	private static final String AZURE_STORAGE_SHARED_ACCOUNT = "AZURE_STORAGE_SHARED_ACCOUNT";
	private static final String AZURE_AUTHENTICATION_FILE = "AZURE_AUTHENTICATION_FILE";

	private static final String GCP_DATALAB_PROJECT_ID = "GCP_DATALAB_PROJECT_ID";
	private static final String GCP_REGION = "GCP_REGION";
	private static final String GCP_REQUEST_TIMEOUT = "GCP_REQUEST_TIMEOUT";
	private static final String GCP_AUTHENTICATION_FILE = "GCP_AUTHENTICATION_FILE";

	private static final String TIMEOUT_JENKINS_AUTOTEST = "TIMEOUT_JENKINS_AUTOTEST";
	private static final String TIMEOUT_UPLOAD_KEY = "TIMEOUT_UPLOAD_KEY";
	private static final String TIMEOUT_SSN_STARTUP = "TIMEOUT_SSN_STARTUP";

	private static final String CLUSTER_OS_USERNAME = "CLUSTER_OS_USERNAME";
	private static final String CLUSTER_OS_FAMILY = "CLUSTER_OS_FAMILY";
    private static final String CONF_TAG_RESOURCE_ID = "CONF_TAG_RESOURCE_ID";

	private static final String JUPYTER_SCENARIO_FILES = "JUPYTER_SCENARIO_FILES";
	private static final String NOTEBOOKS_TO_TEST = "NOTEBOOKS_TO_TEST";
	private static final String SKIPPED_LIBS = "SKIPPED_LIBS";
	private static final String EXECUTION_TREADS = "execution.threads";

    private static final String USE_JENKINS = "USE_JENKINS";
    private static final String SSN_URL = "SSN_URL";
    private static final String SERVICE_BASE_NAME = "SERVICE_BASE_NAME";
    private static final String RUN_MODE_LOCAL = "RUN_MODE_LOCAL";
    private static final String LOCALHOST_IP = "LOCALHOST_IP";

    private static String jenkinsBuildNumber;


    private static final Properties props = new Properties();

    static {
        CONFIG_FILE_NAME = PropertiesResolver.getConfFileLocation();
        jenkinsBuildNumber = System.getProperty("jenkins.buildNumber", "");
        if (jenkinsBuildNumber.isEmpty()) {
            jenkinsBuildNumber = null;
            LOGGER.info("Jenkins build number missed");
        }
        
    	loadProperties();
    }
    
    private ConfigPropertyValue() { }
	
    private static Duration getDuration(String duaration) {
    	return Duration.parse("PT" + duaration);
    }
    
	public static String get(String propertyName) {
		return get(propertyName, "");
	}

	public static String get(String propertyName, String defaultValue) {
		return props.getProperty(propertyName, defaultValue);
	}

	private static int getInt(String value) {
        return Integer.parseInt(value);
    }
	
	public static int get(String propertyName, int defaultValue) {
		if (props.values().isEmpty()) {
			loadProperties();
		}
		String s = props.getProperty(propertyName, String.valueOf(defaultValue)); 
		return Integer.parseInt(s);
	}
	
	private static void printProperty(String propertyName) {
        LOGGER.info("{} is {}", propertyName , props.getProperty(propertyName));
	}
	
	private static void setKeyProperty(String propertyName) {
		String s = props.getProperty(propertyName, "");
		if (!s.isEmpty()) {
            s = Paths.get(PropertiesResolver.getKeysLocation(), s).toAbsolutePath().toString();
            props.setProperty(propertyName, s);
        }
	}
	
	private static void loadProperties() {
        try (FileReader fin = new FileReader(new File(CONFIG_FILE_NAME))) {

	        props.load(fin);

	        PropertiesResolver.overlapProperty(props, CLUSTER_OS_USERNAME, true);
	        PropertiesResolver.overlapProperty(props, CLUSTER_OS_FAMILY, true);
	        PropertiesResolver.overlapProperty(props, AWS_REGION, true);
	        PropertiesResolver.overlapProperty(props, AZURE_REGION, true);
	        PropertiesResolver.overlapProperty(props, GCP_DATALAB_PROJECT_ID, true);
	        PropertiesResolver.overlapProperty(props, GCP_REGION, true);
	        PropertiesResolver.overlapProperty(props, NOTEBOOKS_TO_TEST, false);
	        PropertiesResolver.overlapProperty(props, SKIPPED_LIBS, true);
	        PropertiesResolver.overlapProperty(props, USE_JENKINS, true);
	        PropertiesResolver.overlapProperty(props, JENKINS_JOB_URL, !isUseJenkins());
	        PropertiesResolver.overlapProperty(props, SSN_URL, isUseJenkins());
	        PropertiesResolver.overlapProperty(props, SERVICE_BASE_NAME, isUseJenkins());
	        PropertiesResolver.overlapProperty(props, RUN_MODE_LOCAL, true);

	        setKeyProperty(ACCESS_KEY_PRIV_FILE_NAME);
            setKeyProperty(ACCESS_KEY_PUB_FILE_NAME);
        } catch (Exception e) {
        	LOGGER.fatal("Load properties from file {} fails.", CONFIG_FILE_NAME, e);
	        throw new LoadFailException("Load properties from \"" + CONFIG_FILE_NAME + "\" fails. " +
			        e.getLocalizedMessage(), e);
        }

		printProperty(JENKINS_USERNAME);
		printProperty(JENKINS_PASS);
		printProperty(USERNAME);
		printProperty(PASS);
		printProperty(NOT_IAM_USERNAME);
		printProperty(NOT_IAM_PASS);
		printProperty(NOT_DATALAB_USERNAME);
		printProperty(NOT_DATALAB_PASS);
		printProperty(JENKINS_JOB_URL);
		printProperty(USER_FOR_ACTIVATE_KEY);
		printProperty(PASS_FOR_ACTIVATE_KEY);
		printProperty(ACCESS_KEY_PRIV_FILE_NAME);
		printProperty(ACCESS_KEY_PUB_FILE_NAME);

		printProperty(TIMEOUT_JENKINS_AUTOTEST);
		printProperty(TIMEOUT_UPLOAD_KEY);
		printProperty(TIMEOUT_SSN_STARTUP);

        printProperty(JUPYTER_SCENARIO_FILES);
        printProperty(CLOUD_PROVIDER);

        printProperty(AZURE_DATALAKE_ENABLED);
        printProperty(AZURE_DATALAKE_SHARED_ACCOUNT);
        printProperty(AZURE_STORAGE_SHARED_ACCOUNT);
        printProperty(NOTEBOOKS_TO_TEST);
		printProperty(SKIPPED_LIBS);
		printProperty(CLUSTER_OS_USERNAME);
        printProperty(CLUSTER_OS_FAMILY);
        printProperty(CONF_TAG_RESOURCE_ID);

        printProperty(USE_JENKINS);
        printProperty(RUN_MODE_LOCAL);
        printProperty(LOCALHOST_IP);
	}
    
    
    public static String getJenkinsBuildNumber() {
    	return jenkinsBuildNumber;
    }

    public static void setJenkinsBuildNumber(String jenkinsBuildNumber) {
    	ConfigPropertyValue.jenkinsBuildNumber = jenkinsBuildNumber;
    }

    public static String getJenkinsUsername() {
    	return get(JENKINS_USERNAME);
    }
    
    public static String getJenkinsPassword() {
		return get(JENKINS_PASS);
    }

    public static String getUsername() {
    	return get(USERNAME);
    }
    
    public static String getUsernameSimple() {
    	String s = get(USERNAME);
		int i = s.indexOf('@');
		return (i == -1 ? s : s.substring(0, i).toLowerCase());
	}

    public static String getPassword() {
		return get(PASS);
    }

	public static String getNotIAMUsername() {
		return get(NOT_IAM_USERNAME);
	}

	public static String getNotIAMPassword() {
		return get(NOT_IAM_PASS);
	}

	public static String getNotDataLabUsername() {
		return get(NOT_DATALAB_USERNAME);
	}

	public static String getNotDataLabPassword() {
		return get(NOT_DATALAB_PASS);
	}

	public static String getJenkinsJobURL() {
		return get(JENKINS_JOB_URL);
	}

	public static String getUserForActivateKey() {
		return get(USER_FOR_ACTIVATE_KEY);
	}

    public static String getPasswordForActivateKey() {
		return get(PASS_FOR_ACTIVATE_KEY);
    }


    public static String getAccessKeyPrivFileName() {
    	File file = new File(get(ACCESS_KEY_PRIV_FILE_NAME));
        return file.getAbsolutePath();
    }

    public static String getAccessKeyPubFileName() {
    	File file = new File(get(ACCESS_KEY_PUB_FILE_NAME));
        return file.getAbsolutePath();
    }

    public static String getCloudProvider(){
        return get(CLOUD_PROVIDER);
    }

    public static String getAzureAuthFileName(){
        File file = new File(get(AZURE_AUTHENTICATION_FILE));
        return file.getAbsolutePath();
    }

	public static String getGcpAuthFileName() {
		File file = new File(get(GCP_AUTHENTICATION_FILE));
		return file.getAbsolutePath();
	}

    public static String getAwsAccessKeyId() {
        return get(AWS_ACCESS_KEY_ID);
    }

    public static String getAwsSecretAccessKey() {
        return get(AWS_SECRET_ACCESS_KEY);
    }

	public static String getAwsRegion() {
	    return get(AWS_REGION);
	}

	public static Duration getAwsRequestTimeout() {
    	return getDuration(get(AWS_REQUEST_TIMEOUT, "10s"));
    }

    public static String getAzureRegion() {
        return get(AZURE_REGION);
    }

    public static String getAzureDatalakeEnabled() {
        return get(AZURE_DATALAKE_ENABLED);
    }

	public static String getAzureDatalakeSharedAccount() {
		return get(AZURE_DATALAKE_SHARED_ACCOUNT);
	}

	public static String getAzureStorageSharedAccount() {
		return get(AZURE_STORAGE_SHARED_ACCOUNT);
	}

	public static String getGcpDataLabProjectId() {
		return get(GCP_DATALAB_PROJECT_ID);
	}

	public static String getGcpRegion() {
		return get(GCP_REGION);
	}

	public static Duration getGcpRequestTimeout() {
		return getDuration(get(GCP_REQUEST_TIMEOUT, "10s"));
	}

    public static Duration getAzureRequestTimeout() {
        return getDuration(get(AZURE_REQUEST_TIMEOUT, "10s"));
    }

    public static Duration getTimeoutJenkinsAutotest() {
    	return getDuration(get(TIMEOUT_JENKINS_AUTOTEST, "0s"));
    }

    public static int getExecutionThreads() {
        return getInt(get(EXECUTION_TREADS, "-1"));
    }

    public static Duration getTimeoutUploadKey() {
    	return getDuration(get(TIMEOUT_UPLOAD_KEY, "0s"));
    }

    public static Duration getTimeoutSSNStartup() {
    	return getDuration(get(TIMEOUT_SSN_STARTUP, "0s"));
    }


    public static String getClusterOsUser() {
    	return get(CLUSTER_OS_USERNAME);
    }

    public static String getClusterOsFamily() {
    	return get(CLUSTER_OS_FAMILY);
    }

    public static String getNotebookTemplates() {
    	return get(NOTEBOOKS_TO_TEST);
    }

	public static String getSkippedLibs() {
		return get(SKIPPED_LIBS, "[]");
	}

	public static boolean isUseJenkins() {
        String s = get(USE_JENKINS, "true");
    	return Boolean.valueOf(s);
    }
    
    public static String getSsnUrl() {
        return get(SSN_URL);
    }
    
    public static String getServiceBaseName() {
        return get(SERVICE_BASE_NAME);
    }
    
    public static boolean isRunModeLocal() {
    	String s = get(RUN_MODE_LOCAL, "false");
    	return Boolean.valueOf(s);
    }
}
