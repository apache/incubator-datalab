package com.epam.dlab.backendapi.resources;

import com.epam.dlab.backendapi.service.SecurityService;
import com.google.inject.Inject;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/test")
public class KeycloakResource {


	private final SecurityService securityService;

	@Inject
	public KeycloakResource(SecurityService securityService) {
		this.securityService = securityService;
	}

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getUser(@QueryParam("code") String code) {
		return Response.ok(securityService.getUserInfo(code)).build();
	}
}
