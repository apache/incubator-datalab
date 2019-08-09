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

package com.epam.dlab.backendapi.resources.azure;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.auth.rest.UserSessionDurationAuthorizer;
import com.epam.dlab.backendapi.SelfServiceApplicationConfiguration;
import com.epam.dlab.backendapi.dao.ComputationalDAO;
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.resources.dto.SparkStandaloneClusterCreateForm;
import com.epam.dlab.backendapi.resources.swagger.SwaggerSecurityInfo;
import com.epam.dlab.backendapi.roles.RoleType;
import com.epam.dlab.backendapi.roles.UserRoles;
import com.epam.dlab.backendapi.service.ComputationalService;
import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.dto.aws.computational.ClusterConfig;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.client.RESTService;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.dropwizard.auth.Auth;
import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * Provides the REST API for the computational resource on Azure.
 */
@Path("/infrastructure_provision/computational_resources")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "Service for computational resources on Azure. (NOTE: available only on AZURE platform)",
		authorizations = @Authorization(SwaggerSecurityInfo.TOKEN_AUTH), hidden = true)
@Slf4j
public class ComputationalResourceAzure {

	@Inject
	private ExploratoryDAO exploratoryDAO;

	@Inject
	private ComputationalDAO computationalDAO;

	@Inject
	@Named(ServiceConsts.PROVISIONING_SERVICE_NAME)
	private RESTService provisioningService;

	@Inject
	private SelfServiceApplicationConfiguration configuration;

	@Inject
	private ComputationalService computationalService;

	/**
	 * Asynchronously creates computational Spark cluster.
	 *
	 * @param userInfo user info.
	 * @param form     user info about creation of the computational resource.
	 * @return 200 OK if request success, 302 Found - for duplicates, otherwise throws exception.
	 * @throws IllegalArgumentException if input is not valid or exceeds configuration limits
	 */
	@PUT
	@Path("dataengine")
	@RolesAllowed(UserSessionDurationAuthorizer.SHORT_USER_SESSION_DURATION)
	@ApiOperation("Creates Spark cluster on Azure")
	@ApiResponses({
			@ApiResponse(code = 302, message = "Spark cluster on Azure with current parameters already exists"),
			@ApiResponse(code = 200, message = "Spark cluster on Azure successfully created")
	})
	public Response createDataEngine(@ApiParam(hidden = true) @Auth UserInfo userInfo,
									 @ApiParam(value = "Spark cluster create form DTO", required = true)
									 @Valid @NotNull SparkStandaloneClusterCreateForm form) {
		log.debug("Create computational resources for {} | form is {}", userInfo.getName(), form);

		if (!UserRoles.checkAccess(userInfo, RoleType.COMPUTATIONAL, form.getImage(), userInfo.getRoles())) {
			log.warn("Unauthorized attempt to create a {} by user {}", form.getImage(), userInfo.getName());
			throw new DlabException("You do not have the privileges to create a " + form.getTemplateName());
		}

		return computationalService.createSparkCluster(userInfo, form, form.getProject())
				? Response.ok().build()
				: Response.status(Response.Status.FOUND).build();

	}

	/**
	 * Sends request to provisioning service for termination the computational resource for user.
	 *
	 * @param userInfo          user info.
	 * @param exploratoryName   name of exploratory.
	 * @param computationalName name of computational resource.
	 * @return 200 OK if operation is successfully triggered
	 */
	@DELETE
	@Path("/{exploratoryName}/{computationalName}/terminate")
	@ApiOperation("Terminates computational Spark cluster on Azure")
	@ApiResponses(@ApiResponse(code = 200, message = "Spark cluster on Azure successfully terminated"))
	public Response terminate(@ApiParam(hidden = true) @Auth UserInfo userInfo,
							  @ApiParam(value = "Notebook's name corresponding to computational resource",
									  required = true)
							  @PathParam("exploratoryName") String exploratoryName,
							  @ApiParam(value = "Spark cluster's name for terminating", required = true)
							  @PathParam("computationalName") String computationalName) {

		log.debug("Terminating computational resource {} for user {}", computationalName, userInfo.getName());

		computationalService.terminateComputational(userInfo, exploratoryName, computationalName);

		return Response.ok().build();
	}

