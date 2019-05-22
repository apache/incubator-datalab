package com.epam.dlab.backendapi.resources;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.domain.EndpointDTO;
import com.epam.dlab.backendapi.service.EndpointService;
import com.epam.dlab.rest.dto.ErrorDTO;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

@Path("endpoint")
@RolesAllowed("/api/endpoint")
public class EndpointResource {

	private final EndpointService endpointService;
	@Context
	private UriInfo uriInfo;

	@Inject
	public EndpointResource(EndpointService endpointService) {
		this.endpointService = endpointService;
	}

	@Operation(summary = "Create endpoint", tags = "endpoint")
	@ApiResponses({
			@ApiResponse(responseCode = "201", description = "Endpoint is successfully created",
					headers =
					@Header(required = true, name = "Location", description = "URI of created endpoint resource",
							schema = @Schema(type = "string"))),
			@ApiResponse(responseCode = "400", description = "Validation error", content = @Content(mediaType =
					MediaType.APPLICATION_JSON,
					schema = @Schema(implementation = ErrorDTO.class))),
			@ApiResponse(responseCode = "409", description = "Endpoint with passed name already exist in system",
					content = @Content(mediaType = MediaType.APPLICATION_JSON,
							schema = @Schema(implementation = ErrorDTO.class)))
	})
	@Consumes(MediaType.APPLICATION_JSON)
	@POST
	public Response createEndpoint(@Parameter(hidden = true) @Auth UserInfo userInfo, EndpointDTO endpointDTO) {
		endpointService.create(endpointDTO);
		final URI uri = uriInfo.getRequestUriBuilder().path(endpointDTO.getName()).build();
		return Response
				.ok()
				.location(uri)
				.build();
	}

	@Operation(summary = "Get endpoint info", tags = "endpoint")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Return information about endpoint",
					content = @Content(mediaType = MediaType.APPLICATION_JSON, schema =
					@Schema(implementation = EndpointDTO.class))),
			@ApiResponse(responseCode = "404", description = "Endpoint with passed name not found",
					content = @Content(mediaType = MediaType.APPLICATION_JSON,
							schema = @Schema(implementation = ErrorDTO.class)))
	})
	@GET
	@Path("{name}")
	public Response getEndpoint(@Parameter(hidden = true) @Auth UserInfo userInfo,
								@Parameter(description = "Endpoint name")
								@PathParam("name") String name) {
		endpointService.get(name);
		return Response.ok().build();
	}


	@Operation(summary = "Remove endpoint", tags = "endpoint")
	@ApiResponses({
			@ApiResponse(responseCode = "200", description = "Endpoint is successfully removed"),
			@ApiResponse(responseCode = "404", description = "Endpoint with passed name not found",
					content = @Content(mediaType = MediaType.APPLICATION_JSON,
							schema = @Schema(implementation = ErrorDTO.class)))
	})
	@DELETE
	@Path("{name}")
	public Response removeEndpoint(@Parameter(hidden = true) @Auth UserInfo userInfo,
								   @Parameter(description = "Endpoint name")
								   @PathParam("name") String name) {
		endpointService.remove(name);
		return Response.ok().build();
	}
}
