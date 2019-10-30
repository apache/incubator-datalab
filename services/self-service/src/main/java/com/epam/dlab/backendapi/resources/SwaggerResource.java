package com.epam.dlab.backendapi.resources;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;

@Path("swagger")
public class SwaggerResource {
	private static final String SWAGGER_DESCRIPTION_FILE = "/endpoint-api.yml";
	private final ObjectMapper mapper = new ObjectMapper(new YAMLFactory());

	@GET
	@Produces(MediaType.APPLICATION_JSON)
	public Response getSwaggerConfig() throws IOException {
		return Response.ok(mapper.readValue(getClass().getResourceAsStream(SWAGGER_DESCRIPTION_FILE), Object.class))
				.build();

	}
}
