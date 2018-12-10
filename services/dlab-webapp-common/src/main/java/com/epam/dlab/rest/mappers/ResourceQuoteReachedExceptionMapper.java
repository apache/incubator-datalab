package com.epam.dlab.rest.mappers;

import com.epam.dlab.exceptions.ResourceQuoteReachedException;
import com.epam.dlab.rest.dto.ErrorDTO;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public class ResourceQuoteReachedExceptionMapper implements ExceptionMapper<ResourceQuoteReachedException> {
	@Override
	public Response toResponse(ResourceQuoteReachedException exception) {
		final Response.Status forbidden = Response.Status.FORBIDDEN;
		return Response.status(forbidden)
				.type(MediaType.APPLICATION_JSON_TYPE)
				.entity(new ErrorDTO(forbidden.getStatusCode(), exception.getMessage()))
				.build();
	}
}
