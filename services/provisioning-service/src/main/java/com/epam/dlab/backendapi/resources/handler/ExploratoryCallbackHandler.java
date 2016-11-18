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
import com.epam.dlab.dto.exploratory.ExploratoryStatusDTO;
import com.fasterxml.jackson.databind.JsonNode;

import static com.epam.dlab.registry.ApiCallbacks.EXPLORATORY;
import static com.epam.dlab.registry.ApiCallbacks.STATUS_URI;

public class ExploratoryCallbackHandler extends ResourceCallbackHandler<ExploratoryStatusDTO> {
    private static final String EXPLORATORY_ID_FIELD = "notebook_name";
    private static final String EXPLORATORY_URL_FIELD = "exploratory_url";

    private String exploratoryName;

    @SuppressWarnings("unchecked")
    public ExploratoryCallbackHandler(RESTService selfService, DockerAction action, String originalUuid, String user, String exploratoryName) {
        super(selfService, user, originalUuid, action);
        this.exploratoryName = exploratoryName;
    }

    protected String getCallbackURI() {
        return EXPLORATORY + STATUS_URI;
    }

    protected ExploratoryStatusDTO parseOutResponse(JsonNode resultNode, ExploratoryStatusDTO baseStatus) {
        return baseStatus
                .withExploratoryId(getTextValue(resultNode.get(EXPLORATORY_ID_FIELD)))
                .withExploratoryUrl(getTextValue(resultNode.get(EXPLORATORY_URL_FIELD)));
    }

    protected ExploratoryStatusDTO getBaseStatusDTO(UserInstanceStatus status) {
        return super.getBaseStatusDTO(status).withExploratoryName(exploratoryName);
    }
}
