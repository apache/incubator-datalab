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

import static com.epam.dlab.rest.contracts.ApiCallbacks.INFRASTRUCTURE;
import static com.epam.dlab.rest.contracts.ApiCallbacks.STATUS_URI;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epam.dlab.backendapi.core.commands.DockerAction;
import com.epam.dlab.dto.status.EnvResourceDTO;
import com.epam.dlab.dto.status.EnvResourceList;
import com.epam.dlab.dto.status.EnvStatusDTO;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.client.RESTService;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ResourcesStatusCallbackHandler extends ResourceCallbackHandler<EnvStatusDTO> {
	private static final Logger LOGGER = LoggerFactory.getLogger(ResourcesStatusCallbackHandler.class);
	private static ObjectMapper MAPPER = new ObjectMapper().configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true);
	
    private final EnvResourceDTO dto;

    public ResourcesStatusCallbackHandler(RESTService selfService, DockerAction action, String uuid, EnvResourceDTO dto) {
        super(selfService, dto.getAwsIamUser(), uuid, action);
        this.dto = dto;
    }
    
    protected EnvResourceDTO getDto() {
    	return dto;
    }
    
    @Override
    protected String getCallbackURI() {
        return INFRASTRUCTURE + STATUS_URI;
    }

    @Override
    protected EnvStatusDTO parseOutResponse(JsonNode resultNode, EnvStatusDTO baseStatus) throws DlabException {
    	if (resultNode == null) {
    		return baseStatus;
    	}
    	
    	EnvResourceList resourceList;
    	try {
			resourceList = MAPPER.readValue(resultNode.toString(), EnvResourceList.class);
		} catch (IOException e) {
			throw new DlabException("Docker responce for UUID " + getUUID() + " not valid: " + e.getLocalizedMessage(), e);
		}
    	
        return baseStatus
        		.withResourceList(resourceList)
        		.withUptime(Date.from(Instant.now()));
    }
    
    @Override
    public boolean handle(String fileName, byte[] content) throws Exception {
    	try {
    		return super.handle(fileName, content);
    	} catch (Throwable e) {
    		LOGGER.warn("Could not retrive the status of resources for UUID {} and user {}: {}",
    				getUUID(), getDto().getAwsIamUser(), e.getLocalizedMessage(), e);
    	}
    	return true; // Always necessary return true for status response
    }
    
    @Override
    public void handleError(String errorMessage) {
    	// Nothing action for status response
    }
}
