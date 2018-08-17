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
import com.epam.dlab.dto.computational.CheckInactivityClusterStatus;
import com.epam.dlab.dto.computational.CheckInactivityClusterStatusDTO;
import com.epam.dlab.dto.status.EnvResource;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.client.RESTService;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

@Slf4j
public class CheckInactivityClusterCallbackHandler implements FileHandlerCallback {
	private static final ObjectMapper MAPPER = new ObjectMapper()
			.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true);
	private static final String STATUS_FIELD = "status";
	private static final String CLUSTERS_FIELD = "clusters";
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
	public CheckInactivityClusterCallbackHandler(@JacksonInject RESTService selfService,
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
	@SuppressWarnings("unchecked")
	public boolean handle(String fileName, byte[] content) throws Exception {
		final String fileContent = new String(content);
		log.debug("Got file {} while waiting for UUID {}, check inactivity clusters response: {}", fileName, uuid,
				fileContent);

		final JsonNode treeNode = MAPPER.readTree(fileContent);
		final String status = treeNode.get(STATUS_FIELD).textValue();
		CheckInactivityClusterStatusDTO checkInactivityClusterStatusDTO = "ok".equals(status) ?
				getOkStatusDto(treeNode) : getFailedStatusDto(treeNode);
		selfServicePost(checkInactivityClusterStatusDTO);
		return "ok".equals(status);
	}

	private CheckInactivityClusterStatusDTO getOkStatusDto(JsonNode jsonNode) throws IOException {
		final JsonNode clustersNode = jsonNode.get(CLUSTERS_FIELD);
		ObjectReader reader = MAPPER.readerFor(new TypeReference<List<EnvResource>>() {
		});
		List<EnvResource> clusters = reader.readValue(clustersNode);
		return buildCheckInactivityClustersStatusDTO(CheckInactivityClusterStatus.COMPLETED, clusters);
	}

	private CheckInactivityClusterStatusDTO getFailedStatusDto(JsonNode jsonNode) {
		return buildCheckInactivityClustersStatusDTO(CheckInactivityClusterStatus.FAILED,
				Collections.emptyList())
				.withErrorMessage(jsonNode.get(ERROR_MESSAGE_FIELD).textValue());
	}

	private void selfServicePost(CheckInactivityClusterStatusDTO statusDTO) {
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
		buildCheckInactivityClustersStatusDTO(CheckInactivityClusterStatus.FAILED, Collections.emptyList())
				.withErrorMessage(errorMessage);
	}

	@Override
	public String getUser() {
		return user;
	}

	private CheckInactivityClusterStatusDTO buildCheckInactivityClustersStatusDTO(CheckInactivityClusterStatus status,
																				  List<EnvResource> clusters) {
		return new CheckInactivityClusterStatusDTO()
				.withRequestId(uuid)
				.withClusters(clusters)
				.withCheckInactivityClustersStatus(status)
				.withUser(user);
	}

}

