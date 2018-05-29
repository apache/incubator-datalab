/*
 * Copyright (c) 2017, EPAM SYSTEMS INC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.dlab.backendapi.core.response.handlers;

import com.epam.dlab.dto.UserInstanceStatus;
import com.epam.dlab.backendapi.core.commands.DockerAction;
import com.epam.dlab.dto.base.computational.ComputationalBase;
import com.epam.dlab.dto.computational.ComputationalStatusDTO;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.client.RESTService;
import com.epam.dlab.rest.contracts.ApiCallbacks;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

public class ComputationalCallbackHandler extends ResourceCallbackHandler<ComputationalStatusDTO> {
    private static final String INSTANCE_ID_FIELD = "instance_id";
    private static final String COMPUTATIONAL_ID_FIELD = "hostname";

    private final ComputationalBase<?> dto;
    private ComputationalConfigure computationalConfigure;

    public ComputationalCallbackHandler(ComputationalConfigure computationalConfigure, RESTService selfService,
                                        DockerAction action, String uuid, ComputationalBase<?> dto) {

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
    protected ComputationalStatusDTO parseOutResponse(JsonNode resultNode, ComputationalStatusDTO baseStatus) throws DlabException {
        if (getAction() == DockerAction.CONFIGURE) {
            baseStatus.withExploratoryName(getDto().getExploratoryName());
        }
        if (resultNode == null) {
            return baseStatus;
        }

        switch (getAction()) {
            case CREATE:
                baseStatus
                        .withInstanceId(instanceId(resultNode.get(INSTANCE_ID_FIELD)))
                        .withComputationalId(getTextValue(resultNode.get(COMPUTATIONAL_ID_FIELD)));
                if (UserInstanceStatus.of(baseStatus.getStatus()) == UserInstanceStatus.RUNNING) {
                    baseStatus.withStatus(UserInstanceStatus.CONFIGURING);
                    computationalConfigure.configure(getUUID(), getDto());
                }
                break;
            case CONFIGURE:
                baseStatus
                        .withComputationalName(getDto().getComputationalName())
                        .withUptime(null);
                break;
            default:
                break;
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
            List<String> ids = new ArrayList<>();
            for (JsonNode id : jsonNode) {
                ids.add(id.textValue());
            }
            return String.join(";", ids);
        }

        return getTextValue(jsonNode);
    }
}

