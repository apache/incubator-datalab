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

import com.epam.datalab.dto.aws.computational.ClusterConfig;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.MoreObjects;
import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;

import java.util.List;

/**
 * Stores info about new exploratory.
 */
@Data
public class ExploratoryCreateFormDTO {
    @NotBlank
    @JsonProperty
    private String image;
    @NotBlank
    @JsonProperty("template_name")
    private String templateName;
    @NotBlank
    @JsonProperty
    private String name;
    @NotBlank
    @JsonProperty
    private String project;
    @JsonProperty("custom_tag")
    private String exploratoryTag;
    @NotBlank
    @JsonProperty
    private String endpoint;
    @NotBlank
    @JsonProperty
    private String shape;
    @NotBlank
    @JsonProperty
    private String version;
    @JsonProperty("notebook_image_name")
    private String imageName;
    @JsonProperty("cluster_config")
    private List<ClusterConfig> clusterConfig;
    @JsonProperty("gpu_enabled")
    private Boolean enabledGPU;
    @JsonProperty("gpu_type")
    private String gpuType;
    @JsonProperty("gpu_count")
    private String gpuCount;

    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("name", name)
                .add("templateName", templateName)
                .add("shape", shape)
                .add("version", version)
                .add("image", image)
                .add("imageName", imageName)
                .toString();
    }
}
