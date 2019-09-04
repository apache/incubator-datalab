package com.epam.dlab.backendapi.service;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.SecurityDAO;
import com.epam.dlab.exceptions.DlabException;
import com.google.inject.Inject;
import org.keycloak.common.util.Base64Url;
import org.keycloak.representations.AccessTokenResponse;
import org.keycloak.representations.IDToken;
import org.keycloak.util.JsonSerialization;

public class SecurityServiceImpl implements SecurityService {
	private final KeycloakService keycloakService;
	private final SecurityDAO securityDAO;

	@Inject
	public SecurityServiceImpl(KeycloakService keycloakService, SecurityDAO securityDAO) {
		this.keycloakService = keycloakService;
		this.securityDAO = securityDAO;
	}

	@Override
	public UserInfo getUserInfo(String code) {
		final AccessTokenResponse token = keycloakService.getToken(code);
		final String username = parseToken(token.getToken()).getPreferredUsername();
		securityDAO.saveUser(username, token);
		return new UserInfo(username, token.getToken());
	}

	@Override
	public UserInfo getUserInfoOffline(String username) {
		return securityDAO.getTokenResponse(username)
				.map(AccessTokenResponse::getRefreshToken)
				.map(keycloakService::refreshToken)
				.map(accessTokenResponse -> new UserInfo(parseToken(accessTokenResponse.getToken()).getPreferredUsername(), accessTokenResponse.getToken()))
				.orElseThrow(() -> new DlabException("Can not find token for user " + username));
	}

	private IDToken parseToken(String encoded) {
		try {
			String[] parts = encoded.split("\\.");
			if (parts.length < 2 || parts.length > 3) {
				throw new IllegalArgumentException("Parsing error");
			}
			byte[] bytes = Base64Url.decode(parts[1]);
			return JsonSerialization.readValue(bytes, IDToken.class);
		} catch (Exception e) {
			throw new DlabException("Can not parse token due to: " + e.getMessage());
		}
	}
}
