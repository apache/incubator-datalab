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

package com.epam.datalab.model.library;

import com.epam.datalab.dto.exploratory.LibStatus;
import com.epam.datalab.model.ResourceType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class Library {
    private final String group;
    private final String name;
    private final String version;
    private final LibStatus status;
    @JsonProperty("error_message")
    private final String errorMessage;
    @JsonProperty("available_versions")
    private List<String> availableVersions;
    @JsonProperty("add_pkgs")
    private List<String> addedPackages;
    private String resourceName;
    private ResourceType type;

    public Library withType(ResourceType type) {
        setType(type);
        return this;
    }

    public Library withResourceName(String name) {
        setResourceName(name);
        return this;
    }
}
