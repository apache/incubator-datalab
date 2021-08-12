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

package com.epam.datalab.dto;

import com.epam.datalab.dto.aws.computational.ClusterConfig;
import com.epam.datalab.dto.computational.UserComputationalResource;
import com.epam.datalab.dto.exploratory.LibInstallDTO;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Stores info about the user notebook.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserInstanceDTO {
    @JsonProperty("_id")
    private String id;
    @JsonProperty("instance_id")
    private String instanceId;
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
    @JsonProperty("project")
    private String project;
    @JsonProperty("endpoint")
    private String endpoint;
    @JsonProperty("cloud_provider")
    private String cloudProvider;
    @JsonProperty("template_name")
    private String templateName;
    @JsonProperty
    private String status;
    @JsonProperty
    private String shape;
    @JsonProperty("exploratory_url")
    private List<ResourceURL> resourceUrl;
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
    @JsonProperty("last_activity")
    private LocalDateTime lastActivity;
    @JsonProperty("cluster_config")
    private List<ClusterConfig> clusterConfig;
    @JsonProperty
    private Map<String, String> tags;
    @JsonProperty("gpu_enabled")
    private boolean enabledGPU = false;
    @JsonProperty("gpu_type")
    private String gpuType;
    @JsonProperty("gpu_count")
    private String gpuCount;


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

    public UserInstanceDTO withClusterConfig(List<ClusterConfig> config) {
        setClusterConfig(config);
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

    public UserInstanceDTO withProject(String project) {
        setProject(project);
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
     * Sets library list.
     */
    public UserInstanceDTO withLibs(List<LibInstallDTO> libs) {
        setLibs(libs);
        return this;
    }

    public UserInstanceDTO withEndpoint(String endpoint) {
        setEndpoint(endpoint);
        return this;
    }

    public UserInstanceDTO withCloudProvider(String cloudProvider) {
        setCloudProvider(cloudProvider);
        return this;
    }

    public UserInstanceDTO withTags(Map<String, String> tags) {
        setTags(tags);
        return this;
    }

    public UserInstanceDTO withGPUType(String gpuType) {
        setGpuType(gpuType);
        return this;
    }

    public UserInstanceDTO withGPUEnabled(boolean gpuEnabled) {
        setEnabledGPU(gpuEnabled);
        return this;
    }

    public UserInstanceDTO withGPUCount(String gpuCount) {
        setGpuCount(gpuCount);
        return this;
    }
}
