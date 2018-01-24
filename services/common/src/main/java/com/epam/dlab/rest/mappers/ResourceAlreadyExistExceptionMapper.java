package com.epam.dlab.rest.mappers;

import com.epam.dlab.exceptions.ResourceAlreadyExistException;

import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class ResourceAlreadyExistExceptionMapper implements ExceptionMapper<ResourceAlreadyExistException> {
    @Override
    public Response toResponse(ResourceAlreadyExistException e) {
        return Response.status(Response.Status.CONFLICT).entity(e.getMessage()).build();
    }
}
