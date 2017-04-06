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

import java.io.IOException;

import com.epam.dlab.UserInstanceStatus;
import com.epam.dlab.backendapi.core.commands.DockerAction;
import com.epam.dlab.dto.edge.EdgeInfoDTO;
import com.epam.dlab.dto.keyload.UploadFileResultDTO;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.client.RESTService;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class EdgeCallbackHandler extends ResourceCallbackHandler<UploadFileResultDTO> {
    protected ObjectMapper MAPPER = new ObjectMapper().configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true);
	
    private final String callbackURI;

    public EdgeCallbackHandler(RESTService selfService, DockerAction action, String uuid, String user, String callbackURI) {
        super(selfService, user, uuid, action);
        this.callbackURI = callbackURI;
    }

	@Override
    protected String getCallbackURI() {
        return callbackURI;
    }

    protected UploadFileResultDTO parseOutResponse(JsonNode resultNode, UploadFileResultDTO baseStatus) throws DlabException {
    	if (resultNode != null &&
    		getAction() == DockerAction.CREATE &&
    		UserInstanceStatus.of(baseStatus.getStatus()) != UserInstanceStatus.FAILED) {
            try {
            	EdgeInfoDTO credential = MAPPER.readValue(resultNode.toString(), EdgeInfoDTO.class)
            			.withEdgeStatus(UserInstanceStatus.RUNNING.toString());
            	baseStatus.withEdgeInfo(credential);
            } catch (IOException e) {
            	throw new DlabException("Cannot parse the EDGE info in JSON: " + e.getLocalizedMessage(), e);
            }
    	}

		return baseStatus;
    }
    
    @Override
    public void handleError(String errorMessage) {
    	super.handleError("Could not upload the user key: " + errorMessage);
    }
}
