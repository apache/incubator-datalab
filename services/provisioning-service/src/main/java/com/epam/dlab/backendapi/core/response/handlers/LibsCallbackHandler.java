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
import com.epam.dlab.dto.exploratory.ExploratoryLibsDTO;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.client.RESTService;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.epam.dlab.rest.contracts.ApiCallbacks.EXPLORATORY;
import static com.epam.dlab.rest.contracts.ApiCallbacks.STATUS_URI;

public class LibsCallbackHandler extends ResourceCallbackHandler<ExploratoryLibsDTO> {
	private static final Logger LOGGER = LoggerFactory.getLogger(LibsCallbackHandler.class);

    private static final String INSTANCE_ID_FIELD = "instance_id";
    private static final String FILE = "file";

    private final String exploratoryName;

    public LibsCallbackHandler(RESTService selfService, DockerAction action, String uuid, String user, String exploratoryName) {
        super(selfService, user, uuid, action);
        this.exploratoryName = exploratoryName;
    }

	@Override
    protected String getCallbackURI() {
        return EXPLORATORY + STATUS_URI;
    }

	@Override
    protected ExploratoryLibsDTO parseOutResponse(JsonNode resultNode, ExploratoryLibsDTO status) throws DlabException {
    	if (resultNode == null) {
    		return status;
    	}


        String libsFileLocation = resultNode.get(FILE) == null ? "" : resultNode.get(FILE).asText();

        Path path = Paths.get(libsFileLocation).toAbsolutePath();
        File libs = path.toFile();
        String content = "";
        if(libs.exists()) {
            try {
                content = new String(Files.readAllBytes(path));
            } catch (IOException e) {
                throw new DlabException("Can't read resource " + path.toString() + ": " + e.getLocalizedMessage(), e);
            }
        }

    	return status
    			.withInstanceId(getTextValue(resultNode.get(INSTANCE_ID_FIELD)))
                .withLibs(content);
    }

    @Override
    protected ExploratoryLibsDTO getBaseStatusDTO(UserInstanceStatus status) {
        return super.getBaseStatusDTO(status).withExploratoryName(exploratoryName);
    }
}
