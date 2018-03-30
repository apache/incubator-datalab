/*
 * Copyright (c) 2017, EPAM SYSTEMS INC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.dlab.dto;

import com.epam.dlab.dto.computational.UserComputationalResource;
import com.epam.dlab.dto.exploratory.ExploratoryURL;
import com.epam.dlab.dto.exploratory.LibInstallDTO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Stores info about the user notebook.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserInstanceDTO {
	@JsonProperty("_id")
	private String id;
	@JsonProperty
	private String user;
	@JsonProperty("exploratory_name")
	private String exploratoryName;
	@JsonProperty("exploratory_id")
	private String exploratoryId;
	@JsonProperty("image")
	private String imageName;
	@JsonProperty("version")
	private String imageVersion;
	@JsonProperty("template_name")
	private String templateName;
	@JsonProperty
	private String status;
	@JsonProperty
	private String shape;
	@JsonProperty("exploratory_url")
	private List<ExploratoryURL> exploratoryUrl;
	@JsonProperty("up_time")
	private Date uptime;
	@JsonProperty("computational_resources")
	private List<UserComputationalResource> resources = new ArrayList<>();
	@JsonProperty("private_ip")
	private String privateIp;
	@JsonProperty("scheduler_data")
	private SchedulerJobDTO schedulerData;
	@JsonProperty("reupload_key_required")
	private boolean reuploadKeyRequired = false;
	@JsonInclude(JsonInclude.Include.NON_EMPTY)
	private List<LibInstallDTO> libs = Collections.emptyList();

	/**
	 * Sets the user login name.
	 */
	public UserInstanceDTO withUser(String user) {
		setUser(user);
		return this;
	}

	/**
	 * Sets the name of exploratory.
	 */
	public UserInstanceDTO withExploratoryName(String exploratoryName) {
		setExploratoryName(exploratoryName);
		return this;
	}

	/**
	 * Sets the exploratory id.
	 */
	public UserInstanceDTO withExploratoryId(String exploratoryId) {
		setExploratoryId(exploratoryId);
		return this;
	}

	/**
	 * Sets the image name.
	 */
	public UserInstanceDTO withImageName(String imageName) {
		setImageName(imageName);
		return this;
	}

	/**
	 * Sets the image version.
	 */
	public UserInstanceDTO withImageVersion(String imageVersion) {
		setImageVersion(imageVersion);
		return this;
	}

	/**
	 * Sets the name of template.
	 */
	public UserInstanceDTO withTemplateName(String templateName) {
		setTemplateName(templateName);
		return this;
	}

	/**
	 * Sets the status of notebook.
	 */
	public UserInstanceDTO withStatus(String status) {
		setStatus(status);
		return this;
	}

	/**
	 * Sets the name of notebook shape.
	 */
	public UserInstanceDTO withShape(String shape) {
		setShape(shape);
		return this;
	}

	/**
	 * Sets the URL of exploratory.
	 */
	public UserInstanceDTO withExploratoryUrl(List<ExploratoryURL> exploratoryUrl) {
		setExploratoryUrl(exploratoryUrl);
		return this;
	}

	/**
	 * Sets the date and time when the notebook has created.
	 */
	public UserInstanceDTO withUptime(Date uptime) {
		setUptime(uptime);
		return this;
	}

	/**
	 * Sets private IP address.
	 */
	public UserInstanceDTO withPrivateIp(String privateIp) {
		setPrivateIp(privateIp);
		return this;
	}

	/**
	 * Sets a list of user's computational resources for notebook.
	 */
	public UserInstanceDTO withResources(List<UserComputationalResource> resources) {
		setResources(resources);
		return this;
	}

	/**
	 * Sets scheduler data.
	 */
	public UserInstanceDTO withSchedulerData(SchedulerJobDTO schedulerData) {
		setSchedulerData(schedulerData);
		return this;
	}

	/**
	 * Sets value of requirement key reuploading.
	 */
	public UserInstanceDTO withReuploadKeyRequirement(boolean reuploadKeyRequired) {
		setReuploadKeyRequired(reuploadKeyRequired);
		return this;
	}

	/**
	 * Sets library list.
	 */
	public UserInstanceDTO withLibs(List<LibInstallDTO> libs) {
		setLibs(libs);
		return this;
	}
}
