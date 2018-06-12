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

package com.epam.dlab.auth.azure;

import com.epam.dlab.auth.azure.service.AzureAuthorizationCodeService;
import com.epam.dlab.auth.conf.AzureLoginConfiguration;
import com.epam.dlab.dto.azure.auth.AuthorizationCodeFlowResponse;
import com.epam.dlab.exceptions.DlabAuthenticationException;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Path("/user/azure")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class AzureSecurityResource {

	private static final Object DUMMY = new Object();
	private final Cache<String, Object> cache = CacheBuilder.newBuilder().expireAfterWrite(4, TimeUnit.HOURS)
			.maximumSize(10000).build();

	@Inject
	private AzureAuthorizationCodeService authorizationCodeService;

	@Inject
	private AzureLoginUrlBuilder azureLoginUrlBuilder;

	@Inject
	private AzureLoginConfiguration azureLoginConfiguration;

	@GET
	@Path("/init")
	public Response login() {

		log.debug("Init oauth silent login flow");
		String uuid = UUID.randomUUID().toString();
		log.info("Register oauth state {}", uuid);
		cache.put(uuid, DUMMY);

		return Response.ok(azureLoginUrlBuilder.buildSilentLoginUrl(uuid)).build();
	}

	@POST
	@Path("/oauth")
	public Response login(AuthorizationCodeFlowResponse authorizationCodeFlowResponse) {
		log.info("Authenticate client {}", authorizationCodeFlowResponse);
		if (authorizationCodeFlowResponse.isSuccessful()) {
			log.debug("Successfully received auth code {}", authorizationCodeFlowResponse);
			if (cache.getIfPresent(authorizationCodeFlowResponse.getState()) != null) {
				return getAccessTokenResponse(authorizationCodeFlowResponse);
			} else {
				log.warn("Malformed authorization code is retrieved for state {}", authorizationCodeFlowResponse);
			}
		} else {
			log.info("Check if silent authentication {}", authorizationCodeFlowResponse);
			if (cache.getIfPresent(authorizationCodeFlowResponse.getState()) != null
					&& ("login_required".equals(authorizationCodeFlowResponse.getError())
					|| "interaction_required".equals(authorizationCodeFlowResponse.getError()))) {

				log.debug("Silent authentication detected {}", authorizationCodeFlowResponse);
				return Response.status(Response.Status.FORBIDDEN).header("Location", URI.create(
						azureLoginUrlBuilder.buildLoginUrl(authorizationCodeFlowResponse.getState()))).build();
			}
		}

		log.info("Try to log in one more time");
		cache.invalidate(authorizationCodeFlowResponse.getState());
		return Response.status(Response.Status.FORBIDDEN).header("Location", URI.create(
				azureLoginUrlBuilder.buildLoginUrl())).build();
	}

	private Response getAccessTokenResponse(AuthorizationCodeFlowResponse authorizationCodeFlowResponse) {
		log.debug("Retrieving token from {}", authorizationCodeFlowResponse);
		try {
			final AzureLocalAuthResponse response = authorizationCodeService
					.authenticateAndLogin(new AuthorizationCodeSupplier(azureLoginConfiguration,
							authorizationCodeFlowResponse));
			log.debug("Token retrieve response {}", response);
			return Response.ok(response).build();
		} catch (DlabAuthenticationException e) {
			log.error(e.getMessage());
			return Response.status(Response.Status.UNAUTHORIZED)
					.entity(new AzureLocalAuthResponse(null, null, e.getMessage()))
					.build();
		}
	}
}

