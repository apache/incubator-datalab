/*
 * Copyright (c) 2017, EPAM SYSTEMS INC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.dlab.backendapi.resources.dto.aws;

import com.epam.dlab.dto.UserInstanceStatus;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * Stores info about billing report filter.
 */
@Data
public class AwsBillingFilter {
	@JsonProperty
	private List<String> user;
	@JsonProperty("dlab_id")
	private String dlabId;
	@JsonProperty
	private List<String> product;
	@JsonProperty("resource_type")
	private List<String> resourceType;
	@JsonProperty
	private List<String> shape;
	@JsonProperty("date_start")
	private String dateStart;
	@JsonProperty("date_end")
	private String dateEnd;
	@JsonProperty("status")
	private List<UserInstanceStatus> statuses = Collections.emptyList();
}
