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
import com.epam.dlab.dto.exploratory.ExploratoryLibListDTO;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.client.RESTService;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.epam.dlab.rest.contracts.ApiCallbacks.INFRASTRUCTURE;
import static com.epam.dlab.rest.contracts.ApiCallbacks.STATUS_URI;

public class LibListCallbackHandler extends ResourceCallbackHandler<ExploratoryLibListDTO> {
	private static final Logger LOGGER = LoggerFactory.getLogger(LibListCallbackHandler.class);

    private static final String FILE = "file";
    private final String imageName;

    public LibListCallbackHandler(RESTService selfService, DockerAction action, String uuid, String user, String imageName) {
        super(selfService, user, uuid, action);
        this.imageName = imageName;
    }

	@Override
    protected String getCallbackURI() {
        return INFRASTRUCTURE + STATUS_URI;
    }

	@Override
    protected ExploratoryLibListDTO parseOutResponse(JsonNode resultNode, ExploratoryLibListDTO status) throws DlabException {
    	if (resultNode == null) {
            throw new DlabException("Can't handle response without property " + RESULT_NODE);
    	}

        JsonNode resultFileNode = resultNode.get(FILE);
        if (resultFileNode == null) {
            throw new DlabException("Can't handle response without property " + FILE);
        }

        Path path = Paths.get(resultFileNode.asText()).toAbsolutePath();
        if(path.toFile().exists()) {
            try {
                return status.withLibs(new String(Files.readAllBytes(path)));
            } catch (IOException e) {
                throw new DlabException("Can't read file " + path + " : " + e.getLocalizedMessage(), e);
            }
        } else {
            throw new DlabException("Can't handle response. The file " + path + " does not exist");
        }
    }

    @Override
    protected ExploratoryLibListDTO getBaseStatusDTO(UserInstanceStatus status) {
        return super.getBaseStatusDTO(status).withImageName(imageName);
    }
}
