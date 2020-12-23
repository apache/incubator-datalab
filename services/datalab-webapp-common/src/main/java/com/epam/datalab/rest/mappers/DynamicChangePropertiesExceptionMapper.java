package com.epam.datalab.rest.mappers;

import com.epam.datalab.exceptions.DynamicChangePropertiesException;
import com.epam.datalab.rest.dto.ErrorDTO;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public class DynamicChangePropertiesExceptionMapper implements ExceptionMapper<DynamicChangePropertiesException> {

    @Override
    public Response toResponse(DynamicChangePropertiesException e) {
        final Response.Status status = Response.Status.NO_CONTENT;
        return Response.status(status)
                .type(MediaType.APPLICATION_JSON)
                .entity(new ErrorDTO(status.getStatusCode(), e.getMessage()))
                .build();
    }
}
