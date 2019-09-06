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

package com.epam.dlab.backendapi.resources.dto;

import com.epam.dlab.dto.UserInstanceStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
public abstract class BillingFilter {
	@JsonProperty
	protected List<String> user;
	@JsonProperty("dlab_id")
	protected String dlabId;
	@JsonProperty("resource_type")
	protected List<String> resourceType;
	@JsonProperty("date_start")
	protected String dateStart;
	@JsonProperty("date_end")
	protected String dateEnd;
	@JsonProperty("status")
	protected List<UserInstanceStatus> statuses = Collections.emptyList();

	public abstract List<String> getShapes();
}
