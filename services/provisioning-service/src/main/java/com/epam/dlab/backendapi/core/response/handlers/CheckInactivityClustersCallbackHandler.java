/*
 *
 *  * Copyright (c) 2018, EPAM SYSTEMS INC
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */
package com.epam.dlab.backendapi.core.response.handlers;

import com.epam.dlab.backendapi.core.FileHandlerCallback;
import com.epam.dlab.dto.computational.CheckInactivityClusterCallbackDTO;
import com.epam.dlab.dto.computational.CheckInactivityClustersStatus;
import com.epam.dlab.dto.computational.CheckInactivityClustersStatusDTO;
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
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

@Slf4j
public class CheckInactivityClustersCallbackHandler implements FileHandlerCallback {
	private static final ObjectMapper MAPPER = new ObjectMapper()
			.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true);
	private static final String STATUS_FIELD = "status";
	private static final String LAST_ACTIVITY_FIELD = "last_activity";
	private static final String ERROR_MESSAGE_FIELD = "error_message";
	@JsonProperty
	private final String uuid;
	@JsonProperty
	private final CheckInactivityClusterCallbackDTO dto;
	private final RESTService selfService;
	@JsonProperty
	private final String callbackUrl;
	@JsonProperty
	private final String user;

	@JsonCreator
	public CheckInactivityClustersCallbackHandler(@JacksonInject RESTService selfService,
												  @JsonProperty("callbackUrl") String callbackUrl,
												  @JsonProperty("user") String user,
												  @JsonProperty("dto") CheckInactivityClusterCallbackDTO dto) {
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
		log.debug("Got file {} while waiting for UUID {}, check inactivity clusters response: {}", fileName, uuid,
				fileContent);

		final JsonNode jsonNode = MAPPER.readTree(fileContent);
		final String status = jsonNode.get(STATUS_FIELD).textValue();
		CheckInactivityClustersStatusDTO checkInactivityClustersStatusDTO;
		if ("ok".equals(status)) {
			String lastActivityDate = jsonNode.get(LAST_ACTIVITY_FIELD).textValue();
			Date date = getDate(lastActivityDate);
			checkInactivityClustersStatusDTO =
					buildCheckInactivityClustersStatusDTO(CheckInactivityClustersStatus.COMPLETED)
					.withLastActivityDate(date);
		} else {
			checkInactivityClustersStatusDTO =
					buildCheckInactivityClustersStatusDTO(CheckInactivityClustersStatus.FAILED)
					.withErrorMessage(jsonNode.get(ERROR_MESSAGE_FIELD).textValue());
		}
		selfServicePost(checkInactivityClustersStatusDTO);
		return "ok".equals(status);
	}

	private Date getDate(String stringDateValue) throws ParseException {
		DateFormat formatter = new SimpleDateFormat("d-MMM-yyyy,HH:mm:ss");
		return formatter.parse(stringDateValue);
	}

	private void selfServicePost(CheckInactivityClustersStatusDTO statusDTO) {
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
		buildCheckInactivityClustersStatusDTO(CheckInactivityClustersStatus.FAILED)
				.withErrorMessage(errorMessage);
	}

	private CheckInactivityClustersStatusDTO buildCheckInactivityClustersStatusDTO(CheckInactivityClustersStatus status) {
		return new CheckInactivityClustersStatusDTO()
				.withRequestId(uuid)
				.withCheckInactivityClusterCallbackDTO(dto)
				.withCheckInactivityClustersStatus(status)
				.withUser(user);
	}

}

