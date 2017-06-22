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

import static com.epam.dlab.rest.contracts.ApiCallbacks.GIT_CREDS;
import static com.epam.dlab.rest.contracts.ApiCallbacks.STATUS_URI;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epam.dlab.UserInstanceStatus;
import com.epam.dlab.backendapi.core.commands.DockerAction;
import com.epam.dlab.dto.exploratory.ExploratoryStatusDTO;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.client.RESTService;
import com.fasterxml.jackson.databind.JsonNode;

public class ExploratoryGitCredsCallbackHandler extends ResourceCallbackHandler<ExploratoryStatusDTO> {
	private static final Logger LOGGER = LoggerFactory.getLogger(ExploratoryGitCredsCallbackHandler.class);
	
	private final String exploratoryName;

    public ExploratoryGitCredsCallbackHandler(RESTService selfService, DockerAction action, String uuid, String user, String exploratoryName) {
        super(selfService, user, uuid, action);
        this.exploratoryName = exploratoryName;
    }

	@Override
    protected String getCallbackURI() {
        return GIT_CREDS + STATUS_URI;
    }

	@Override
    protected ExploratoryStatusDTO parseOutResponse(JsonNode resultNode, ExploratoryStatusDTO baseStatus) throws DlabException {
		LOGGER.trace("Parse GIT Creds: "  + resultNode);
    	return baseStatus;
    }

    @Override
    protected ExploratoryStatusDTO getBaseStatusDTO(UserInstanceStatus status) {
        return super.getBaseStatusDTO(status)
        			.withExploratoryName(exploratoryName);
    }
}
