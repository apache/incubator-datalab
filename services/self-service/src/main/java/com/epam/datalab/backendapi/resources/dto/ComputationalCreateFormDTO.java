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
import lombok.Data;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.Valid;
import java.util.List;

/**
 * Stores info about creation of the computational resource.
 */
@Data
public class ComputationalCreateFormDTO {

    @NotBlank
    @JsonProperty("template_name")
    private String templateName;

    @NotBlank
    @JsonProperty
    private String image;

    @NotBlank
    @JsonProperty
    private String name;

    @NotBlank
    @JsonProperty
    private String project;
    @JsonProperty("custom_tag")
    private String customTag;

    @NotBlank
    @JsonProperty("notebook_name")
    private String notebookName;

    @JsonProperty("check_inactivity_required")
    private boolean checkInactivityRequired = true;

    @Valid
    private List<ClusterConfig> config;

    @JsonProperty("gpu_tag")
    protected boolean gpuTag = false;

}