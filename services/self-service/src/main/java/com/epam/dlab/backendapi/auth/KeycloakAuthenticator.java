package com.epam.dlab.backendapi.auth;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.SelfServiceApplication;
import com.epam.dlab.backendapi.dao.SecurityDAO;
import de.ahus1.keycloak.dropwizard.AbstractKeycloakAuthenticator;
import de.ahus1.keycloak.dropwizard.KeycloakConfiguration;
import io.dropwizard.auth.AuthenticationException;
import org.apache.commons.lang3.StringUtils;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.representations.AccessToken;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;

public class KeycloakAuthenticator extends AbstractKeycloakAuthenticator<UserInfo> {

	private static final String TOKEN_PREFIX = "Bearer ";
	private static final String GROUPS_CLAIM = "groups";

	public KeycloakAuthenticator(KeycloakConfiguration keycloakConfiguration) {
		super(keycloakConfiguration);
	}

	@Override
	public Optional<UserInfo> authenticate(HttpServletRequest request) throws AuthenticationException {
		final String token = StringUtils.substringAfter(request.getHeader(HttpHeaders.AUTHORIZATION), TOKEN_PREFIX);
		final Optional<UserInfo> cachedUser =
				SelfServiceApplication.getInjector().getInstance(SecurityDAO.class).getUser(token);
		if (!cachedUser.isPresent()) {
			return super.authenticate(request);
		} else {
			return cachedUser;
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	protected UserInfo prepareAuthentication(KeycloakSecurityContext keycloakSecurityContext,
											 HttpServletRequest httpServletRequest,
											 KeycloakConfiguration keycloakConfiguration) {
		final AccessToken token = keycloakSecurityContext.getToken();
		final UserInfo userInfo = new UserInfo(token.getPreferredUsername(),
				keycloakSecurityContext.getTokenString());
		userInfo.addRoles((List<String>) token.getOtherClaims().getOrDefault(GROUPS_CLAIM, emptyList()));
		return userInfo;
	}
}
