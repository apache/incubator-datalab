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

package com.epam.datalab.backendapi.domain;

import com.epam.datalab.dto.UserInstanceStatus;
import com.epam.datalab.dto.billing.BillingResourceType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@JsonIgnoreProperties(ignoreUnknown = true)
@AllArgsConstructor
@NoArgsConstructor
public class BillingReportLine {
    private String datalabId;
    private String application;
    @JsonProperty("resource_name")
    private String resourceName;
    private String project;
    private String endpoint;
    private String user;
    @JsonProperty("from")
    private LocalDate usageDateFrom;
    @JsonProperty("to")
    private LocalDate usageDateTo;
    private String usageDate;
    private String product;
    private String usageType;
    private Double cost;
    private String currency;
    @JsonProperty("resource_type")
    private BillingResourceType resourceType;
    private UserInstanceStatus status;
    private String shape;
    private String exploratoryName;
}