	/**
	 * Sends request to provisioning service for stopping the computational resource for user.
	 *
	 * @param userInfo          user info.
	 * @param exploratoryName   name of exploratory.
	 * @param computationalName name of computational resource.
	 * @return 200 OK if operation is successfully triggered
	 */
	@DELETE
	@Path("/{project}/{exploratoryName}/{computationalName}/stop")
	@ApiOperation("Stops Spark cluster on Azure")
	@ApiResponses(@ApiResponse(code = 200, message = "Spark cluster on Azure successfully stopped"))
	public Response stop(@ApiParam(hidden = true) @Auth UserInfo userInfo,
						 @ApiParam(value = "Notebook's name corresponding to Spark cluster", required = true)
						 @PathParam("exploratoryName") String exploratoryName,
						 @ApiParam(value = "Spark cluster's name for stopping", required = true)
						 @PathParam("computationalName") String computationalName) {
		log.debug("Stopping computational resource {} for user {}", computationalName, userInfo.getName());

		computationalService.stopSparkCluster(userInfo, exploratoryName, computationalName);

		return Response.ok().build();
	}

	/**
	 * Sends request to provisioning service for starting the computational resource for user.
	 *
	 * @param userInfo          user info.
	 * @param exploratoryName   name of exploratory.
	 * @param computationalName name of computational resource.
	 * @return 200 OK if operation is successfully triggered
	 */
	@PUT
	@Path("/{project}/{exploratoryName}/{computationalName}/start")
	@ApiOperation("Starts Spark cluster on Azure")
	@ApiResponses(@ApiResponse(code = 200, message = "Spark cluster on Azure successfully started"))
	public Response start(@ApiParam(hidden = true) @Auth UserInfo userInfo,
						  @ApiParam(value = "Notebook's name corresponding to Spark cluster", required = true)
						  @PathParam("exploratoryName") String exploratoryName,
						  @ApiParam(value = "Spark cluster's name for starting", required = true)
						  @PathParam("computationalName") String computationalName,
						  @ApiParam(value = "Project name", required = true)
						  @PathParam("project") String project) {
		log.debug("Starting computational resource {} for user {}", computationalName, userInfo.getName());

		computationalService.startSparkCluster(userInfo, exploratoryName, computationalName, project);

		return Response.ok().build();
	}

	@PUT
	@Path("dataengine/{exploratoryName}/{computationalName}/config")
	@ApiOperation("Updates Spark cluster configuration on AWS")
	@ApiResponses(
			@ApiResponse(code = 200, message = "Spark cluster configuration on AWS successfully updated")
	)
	public Response updateDataEngineConfig(@ApiParam(hidden = true) @Auth UserInfo userInfo,
										   @ApiParam(value = "Notebook's name corresponding to Spark cluster",
												   required = true)
										   @PathParam("exploratoryName") String exploratoryName,
										   @ApiParam(value = "Spark cluster's name for reconfiguring", required = true)
										   @PathParam("computationalName") String computationalName,
										   @ApiParam(value = "Spark cluster config", required = true)
										   @Valid @NotNull List<ClusterConfig> config) {

		computationalService.updateSparkClusterConfig(userInfo, exploratoryName, computationalName, config);
		return Response.ok().build();
	}

	@GET
	@Path("{exploratoryName}/{computationalName}/config")
	@ApiOperation("Returns Spark cluster configuration on AWS")
	@ApiResponses(
			@ApiResponse(code = 200, message = "Spark cluster configuration on AWS successfully returned")
	)
	public Response getClusterConfig(@ApiParam(hidden = true) @Auth UserInfo userInfo,
									 @ApiParam(value = "Notebook's name corresponding to Spark cluster",
											 required = true)
									 @PathParam("exploratoryName") String exploratoryName,
									 @ApiParam(value = "Spark cluster's name for reconfiguring", required = true)
									 @PathParam("computationalName") String computationalName) {
		return Response.ok(computationalService.getClusterConfig(userInfo, exploratoryName, computationalName)).build();
	}
}
