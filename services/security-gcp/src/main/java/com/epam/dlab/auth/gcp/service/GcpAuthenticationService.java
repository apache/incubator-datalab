package com.epam.dlab.auth.gcp.service;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.auth.UserInfoDAO;
import com.epam.dlab.auth.oauth2.Oauth2AuthenticationService;
import com.epam.dlab.config.gcp.GcpLoginConfiguration;
import com.epam.dlab.exceptions.DlabAuthenticationException;
import com.epam.dlab.exceptions.DlabException;
import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.TokenResponse;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.plus.Plus;
import com.google.api.services.plus.model.Person;
import com.google.common.cache.Cache;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.Objects;
import java.util.UUID;

@Singleton
@Slf4j
public class GcpAuthenticationService implements Oauth2AuthenticationService {

	private static final Object DUMMY = new Object();
	@Inject
	private AuthorizationCodeFlow codeFlow;
	@Inject
	private GcpLoginConfiguration configuration;
	@Inject
	private Cache<String, Object> cache;
	@Inject
	private UserInfoDAO userInfoDAO;
	@Inject
	private HttpTransport httpTransport;
	@Inject
	private JacksonFactory jacksonFactory;

	@Override
	public String getRedirectedUrl() {
		String uuid = UUID.randomUUID().toString();
		log.trace("Registered oauth state {}", uuid);
		cache.put(uuid, DUMMY);
		return codeFlow.newAuthorizationUrl()
				.setState(uuid)
				.setRedirectUri(configuration.getRedirectedUri()).build();
	}

	@Override
	public String authorize(String code, String state) {
		if (Objects.nonNull(cache.getIfPresent(state))) {
			final UserInfo userInfo = getUserInfo(code);
			userInfoDAO.saveUserInfo(userInfo);
			log.trace("Removing oauth state {}", state);
			cache.invalidate(state);
			log.debug("Successfully login user {} using oauth2", userInfo.getName());
			return userInfo.getAccessToken();
		}
		log.error("There is no state {} present in cache", state);
		throw new DlabAuthenticationException("You do not have proper permissions to use DLab. Please contact your " +
				"administrator");
	}

	private UserInfo getUserInfo(String code) {
		try {
			final TokenResponse tokenResponse = codeFlow.newTokenRequest(code)
					.setRedirectUri(configuration.getRedirectedUri())
					.execute();
			Plus plus = new Plus.
					Builder(httpTransport, jacksonFactory, codeFlow.createAndStoreCredential(tokenResponse, null))
					.setApplicationName(configuration.getApplicationName())
					.build();
			return toUserInfo(plus.people().get("me").execute());
		} catch (IOException e) {
			log.error("Exception occurred during google oauth2 authentication: {}", e.getMessage());
			throw new DlabException("Exception occurred during google oauth2 authentication: " + e.getMessage());
		}
	}

	private UserInfo toUserInfo(Person googleUser) {
		log.trace("Creating user from google user: {}", googleUser.getDisplayName());
		final UserInfo userInfo = new UserInfo(googleUser.getDisplayName(), UUID.randomUUID().toString());
		final Person.Name name = googleUser.getName();
		userInfo.setFirstName(name.getGivenName());
		userInfo.setLastName(name.getFamilyName());
		return userInfo;
	}
}
