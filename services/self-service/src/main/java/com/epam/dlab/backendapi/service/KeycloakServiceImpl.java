package com.epam.dlab.backendapi.service;

import com.epam.dlab.backendapi.conf.SelfServiceApplicationConfiguration;
import com.epam.dlab.exceptions.DlabException;
import com.google.inject.Inject;
import de.ahus1.keycloak.dropwizard.KeycloakConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.internal.util.Base64;
import org.keycloak.representations.AccessTokenResponse;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

@Slf4j
public class KeycloakServiceImpl implements KeycloakService {

	private static final String URI = "/realms/%s/protocol/openid-connect/token";
	private final Client httpClient;
	private final KeycloakConfiguration conf;

	@Inject
	public KeycloakServiceImpl(Client httpClient, SelfServiceApplicationConfiguration conf) {
		this.httpClient = httpClient;
		this.conf = conf.getKeycloakConfiguration();
	}

	@Override
	public AccessTokenResponse getToken(String code) {
		return requestToken(accessTokenRequestForm(code));
	}

	@Override
	public AccessTokenResponse refreshToken(String refreshToken) {
		return requestToken(refreshTokenRequestForm(refreshToken));
	}

	private AccessTokenResponse requestToken(Form requestForm) {
		final String credentials = Base64.encodeAsString(String.join(":", conf.getResource(),
				String.valueOf(conf.getCredentials().get("secret"))));
		final Response response =
				httpClient.target(conf.getAuthServerUrl() + String.format(URI, conf.getRealm())).request()
				.header(HttpHeaders.AUTHORIZATION, "Basic " + credentials)
				.post(Entity.form(requestForm));
		if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {

			log.error("Error getting token:code {}, body {}", response.getStatus(), response.readEntity(String.class));
			throw new DlabException("can not get token");
		}
		return response.readEntity(AccessTokenResponse.class);
	}

	private Form accessTokenRequestForm(String code) {
		return new Form()
				.param("grant_type", "authorization_code")
				.param("code", code);
	}

	private Form refreshTokenRequestForm(String refreshToken) {
		return new Form()
				.param("grant_type", "refresh_token")
				.param("refresh_token", refreshToken);
	}
}
