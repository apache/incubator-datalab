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
import com.epam.dlab.dto.status.EnvResourceList;
import com.epam.dlab.dto.status.EnvStatusDTO;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.client.RESTService;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;

import static com.epam.dlab.rest.contracts.ApiCallbacks.INFRASTRUCTURE;
import static com.epam.dlab.rest.contracts.ApiCallbacks.STATUS_URI;

@Slf4j
public class ResourcesStatusCallbackHandler extends ResourceCallbackHandler<EnvStatusDTO> {

	@JsonCreator
	public ResourcesStatusCallbackHandler(
			@JacksonInject RESTService selfService, @JsonProperty("action") DockerAction
			action, @JsonProperty("uuid") String uuid, @JsonProperty("user") String user) {
		super(selfService, user, uuid, action);
	}

	@Override
	protected String getCallbackURI() {
		return INFRASTRUCTURE + STATUS_URI;
	}

	@Override
	protected EnvStatusDTO parseOutResponse(JsonNode resultNode, EnvStatusDTO baseStatus) {
		if (resultNode == null) {
			return baseStatus;
		}

		EnvResourceList resourceList;
		try {
			resourceList = mapper.readValue(resultNode.toString(), EnvResourceList.class);
		} catch (IOException e) {
			throw new DlabException("Docker response for UUID " + getUUID() + " not valid: " + e.getLocalizedMessage()
					, e);
		}

		baseStatus.withResourceList(resourceList)
				.withUptime(Date.from(Instant.now()));

		log.trace("Inner status {}", baseStatus);

		return baseStatus;
	}

	@Override
	public boolean handle(String fileName, byte[] content) throws Exception {
		try {
			return super.handle(fileName, content);
		} catch (Exception e) {
			log.warn("Could not retrive the status of resources for UUID {} and user {}: {}",
					getUUID(), getUser(), e.getLocalizedMessage(), e);
		}
		return true; // Always necessary return true for status response
	}

	@Override
	public void handleError(String errorMessage) {
		// Nothing action for status response
	}
}
