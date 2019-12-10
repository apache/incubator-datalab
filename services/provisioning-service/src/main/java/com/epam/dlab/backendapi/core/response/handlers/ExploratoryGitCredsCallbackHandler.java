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

package com.epam.dlab.backendapi.core.response.handlers;

import com.epam.dlab.backendapi.core.commands.DockerAction;
import com.epam.dlab.dto.UserInstanceStatus;
import com.epam.dlab.dto.exploratory.ExploratoryStatusDTO;
import com.epam.dlab.rest.client.RESTService;
import com.epam.dlab.rest.contracts.ApiCallbacks;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ExploratoryGitCredsCallbackHandler extends ResourceCallbackHandler<ExploratoryStatusDTO> {

	@JsonProperty
	private final String exploratoryName;

	@JsonCreator
	public ExploratoryGitCredsCallbackHandler(@JacksonInject RESTService selfService,
											  @JsonProperty("action") DockerAction action,
											  @JsonProperty("uuid") String uuid,
											  @JsonProperty("user") String user,
											  @JsonProperty("exploratoryName") String exploratoryName) {
		super(selfService, user, uuid, action);
		this.exploratoryName = exploratoryName;
	}

	@Override
	protected String getCallbackURI() {
		return ApiCallbacks.GIT_CREDS;
	}

	@Override
	protected ExploratoryStatusDTO parseOutResponse(JsonNode resultNode, ExploratoryStatusDTO baseStatus) {
		log.trace("Parse GIT Creds: {}", resultNode);
		return baseStatus;
	}

	@Override
	protected ExploratoryStatusDTO getBaseStatusDTO(UserInstanceStatus status) {
		return super.getBaseStatusDTO(status)
				.withExploratoryName(exploratoryName);
	}
}
