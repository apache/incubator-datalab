/***************************************************************************

Copyright (c) 2016, EPAM SYSTEMS INC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

****************************************************************************/

package com.epam.dlab.backendapi.core.response.handlers;

import com.epam.dlab.UserInstanceStatus;
import com.epam.dlab.backendapi.core.FileHandlerCallback;
import com.epam.dlab.backendapi.core.commands.DockerAction;
import com.epam.dlab.dto.StatusBaseDTO;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.client.RESTService;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.ParameterizedType;
import java.time.Instant;
import java.util.Date;

abstract public class ResourceCallbackHandler<T extends StatusBaseDTO> implements FileHandlerCallback {
    private static final Logger LOGGER = LoggerFactory.getLogger(ResourceCallbackHandler.class);
    private ObjectMapper MAPPER = new ObjectMapper().configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true);

    private static final String STATUS_FIELD = "status";
    private static final String RESPONSE_NODE = "response";
    private static final String RESULT_NODE = "result";
    private static final String CONF_NODE = "conf";

    private static final String OK_STATUS = "ok";
    private static final String ERROR_STATUR = "err";

    private RESTService selfService;
    private String user;
    private String originalUuid;
    private DockerAction action;
    private Class<T> resultType;

    @SuppressWarnings("unchecked")
    public ResourceCallbackHandler(RESTService selfService, String user, String originalUuid, DockerAction action) {
        this.selfService = selfService;
        this.user = user;
        this.originalUuid = originalUuid;
        this.action = action;
        this.resultType = (Class<T>) ((ParameterizedType) this.getClass().getGenericSuperclass()).getActualTypeArguments()[0];
    }

    @Override
    public boolean checkUUID(String uuid) {
        return originalUuid.equals(uuid);
    }

    @Override
    public boolean handle(String fileName, byte[] content) throws Exception {
        LOGGER.debug("Got file {} while waiting for {}", fileName, originalUuid);
        JsonNode document = MAPPER.readTree(content);
        boolean success = isSuccess(document);
        UserInstanceStatus status = calcStatus(action, success);
        T result = getBaseStatusDTO(status);
        JsonNode resultNode;
        if (success) {
            resultNode = document.get(RESPONSE_NODE).get(RESULT_NODE);
            LOGGER.debug("Did {} resource for user: {}, request: {}, docker response: {}", action, user, originalUuid, new String(content));
        } else {
            resultNode = document.get(RESPONSE_NODE).get(RESULT_NODE).get(CONF_NODE);
            LOGGER.error("Could not {} resource for user: {}, request: {}, docker response: {}", action, user, originalUuid, new String(content));
        }
        result = parseOutResponse(resultNode, result);
        selfService.post(getCallbackURI(), result, resultType);
        return !UserInstanceStatus.FAILED.equals(status);
    }

    @Override
    public void handleError() {
        try {
            selfService.post(getCallbackURI(), getBaseStatusDTO(UserInstanceStatus.FAILED), resultType);
        } catch (Throwable t) {
            throw new DlabException("Could not send status update for request " + originalUuid + ", user " + user, t);
        }
    }

    abstract protected String getCallbackURI();

    abstract protected T parseOutResponse(JsonNode document, T baseStatus);

    @SuppressWarnings("unchecked")
    protected T getBaseStatusDTO(UserInstanceStatus status) {
        try {
            return (T) resultType.newInstance().withUser(user).withStatus(status).withUptime(getUptime(status));
        } catch (Throwable t) {
            throw new DlabException("Something went wrong", t);
        }
    }

    private boolean isSuccess(JsonNode document) {
        return OK_STATUS.equals(document.get(STATUS_FIELD).textValue());
    }

    private UserInstanceStatus calcStatus(DockerAction action, boolean success) {
        if (success) {
            switch (action) {
                case CREATE:
                    return UserInstanceStatus.RUNNING;
                case START:
                    return UserInstanceStatus.RUNNING;
                case STOP:
                    return UserInstanceStatus.STOPPED;
                case TERMINATE:
                    return UserInstanceStatus.TERMINATED;
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
}
