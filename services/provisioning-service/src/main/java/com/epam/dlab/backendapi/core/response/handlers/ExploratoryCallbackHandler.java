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
import com.epam.dlab.dto.ResourceURL;
import com.epam.dlab.dto.UserInstanceStatus;
import com.epam.dlab.dto.exploratory.ExploratoryStatusDTO;
import com.epam.dlab.rest.client.RESTService;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

import static com.epam.dlab.rest.contracts.ApiCallbacks.EXPLORATORY;
import static com.epam.dlab.rest.contracts.ApiCallbacks.STATUS_URI;

public class ExploratoryCallbackHandler extends ResourceCallbackHandler<ExploratoryStatusDTO> {
	private static final Logger LOGGER = LoggerFactory.getLogger(ExploratoryCallbackHandler.class);

	private static final String INSTANCE_ID_FIELD = "instance_id";
	private static final String EXPLORATORY_ID_FIELD = "notebook_name";
	private static final String EXPLORATORY_PRIVATE_IP_FIELD = "ip";
	private static final String EXPLORATORY_URL_FIELD = "exploratory_url";
	private static final String EXPLORATORY_USER_FIELD = "exploratory_user";
	private static final String EXPLORATORY_PASSWORD_FIELD = "exploratory_pass";

	@JsonProperty
	private final String exploratoryName;

	@JsonCreator
	public ExploratoryCallbackHandler(@JacksonInject RESTService selfService,
									  @JsonProperty("action") DockerAction action,
									  @JsonProperty("uuid") String uuid, @JsonProperty("user") String user,
									  @JsonProperty("exploratoryName") String exploratoryName) {
		super(selfService, user, uuid, action);
		this.exploratoryName = exploratoryName;
	}

	@Override
	protected String getCallbackURI() {
		return EXPLORATORY + STATUS_URI;
	}

	@Override
	protected ExploratoryStatusDTO parseOutResponse(JsonNode resultNode, ExploratoryStatusDTO baseStatus) {
		if (resultNode == null) {
			return baseStatus;
		}
		final JsonNode nodeUrl = resultNode.get(EXPLORATORY_URL_FIELD);
		List<ResourceURL> url = null;
		if (nodeUrl != null) {
			try {
				url = mapper.readValue(nodeUrl.toString(), new TypeReference<List<ResourceURL>>() {
				});
			} catch (IOException e) {
				LOGGER.warn("Cannot parse field {} for UUID {} in JSON",
						RESPONSE_NODE + "." + RESULT_NODE + "." + EXPLORATORY_URL_FIELD, getUUID(), e);
			}
		}

		String exploratoryId = getTextValue(resultNode.get(EXPLORATORY_ID_FIELD));
		if (getAction() == DockerAction.CREATE && exploratoryId == null) {
			LOGGER.warn("Empty field {} for UUID {} in JSON", String.format("%s.%s.%s", RESPONSE_NODE, RESULT_NODE,
					EXPLORATORY_ID_FIELD), getUUID());
		}

		return baseStatus
				.withInstanceId(getTextValue(resultNode.get(INSTANCE_ID_FIELD)))
				.withExploratoryId(exploratoryId)
				.withExploratoryUrl(url)
				.withPrivateIp(getTextValue(resultNode.get(EXPLORATORY_PRIVATE_IP_FIELD)))
				.withExploratoryUser(getTextValue(resultNode.get(EXPLORATORY_USER_FIELD)))
				.withExploratoryPassword(getTextValue(resultNode.get(EXPLORATORY_PASSWORD_FIELD)));
	}

	@Override
	protected ExploratoryStatusDTO getBaseStatusDTO(UserInstanceStatus status) {
		return super.getBaseStatusDTO(status).withExploratoryName(exploratoryName);
	}
}
