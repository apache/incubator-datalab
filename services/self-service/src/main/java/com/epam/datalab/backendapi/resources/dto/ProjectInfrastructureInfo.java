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

import com.epam.datalab.backendapi.domain.BillingReport;
import com.epam.datalab.backendapi.domain.EndpointDTO;
import com.epam.datalab.backendapi.domain.OdahuDTO;
import com.epam.datalab.dto.UserInstanceDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;
import java.util.Map;

@AllArgsConstructor
@Builder
@EqualsAndHashCode
@ToString
public class ProjectInfrastructureInfo {
	@JsonProperty
	private String project;
	@JsonProperty
	private int billingQuoteUsed;
	@JsonProperty
	private Map<String, Map<String, String>> shared;
	@JsonProperty
	private List<UserInstanceDTO> exploratory;
	@JsonProperty
	private List<BillingReport> exploratoryBilling;
	@JsonProperty
	private List<OdahuDTO> odahu;
	@JsonProperty
	private List<EndpointDTO> endpoints;
}
