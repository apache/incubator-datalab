package com.epam.dlab.backendapi.resources;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.conf.KeycloakConfiguration;
import com.epam.dlab.backendapi.conf.SelfServiceApplicationConfiguration;
import com.epam.dlab.backendapi.dao.SecurityDAO;
import com.epam.dlab.backendapi.roles.UserRoles;
import com.epam.dlab.backendapi.service.SecurityService;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;

import static java.lang.String.format;

@Path("/oauth")
public class KeycloakResource {
	private static final String LOGIN_URI_FORMAT = "%s/realms/%s/protocol/openid-connect/auth?client_id=%s" +
			"&response_type=code";
	private static final String KEYCLOAK_LOGOUT_URI_FORMAT = "%s/realms/%s/protocol/openid-connect/logout" +
			"?redirect_uri=";
	private final SecurityService securityService;
	private final SecurityDAO securityDAO;
	private final String loginUri;
	private final String logoutUri;
	private final String redirectUri;
	private final boolean defaultAccess;

	@Inject
	public KeycloakResource(SecurityService securityService, SelfServiceApplicationConfiguration configuration,
							SecurityDAO securityDAO) {
		this.securityDAO = securityDAO;
		this.defaultAccess = configuration.getRoleDefaultAccess();
		final KeycloakConfiguration keycloakConfiguration = configuration.getKeycloakConfiguration();
		this.redirectUri = keycloakConfiguration.getRedirectUri();
		this.securityService = securityService;

		loginUri =
				format(LOGIN_URI_FORMAT,
						keycloakConfiguration.getAuthServerUrl(),
						keycloakConfiguration.getRealm(),
						keycloakConfiguration.getResource());
		logoutUri =
				format(KEYCLOAK_LOGOUT_URI_FORMAT,
						keycloakConfiguration.getAuthServerUrl(),
						keycloakConfiguration.getRealm());
	}

	@GET
	@Produces(MediaType.TEXT_PLAIN)
	public Response getLoginUri() {
		return Response.ok(loginUri)
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
				.location(new URI(logoutUri + redirectUri))
				.build();
	}
}
