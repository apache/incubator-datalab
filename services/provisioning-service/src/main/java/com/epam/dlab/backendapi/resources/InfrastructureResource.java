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

package com.epam.dlab.backendapi.resources;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.core.commands.DockerAction;
import com.epam.dlab.backendapi.service.CheckInactivityService;
import com.epam.dlab.dto.status.EnvResource;
import com.epam.dlab.rest.contracts.InfrasctructureAPI;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

@Path(InfrasctructureAPI.INFRASTRUCTURE)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class InfrastructureResource {

	@Inject
	private CheckInactivityService checkInactivityService;

	/**
	 * Return status of provisioning service.
	 */
	@GET
	public Response status(@Auth UserInfo ui) {
		return Response.status(Response.Status.OK).build();
	}

	@POST
	@Path("check_inactivity")
	public String checkClusterInactivity(@Auth UserInfo ui, List<EnvResource> resources) {
		return checkInactivityService.checkClusterAction(ui.getName(), resources, DockerAction.CHECK_INACTIVITY);
	}
}
