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

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

/**
 * Store info about libraries which user should be installed.
 */
@Getter
@Setter
@ToString(callSuper = true)
public class LibraryInstallDTO extends ExploratoryActionDTO<LibraryInstallDTO> {
    @JsonProperty("libs")
    private List<LibInstallDTO> libs;

    @JsonProperty("computational_id")
    private String computationalId;

    @JsonProperty("computational_image")
    private String computationalImage;

    @JsonProperty("computational_name")
    private String computationalName;

    public LibraryInstallDTO withLibs(List<LibInstallDTO> libs) {
        setLibs(libs);
        return this;
    }

    public LibraryInstallDTO withComputationalId(String computationalId) {
        setComputationalId(computationalId);
        return this;
    }

    public LibraryInstallDTO withComputationalImage(String computationalImage) {
        setComputationalImage(computationalImage);
        return this;
    }

    public LibraryInstallDTO withComputationalName(String computationalName) {
        setComputationalName(computationalName);
        return this;
    }
}
