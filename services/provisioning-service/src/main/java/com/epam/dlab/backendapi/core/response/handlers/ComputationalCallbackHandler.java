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
import com.epam.dlab.dto.base.computational.ComputationalBase;
import com.epam.dlab.dto.computational.ComputationalStatusDTO;
import com.epam.dlab.rest.client.RESTService;
import com.epam.dlab.rest.contracts.ApiCallbacks;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
public class ComputationalCallbackHandler extends ResourceCallbackHandler<ComputationalStatusDTO> {
	private static final String INSTANCE_ID_FIELD = "instance_id";
	private static final String COMPUTATIONAL_ID_FIELD = "hostname";
	private static final String COMPUTATIONAL_URL_FIELD = "computational_url";

	@JsonProperty
	private final ComputationalBase<?> dto;
	private ComputationalConfigure computationalConfigure;

	@JsonCreator
	public ComputationalCallbackHandler(@JacksonInject ComputationalConfigure computationalConfigure,
										@JacksonInject RESTService selfService,
										@JsonProperty("action") DockerAction action,
										@JsonProperty("uuid") String uuid,
										@JsonProperty("dto") ComputationalBase<?> dto) {

		super(selfService, dto.getCloudSettings().getIamUser(), uuid, action);
		this.computationalConfigure = computationalConfigure;
		this.dto = dto;
	}

	protected ComputationalBase<?> getDto() {
		return dto;
	}

	@Override
	protected String getCallbackURI() {
		return ApiCallbacks.COMPUTATIONAL + ApiCallbacks.STATUS_URI;
	}

	@Override
	protected ComputationalStatusDTO parseOutResponse(JsonNode resultNode, ComputationalStatusDTO baseStatus) {
		if (resultNode == null) {
			return baseStatus;
		}
		baseStatus.withComputationalUrl(extractUrl(resultNode));
		baseStatus.withLastActivity(Date.from(Instant.now()));

		if (DockerAction.CREATE == getAction()) {
			baseStatus
					.withInstanceId(instanceId(resultNode.get(INSTANCE_ID_FIELD)))
					.withComputationalId(getTextValue(resultNode.get(COMPUTATIONAL_ID_FIELD)));
			if (UserInstanceStatus.of(baseStatus.getStatus()) == UserInstanceStatus.RUNNING) {
				baseStatus.withStatus(UserInstanceStatus.CONFIGURING);
				computationalConfigure.configure(getUUID(), getDto());
			}
		}
		return baseStatus;
	}

	@Override
	protected ComputationalStatusDTO getBaseStatusDTO(UserInstanceStatus status) {
		return super.getBaseStatusDTO(status)
				.withExploratoryName(dto.getExploratoryName())
				.withComputationalName(dto.getComputationalName());
	}

	private String instanceId(JsonNode jsonNode) {
		if (jsonNode != null && jsonNode.isArray()) {
			return StreamSupport.stream(jsonNode.spliterator(), false)
					.map(JsonNode::textValue)
					.collect(Collectors.joining(";"));
		} else {
			return getTextValue(jsonNode);
		}

	}

	private List<ResourceURL> extractUrl(JsonNode resultNode) {
		final JsonNode nodeUrl = resultNode.get(COMPUTATIONAL_URL_FIELD);
		return Optional.ofNullable(nodeUrl)
				.map(this::getUrls)
				.orElse(Collections.emptyList());
	}

	private List<ResourceURL> getUrls(JsonNode nodeUrl) {
		try {
			return mapper.readValue(nodeUrl.toString(), new TypeReference<List<ResourceURL>>() {
			});
		} catch (IOException e) {
			log.warn("Cannot parse field {} for UUID {} in JSON", RESPONSE_NODE + "." + RESULT_NODE + "." +
					COMPUTATIONAL_URL_FIELD, getUUID(), e);
		}
		return Collections.emptyList();
	}
}

