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

package com.epam.dlab.backendapi.resources;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.auth.rest.UserSessionDurationAuthorizer;
import com.epam.dlab.backendapi.resources.dto.ExploratoryActionFormDTO;
import com.epam.dlab.backendapi.resources.dto.ExploratoryCreateFormDTO;
import com.epam.dlab.backendapi.roles.RoleType;
import com.epam.dlab.backendapi.roles.UserRoles;
import com.epam.dlab.backendapi.service.ExploratoryService;
import com.epam.dlab.dto.aws.computational.ClusterConfig;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.model.exploratory.Exploratory;
import com.epam.dlab.rest.contracts.ExploratoryAPI;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * Provides the REST API for the exploratory.
 */
@Path("/infrastructure_provision/exploratory_environment")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class ExploratoryResource implements ExploratoryAPI {

	private ExploratoryService exploratoryService;

	@Inject
	public ExploratoryResource(ExploratoryService exploratoryService) {
		this.exploratoryService = exploratoryService;
	}

	@GET
	public Response getExploratoryPopUp(@Auth UserInfo userInfo) {
		return Response.ok(exploratoryService.getUserInstances(userInfo)).build();
	}
	/**
	 * Creates the exploratory environment for user.
	 *
	 * @param userInfo user info.
	 * @param formDTO  description for the exploratory environment.
	 * @return {@link Response.Status#OK} request for provisioning service has been accepted.<br>
	 * {@link Response.Status#FOUND} request for provisioning service has been duplicated.
	 */
	@PUT
	public Response create(@Auth UserInfo userInfo,
						   @Valid @NotNull ExploratoryCreateFormDTO formDTO) {
		log.debug("Creating exploratory environment {} with name {} for user {}",
				formDTO.getImage(), formDTO.getName(), userInfo.getName());
		if (!UserRoles.checkAccess(userInfo, RoleType.EXPLORATORY, formDTO.getImage(), userInfo.getRoles())) {
			log.warn("Unauthorized attempt to create a {} by user {}", formDTO.getImage(), userInfo.getName());
			throw new DlabException("You do not have the privileges to create a " + formDTO.getTemplateName());
		}
		String uuid = exploratoryService.create(userInfo, getExploratory(formDTO), formDTO.getProject());
		return Response.ok(uuid).build();

	}


	/**
	 * Starts exploratory environment for user.
	 *
	 * @param userInfo user info.
	 * @param formDTO  description of exploratory action.
	 * @return Invocation response as JSON string.
	 */
	@POST
	@RolesAllowed(UserSessionDurationAuthorizer.SHORT_USER_SESSION_DURATION)
	public String start(@Auth UserInfo userInfo,
						@Valid @NotNull ExploratoryActionFormDTO formDTO) {
		log.debug("Starting exploratory environment {} for user {}", formDTO.getNotebookInstanceName(),
				userInfo.getName());
		return exploratoryService.start(userInfo, formDTO.getNotebookInstanceName(), formDTO.getProjectName());
	}

	/**
	 * Stops exploratory environment for user.
	 *
	 * @param userInfo user info.
	 * @param name     name of exploratory environment.
	 * @return Invocation response as JSON string.
	 */
	@DELETE
	@Path("/{project}/{name}/stop")
	public String stop(@Auth UserInfo userInfo,
					   @PathParam("project") String project,
					   @PathParam("name") String name) {
		log.debug("Stopping exploratory environment {} for user {}", name, userInfo.getName());
		return exploratoryService.stop(userInfo, project, name);
	}

	/**
	 * Terminates exploratory environment for user.
	 *
	 * @param userInfo user info.
	 * @param name     name of exploratory environment.
	 * @return Invocation response as JSON string.
	 */
	@DELETE
	@Path("/{project}/{name}/terminate")
	public String terminate(@Auth UserInfo userInfo,
							@PathParam("project") String project,
							@PathParam("name") String name) {
		log.debug("Terminating exploratory environment {} for user {}", name, userInfo.getName());
		return exploratoryService.terminate(userInfo, project, name);
	}

	@PUT
	@Path("/{project}/{name}/reconfigure")
	public Response reconfigureSpark(@Auth UserInfo userInfo,
									 @PathParam("project") String project,
									 @PathParam("name") String name,
									 List<ClusterConfig> config) {
		log.debug("Updating exploratory {} spark cluster for user {}", name, userInfo.getName());
		exploratoryService.updateClusterConfig(userInfo, project, name, config);
		return Response.ok().build();
	}

	@GET
	@Path("/{project}/{name}/cluster/config")
	public Response getClusterConfig(@Auth UserInfo userInfo,
									 @PathParam("project") String project,
									 @PathParam("name") String name) {
		log.debug("Getting exploratory {} spark cluster configuration for user {}", name, userInfo.getName());
		return Response.ok(exploratoryService.getClusterConfig(userInfo, project, name)).build();
	}

	private Exploratory getExploratory(ExploratoryCreateFormDTO formDTO) {
		return Exploratory.builder()
				.name(formDTO.getName())
				.dockerImage(formDTO.getImage())
				.imageName(formDTO.getImageName())
				.templateName(formDTO.getTemplateName())
				.version(formDTO.getVersion())
				.clusterConfig(formDTO.getClusterConfig())
				.shape(formDTO.getShape())
				.endpoint(formDTO.getEndpoint())
				.project(formDTO.getProject())
				.exploratoryTag(formDTO.getExploratoryTag())
				.build();
	}
}
