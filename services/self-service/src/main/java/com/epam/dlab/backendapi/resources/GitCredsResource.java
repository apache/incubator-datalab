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

import com.epam.dlab.UserInstanceStatus;
import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.core.UserInstanceDTO;
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.dao.GitCredsDAO;
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.util.RequestBuilder;
import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.dto.exploratory.ExploratoryGitCredsDTO;
import com.epam.dlab.dto.exploratory.ExploratoryGitCredsUpdateDTO;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.client.RESTService;
import com.epam.dlab.rest.contracts.ExploratoryAPI;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * Provides the REST API for managing git credentials
 */
@Path("/user/git_creds")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class GitCredsResource implements ExploratoryAPI {

    @Inject
    private GitCredsDAO gitCredsDAO;
    @Inject
    private ExploratoryDAO exploratoryDAO;
    @Inject
    @Named(ServiceConsts.PROVISIONING_SERVICE_NAME)
    private RESTService provisioningService;

    /**
     * Update GIT credentials for user.
     *
     * @param userInfo user info.
     * @param formDTO  the list of credentials.
     * @return {@link Response.Status#OK} request for provisioning service has been accepted.<br>
     */
    @PUT
    public Response updateGitCreds(@Auth UserInfo userInfo, @Valid @NotNull ExploratoryGitCredsDTO formDTO) {
        log.debug("Updating GIT creds for user {} to {}", userInfo.getName(), formDTO);
        try {
            gitCredsDAO.updateGitCreds(userInfo.getName(), formDTO);

            String failedOnExploratories = "";
            List<UserInstanceDTO> instances = exploratoryDAO.fetchExploratoryFields(userInfo.getName());

            for (UserInstanceDTO instance : instances) {
                if (UserInstanceStatus.RUNNING == UserInstanceStatus.of(instance.getStatus())) {
                    ExploratoryGitCredsUpdateDTO dto = RequestBuilder.newGitCredentialsUpdate(userInfo, instance, formDTO);
                    try {
                        log.debug("Updating GIT creds for user {} on exploratory {}", userInfo.getName(), dto.getExploratoryName());
                        String uuid = provisioningService.post(EXPLORATORY_GIT_CREDS, userInfo.getAccessToken(), dto, String.class);
                        RequestId.put(userInfo.getName(), uuid);
                    } catch (Exception t) {
                        log.error("Cannot update the GIT creds for user {} on exploratory {}",
                                userInfo.getName(), dto.getExploratoryName(), t);
                        failedOnExploratories += String.join(", ", failedOnExploratories, dto.getExploratoryName());
                    }
                }
            }

            if (failedOnExploratories.isEmpty()) {
                return Response.ok().build();
            } else {
                throw new DlabException("Requests for notebooks failed: " + failedOnExploratories);
            }
        } catch (Exception t) {
            log.error("Cannot update the GIT creds for user {}", userInfo.getName(), t);
            throw new DlabException("Cannot update the GIT credentials: " + t.getLocalizedMessage(), t);
        }
    }

    /**
     * Returns the list of the provisioned user resources.
     *
     * @param userInfo user info.
     */
    @GET
    public ExploratoryGitCredsDTO getGitCreds(@Auth UserInfo userInfo) throws DlabException {
        log.debug("Loading GIT creds for user {}", userInfo.getName());
        try {
            return gitCredsDAO.findGitCreds(userInfo.getName(), true);
        } catch (Exception t) {
            log.error("Cannot load list of GIT creds for user: {}", userInfo.getName(), t);
            throw new DlabException("Cannot load GIT credentials for user " + userInfo.getName() + ": " + t.getLocalizedMessage(), t);
        }
    }
}
