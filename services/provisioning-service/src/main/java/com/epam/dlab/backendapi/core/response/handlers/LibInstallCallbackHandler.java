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

import static com.epam.dlab.rest.contracts.ApiCallbacks.EXPLORATORY;
import static com.epam.dlab.rest.contracts.ApiCallbacks.LIB_STATUS_URI;

import java.io.IOException;
import java.time.Instant;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epam.dlab.UserInstanceStatus;
import com.epam.dlab.backendapi.core.commands.DockerAction;
import com.epam.dlab.dto.exploratory.ExploratoryLibInstallDTO;
import com.epam.dlab.dto.exploratory.ExploratoryLibInstallStatusDTO;
import com.epam.dlab.dto.exploratory.LibInstallDTO;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.client.RESTService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;

/** Handler of docker response for the request for libraries installation.
 */
public class LibInstallCallbackHandler extends ResourceCallbackHandler<ExploratoryLibInstallStatusDTO> {
	private static final Logger LOGGER = LoggerFactory.getLogger(LibInstallCallbackHandler.class);
	
	/** Name of node in response "Libs".
	 */
    private static final String LIBS = "Libs";
    
	/** Full name of node in response "Libs".
	 */
    private static final String LIBS_ABSOLUTE_PATH = RESPONSE_NODE + "." + RESULT_NODE + "." + LIBS;

    /** Exploratory DTO.
     */
    private final ExploratoryLibInstallDTO dto;

    /** Instantiate handler for process of docker response for libraries installation.
     * @param selfService REST pointer for Self Service.
     * @param action docker action.
     * @param uuid request UID.
     * @param exploratoryName the name of exploratory.
     */
    public LibInstallCallbackHandler(RESTService selfService, DockerAction action, String uuid, String user, ExploratoryLibInstallDTO dto) {
        super(selfService, user, uuid, action);
        this.dto = dto;
    }

	@Override
    protected String getCallbackURI() {
        return EXPLORATORY + LIB_STATUS_URI;
    }

	@Override
    protected ExploratoryLibInstallStatusDTO parseOutResponse(JsonNode resultNode, ExploratoryLibInstallStatusDTO status) throws DlabException {
        if (UserInstanceStatus.FAILED == UserInstanceStatus.of(status.getStatus())) {
        	for (LibInstallDTO lib : dto.getLibs()) {
        		lib
        			.withStatus(status.getStatus())
        			.withErrorMessage(status.getErrorMessage());
        	}
        	return status
        			.withLibs(dto.getLibs());
        }
    	if (resultNode == null) {
    		throw new DlabException("Can't handle response result node is null");
    	}

        JsonNode nodeLibs = resultNode.get(LIBS);
        if (nodeLibs == null) {
            throw new DlabException("Can't handle response without property " + LIBS_ABSOLUTE_PATH);
        }
        try {
    		status.withLibs(MAPPER.readValue(nodeLibs.toString(), new TypeReference<List<LibInstallDTO>>() {}));
		} catch (IOException e) {
			LOGGER.warn("Can't parse field {} for UUID {} in JSON", LIBS_ABSOLUTE_PATH, getUUID(), e);
		}

        return status;
    }

    @Override
    protected ExploratoryLibInstallStatusDTO getBaseStatusDTO(UserInstanceStatus status) {
        return super.getBaseStatusDTO(status)
        		.withExploratoryName(dto.getExploratoryName())
        		.withUptime(Date.from(Instant.now()));
    }
}
