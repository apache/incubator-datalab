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

package com.epam.datalab.automation.test.libs;

import com.epam.datalab.automation.helper.NamingHelper;

import static com.epam.datalab.automation.helper.NamingHelper.DEEPLEARNING;
import static com.epam.datalab.automation.helper.NamingHelper.JUPYTER;
import static com.epam.datalab.automation.helper.NamingHelper.RSTUDIO;
import static com.epam.datalab.automation.helper.NamingHelper.TENSOR;
import static com.epam.datalab.automation.helper.NamingHelper.ZEPPELIN;

public class LibsHelper {

	private static final String LIB_GROUPS_JSON = "lib_groups.json";
	private static final String LIB_LIST_JSON = "lib_list.json";

    public static String getLibGroupsPath(String notebookName){
		if (notebookName.contains(NamingHelper.getSimpleNotebookNames().get(DEEPLEARNING))) {
			return DEEPLEARNING + "/" + LIB_GROUPS_JSON;
		} else if (notebookName.contains(NamingHelper.getSimpleNotebookNames().get(JUPYTER))) {
			return JUPYTER + "/" + LIB_GROUPS_JSON;
		} else if (notebookName.contains(NamingHelper.getSimpleNotebookNames().get(RSTUDIO))) {
			return RSTUDIO + "/" + LIB_GROUPS_JSON;
		} else if (notebookName.contains(NamingHelper.getSimpleNotebookNames().get(TENSOR))) {
			return TENSOR + "/" + LIB_GROUPS_JSON;
		} else if (notebookName.contains(NamingHelper.getSimpleNotebookNames().get(ZEPPELIN))) {
			return ZEPPELIN + "/" + LIB_GROUPS_JSON;
		} else return LIB_GROUPS_JSON;
    }

    public static String getLibListPath(String notebookName){
		if (notebookName.contains(NamingHelper.getSimpleNotebookNames().get(DEEPLEARNING))) {
			return DEEPLEARNING + "/" + LIB_LIST_JSON;
		} else if (notebookName.contains(NamingHelper.getSimpleNotebookNames().get(JUPYTER))) {
			return JUPYTER + "/" + LIB_LIST_JSON;
		} else if (notebookName.contains(NamingHelper.getSimpleNotebookNames().get(RSTUDIO))) {
			return RSTUDIO + "/" + LIB_LIST_JSON;
		} else if (notebookName.contains(NamingHelper.getSimpleNotebookNames().get(TENSOR))) {
			return TENSOR + "/" + LIB_LIST_JSON;
		} else if (notebookName.contains(NamingHelper.getSimpleNotebookNames().get(ZEPPELIN))) {
			return NamingHelper.ZEPPELIN + "/" + LIB_LIST_JSON;
		} else return LIB_LIST_JSON;
    }
}
