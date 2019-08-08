package com.epam.dlab.backendapi.resources;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.conf.SelfServiceApplicationConfiguration;
import com.epam.dlab.backendapi.service.SecurityService;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;

@Path("/oauth")
public class KeycloakResource {
	private static final String LOGIN_URI_FORMAT = "%s/realms/%s/protocol/openid-connect/auth?client_id=%s" +
			"&response_type=code";
	private final SecurityService securityService;
	private final String loginUri;

	@Inject
	public KeycloakResource(SecurityService securityService, SelfServiceApplicationConfiguration configuration) {
		this.securityService = securityService;
		loginUri =
				String.format(LOGIN_URI_FORMAT,
						configuration.getKeycloakConfiguration().getAuthServerUrl(),
						configuration.getKeycloakConfiguration().getRealm(),
						configuration.getKeycloakConfiguration().getResource());
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
}
