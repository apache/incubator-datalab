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
import com.epam.dlab.backendapi.service.GitCredentialService;
import com.epam.dlab.dto.exploratory.ExploratoryGitCredsDTO;
import com.epam.dlab.rest.contracts.ExploratoryAPI;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
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
@Slf4j
public class GitCredsResource implements ExploratoryAPI {

	private GitCredentialService gitCredentialService;

	@Inject
	GitCredsResource(GitCredentialService gitCredentialService) {
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
	public Response updateGitCreds(@Auth UserInfo userInfo, @Valid @NotNull ExploratoryGitCredsDTO formDTO) {
		gitCredentialService.updateGitCredentials(userInfo, formDTO);
		return Response.ok().build();
	}

	/**
	 * Returns the list of the provisioned user resources.
	 *
	 * @param userInfo user info.
	 */
	@GET
	public ExploratoryGitCredsDTO getGitCreds(@Auth UserInfo userInfo) {
		return gitCredentialService.getGitCredentials(userInfo.getName());
	}
}
