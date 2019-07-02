package com.epam.dlab.backendapi.service;

import org.keycloak.representations.AccessTokenResponse;

public interface KeycloakService {
	AccessTokenResponse getToken(String code);
	AccessTokenResponse refreshToken(String refreshToken);
}
