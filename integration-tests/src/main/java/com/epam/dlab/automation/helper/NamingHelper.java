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

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class NamingHelper {
	public static final String CLUSTER_ABSENT = "cluster_absent";
	public static final String DATA_ENGINE = "dataengine";
	public static final String DATA_ENGINE_SERVICE = "dataengine-service";
	public static final String DEEPLEARNING = "deeplearning";
	public static final String JUPYTER = "jupyter";
	public static final String TENSOR = "tensor";
	public static final String RSTUDIO = "rstudio";
	public static final String ZEPPELIN = "zeppelin";

	private static final Map<String, String> SIMPLE_NOTEBOOK_NAMES = new HashMap<>();

    private static AtomicInteger idCounter = new AtomicInteger(0);
    
    private static String serviceBaseName;
    private static String ssnURL;
    private static String ssnIp;
    private static String ssnToken;

	static {
		SIMPLE_NOTEBOOK_NAMES.put(DEEPLEARNING, "dlr");
		SIMPLE_NOTEBOOK_NAMES.put(JUPYTER, "jup");
		SIMPLE_NOTEBOOK_NAMES.put(TENSOR, "tfl");
		SIMPLE_NOTEBOOK_NAMES.put(RSTUDIO, "rst");
		SIMPLE_NOTEBOOK_NAMES.put(ZEPPELIN, "zep");
	}

    private NamingHelper(){}

	public static Map<String, String> getSimpleNotebookNames() {
		return SIMPLE_NOTEBOOK_NAMES;
	}

	public static String getServiceBaseName() {
    	return serviceBaseName;
    }
    
    public static void setServiceBaseName(String serviceBaseName) {
    	if (NamingHelper.serviceBaseName != null) {
    		throw new IllegalArgumentException("Field serviceBaseName already has a value");
    	}
    	NamingHelper.serviceBaseName = serviceBaseName;
    }
    
    public static String getSsnURL() {
    	return ssnURL;
    }
    
    public static void setSsnURL(String ssnURL) {
    	if (NamingHelper.ssnURL != null) {
    		throw new IllegalArgumentException("Field ssnURL already has a value");
    	}
    	NamingHelper.ssnURL = ssnURL;
    }

    public static String getSsnName() {
    	return serviceBaseName + "-ssn";
    }
    
    public static String getSsnIp() {
    	return ssnIp;
    }
    
    public static void setSsnIp(String ssnIp) {
    	if (NamingHelper.ssnIp != null) {
    		throw new IllegalArgumentException("Field ssnIp already has a value");
    	}
    	NamingHelper.ssnIp = ssnIp;
    }

    public static String getSsnToken() {
    	return ssnToken;
    }
    
    public static void setSsnToken(String ssnToken) {
    	if (NamingHelper.ssnToken != null) {
    		throw new IllegalArgumentException("Field ssnToken already has a value");
    	}
    	NamingHelper.ssnToken = ssnToken;
    }
    
    public static String getEdgeName() {
        switch (ConfigPropertyValue.getCloudProvider()) {
            case CloudProvider.AWS_PROVIDER:
				return String.join("-", serviceBaseName, ConfigPropertyValue.getUsernameSimple(), "edge");
            case CloudProvider.AZURE_PROVIDER:
			case CloudProvider.GCP_PROVIDER:
				return String.join("-", serviceBaseName, ConfigPropertyValue.getUsernameSimple(), "edge")
                        .replace('_', '-');
			default:
                return null;
        }
    }
    
    public static String getNotebookInstanceName(String notebookName) {
        switch (ConfigPropertyValue.getCloudProvider()) {
            case CloudProvider.AWS_PROVIDER:
				return String.join("-", serviceBaseName, ConfigPropertyValue.getUsernameSimple(), "nb", notebookName);
            case CloudProvider.AZURE_PROVIDER:
			case CloudProvider.GCP_PROVIDER:
				return String.join("-", serviceBaseName, ConfigPropertyValue.getUsernameSimple(), "nb", notebookName)
                        .replace('_', '-');
			default:
                return null;
        }
    }
    
    public static String getClusterInstanceName(String notebookName, String clusterName, String dataEngineType) {
		if (DATA_ENGINE.equals(dataEngineType)) {
            switch (ConfigPropertyValue.getCloudProvider()) {
                case CloudProvider.AWS_PROVIDER:
					return String.join("-", getClusterInstanceNameForTestDES(notebookName, clusterName,
							dataEngineType), "m");
                case CloudProvider.AZURE_PROVIDER:
				case CloudProvider.GCP_PROVIDER:
					return String.join("-", getClusterInstanceNameForTestDES(notebookName, clusterName,
							dataEngineType), "m").replace('_', '-');
				default:
                    return null;
            }
    	}
    	else {
    		return getClusterInstanceNameForTestDES(notebookName,clusterName,dataEngineType);
    	}
    }
    
    public static String getClusterInstanceNameForTestDES(String notebookName, String clusterName, String dataEngineType) {
        switch (ConfigPropertyValue.getCloudProvider()) {
            case CloudProvider.AWS_PROVIDER:
				return DATA_ENGINE.equals(dataEngineType) ?
						String.join("-", serviceBaseName, ConfigPropertyValue.getUsernameSimple(),
								"de", notebookName, clusterName) :
						String.join("-", serviceBaseName, ConfigPropertyValue.getUsernameSimple(),
								"des", notebookName, clusterName);

            case CloudProvider.AZURE_PROVIDER:
				return DATA_ENGINE.equals(dataEngineType) ?
						String.join("-", serviceBaseName, ConfigPropertyValue.getUsernameSimple(),
								"de", notebookName, clusterName).replace('_', '-') :
						String.join("-", serviceBaseName, ConfigPropertyValue.getUsernameSimple(),
								"des", notebookName, clusterName).replace('_', '-');

			case CloudProvider.GCP_PROVIDER:
				return DATA_ENGINE.equals(dataEngineType) ?
						String.join("-", serviceBaseName, ConfigPropertyValue.getUsernameSimple(),
								"de", notebookName, clusterName).replace('_', '-') :
						String.join("-", serviceBaseName, ConfigPropertyValue.getUsernameSimple(),
								"des", notebookName, clusterName, "m").replace('_', '-');
			default:
                return null;
        }

    }

	public static String getNotebookContainerName(String notebookName, String action) {
    	return String.join("_", ConfigPropertyValue.getUsernameSimple(), action, "exploratory", notebookName);
    }

	public static String getClusterContainerName(String notebookName, String clusterName, String action) {
		return String.join("_", ConfigPropertyValue.getUsernameSimple(), action, "computational",
				notebookName, clusterName);
    }
    
    public static String generateRandomValue() {
		SimpleDateFormat df = new SimpleDateFormat("yyyyMMddhmmss");
        return String.join("_",  "ITest", df.format(new Date()), String.valueOf(idCounter.incrementAndGet()));
    }

    public static String generateRandomValue(String notebokTemplateName) {
		return String.join("_", SIMPLE_NOTEBOOK_NAMES.get(notebokTemplateName),
				String.valueOf(idCounter.incrementAndGet()));
    }
    
    public static String getSelfServiceURL(String path) {
        return ssnURL + path;
    }
    
    public static String getStorageName() {
        switch (ConfigPropertyValue.getCloudProvider()) {
            case CloudProvider.AWS_PROVIDER:
			case CloudProvider.GCP_PROVIDER:
                return String.format("%s-%s-%s", serviceBaseName, ConfigPropertyValue.getUsernameSimple(),
                        CloudHelper.getStorageNameAppendix()).replace('_', '-').toLowerCase();
            case CloudProvider.AZURE_PROVIDER:
                return String.format("%s-%s-%s", serviceBaseName, "shared",
                        CloudHelper.getStorageNameAppendix()).replace('_', '-').toLowerCase();
			default:
                return null;
        }
    }

	public static String getClusterName(String clusterInstanceName, String dataEngineType, boolean restrictionMode)
			throws IOException {
		switch (ConfigPropertyValue.getCloudProvider()) {
			case CloudProvider.AWS_PROVIDER:
			case CloudProvider.AZURE_PROVIDER:
				return DATA_ENGINE.equals(dataEngineType) ? clusterInstanceName :
						CloudHelper.getInstanceNameByCondition(clusterInstanceName, restrictionMode);

			case CloudProvider.GCP_PROVIDER:
				return DATA_ENGINE.equals(dataEngineType) ? clusterInstanceName :
						CloudHelper.getGcpDataprocClusterName(
								CloudHelper.getInstanceNameByCondition(clusterInstanceName, restrictionMode));
			default:
				return null;
		}
    }

	public static String getNotebookTestTemplatesPath(String notebookName) {
		if (notebookName.contains(getSimpleNotebookNames().get(DEEPLEARNING))) {
            return "test_templates/deeplearning/";
		} else if (notebookName.contains(getSimpleNotebookNames().get(JUPYTER))) {
            return "test_templates/jupyter/";
		} else if (notebookName.contains(getSimpleNotebookNames().get(RSTUDIO))) {
            return "test_templates/rstudio/";
		} else if (notebookName.contains(getSimpleNotebookNames().get(TENSOR))) {
            return "test_templates/tensor/";
		} else if (notebookName.contains(getSimpleNotebookNames().get(ZEPPELIN))) {
            return "test_templates/zeppelin/";
        }
        else return "";

    }

    public static String getNotebookType(String notebookName){
		if (notebookName.contains(getSimpleNotebookNames().get(DEEPLEARNING))) {
			return DEEPLEARNING + "/";
		} else if (notebookName.contains(getSimpleNotebookNames().get(JUPYTER))) {
			return JUPYTER + "/";
		} else if (notebookName.contains(getSimpleNotebookNames().get(RSTUDIO))) {
			return RSTUDIO + "/";
		} else if (notebookName.contains(getSimpleNotebookNames().get(TENSOR))) {
			return TENSOR + "/";
		} else if (notebookName.contains(getSimpleNotebookNames().get(ZEPPELIN))) {
			return ZEPPELIN + "/";
        }
        else return "";

    }

	public static boolean isClusterRequired(String notebookName) {
		if (notebookName.contains(getSimpleNotebookNames().get(DEEPLEARNING))) {
			return false;
		} else if (notebookName.contains(getSimpleNotebookNames().get(JUPYTER))) {
			return true;
		} else if (notebookName.contains(getSimpleNotebookNames().get(RSTUDIO))) {
			return true;
		} else if (notebookName.contains(getSimpleNotebookNames().get(TENSOR))) {
			return false;
		} else if (notebookName.contains(getSimpleNotebookNames().get(ZEPPELIN))) {
			return true;
		}
		return true;
	}
}
