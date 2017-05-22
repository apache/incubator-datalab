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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static com.epam.dlab.rest.contracts.ApiCallbacks.EXPLORATORY;

public class LibInstallCallbackHandler extends ResourceCallbackHandler<ExploratoryLibsInstallStatusDTO> {
	private static final Logger LOGGER = LoggerFactory.getLogger(LibInstallCallbackHandler.class);

    private static final String LIBS = "Libs";

    private final String imageName;

    public LibInstallCallbackHandler(RESTService selfService, DockerAction action, String uuid, String user, String imageName) {
        super(selfService, user, uuid, action);
        this.imageName = imageName;
    }

    //TODO: usein: put proper url here
	@Override
    protected String getCallbackURI() {
        return EXPLORATORY + "INSTALL_LIBBS_URL";
    }

	@Override
    protected ExploratoryLibsInstallStatusDTO parseOutResponse(JsonNode resultNode, ExploratoryLibsInstallStatusDTO status) throws DlabException {
        if (resultNode == null) {
            throw new DlabException("Can't handle response without property " + RESULT_NODE);
        }

        JsonNode resultFileNode = resultNode.get(LIBS);
        if (resultFileNode == null) {
            throw new DlabException("Can't handle response without property " + LIBS);
        }
        List<String> libs = new ArrayList();
        Iterator<JsonNode> iterator = resultFileNode.iterator();
        while(iterator.hasNext()) {
            JsonNode next = iterator.next();
            if(null != next) {
                libs.add(next.toString());
            }
        }


        return status.withLibs(libs);
    }

    @Override
    protected ExploratoryLibsInstallStatusDTO getBaseStatusDTO(UserInstanceStatus status) {
        return super.getBaseStatusDTO(status).withImageName(imageName);
    }
}
