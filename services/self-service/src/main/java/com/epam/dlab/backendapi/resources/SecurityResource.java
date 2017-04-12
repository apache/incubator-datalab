/***************************************************************************

Copyright (c) 2016, EPAM SYSTEMS INC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

****************************************************************************/

package com.epam.dlab.backendapi.resources;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.MongoCollections;
import com.epam.dlab.backendapi.dao.SecurityDAO;
import com.epam.dlab.backendapi.dao.SettingsDAO;
import com.epam.dlab.backendapi.domain.EnvStatusListener;
import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.dto.UserCredentialDTO;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.client.RESTService;
import com.epam.dlab.rest.contracts.SecurityAPI;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.dropwizard.auth.Auth;
import org.hibernate.validator.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/** Provides the REST API for the user authorization.
 */
@Path("/user")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SecurityResource implements MongoCollections, SecurityAPI {
    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityResource.class);

    @Inject
    private SecurityDAO dao;
    @Inject
    @Named(ServiceConsts.SECURITY_SERVICE_NAME)
    RESTService securityService;
    @Inject
    private SettingsDAO settingsDAO;

    /** Login method for the dlab user.
     * @param credential user credential.
     * @return 500 Internal Server Error if post response fails.
     */
    @POST
    @Path("/login")
    public Response login(@NotNull UserCredentialDTO credential) {
        LOGGER.debug("Try login for user {}", credential.getUsername());
        try {
            dao.writeLoginAttempt(credential);
			return securityService.post(LOGIN, credential, Response.class);
        } catch (Throwable t) {
        	LOGGER.error("Try login for user {} fail", credential.getUsername(), t);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(t.getLocalizedMessage()).build();
        }
    }


    /** Authorize method for the dlab user.
     * @param userInfo user info.
     * @param username user name.
     * @return 500 Internal Server Error if post response fails.
     */
    @POST
    @Path("/authorize")
    public Response authorize(@Auth UserInfo userInfo, @Valid @NotBlank String username) throws DlabException {
        LOGGER.debug("Try authorize accessToken {} for user info {}", userInfo.getAccessToken(), userInfo);
        Status status = userInfo.getName().equalsIgnoreCase(username) ?
                Status.OK :
                Status.FORBIDDEN;
        if (status == Status.OK) {
        	EnvStatusListener.listen(userInfo.getName(), userInfo.getAccessToken(), settingsDAO.getAwsRegion());
        }
        return Response.status(status).build();
    }

    /** Logout method for the DLab user.
     * @param userInfo user info.
     * @return 200 OK or 403 Forbidden.
     */
    @POST
    @Path("/logout")
    public Response logout(@Auth UserInfo userInfo) {
        LOGGER.debug("Try logout for accessToken {}", userInfo.getAccessToken());
        try {
        	EnvStatusListener.listenStop(userInfo.getName());
            return securityService.post(LOGOUT, userInfo.getAccessToken(), Response.class);
        } catch(Throwable t) {
        	LOGGER.error("Try logout for accessToken {}", userInfo.getAccessToken(), t.getLocalizedMessage());
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(t.getLocalizedMessage()).build();
        }
    }
}