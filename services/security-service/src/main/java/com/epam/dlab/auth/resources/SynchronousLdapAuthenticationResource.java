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

package com.epam.dlab.auth.resources;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.auth.dto.UserCredentialDTO;
import com.epam.dlab.auth.service.AuthenticationService;
import com.epam.dlab.rest.dto.ErrorDTO;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Used for authentication against LDAP server
 */
@Path("/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class SynchronousLdapAuthenticationResource {
	private static final String INVALID_CREDENTIALS = "Username or password is invalid";
	private final AuthenticationService authenticationService;

	@Inject
	public SynchronousLdapAuthenticationResource(AuthenticationService authenticationService) {
		this.authenticationService = authenticationService;
	}

	@POST
	@Path("/login")
	public Response login(UserCredentialDTO cred) {
		log.debug("validating username:{} password:****** token:{}", cred.getUsername(), cred.getAccessToken());
		return authenticationService.login(cred)
				.map(userInfo -> Response.ok(userInfo.getAccessToken()).build())
				.orElse(unauthorizedResponse());
	}

	@POST
	@Path("/getuserinfo")
	public UserInfo getUserInfo(String accessToken) {
		return authenticationService.getUserInfo(accessToken).orElse(null);
	}

	@POST
	@Path("/logout")
	public Response logout(String accessToken) {
		authenticationService.logout(accessToken);
		return Response.ok().build();
	}

	private Response unauthorizedResponse() {
		return Response.status(Response.Status.UNAUTHORIZED)
				.entity(new ErrorDTO(Response.Status.UNAUTHORIZED.getStatusCode(), INVALID_CREDENTIALS))
				.type(MediaType.APPLICATION_JSON)
				.build();
	}
}
