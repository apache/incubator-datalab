package com.epam.dlab.backendapi.resources;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.conf.SelfServiceApplicationConfiguration;
import com.epam.dlab.backendapi.service.SecurityService;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;

@Path("/oauth")
public class KeycloakResource {
	private static final String LOGIN_URI_FORMAT = "%s/realms/%s/protocol/openid-connect/auth?client_id=%s" +
			"&response_type=code";
	private static final String LOGOUT_URI_FORMAT = "%s/realms/%s/protocol/openid-connect/logout?redirect_uri=";
	private final SecurityService securityService;
	private final String loginUri;
	private final String logoutUri;

	@Inject
	public KeycloakResource(SecurityService securityService, SelfServiceApplicationConfiguration configuration) {
		this.securityService = securityService;
		loginUri =
				String.format(LOGIN_URI_FORMAT,
						configuration.getKeycloakConfiguration().getAuthServerUrl(),
						configuration.getKeycloakConfiguration().getRealm(),
						configuration.getKeycloakConfiguration().getResource());
		logoutUri =
				String.format(LOGOUT_URI_FORMAT,
						configuration.getKeycloakConfiguration().getAuthServerUrl(),
						configuration.getKeycloakConfiguration().getRealm());
	}

	@GET
	public Response getLoginUri() throws URISyntaxException {
		return Response.noContent()
				.location(new URI(loginUri))
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
		return Response.ok().build();
	}

	@GET
	@Path("/logout")
	public Response logout(final @Context HttpServletRequest request) throws URISyntaxException {
		StringBuilder redirectUri = new StringBuilder(logoutUri);
		redirectUri.append(request.getScheme());
		redirectUri.append("://");
		redirectUri.append(request.getServerName());
		redirectUri.append("/#/login");
		return Response.noContent()
				.location(new URI(redirectUri.toString()))
				.build();
	}
}
