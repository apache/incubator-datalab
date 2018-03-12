package com.epam.dlab.rest.mappers;

import com.epam.dlab.exceptions.ResourceConflictException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class ResourceConflictExceptionMapper implements ExceptionMapper<ResourceConflictException> {
    @Override
	public Response toResponse(ResourceConflictException e) {
        return Response.status(Response.Status.CONFLICT)
                .type(MediaType.TEXT_PLAIN_TYPE)
                .entity(e.getMessage())
                .build();
    }
}
