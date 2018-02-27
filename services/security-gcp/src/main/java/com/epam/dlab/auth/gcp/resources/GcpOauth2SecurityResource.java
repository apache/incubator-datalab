package com.epam.dlab.auth.gcp.resources;


import com.epam.dlab.auth.gcp.dto.GcpOauth2AuthorizationCodeResponse;
import com.epam.dlab.auth.oauth2.Oauth2AuthenticationService;
import com.epam.dlab.exceptions.DlabAuthenticationException;
import com.epam.dlab.rest.contracts.SecurityAPI;
import com.google.inject.Inject;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Response;
import java.net.URI;

import static java.util.Objects.nonNull;

@Path("/")
public class GcpOauth2SecurityResource {

	@Inject
	private Oauth2AuthenticationService authenticationService;

	@GET
	@Path(SecurityAPI.INIT_LOGIN_OAUTH_GCP)
	public Response redirectedUrl() {
		return Response.seeOther(URI.create(authenticationService.getRedirectedUrl())).build();
	}

	@POST
	@Path(SecurityAPI.LOGIN_OAUTH)
	public Response oauthLogin(GcpOauth2AuthorizationCodeResponse codeResponse) {
		if (nonNull(codeResponse.getErrorMessage())) {
			throw new DlabAuthenticationException(codeResponse.getErrorMessage());
		}
		return Response.ok(authenticationService.authorize(codeResponse.getCode(), codeResponse.getState())).build();
	}

}
