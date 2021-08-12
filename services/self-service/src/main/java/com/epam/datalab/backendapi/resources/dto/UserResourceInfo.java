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
package com.epam.datalab.backendapi.resources.dto;

import com.epam.datalab.dto.ResourceURL;
import com.epam.datalab.dto.computational.UserComputationalResource;
import com.epam.datalab.model.ResourceEnum;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserResourceInfo {
    @JsonProperty
    private String user;
    @JsonProperty
    private String project;
    @JsonProperty
    private String endpoint;
    @JsonProperty("resource_type")
    private ResourceEnum resourceType;
    @JsonProperty("resource_name")
    private String resourceName;
    @JsonProperty("shape")
    private String resourceShape;
    @JsonProperty("status")
    private String resourceStatus;
    @JsonProperty("computational_resources")
    private List<UserComputationalResource> computationalResources;
    @JsonProperty("public_ip")
    private String ip;
    @JsonProperty("cloud_provider")
    private String cloudProvider;
    @JsonProperty("exploratory_urls")
    private List<ResourceURL> exploratoryUrls;

    @JsonProperty("gpu_enabled")
    private Boolean gpuEnabled;
    @JsonProperty("gpu_type")
    private String gpuType;
    @JsonProperty("gpu_count")
    private String gpuCount;
    @JsonProperty
    private Map<String, String> tags;
}
