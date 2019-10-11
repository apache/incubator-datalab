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
import com.epam.dlab.dto.reuploadkey.ReuploadKeyCallbackDTO;
import com.epam.dlab.dto.reuploadkey.ReuploadKeyStatus;
import com.epam.dlab.dto.reuploadkey.ReuploadKeyStatusDTO;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.client.RESTService;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.core.Response;

@Slf4j
public class ReuploadKeyCallbackHandler implements FileHandlerCallback {
	private static final ObjectMapper MAPPER = new ObjectMapper()
			.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true);
	private static final String STATUS_FIELD = "status";
	private static final String ERROR_MESSAGE_FIELD = "error_message";
	@JsonProperty
	private final String uuid;
	@JsonProperty
	private final ReuploadKeyCallbackDTO dto;
	private final RESTService selfService;
	@JsonProperty
	private final String callbackUrl;
	@JsonProperty
	private final String user;

	@JsonCreator
	public ReuploadKeyCallbackHandler(@JacksonInject RESTService selfService,
									  @JsonProperty("callbackUrl") String callbackUrl,
									  @JsonProperty("user") String user,
									  @JsonProperty("dto") ReuploadKeyCallbackDTO dto) {
		this.selfService = selfService;
		this.uuid = dto.getId();
		this.callbackUrl = callbackUrl;
		this.user = user;
		this.dto = dto;
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
	public boolean handle(String fileName, byte[] content) throws Exception {
		final String fileContent = new String(content);
		log.debug("Got file {} while waiting for UUID {}, reupload key response: {}", fileName, uuid, fileContent);

		final JsonNode jsonNode = MAPPER.readTree(fileContent);
		final String status = jsonNode.get(STATUS_FIELD).textValue();
		ReuploadKeyStatusDTO reuploadKeyStatusDTO;
		if ("ok".equals(status)) {
			reuploadKeyStatusDTO = buildReuploadKeyStatusDto(ReuploadKeyStatus.COMPLETED);
		} else {
			reuploadKeyStatusDTO = buildReuploadKeyStatusDto(ReuploadKeyStatus.FAILED)
					.withErrorMessage(jsonNode.get(ERROR_MESSAGE_FIELD).textValue());
		}
		selfServicePost(reuploadKeyStatusDTO);
		return "ok".equals(status);
	}

	private void selfServicePost(ReuploadKeyStatusDTO statusDTO) {
		log.debug("Send post request to self service for UUID {}, object is {}", uuid, statusDTO);
		try {
			selfService.post(callbackUrl, statusDTO, Response.class);
		} catch (Exception e) {
			log.error("Send request or response error for UUID {}: {}", uuid, e.getLocalizedMessage(), e);
			throw new DlabException("Send request or response error for UUID " + uuid + ": "
					+ e.getLocalizedMessage(), e);
		}
	}

	@Override
	public void handleError(String errorMessage) {
		buildReuploadKeyStatusDto(ReuploadKeyStatus.FAILED)
				.withErrorMessage(errorMessage);
	}

	@Override
	public String getUser() {
		return user;
	}

	private ReuploadKeyStatusDTO buildReuploadKeyStatusDto(ReuploadKeyStatus status) {
		return new ReuploadKeyStatusDTO()
				.withRequestId(uuid)
				.withReuploadKeyCallbackDto(dto)
				.withReuploadKeyStatus(status)
				.withUser(user);
	}

}

