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
import com.epam.dlab.backendapi.resources.swagger.SwaggerSecurityInfo;
import com.epam.dlab.backendapi.service.InfrastructureTemplateService;
import com.epam.dlab.dto.base.computational.FullComputationalTemplate;
import com.epam.dlab.dto.imagemetadata.ExploratoryMetadataDTO;
import com.epam.dlab.rest.contracts.DockerAPI;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.Authorization;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Provides the REST API to retrieve exploratory/computational templates.
 */
@Path("/infrastructure_templates")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "Service for retrieving notebook and cluster templates",
		authorizations = @Authorization(SwaggerSecurityInfo.TOKEN_AUTH))
public class InfrastructureTemplateResource implements DockerAPI {

	private InfrastructureTemplateService infrastructureTemplateService;

	@Inject
	public InfrastructureTemplateResource(InfrastructureTemplateService infrastructureTemplateService) {
		this.infrastructureTemplateService = infrastructureTemplateService;
	}

	/**
	 * Returns the list of the computational resources templates for user.
	 *
	 * @param userInfo user info.
	 */
	@GET
	@Path("/computational_templates")
	@ApiOperation("Returns list of cluster's templates")
	public Iterable<FullComputationalTemplate> getComputationalTemplates(@ApiParam(hidden = true)
																		 @Auth UserInfo userInfo) {
		return infrastructureTemplateService.getComputationalTemplates(userInfo);
	}

	/**
	 * Returns the list of the exploratory environment templates for user.
	 *
	 * @param userInfo user info.
	 */
	@GET
	@Path("/exploratory_templates")
	@ApiOperation("Returns list of notebook's templates")
	public Iterable<ExploratoryMetadataDTO> getExploratoryTemplates(@ApiParam(hidden = true) @Auth UserInfo userInfo) {
		return infrastructureTemplateService.getExploratoryTemplates(userInfo);
	}
}

