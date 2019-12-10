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
import com.epam.dlab.backendapi.core.commands.DockerAction;
import com.epam.dlab.dto.StatusBaseDTO;
import com.epam.dlab.dto.UserInstanceStatus;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.client.RESTService;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.ParameterizedType;
import java.time.Instant;
import java.util.Date;

public abstract class ResourceCallbackHandler<T extends StatusBaseDTO<?>> implements FileHandlerCallback {
	private static final Logger log = LoggerFactory.getLogger(ResourceCallbackHandler.class);
	final ObjectMapper mapper = new ObjectMapper().configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true);

	private static final String STATUS_FIELD = "status";
	protected static final String RESPONSE_NODE = "response";
	protected static final String RESULT_NODE = "result";
	private static final String ERROR_NODE = "error";

	private static final String OK_STATUS = "ok";

	@JsonIgnore
	private final RESTService selfService;
	@JsonProperty
	private final String user;
	@JsonProperty
	private final String uuid;
	@JsonProperty
	private final DockerAction action;
	@JsonProperty
	private final Class<T> resultType;

	@SuppressWarnings("unchecked")
	public ResourceCallbackHandler(RESTService selfService, String user,
								   String uuid, DockerAction action) {
		this.selfService = selfService;
		this.user = user;
		this.uuid = uuid;
		this.action = action;
		this.resultType =
				(Class<T>) ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
	}

	public ResourceCallbackHandler(RESTService selfService, String user,
								   String uuid,
								   DockerAction action,
								   Class<T> resultType) {
		this.selfService = selfService;
		this.user = user;
		this.uuid = uuid;
		this.action = action;
		this.resultType = resultType;
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
	public String getUser() {
		return user;
	}

	public DockerAction getAction() {
		return action;
	}

	private void selfServicePost(T object) {
		debugMessage("Send post request to self service {} for UUID {}, object is {}",
				getCallbackURI(), uuid, object);
		try {
			selfService.post(getCallbackURI(), object, resultType);
		} catch (Exception e) {
			log.error("Send request or response error for UUID {}: {}", uuid, e.getLocalizedMessage(), e);
			throw new DlabException("Send request or responce error for UUID " + uuid + ": " + e.getLocalizedMessage()
					, e);
		}
	}

	@Override
	public boolean handle(String fileName, byte[] content) throws Exception {
		debugMessage("Got file {} while waiting for UUID {}, for action {}, docker response: {}",
				fileName, uuid, action.name(), new String(content));
		JsonNode document = mapper.readTree(content);
		boolean success = isSuccess(document);
		UserInstanceStatus status = calcStatus(action, success);
		T result = getBaseStatusDTO(status);

		JsonNode resultNode = document.get(RESPONSE_NODE).get(RESULT_NODE);
		if (success) {
			debugMessage("Did {} resource for user: {}, UUID: {}", action, user, uuid);
		} else {
			log.error("Could not {} resource for user: {}, UUID: {}", action, user, uuid);
			result.setErrorMessage(getTextValue(resultNode.get(ERROR_NODE)));
		}
		result = parseOutResponse(resultNode, result);

		selfServicePost(result);
		return !UserInstanceStatus.FAILED.equals(status);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void handleError(String errorMessage) {
		try {
			selfServicePost((T) getBaseStatusDTO(UserInstanceStatus.FAILED)
					.withErrorMessage(errorMessage));
		} catch (Exception t) {
			throw new DlabException("Could not send error message to Self Service for UUID " + uuid + ", user " + user + ": " + errorMessage, t);
		}
	}

	protected abstract String getCallbackURI();

	protected abstract T parseOutResponse(JsonNode document, T baseStatus);

	@SuppressWarnings("unchecked")
	protected T getBaseStatusDTO(UserInstanceStatus status) {
		try {
			return (T) resultType.newInstance()
					.withRequestId(uuid)
					.withUser(user)
					.withStatus(status)
					.withUptime(getUptime(status));
		} catch (Exception t) {
			throw new DlabException("Something went wrong", t);
		}
	}

	private boolean isSuccess(JsonNode document) {
		return OK_STATUS.equals(document.get(STATUS_FIELD).textValue());
	}

	private UserInstanceStatus calcStatus(DockerAction action, boolean success) {
		if (success) {
			switch (action) {
				case STATUS:
				case GIT_CREDS:
				case LIB_LIST:
				case LIB_INSTALL:
				case CREATE_IMAGE:
					return UserInstanceStatus.CREATED; // Any status besides failed
				case CREATE:
				case CONFIGURE:
				case START:
				case RECONFIGURE_SPARK:
					return UserInstanceStatus.RUNNING;
				case STOP:
					return UserInstanceStatus.STOPPED;
				case TERMINATE:
					return UserInstanceStatus.TERMINATED;
				default:
					break;
			}
		}
		return UserInstanceStatus.FAILED;
	}

	protected Date getUptime(UserInstanceStatus status) {
		return UserInstanceStatus.RUNNING == status ? Date.from(Instant.now()) : null;
	}

	protected String getTextValue(JsonNode jsonNode) {
		return jsonNode != null ? jsonNode.textValue() : null;
	}

	private void debugMessage(String format, Object... arguments) {
		if (action == DockerAction.STATUS) {
			log.trace(format, arguments);
		} else {
			log.debug(format, arguments);
		}
	}

}
