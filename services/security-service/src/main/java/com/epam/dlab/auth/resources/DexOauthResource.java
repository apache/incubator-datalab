package com.epam.dlab.auth.resources;

import com.epam.dlab.auth.service.DexOauthService;
import com.google.inject.Inject;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("dex")
public class DexOauthResource {

	private final DexOauthService dexOauthService;

	@Inject
	public DexOauthResource(DexOauthService dexOauthService) {
		this.dexOauthService = dexOauthService;
	}

	@GET
	@Path("init")
	public Response redirectUrl() {
		return Response.ok(dexOauthService.getDexOauthUrl()).build();
	}


	@GET
	@Path("user")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getUserInfo(@QueryParam("code") String code) {
		return Response.ok(dexOauthService.getUserInfo(code)).build();
	}
}
