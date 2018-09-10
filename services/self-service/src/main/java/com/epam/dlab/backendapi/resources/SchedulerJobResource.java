/*
 * Copyright (c) 2018, EPAM SYSTEMS INC
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
import com.epam.dlab.backendapi.resources.swagger.SwaggerSecurityInfo;
import com.epam.dlab.backendapi.service.SchedulerJobService;
import com.epam.dlab.backendapi.validation.annotation.SchedulerJobDTOValid;
import com.epam.dlab.dto.SchedulerJobDTO;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Manages scheduler jobs for exploratory environment
 */
@Path("/infrastructure_provision/exploratory_environment/scheduler")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "Service for scheduling operations with notebooks or clusters",
		authorizations = @Authorization(SwaggerSecurityInfo.TOKEN_AUTH))
@Slf4j
public class SchedulerJobResource {

	private SchedulerJobService schedulerJobService;

	@Inject
	public SchedulerJobResource(SchedulerJobService schedulerJobService) {
		this.schedulerJobService = schedulerJobService;
	}


	/**
	 * Updates exploratory <code>exploratoryName<code/> for user <code>userInfo<code/> with new scheduler job data
	 *
	 * @param userInfo        user info
	 * @param exploratoryName name of exploratory resource
	 * @param dto             scheduler job data
	 * @return response
	 */
	@POST
	@Path("/{exploratoryName}")
	@ApiOperation("Updates scheduler's data for notebook")
	@ApiResponses(@ApiResponse(code = 200, message = "Scheduler's data for notebook was updated successfully"))
	public Response updateExploratoryScheduler(@ApiParam(hidden = true) @Auth UserInfo userInfo,
											   @ApiParam(value = "Notebook's name", required = true)
											   @PathParam("exploratoryName") String exploratoryName,
											   @ApiParam(value = "Scheduler's data", required = true)
											   @SchedulerJobDTOValid SchedulerJobDTO dto) {
		schedulerJobService.updateExploratorySchedulerData(userInfo.getName(), exploratoryName, dto);
		return Response.ok().build();
	}

	/**
	 * Updates computational resource <code>computationalName<code/> affiliated with exploratory
	 * <code>exploratoryName<code/> for user <code>userInfo<code/> with new scheduler job data
	 *
	 * @param userInfo          user info
	 * @param exploratoryName   name of exploratory resource
	 * @param computationalName name of computational resource
	 * @param dto               scheduler job data
	 * @return response
	 */
	@POST
	@Path("/{exploratoryName}/{computationalName}")
	@ApiOperation("Updates scheduler's data for cluster")
	@ApiResponses(@ApiResponse(code = 200, message = "Scheduler's data for cluster was updated successfully"))
	public Response updateComputationalScheduler(@ApiParam(hidden = true) @Auth UserInfo userInfo,
												 @ApiParam(value = "Notebook's name", required = true)
												 @PathParam("exploratoryName") String exploratoryName,
												 @ApiParam(value = "Cluster's name affiliated with notebook",
														 required = true)
												 @PathParam("computationalName") String computationalName,
												 @ApiParam(value = "Scheduler's data", required = true)
												 @SchedulerJobDTOValid SchedulerJobDTO dto) {
		schedulerJobService.updateComputationalSchedulerData(userInfo.getName(), exploratoryName,
				computationalName, dto);
		return Response.ok().build();
	}


	/**
	 * Returns scheduler job for exploratory resource <code>exploratoryName<code/>
	 *
	 * @param userInfo        user info
	 * @param exploratoryName name of exploratory resource
	 * @return scheduler job data
	 */
	@GET
	@Path("/{exploratoryName}")
	@ApiOperation("Returns scheduler's data for notebook")
	@ApiResponses(@ApiResponse(code = 200, message = "Scheduler's data for notebook fetched successfully"))
	public Response fetchSchedulerJobForUserAndExploratory(@ApiParam(hidden = true) @Auth UserInfo userInfo,
														   @ApiParam(value = "Notebook's name", required = true)
														   @PathParam("exploratoryName") String exploratoryName) {
		log.debug("Loading scheduler job for user {} and exploratory {}...", userInfo.getName(), exploratoryName);
		final SchedulerJobDTO schedulerJob =
				schedulerJobService.fetchSchedulerJobForUserAndExploratory(userInfo.getName(), exploratoryName);
		return Response.ok(schedulerJob).build();
	}

	/**
	 * Returns scheduler job for computational resource <code>computationalName<code/> affiliated with
	 * exploratory <code>exploratoryName<code/>
	 *
	 * @param userInfo          user info
	 * @param exploratoryName   name of exploratory resource
	 * @param computationalName name of computational resource
	 * @return scheduler job data
	 */
	@GET
	@Path("/{exploratoryName}/{computationalName}")
	@ApiOperation("Returns scheduler's data for cluster")
	@ApiResponses(@ApiResponse(code = 200, message = "Scheduler's data for cluster fetched successfully"))
	public Response fetchSchedulerJobForComputationalResource(@ApiParam(hidden = true) @Auth UserInfo userInfo,
															  @ApiParam(value = "Notebook's name", required = true)
															  @PathParam("exploratoryName") String exploratoryName,
															  @ApiParam(value = "Cluster's name", required = true)
															  @PathParam("computationalName") String computationalName) {
		log.debug("Loading scheduler job for user {}, exploratory {} and computational resource {}...",
				userInfo.getName(), exploratoryName, computationalName);
		final SchedulerJobDTO schedulerJob = schedulerJobService
				.fetchSchedulerJobForComputationalResource(userInfo.getName(), exploratoryName, computationalName);
		return Response.ok(schedulerJob).build();
	}

}

