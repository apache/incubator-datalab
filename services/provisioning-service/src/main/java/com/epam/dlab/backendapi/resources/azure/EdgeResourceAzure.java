/*
 * Copyright (c) 2017, EPAM SYSTEMS INC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.dlab.backendapi.resources.azure;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.core.FileHandlerCallback;
import com.epam.dlab.backendapi.core.commands.DockerAction;
import com.epam.dlab.backendapi.core.response.handlers.EdgeCallbackHandler;
import com.epam.dlab.backendapi.resources.EdgeService;
import com.epam.dlab.dto.azure.AzureResource;
import com.epam.dlab.dto.azure.keyload.UploadFileAzure;
import com.epam.dlab.rest.contracts.EdgeAPI;
import io.dropwizard.auth.Auth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;

import static com.epam.dlab.rest.contracts.ApiCallbacks.*;

/**
 * Provides API to manage Edge node on Azure
 */
@Path(EdgeAPI.EDGE)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class EdgeResourceAzure extends EdgeService {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    public EdgeResourceAzure() {
        logger.info("{} is initialized", getClass().getSimpleName());
    }

    @POST
    @Path("/create")
    public String create(@Auth UserInfo ui, UploadFileAzure dto) throws IOException, InterruptedException {
        saveKeyToFile(dto.getEdge().getEdgeUserName(), dto.getContent());
        return action(ui.getName(), dto.getEdge(), dto.getEdge().getAzureIamUser(), KEY_LOADER, DockerAction.CREATE);
    }

    @POST
    @Path("/start")
    public String start(@Auth UserInfo ui, AzureResource<?> dto) throws IOException, InterruptedException {
        return action(ui.getName(), dto, dto.getAzureIamUser(), EDGE + STATUS_URI, DockerAction.START);
    }

    @POST
    @Path("/stop")
    public String stop(@Auth UserInfo ui, AzureResource<?> dto) throws IOException, InterruptedException {
        return action(ui.getName(), dto, dto.getAzureIamUser(), EDGE + STATUS_URI, DockerAction.STOP);
    }

    @SuppressWarnings("unchecked")
    protected FileHandlerCallback getFileHandlerCallback(DockerAction action, String uuid, String user, String callbackURI) {
        return new EdgeCallbackHandler(selfService, action, uuid, user, callbackURI, EdgeResourceAzure.class);
    }
}
