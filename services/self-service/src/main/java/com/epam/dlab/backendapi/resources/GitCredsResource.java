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
import com.epam.dlab.backendapi.service.GitCredentialService;
import com.epam.dlab.dto.exploratory.ExploratoryGitCredsDTO;
import com.epam.dlab.rest.contracts.ExploratoryAPI;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import io.swagger.annotations.*;
import lombok.extern.slf4j.Slf4j;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Provides the REST API for managing git credentials
 */
@Path("/user/git_creds")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Api(value = "Service for updating or retrieving GIT credentials",
		authorizations = @Authorization(SwaggerSecurityInfo.TOKEN_AUTH))
@Slf4j
public class GitCredsResource implements ExploratoryAPI {

	private GitCredentialService gitCredentialService;

	@Inject
	public GitCredsResource(GitCredentialService gitCredentialService) {
		this.gitCredentialService = gitCredentialService;
	}

	/**
	 * Update GIT credentials for user.
	 *
	 * @param userInfo user info.
	 * @param formDTO  the list of credentials.
	 * @return {@link Response.Status#OK} request for provisioning service has been accepted.<br>
	 */
	@PUT
	@ApiOperation("Updates GIT credentials")
	@ApiResponses(@ApiResponse(code = 200, message = "GIT credentials updated successfully"))
	public Response updateGitCreds(@ApiParam(hidden = true) @Auth UserInfo userInfo,
								   @ApiParam(value = "Notebook GIT credentials form DTO", required = true)
								   @Valid @NotNull ExploratoryGitCredsDTO formDTO) {
		gitCredentialService.updateGitCredentials(userInfo, formDTO);
		return Response.ok().build();
	}

	/**
	 * Returns info about GIT credentials for user.
	 *
	 * @param userInfo user info.
	 */
	@GET
	@ApiOperation("Fetches info about GIT credentials")
	public ExploratoryGitCredsDTO getGitCreds(@ApiParam(hidden = true) @Auth UserInfo userInfo) {
		return gitCredentialService.getGitCredentials(userInfo.getName());
	}
}
