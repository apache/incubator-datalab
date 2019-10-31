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
import com.epam.dlab.dto.exploratory.ExploratoryImageDTO;
import com.epam.dlab.dto.exploratory.ImageCreateStatusDTO;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.client.RESTService;
import com.epam.dlab.rest.contracts.ApiCallbacks;
import com.fasterxml.jackson.annotation.JacksonInject;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class ImageCreateCallbackHandler extends ResourceCallbackHandler<ImageCreateStatusDTO> {
	@JsonProperty
	private final String imageName;
	@JsonProperty
	private final String exploratoryName;

	public ImageCreateCallbackHandler(
			RESTService selfService, String uuid, DockerAction action,
			ExploratoryImageDTO image) {
		super(selfService, image.getCloudSettings().getIamUser(), uuid, action);
		this.imageName = image.getImageName();
		this.exploratoryName = image.getExploratoryName();
	}

	@JsonCreator
	private ImageCreateCallbackHandler(
			@JacksonInject RESTService selfService, @JsonProperty("uuid") String uuid,
			@JsonProperty("action") DockerAction action,
			@JsonProperty("user") String user,
			@JsonProperty("imageName") String imageName,
			@JsonProperty("exploratoryName") String exploratoryName) {
		super(selfService, user, uuid, action);
		this.imageName = imageName;
		this.exploratoryName = exploratoryName;
	}

	@Override
	protected String getCallbackURI() {
		return ApiCallbacks.IMAGE_STATUS_URI;
	}

	@Override
	protected ImageCreateStatusDTO parseOutResponse(JsonNode document, ImageCreateStatusDTO statusDTO) {
		if (document != null) {
			statusDTO.withImageCreateDto(toImageCreateDto(document.toString()));
		}
		return statusDTO;
	}

	@Override
	protected ImageCreateStatusDTO getBaseStatusDTO(UserInstanceStatus status) {
		final ImageCreateStatusDTO statusDTO = super.getBaseStatusDTO(status);
		statusDTO.setExploratoryName(exploratoryName);
		statusDTO.setName(imageName);
		statusDTO.withoutImageCreateDto();
		return statusDTO;
	}

	private ImageCreateStatusDTO.ImageCreateDTO toImageCreateDto(String content) {
		try {
			return mapper.readValue(content, ImageCreateStatusDTO.ImageCreateDTO.class);
		} catch (IOException e) {
			log.error("Can't parse create image response with content {} for uuid {}", content, getUUID());
			throw new DlabException(String.format("Can't parse create image response with content %s for uuid %s",
					content, getUUID()), e);
		}
	}
}
