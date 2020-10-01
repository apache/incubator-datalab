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

package com.epam.datalab.backendapi.resources;

import com.epam.datalab.auth.UserInfo;
import com.epam.datalab.backendapi.service.GitCredentialService;
import com.epam.datalab.dto.exploratory.ExploratoryGitCredsDTO;
import com.epam.datalab.rest.contracts.ExploratoryAPI;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
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

    private final GitCredentialService gitCredentialService;

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
    public Response updateGitCreds(@Auth UserInfo userInfo, @Valid @NotNull ExploratoryGitCredsDTO formDTO) {
        gitCredentialService.updateGitCredentials(userInfo, formDTO);
        return Response.ok().build();
    }

    /**
     * Returns info about GIT credentials for user.
     *
     * @param userInfo user info.
     */
    @GET
    public ExploratoryGitCredsDTO getGitCreds(@Auth UserInfo userInfo) {
        return gitCredentialService.getGitCredentials(userInfo.getName());
    }
}
