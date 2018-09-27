package com.epam.dlab.backendapi.resources;

import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.dto.auth.AuthorizationCodeFlowResponse;
import com.epam.dlab.rest.client.RESTService;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.Collections;
import java.util.Map;

@Path("dex")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class DexOauthResource {

	private static final String CODE = "code";
	@Inject
	@Named(ServiceConsts.SECURITY_SERVICE_NAME)
	private RESTService securityService;


	@GET
	@Path("/init")
	public Response redirectedUrl() {
		return Response.seeOther(URI.create(securityService.get("/dex/init", String.class)))
				.build();
	}

	@POST
	@Path("/oauth")
	public Response login(AuthorizationCodeFlowResponse codeFlowResponse) {
		final Map<String, Object> params = Collections.singletonMap(CODE, codeFlowResponse.getCode());
		return securityService.get("/dex/user", params, Response.class);
	}

}
