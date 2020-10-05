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

package com.epam.datalab.automation.docker;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class DockerContainer {

	@JsonProperty("Id")
	private String id;

	@JsonProperty("Names")
	private List<String> names;

	@JsonProperty("Image")
	private String image;

	@JsonProperty("ImageID")
	private String imageID;

	@JsonProperty("Command")
	private String command;

	@JsonProperty("Created")
	private int created;

	@JsonProperty("Ports")
	private List<Object> ports;

	@JsonProperty("Labels")
	private Labels labels;

	@JsonProperty("State")
	private String state;

	@JsonProperty("Status")
	private String status;

	@JsonProperty("HostConfig")
	private HostConfig hostConfig;

	@JsonProperty("NetworkSettings")
	private NetworkSettings networkSettings;

	@JsonProperty("Mounts")
	private List<Object> mounts;


    public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public List<String> getNames() {
		return names;
	}

	public void setNames(List<String> names) {
		this.names = names;
	}

	public String getImage() {
		return image;
	}

	public void setImage(String image) {
		this.image = image;
	}

	public String getImageID() {
		return imageID;
	}

	public void setImageID(String imageID) {
		this.imageID = imageID;
	}

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public int getCreated() {
		return created;
	}

	public void setCreated(int created) {
		this.created = created;
	}

	public List<Object> getPorts() {
		return ports;
	}

	public void setPorts(List<Object> ports) {
		this.ports = ports;
	}

	public Labels getLabels() {
		return labels;
	}

	public void setLabels(Labels labels) {
		this.labels = labels;
	}

	public String getState() {
		return state;
	}

	public void setState(String state) {
		this.state = state;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		this.status = status;
	}

	public HostConfig getHostConfig() {
		return hostConfig;
	}

	public void setHostConfig(HostConfig hostConfig) {
		this.hostConfig = hostConfig;
	}

	public NetworkSettings getNetworkSettings() {
		return networkSettings;
	}

	public void setNetworkSettings(NetworkSettings networkSettings) {
		this.networkSettings = networkSettings;
	}

	public List<Object> getMounts() {
		return mounts;
	}

	public void setMounts(List<Object> mounts) {
		this.mounts = mounts;
	}
}
