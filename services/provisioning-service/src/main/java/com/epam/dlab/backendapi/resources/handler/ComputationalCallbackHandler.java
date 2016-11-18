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

package com.epam.dlab.backendapi.resources.handler;

import com.epam.dlab.backendapi.core.docker.command.DockerAction;
import com.epam.dlab.client.restclient.RESTService;
import com.epam.dlab.constants.UserInstanceStatus;
import com.epam.dlab.dto.computational.ComputationalStatusDTO;
import com.fasterxml.jackson.databind.JsonNode;

import static com.epam.dlab.registry.ApiCallbacks.COMPUTATIONAL;
import static com.epam.dlab.registry.ApiCallbacks.STATUS_URI;

public class ComputationalCallbackHandler extends ResourceCallbackHandler<ComputationalStatusDTO> {
    private static final String COMPUTATIONAL_ID_FIELD = "hostname";

    private String exploratoryName;
    private String computationalName;

    @SuppressWarnings("unchecked")
    public ComputationalCallbackHandler(RESTService selfService, DockerAction action, String originalUuid, String user, String exploratoryName, String computationalName) {
        super(selfService, user, originalUuid, action);
        this.exploratoryName = exploratoryName;
        this.computationalName = computationalName;
    }

    @Override
    protected String getCallbackURI() {
        return COMPUTATIONAL + STATUS_URI;
    }

    @Override
    protected ComputationalStatusDTO parseOutResponse(JsonNode resultNode, ComputationalStatusDTO baseStatus) {
        return baseStatus
                .withComputationalId(getTextValue(resultNode.get(COMPUTATIONAL_ID_FIELD)));
    }

    @Override
    protected ComputationalStatusDTO getBaseStatusDTO(UserInstanceStatus status) {
        return super.getBaseStatusDTO(status).withExploratoryName(exploratoryName).withComputationalName(computationalName);
    }

}

