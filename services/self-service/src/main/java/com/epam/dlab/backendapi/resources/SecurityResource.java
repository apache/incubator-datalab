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
import com.epam.dlab.auth.contract.SecurityAPI;
import com.epam.dlab.auth.dto.UserCredentialDTO;
import com.epam.dlab.backendapi.conf.SelfServiceApplicationConfiguration;
import com.epam.dlab.backendapi.dao.SecurityDAO;
import com.epam.dlab.backendapi.domain.EnvStatusListener;
import com.epam.dlab.backendapi.resources.swagger.SwaggerSecurityInfo;
import com.epam.dlab.backendapi.roles.UserRoles;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.client.RESTService;
import com.epam.dlab.rest.dto.ErrorDTO;
import com.epam.dlab.validation.AwsValidation;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import io.swagger.annotations.*;
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
@Api("Authorization service")
@Slf4j
public class SecurityResource implements SecurityAPI {

	private SecurityDAO dao;
	private RESTService securityService;
	private EnvStatusListener envStatusListener;
	private SelfServiceApplicationConfiguration configuration;

	@Inject
	public SecurityResource(SecurityDAO dao,
							EnvStatusListener envStatusListener, SelfServiceApplicationConfiguration configuration) {
		this.dao = dao;
		this.securityService = null;
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
	@Produces(MediaType.TEXT_PLAIN)
	@Path("/login")
	@ApiOperation("Login attempt for user")
	@ApiResponses({@ApiResponse(code = 500, message = "Internal server error occurred"),
			@ApiResponse(code = 200, message = "User logged in successfully")})
	public Response userLogin(@ApiParam(value = "User credential DTO", required = true)
							  @Valid @NotNull UserCredentialDTO credential) {
		log.debug("Try login for user {}", credential.getUsername());
		try {
			dao.writeLoginAttempt(credential);
			return securityService.post(LOGIN, credential, Response.class);
		} catch (Exception e) {
			log.error("Try login for user {} fail", credential.getUsername(), e);
			final Status internalServerError = Status.INTERNAL_SERVER_ERROR;
			return Response.status(internalServerError)
					.entity(new ErrorDTO(internalServerError.getStatusCode(), e.getMessage()))
					.type(MediaType.APPLICATION_JSON)
					.build();
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
	@Consumes(MediaType.TEXT_PLAIN)
	@Path("/authorize")
	@ApiOperation(value = "Authorize attempt for user", authorizations =
	@Authorization(SwaggerSecurityInfo.TOKEN_AUTH))
	@ApiResponses({
			@ApiResponse(code = 500, message = "Access forbidden"),
			@ApiResponse(code = 200, message = "User authorized successfully")
	})
	public Response authorize(@ApiParam(hidden = true) @Auth UserInfo userInfo,
							  @ApiParam(value = "User's name", required = true)
							  @Valid @NotBlank(groups = AwsValidation.class) String username) {
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
	@ApiOperation(value = "Logout attempt for user", authorizations = @Authorization(SwaggerSecurityInfo.TOKEN_AUTH))
	@ApiResponses({@ApiResponse(code = 500, message = "Internal server error occured"),
			@ApiResponse(code = 403, message = "Logout failed"),
			@ApiResponse(code = 200, message = "User logged out successfully")})
	public Response userLogout(@ApiParam(hidden = true) @Auth UserInfo userInfo) {
		log.debug("Try logout for accessToken {}", userInfo.getAccessToken());
		try {
			envStatusListener.unregisterSession(userInfo);
			return securityService.post(LOGOUT, userInfo.getAccessToken(), Response.class);
		} catch (Exception e) {
			log.error("Try logout for accessToken {}", userInfo.getAccessToken(), e);
			final Status internalServerError = Status.INTERNAL_SERVER_ERROR;
			return Response.status(internalServerError)
					.entity(new ErrorDTO(internalServerError.getStatusCode(), e.getMessage()))
					.type(MediaType.APPLICATION_JSON)
					.build();
		}
	}
}