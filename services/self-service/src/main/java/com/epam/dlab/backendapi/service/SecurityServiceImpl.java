package com.epam.dlab.backendapi.service;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.SecurityDAO;
import com.epam.dlab.backendapi.util.KeycloakUtil;
import com.epam.dlab.exceptions.DlabException;
import com.google.inject.Inject;
import org.keycloak.representations.AccessTokenResponse;

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
		final String username = KeycloakUtil.parseToken(token.getToken()).getPreferredUsername();
		securityDAO.saveUser(username, token);
		UserInfo userInfo = new UserInfo(username, token.getToken());
		userInfo.setRefreshToken(token.getRefreshToken());
		return userInfo;
	}

	@Override
	public UserInfo getUserInfoOffline(String username) {
		return securityDAO.getTokenResponse(username)
				.map(AccessTokenResponse::getRefreshToken)
				.map(keycloakService::refreshToken)
				.map(accessTokenResponse -> new UserInfo(KeycloakUtil.parseToken(accessTokenResponse.getToken()).getPreferredUsername(),
						accessTokenResponse.getToken()))
				.orElseThrow(() -> new DlabException("Can not find token for user " + username));
	}

	@Override
	public UserInfo getServiceAccountInfo(String username) {
		AccessTokenResponse accessTokenResponse = keycloakService.generateServiceAccountToken();
		return new UserInfo(username, accessTokenResponse.getToken());
	}
}
