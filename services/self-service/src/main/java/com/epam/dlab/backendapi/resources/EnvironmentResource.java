package com.epam.dlab.backendapi.resources;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.service.EnvironmentService;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("environment")
@Slf4j
@RolesAllowed("environment/*")
public class EnvironmentResource {

	@Inject
	private EnvironmentService environmentService;

	@GET
	@Path("user/active")
	@Produces(MediaType.APPLICATION_JSON)
	public Response getUsersWithActiveEnv(@Auth UserInfo userInfo) {
		log.debug("User {} requested information about active environments", userInfo.getName());
		return Response.ok(environmentService.getActiveUsers()).build();
	}

	@POST
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("terminate")
	public Response terminateEnv(@Auth UserInfo userInfo, @NotEmpty String user) {
		log.info("User {} is terminating {} environment", userInfo.getName(), user);
		environmentService.terminateEnvironment(user);
		return Response.ok().build();
	}

	@POST
	@Consumes(MediaType.TEXT_PLAIN)
	@Produces(MediaType.APPLICATION_JSON)
	@Path("stop")
	public Response stopEnv(@Auth UserInfo userInfo, @NotEmpty String user) {
		log.info("User {} is stopping {} environment", userInfo.getName(), user);
		environmentService.stopEnvironment(user);
		return Response.ok().build();
	}
}
