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

package com.epam.dlab.dto.status;

import com.epam.dlab.model.ResourceType;
import com.epam.dlab.util.mongo.IsoLocalDateTimeDeSerializer;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Describe the resource (host, cluster, storage) for check status in Cloud.
 */
@NoArgsConstructor
public class EnvResource {
	@JsonProperty
	private String id;
	@JsonProperty
	private String status;
	@JsonProperty
	private String name;
	@JsonProperty
	private ResourceType resourceType;
	@JsonProperty("project_name")
	private String project;
	@JsonDeserialize(using = IsoLocalDateTimeDeSerializer.class)
	@JsonProperty
	private LocalDateTime lastActivity;

	public EnvResource(String id, String name, ResourceType resourceType, String project) {
		this.id = id;
		this.name = name;
		this.resourceType = resourceType;
		this.project = project;
	}

	/**
	 * Return the id of resource. instanceId for host, clusterId for cluster, path for storage.
	 */
	public String getId() {
		return id;
	}

	/**
	 * Set the id of resource. instanceId for host, clusterId for cluster, path for storage.
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * Set the id of resource. instanceId for host, clusterId for cluster, path for storage.
	 */
	public EnvResource withId(String id) {
		setId(id);
		return this;
	}

	/**
	 * Return the status of resource.
	 */
	public String getStatus() {
		return status;
	}

	/**
	 * Set the status of resource.
	 */
	public void setStatus(String status) {
		this.status = status;
	}

	/**
	 * Set the status of resource.
	 */
	public EnvResource withStatus(String status) {
		setStatus(status);
		return this;
	}

	public String getName() {
		return name;
	}

	public ResourceType getResourceType() {
		return resourceType;
	}

	public void setName(String name) {
		this.name = name;
	}

	public EnvResource withName(String name) {
		setName(name);
		return this;
	}

	public EnvResource withResourceType(ResourceType resourceType) {
		setResourceType(resourceType);
		return this;
	}

	public void setResourceType(ResourceType resourceType) {
		this.resourceType = resourceType;
	}

	public LocalDateTime getLastActivity() {
		return lastActivity;
	}

	public void setLastActivity(LocalDateTime lastActivity) {
		this.lastActivity = lastActivity;
	}

	public EnvResource withLastActivity(LocalDateTime lastActivity) {
		setLastActivity(lastActivity);
		return this;
	}

	public ToStringHelper toStringHelper(Object self) {
		return MoreObjects.toStringHelper(self)
				.add("id", id)
				.add("status", status)
				.add("name", name)
				.add("resourceType", resourceType)
				.add("lastActivity", lastActivity);
	}

	@Override
	public String toString() {
		return toStringHelper(this).toString();
	}
}
