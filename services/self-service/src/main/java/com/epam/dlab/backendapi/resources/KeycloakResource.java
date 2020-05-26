package com.epam.dlab.backendapi.resources;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.conf.KeycloakConfiguration;
import com.epam.dlab.backendapi.conf.SelfServiceApplicationConfiguration;
import com.epam.dlab.backendapi.dao.SecurityDAO;
import com.epam.dlab.backendapi.roles.UserRoles;
import com.epam.dlab.backendapi.service.KeycloakService;
import com.epam.dlab.backendapi.service.SecurityService;
import com.epam.dlab.exceptions.DlabException;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import org.keycloak.representations.AccessTokenResponse;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;

import static java.lang.String.format;

@Path("/oauth")
public class KeycloakResource {
	private static final String LOGIN_URI_FORMAT = "%s/realms/%s/protocol/openid-connect/auth?client_id=%s" +
			"&redirect_uri=%s&response_type=code";
	private static final String KEYCLOAK_LOGOUT_URI_FORMAT = "%s/realms/%s/protocol/openid-connect/logout" +
			"?redirect_uri=%s";
	private final SecurityService securityService;
	private final KeycloakService keycloakService;
	private final SecurityDAO securityDAO;
	private final String loginUri;
	private final String logoutUri;
	private final String redirectUri;
	private final boolean defaultAccess;

	@Inject
	public KeycloakResource(SecurityService securityService, SelfServiceApplicationConfiguration configuration,
							SecurityDAO securityDAO, KeycloakService keycloakService) {
		this.securityDAO = securityDAO;
		this.defaultAccess = configuration.getRoleDefaultAccess();
		final KeycloakConfiguration keycloakConfiguration = configuration.getKeycloakConfiguration();
		this.redirectUri = keycloakConfiguration.getRedirectUri();
		this.securityService = securityService;
		this.keycloakService = keycloakService;

		loginUri =
				format(LOGIN_URI_FORMAT,
						keycloakConfiguration.getAuthServerUrl(),
						keycloakConfiguration.getRealm(),
						keycloakConfiguration.getResource(),
						redirectUri);
		logoutUri =
				format(KEYCLOAK_LOGOUT_URI_FORMAT,
						keycloakConfiguration.getAuthServerUrl(),
						keycloakConfiguration.getRealm(),
						redirectUri);
	}

	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public Response getLoginUri() throws URISyntaxException {
		return Response.ok(new URI(loginUri).toString())
				.build();
	}

	@POST
	@Produces(MediaType.APPLICATION_JSON)
	public Response getUser(@QueryParam("code") String code) {
		return Response.ok(securityService.getUserInfo(code)).build();
	}

	@POST
	@Path("/authorize")
	public Response authorize(@Auth UserInfo userInfo) {
		UserRoles.initialize(securityDAO, defaultAccess);
		return Response.ok().build();
	}

	@GET
	@Path("/logout")
	public Response getLogoutUrl() throws URISyntaxException {
		return Response.noContent()
				.location(new URI(logoutUri))
				.build();
	}

	@POST
	@Path("/refresh/{refresh_token}")
	@Produces(MediaType.APPLICATION_JSON)
	public Response refreshAccessToken(@PathParam("refresh_token") String refreshToken) throws URISyntaxException {
		AccessTokenResponse tokenResponse;
		try {
			tokenResponse = keycloakService.generateAccessToken(refreshToken);
		} catch (DlabException e) {
			return Response.status(Response.Status.BAD_REQUEST)
					.location(new URI(logoutUri))
					.build();
		}
		return Response.ok(new TokenInfo(tokenResponse.getToken(), tokenResponse.getRefreshToken())).build();
	}

	class TokenInfo {
		@JsonProperty("access_token")
		private final String accessToken;
		@JsonProperty("refresh_token")
		private final String refreshToken;

		TokenInfo(String accessToken, String refreshToken) {
			this.accessToken = accessToken;
			this.refreshToken = refreshToken;
		}
	}
}
