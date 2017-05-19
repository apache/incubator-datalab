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
import com.epam.dlab.backendapi.core.commands.DockerAction;
import com.epam.dlab.dto.exploratory.ExploratoryLibsInstallStatusDTO;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.client.RESTService;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.epam.dlab.rest.contracts.ApiCallbacks.EXPLORATORY;
import static com.epam.dlab.rest.contracts.ApiCallbacks.STATUS_URI;

public class LibsStatusCallbackHandler extends ResourceCallbackHandler<ExploratoryLibsInstallStatusDTO> {
	private static final Logger LOGGER = LoggerFactory.getLogger(LibsStatusCallbackHandler.class);

    private static final String INSTANCE_ID_FIELD = "instance_id";
    private static final String LIBS = "Libs";

    private final String exploratoryName;

    public LibsStatusCallbackHandler(RESTService selfService, DockerAction action, String uuid, String user, String exploratoryName) {
        super(selfService, user, uuid, action);
        this.exploratoryName = exploratoryName;
    }

	@Override
    protected String getCallbackURI() {
        return EXPLORATORY + STATUS_URI;
    }

	@Override
    protected ExploratoryLibsInstallStatusDTO parseOutResponse(JsonNode resultNode, ExploratoryLibsInstallStatusDTO status) throws DlabException {
    	if (resultNode == null) {
    		return status;
    	}

    	return status
    			.withInstanceId(getTextValue(resultNode.get(INSTANCE_ID_FIELD)))
                .withLibs(resultNode.get(LIBS) == null ? "" :resultNode.get(LIBS).toString());
    }

    @Override
    protected ExploratoryLibsInstallStatusDTO getBaseStatusDTO(UserInstanceStatus status) {
        return super.getBaseStatusDTO(status).withExploratoryName(exploratoryName);
    }
}
