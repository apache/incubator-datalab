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

package com.epam.datalab.dto.exploratory;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Stores info about libraries.
 */
@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class LibInstallDTO {
    @JsonProperty
    private String group;

    @JsonProperty
    private String name;

    @JsonProperty
    private String version;

    @JsonProperty
    private String status;

    @JsonProperty("error_message")
    private String errorMessage;

    @JsonProperty
    private boolean override;

    @JsonProperty("available_versions")
    private List<String> availableVersions;

    @JsonProperty("add_pkgs")
    private List<String> addedPackages;

    public LibInstallDTO(String group, String name, String version) {
        this.group = group;
        this.name = name;
        this.version = version;
    }

    public LibInstallDTO withName(String name) {
        setName(name);
        return this;
    }

    public LibInstallDTO withStatus(String status) {
        setStatus(status);
        return this;
    }

    public LibInstallDTO withErrorMessage(String errorMessage) {
        setErrorMessage(errorMessage);
        return this;
    }

    public LibInstallDTO withAddedPackages(List<String> addedPackages) {
        setAddedPackages(addedPackages);
        return this;
    }
}
