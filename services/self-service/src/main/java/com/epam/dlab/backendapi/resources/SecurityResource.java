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
import com.epam.dlab.auth.dto.UserCredentialDTO;
import com.epam.dlab.backendapi.SelfServiceApplicationConfiguration;
import com.epam.dlab.backendapi.dao.SecurityDAO;
import com.epam.dlab.backendapi.domain.EnvStatusListener;
import com.epam.dlab.backendapi.roles.UserRoles;
import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.client.RESTService;
import com.epam.dlab.auth.contract.SecurityAPI;
import com.epam.dlab.validation.AwsValidation;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * Provides the REST API for the user authorization.
 */
@Path("/user")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class SecurityResource implements SecurityAPI {

    private SecurityDAO dao;
    private RESTService securityService;
    private EnvStatusListener envStatusListener;
    private SelfServiceApplicationConfiguration configuration;

	@Inject
	public SecurityResource(SecurityDAO dao, @Named(ServiceConsts.SECURITY_SERVICE_NAME) RESTService securityService,
							EnvStatusListener envStatusListener, SelfServiceApplicationConfiguration configuration) {
		this.dao = dao;
		this.securityService = securityService;
		this.envStatusListener = envStatusListener;
		this.configuration = configuration;
	}

    /**
     * Login method for the DLab user.
     *
     * @param credential user credential.
     * @return 500 Internal Server Error if post response fails.
     */
    @POST
    @Path("/login")
    public Response userLogin(@Valid @NotNull UserCredentialDTO credential) {
        log.debug("Try login for user {}", credential.getUsername());
        try {
            dao.writeLoginAttempt(credential);
            return securityService.post(LOGIN, credential, Response.class);
        } catch (Exception e) {
            log.error("Try login for user {} fail", credential.getUsername(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getLocalizedMessage()).build();
        }
    }


    /**
     * Authorize method for the dlab user.
     *
     * @param userInfo user info.
     * @param username user name.
     * @return 500 Internal Server Error if post request fails.
     */
    @POST
    @Path("/authorize")
    public Response authorize(@Auth UserInfo userInfo, @Valid @NotBlank(groups = AwsValidation.class) String username) {
        log.debug("Try authorize accessToken {} for user info {}", userInfo.getAccessToken(), userInfo);
        try {
        	Status status = userInfo.getName().equalsIgnoreCase(username) ?
        			Status.OK :
        			Status.FORBIDDEN;
        	if (status == Status.OK) {
        		envStatusListener.registerSession(userInfo);
        		if (configuration.isRolePolicyEnabled()) {
        			UserRoles.initialize(dao, configuration.getRoleDefaultAccess());
        		}
        	}
            return Response.status(status).build();
        } catch (Exception e) {
            throw new DlabException("Cannot authorize user " + username + ". " + e.getLocalizedMessage(), e);
        }
    }

    /**
     * Logout method for the DLab user.
     *
     * @param userInfo user info.
     * @return 200 OK or 403 Forbidden.
     */
    @POST
    @Path("/logout")
    public Response userLogout(@Auth UserInfo userInfo) {
        log.debug("Try logout for accessToken {}", userInfo.getAccessToken());
        try {
            envStatusListener.unregisterSession(userInfo);
            return securityService.post(LOGOUT, userInfo.getAccessToken(), Response.class);
        } catch (Exception e) {
            log.error("Try logout for accessToken {}", userInfo.getAccessToken(), e);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(e.getLocalizedMessage()).build();
        }
    }
}