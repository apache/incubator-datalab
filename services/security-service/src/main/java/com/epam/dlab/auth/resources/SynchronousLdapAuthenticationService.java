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

import com.epam.dlab.auth.SecurityServiceConfiguration;
import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.auth.UserInfoDAO;
import com.epam.dlab.auth.UserVerificationService;
import com.epam.dlab.auth.dao.LdapUserDAO;
import com.epam.dlab.auth.dto.UserCredentialDTO;
import com.epam.dlab.auth.rest.AbstractAuthenticationService;
import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.exceptions.DlabException;
import com.google.inject.Inject;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Used for authentication against LDAP server
 */
@Path("/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class SynchronousLdapAuthenticationService extends AbstractAuthenticationService<SecurityServiceConfiguration> {
	private final LdapUserDAO ldapUserDAO;
	private final UserInfoDAO userInfoDao;
	private final UserVerificationService userVerificationService;

	@Inject
	public SynchronousLdapAuthenticationService(SecurityServiceConfiguration config, UserInfoDAO userInfoDao,
												LdapUserDAO ldapUserDAO,
												UserVerificationService userVerificationService) {
		super(config);
		this.ldapUserDAO = ldapUserDAO;
		this.userInfoDao = userInfoDao;
		this.userVerificationService = userVerificationService;
	}

	@Override
	@POST
	@Path("/login")
	public Response login(UserCredentialDTO credential, @Context HttpServletRequest request) {

		String username = credential.getUsername();
		String password = credential.getPassword();
		String accessToken = credential.getAccessToken();
		String remoteIp = request.getRemoteAddr();
		String userAgent = request.getHeader(HttpHeaders.USER_AGENT);

		log.debug("validating username:{} password:****** token:{} ip:{}", username, accessToken, remoteIp);

		if (accessToken != null && !accessToken.isEmpty()) {
			UserInfo ui = getUserInfo(accessToken, userAgent, remoteIp);
			if (ui != null) {
				return Response.ok(accessToken).build();
			} else {
				log.debug("User info not found on login by access_token for user", username);
				return Response.status(Response.Status.UNAUTHORIZED).build();
			}
		}

		try {

			login(username, password);
			UserInfo enriched = enrichUser(username);
			userVerificationService.verify(username, enriched);

			enriched.setRemoteIp(remoteIp);
			log.info("User authenticated is {}", enriched);
			String token = getRandomToken();

			userInfoDao.saveUserInfo(enriched.withToken(token));
			return Response.ok(token).build();

		} catch (Exception e) {
			log.error("User {} is not authenticated", username, e);
			return Response.status(Response.Status.UNAUTHORIZED).entity(e.getMessage()).build();
		}
	}

	@Override
	@POST
	@Path("/getuserinfo")
	public UserInfo getUserInfo(String accessToken, @Context HttpServletRequest request) {
		String userAgent = request.getHeader(HttpHeaders.USER_AGENT);
		String remoteIp = request.getRemoteAddr();

		UserInfo ui = getUserInfo(accessToken, userAgent, remoteIp);

		if (ui != null) {
			return ui;
		}

		log.debug("Session {} is expired", accessToken);

		return null;
	}

	private UserInfo getUserInfo(String accessToken, String userAgent, String remoteIp) {

		UserInfo ui = userInfoDao.getUserInfoByAccessToken(accessToken);

		if (ui != null) {
			ui = ui.withToken(accessToken);
			updateTTL(accessToken, ui, userAgent);
			log.debug("restored UserInfo from DB {}", ui);

			log.debug("Authorized {} {} {}", accessToken, ui, remoteIp);
			return ui;
		}

		return null;

	}

	@Override
	@POST
	@Path("/logout")
	public Response logout(String accessToken) {
		userInfoDao.deleteUserInfo(accessToken);
		log.info("Logged out user {}", accessToken);
		return Response.ok().build();
	}

	private UserInfo login(String username, String password) {
		try {
			UserInfo userInfo = ldapUserDAO.getUserInfo(username, password);
			log.debug("User Authenticated: {}", username);
			return userInfo;
		} catch (Exception e) {
			log.error("Authentication error", e);
			throw new DlabException("Username or password are not valid", e);
		}
	}

	private UserInfo enrichUser(String username) {

		try {
			UserInfo userInfo = ldapUserDAO.enrichUserInfo(new UserInfo(username, null));
			log.debug("User Enriched: {}", username);
			return userInfo;
		} catch (Exception e) {
			log.error("Authentication error", e);
			throw new DlabException("User not authorized. Please contact DLAB administrator.");
		}
	}


	private void updateTTL(String accessToken, UserInfo ui, String userAgent) {
		log.debug("updating TTL agent {} {}", userAgent, ui);
		if (ServiceConsts.PROVISIONING_USER_AGENT.equals(userAgent)) {
			return;
		}

		userInfoDao.updateUserInfoTTL(accessToken, ui);
	}
}
