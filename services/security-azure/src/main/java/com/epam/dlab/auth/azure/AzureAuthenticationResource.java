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

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.auth.UserInfoDAO;
import com.epam.dlab.auth.azure.service.AzureAuthorizationCodeService;
import com.epam.dlab.auth.conf.AzureLoginConfiguration;
import com.epam.dlab.auth.contract.SecurityAPI;
import com.epam.dlab.auth.dto.UserCredentialDTO;
import com.epam.dlab.auth.rest.AbstractAuthenticationService;
import com.epam.dlab.dto.azure.auth.AuthorizationCodeFlowResponse;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.microsoft.aad.adal4j.AuthenticationException;
import io.dropwizard.Configuration;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

/**
 * Used to authenticate users against Azure Active Directory
 *
 * @param <C> holds application configuration info
 */
@Path("/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AzureAuthenticationResource<C extends Configuration> extends AbstractAuthenticationService<C> {

	private final UserInfoDAO userInfoDao;
	private final AzureLoginConfiguration azureLoginConfiguration;
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final AzureAuthorizationCodeService authorizationCodeService;

	@Inject
	public AzureAuthenticationResource(C config, UserInfoDAO userInfoDao,
									   AzureLoginConfiguration azureLoginConfiguration, AzureAuthorizationCodeService
											   authorizationCodeService) {
		super(config);
		this.userInfoDao = userInfoDao;
		this.azureLoginConfiguration = azureLoginConfiguration;
		this.authorizationCodeService = authorizationCodeService;
	}

	/**
	 * Authenticates user by given <code>credential</code>
	 *
	 * @param credential contains username and password
	 * @param request    http request
	 * @return authentication result in {@link Response}
	 */
	@Path(SecurityAPI.LOGIN)
	@POST
	public Response login(UserCredentialDTO credential, @Context HttpServletRequest request) {

		log.info("Basic authentication {}", credential);

		try {
			return Response.ok(authorizationCodeService.authenticateAndLogin(new UsernamePasswordSupplier
					(azureLoginConfiguration, credential))).build();
		} catch (AuthenticationException e) {
			log.error("Basic authentication failed", e);
			return handleUserCredentialsLogin(e);
		}
	}

	/**
	 * Returns user info that is mapped with <code>accessToken</code>
	 *
	 * @param accessToken input access token
	 * @param request     http request
	 * @return user info
	 */
	@Override
	@Path(SecurityAPI.GET_USER_INFO)
	@POST
	public UserInfo getUserInfo(String accessToken, @Context HttpServletRequest request) {
		String remoteIp = request.getRemoteAddr();

		UserInfo ui = userInfoDao.getUserInfoByAccessToken(accessToken);

		if (ui != null) {
			ui = ui.withToken(accessToken);
			userInfoDao.updateUserInfoTTL(accessToken, ui);
			log.debug("restored UserInfo from DB {}", ui);
		}

		log.debug("Authorized {} {} {}", accessToken, ui, remoteIp);
		return ui;
	}

	/**
	 * Logs out user by input <code>accessToken</code>
	 *
	 * @param accessToken input access yoken
	 * @return result of the operation
	 */
	@Override
	@Path(SecurityAPI.LOGOUT)
	@POST
	public Response logout(String accessToken) {
		userInfoDao.deleteUserInfo(accessToken);
		log.info("Logged out user {}", accessToken);
		return Response.ok().build();
	}

	/**
	 * Using OAuth2 authorization code grant approach authenticates user by given authorization code in
	 * <code>response</code>
	 *
	 * @param response contains username and passwrd
	 * @return authentication result in {@link Response}
	 */
	@Path(SecurityAPI.LOGIN_OAUTH)
	@POST
	public Response authenticateOAuth(AuthorizationCodeFlowResponse response) {

		log.info("Try to login using authorization code {}", response);

		try {
			return Response.ok(authorizationCodeService.authenticateAndLogin(new AuthorizationCodeSupplier
					(azureLoginConfiguration, response))).build();
		} catch (AuthenticationException e) {
			log.error("OAuth authentication failed", e);
			return Response.status(Response.Status.UNAUTHORIZED)
					.entity(new AzureLocalAuthResponse(null, null,
							"User authentication failed")).build();
		}
	}

	private Response handleUserCredentialsLogin(AuthenticationException e) {
		String message = e.getMessage();

		log.info("Try to handle exception with message {}", message);

		String invalidGrantError = "invalid_grant";
		String errorCode = "AADSTS65001";
		String errorDescriptionKey = "error_description";

		if (StringUtils.isNotEmpty(message)) {
			try {
				Map<String, String> errors = objectMapper
						.readValue(message,
								new TypeReference<HashMap<String, String>>() {
								});
				if (errors != null
						&& invalidGrantError.equalsIgnoreCase(errors.get("error"))
						&& StringUtils.isNotEmpty(errors.get(errorDescriptionKey))
						&& errors.get(errorDescriptionKey).startsWith(errorCode)) {

					return Response.status(Response.Status.FORBIDDEN)
							.header("Location", URI.create(azureLoginConfiguration.getRedirectUrl()
									+ "api" + SecurityAPI.INIT_LOGIN_OAUTH_AZURE)).build();
				}
			} catch (IOException ioException) {
				log.warn("Cannot handle authentication exception", ioException);
			}
		}
		return Response.status(Response.Status.UNAUTHORIZED)
				.entity(new AzureLocalAuthResponse(null, null,
						"User authentication failed")).build();
	}
}
