/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.epam.dlab.backendapi.resources.gcp;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.core.FileHandlerCallback;
import com.epam.dlab.backendapi.core.commands.DockerAction;
import com.epam.dlab.backendapi.core.response.handlers.EdgeCallbackHandler;
import com.epam.dlab.backendapi.resources.base.EdgeService;
import com.epam.dlab.dto.ResourceSysBaseDTO;
import com.epam.dlab.dto.base.keyload.UploadFileResult;
import com.epam.dlab.dto.gcp.edge.EdgeInfoGcp;
import com.epam.dlab.dto.gcp.keyload.UploadFileGcp;
import com.epam.dlab.rest.contracts.EdgeAPI;
import com.epam.dlab.util.FileUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;

import static com.epam.dlab.rest.contracts.ApiCallbacks.*;

/**
 * Provides API to manage Edge node on GCP
 */
@Path(EdgeAPI.EDGE)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class EdgeResourceGcp extends EdgeService {

	public EdgeResourceGcp() {
		log.info("{} is initialized", getClass().getSimpleName());
	}

	@POST
	@Path("/create")
	public String create(@Auth UserInfo ui, UploadFileGcp dto) throws IOException {
		FileUtils.saveToFile(getKeyFilename(dto.getEdge().getEdgeUserName()), getKeyDirectory(), dto.getContent());
		return action(ui.getName(), dto.getEdge(), dto.getEdge().getCloudSettings().getIamUser(), KEY_LOADER,
				DockerAction.CREATE);
	}

	@POST
	@Path("/start")
	public String start(@Auth UserInfo ui, ResourceSysBaseDTO<?> dto) throws JsonProcessingException {
		return action(ui.getName(), dto, dto.getCloudSettings().getIamUser(), EDGE + STATUS_URI, DockerAction.START);
	}

	@POST
	@Path("/stop")
	public String stop(@Auth UserInfo ui, ResourceSysBaseDTO<?> dto) throws JsonProcessingException {
		return action(ui.getName(), dto, dto.getCloudSettings().getIamUser(), EDGE + STATUS_URI, DockerAction.STOP);
	}


	@POST
	@Path("/terminate")
	public String terminate(@Auth UserInfo ui, ResourceSysBaseDTO<?> dto) throws JsonProcessingException {
		return action(ui.getName(), dto, dto.getCloudSettings().getIamUser(), EDGE + STATUS_URI,
				DockerAction.TERMINATE);
	}

	@SuppressWarnings("unchecked")
	@Override
	protected FileHandlerCallback getFileHandlerCallback(DockerAction action, String uuid, String user, String
			callbackURI) {
		return new EdgeCallbackHandler(selfService, action, uuid, user, callbackURI,
				EdgeInfoGcp.class,
				UploadFileResult.class);
	}
}
