package com.epam.dlab.backendapi.auth;

import com.epam.dlab.auth.UserInfo;
import de.ahus1.keycloak.dropwizard.AbstractKeycloakAuthenticator;
import de.ahus1.keycloak.dropwizard.KeycloakConfiguration;
import io.dropwizard.auth.AuthenticationException;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.representations.AccessToken;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;

public class KeycloakAuthenticator extends AbstractKeycloakAuthenticator<UserInfo> {

	private static final String GROUPS_CLAIM = "groups";

	public KeycloakAuthenticator(KeycloakConfiguration keycloakConfiguration) {
		super(keycloakConfiguration);
	}

	@Override
	public Optional<UserInfo> authenticate(HttpServletRequest request) throws AuthenticationException {
		return super.authenticate(request);

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
