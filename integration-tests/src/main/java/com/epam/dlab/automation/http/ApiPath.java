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

package com.epam.datalab.automation.http;

public class ApiPath {

    public static final String LOGIN = "/api/user/login";
    public static final String LOGOUT = "/api/user/logout";
    public static final String UPLOAD_KEY = "/api/user/access_key"; 
    public static final String AUTHORIZE_USER = "/api/user/authorize";
    public static final String EXP_ENVIRONMENT = "/api/infrastructure_provision/exploratory_environment";
    public static final String PROVISIONED_RES = "/api/infrastructure/info";
    public static final String COMPUTATIONAL_RES = "/api/infrastructure_provision/computational_resources/dataengine-service";
    public static final String COMPUTATIONAL_RES_SPARK = "/api/infrastructure_provision/computational_resources/dataengine";
    private static final String STOP_NOTEBOOK = EXP_ENVIRONMENT + "/%s/stop";
    private static final String TERMINATE_CLUSTER =
			"/api/infrastructure_provision/computational_resources/%s/%s/terminate";
	private static final String START_CLUSTER = "/api/infrastructure_provision/computational_resources/%s/%s/start";
	private static final String STOP_CLUSTER = "/api/infrastructure_provision/computational_resources/%s/%s/stop";
    private static final String TERMINATE_NOTEBOOK = EXP_ENVIRONMENT + "/%s/terminate";
    public static final String LIB_GROUPS = "/api/infrastructure_provision/exploratory_environment/lib_groups";
    public static final String LIB_LIST = "/api/infrastructure_provision/exploratory_environment/search/lib_list";
    public static final String LIB_INSTALL = "/api/infrastructure_provision/exploratory_environment/lib_install";
    public static final String LIB_LIST_EXPLORATORY_FORMATTED = "/api/infrastructure_provision/exploratory_environment/lib_list/formatted";
    public static final String IMAGE_CREATION = "/api/infrastructure_provision/exploratory_environment/image";

    private ApiPath(){}


    private static String configureURL(String url, Object... args) {
        return String.format(url, args);        
    }
    
    public static String getStopNotebookUrl(String serviceBaseName) {
        return configureURL(STOP_NOTEBOOK, serviceBaseName);
    }
    
    public static String getTerminateClusterUrl(String notebookName, String desName) {
        return configureURL(TERMINATE_CLUSTER, notebookName, desName);
    }
    
    public static String getTerminateNotebookUrl(String serviceBaseName) {
        return configureURL(TERMINATE_NOTEBOOK, serviceBaseName);
    }

	public static String getStartClusterUrl(String notebookName, String desName) {
		return configureURL(START_CLUSTER, notebookName, desName);
	}

	public static String getStopClusterUrl(String notebookName, String desName) {
		return configureURL(STOP_CLUSTER, notebookName, desName);
	}
}
