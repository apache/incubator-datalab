package com.epam.dlab.rest.mappers;

import com.epam.dlab.exceptions.DlabValidationException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public class ValidationExceptionMapper implements ExceptionMapper<DlabValidationException> {
	@Override
	public Response toResponse(DlabValidationException exception) {
		return Response.status(Response.Status.BAD_REQUEST)
				.entity(exception.getMessage())
				.type(MediaType.TEXT_PLAIN_TYPE)
				.build();
	}
}
