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

import com.epam.datalab.dto.UserInstanceStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.NonNull;

import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class BillingFilter {
    @NonNull
    private List<String> users;
    @NonNull
    private String datalabId;
    @NonNull
    @JsonProperty("date_start")
    private String dateStart;
    @NonNull
    @JsonProperty("date_end")
    private String dateEnd;
    @NonNull
    @JsonProperty("resource_type")
    private List<String> resourceTypes;
    @NonNull
    private List<UserInstanceStatus> statuses = Collections.emptyList();
    @NonNull
    private List<String> projects;
    @NonNull
    private List<String> products;
    @NonNull
    private List<String> shapes;
}
