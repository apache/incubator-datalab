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

import com.epam.dlab.backendapi.core.FileHandlerCallback;
import com.epam.dlab.dto.UserInstanceStatus;
import com.epam.dlab.dto.computational.CheckInactivityStatusDTO;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.client.RESTService;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.core.Response;

@Slf4j
@Singleton
public class CheckInactivityCallbackHandler implements FileHandlerCallback {
	private static final ObjectMapper MAPPER = new ObjectMapper()
			.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true);
	private static final String STATUS_FIELD = "status";
	private static final String ERROR_MESSAGE_FIELD = "error_message";
	private static final String RESPONSE = "response";
	private static final String OK_STATUS_STRING = "ok";
	private static final String RESULT_NODE = "result";
	@JsonProperty
	private final String uuid;
	private final RESTService selfService;
	@JsonProperty
	private final String callbackUrl;
	@JsonProperty
	private final String user;
	@JsonProperty
	private final String exploratoryName;
	@JsonProperty
	private final String computationalName;

	@JsonCreator
	public CheckInactivityCallbackHandler(@JacksonInject RESTService selfService,
										  @JsonProperty("callbackUrl") String callbackUrl,
										  @JsonProperty("user") String user, String uuid, String exploratoryName,
										  String computationalName) {
		this.selfService = selfService;
		this.uuid = uuid;
		this.callbackUrl = callbackUrl;
		this.user = user;
		this.exploratoryName = exploratoryName;
		this.computationalName = computationalName;
	}

	public CheckInactivityCallbackHandler(RESTService selfService,
										  String callbackUrl, String user, String uuid, String exploratoryName) {
		this(selfService, callbackUrl, user, uuid, exploratoryName, null);
	}

	@Override
	public String getUUID() {
		return uuid;
	}

	@Override
	public boolean checkUUID(String uuid) {
		return this.uuid.equals(uuid);
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean handle(String fileName, byte[] content) throws Exception {
		final String fileContent = new String(content);
		log.debug("Got file {} while waiting for UUID {}, check inactivity resources response: {}", fileName, uuid,
				fileContent);

		final JsonNode treeNode = MAPPER.readTree(fileContent);
		final String status = treeNode.get(STATUS_FIELD).textValue();
		CheckInactivityStatusDTO checkInactivityStatusDTO = OK_STATUS_STRING.equals(status) ?
				getOkStatusDto(treeNode) : getFailedStatusDto(treeNode.get(ERROR_MESSAGE_FIELD).textValue());
		selfServicePost(checkInactivityStatusDTO);
		return OK_STATUS_STRING.equals(status);
	}

	@Override
	public void handleError(String errorMessage) {
		log.error(errorMessage);
		selfServicePost(getFailedStatusDto(errorMessage).withErrorMessage(errorMessage));
	}

	@Override
	public String getUser() {
		return user;
	}

	private CheckInactivityStatusDTO getOkStatusDto(JsonNode jsonNode) {
		final CheckInactivityStatusDTO statusDTO = new CheckInactivityStatusDTO().withStatus(OK_STATUS_STRING)
				.withRequestId(uuid);
		statusDTO.setComputationalName(computationalName);
		statusDTO.setExploratoryName(exploratoryName);
		final long lastActivity = Long.parseLong(jsonNode.get(RESPONSE).get(RESULT_NODE).textValue());
		statusDTO.setLastActivityUnixTime(lastActivity);
		return statusDTO;
	}

	private CheckInactivityStatusDTO getFailedStatusDto(String errorMessage) {
		return new CheckInactivityStatusDTO().withStatus(UserInstanceStatus.FAILED)
				.withRequestId(uuid)
				.withErrorMessage(errorMessage);
	}

	private void selfServicePost(CheckInactivityStatusDTO statusDTO) {
		log.debug("Send post request to self service for UUID {}, object is {}", uuid, statusDTO);
		try {
			selfService.post(callbackUrl, statusDTO, Response.class);
		} catch (Exception e) {
			log.error("Send request or response error for UUID {}: {}", uuid, e.getLocalizedMessage(), e);
			throw new DlabException("Send request or response error for UUID " + uuid + ": "
					+ e.getLocalizedMessage(), e);
		}
	}

}

