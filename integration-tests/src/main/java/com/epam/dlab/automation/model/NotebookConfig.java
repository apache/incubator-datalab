/***************************************************************************

Copyright (c) 2016, EPAM SYSTEMS INC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

****************************************************************************/

package com.epam.dlab.automation.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;

public class NotebookConfig {

    @JsonProperty("notebook_template")
    private String notebookTemplate;

    @JsonProperty("data_engine_type")
    private String dataEngineType;

    @JsonProperty("full_test")
    private boolean fullTest;


    @JsonProperty("timeout_notebook_create")
    private String timeoutNotebookCreate = "60m";

    @JsonProperty("timeout_notebook_startup")
    private String timeoutNotebookStartup = "20m";

    @JsonProperty("timeout_notebook_shutdown")
    private String timeoutNotebookShutdown = "20m";


    @JsonProperty("timeout_cluster_create")
    private String timeoutClusterCreate = "60m";

    @JsonProperty("timeout_cluster_terminate")
    private String timeoutClusterTerminate = "20m";


    @JsonProperty("timeout_lib_groups")
    private String timeoutLibGroups = "5m";

    @JsonProperty("timeout_lib_list")
    private String timeoutLibList = "5m";

    @JsonProperty("timeout_lib_install")
    private String timeoutLibInstall = "15m";

	@JsonProperty("timeout_image_create")
	private String timeoutImageCreate = "60m";

	@JsonProperty("image_test_required")
	private boolean imageTestRequired = false;


    public String getTimeoutNotebookCreate() {
    	return timeoutNotebookCreate;
    }

    public String getTimeoutNotebookStartup() {
    	return timeoutNotebookStartup;
    }

    public String getTimeoutNotebookShutdown() {
    	return timeoutNotebookShutdown;
    }

    public String getTimeoutClusterCreate() {
    	return timeoutClusterCreate;
    }

    public String getTimeoutClusterTerminate() {
    	return timeoutClusterTerminate;
    }

    public String getTimeoutLibGroups() {
    	return timeoutLibGroups;
    }

    public String getTimeoutLibList() {
    	return timeoutLibList;
    }

    public String getTimeoutLibInstall() {
    	return timeoutLibInstall;
    }

	public String getTimeoutImageCreate() {
		return timeoutImageCreate;
	}

    public String getNotebookTemplate() {
    	return notebookTemplate;
    }


    public String getDataEngineType() {
    	return dataEngineType;
    }


    public boolean isFullTest() {
    	return fullTest;
    }

	public boolean isImageTestRequired() {
		return imageTestRequired;
	}

	public void setImageTestRequired(boolean imageTestRequired) {
		this.imageTestRequired = imageTestRequired;
	}


	@Override
    public String toString() {
    	return MoreObjects.toStringHelper(this)
    			.add("timeoutClusterCreate", timeoutClusterCreate)
    			.add("timeoutClusterTerminate", timeoutClusterTerminate)
    			.add("timeoutLibGroups", timeoutLibGroups)
    			.add("timeoutLibInstall", timeoutLibInstall)
				.add("timeoutImageCreate", timeoutImageCreate)
    			.add("timeoutLibList", timeoutLibList)
    			.add("timeoutNotebookCreate", timeoutNotebookCreate)
    			.add("timeoutNotebookShutdown", timeoutNotebookShutdown)
    			.add("timeoutNotebookStartup", timeoutNotebookStartup)
    			.add("notebookTemplate", notebookTemplate)
    			.add("dataEngineType", dataEngineType)
    			.add("fullTest", fullTest)
				.add("imageTestRequired", imageTestRequired)
    			.toString();
    }

}
