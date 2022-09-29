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

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.util.List;

/**
 * Stores the health statuses for environment resources.
 */
@Data
@Builder
public class HealthStatusPageDTO {
    @JsonProperty
    private String status;
    @JsonProperty("list_resources")
    private List<HealthStatusResource> listResources;
    @JsonProperty
    private boolean billingEnabled;
    @JsonProperty
    private boolean auditEnabled;
    @JsonProperty
    private boolean admin;
    @JsonProperty
    private boolean projectAdmin;
    @JsonProperty
    private boolean projectAssigned;
    @JsonProperty
    private BucketBrowser bucketBrowser;
    @JsonProperty
    private ConnectedPlatforms connectedPlatforms;

    @Builder
    @Data
    public static class BucketBrowser {
        private final boolean view;
        private final boolean upload;
        private final boolean download;
        private final boolean delete;
    }

    @Builder
    @Data
    public static class ConnectedPlatforms {
        private final boolean view;
        private final boolean add;
        private final boolean disconnect;
    }
}