package com.epam.dlab.backendapi.resources.azure;

import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.dto.azure.auth.AuthorizationCodeFlowResponse;
import com.epam.dlab.rest.client.RESTService;
import com.epam.dlab.auth.contract.SecurityAPI;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

@Path("/user/azure")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AzureOauthResource {

	@Inject
	@Named(ServiceConsts.SECURITY_SERVICE_NAME)
	private RESTService securityService;


	@GET
	@Path("/init")
	public Response redirectedUrl() {
		return Response.seeOther(URI.create(securityService.get(SecurityAPI.INIT_LOGIN_OAUTH_AZURE, String.class)))
				.build();
	}

	@POST
	@Path("/oauth")
	public Response login(AuthorizationCodeFlowResponse codeFlowResponse) {
		return securityService.post(SecurityAPI.LOGIN_OAUTH_AZURE, codeFlowResponse, Response.class);
	}

}
