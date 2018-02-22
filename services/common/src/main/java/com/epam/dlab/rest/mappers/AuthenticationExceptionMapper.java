package com.epam.dlab.rest.mappers;

import com.epam.dlab.exceptions.DlabAuthenticationException;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

public class AuthenticationExceptionMapper implements ExceptionMapper<DlabAuthenticationException> {
	@Override
	public Response toResponse(DlabAuthenticationException exception) {
		return Response.status(Response.Status.UNAUTHORIZED).entity(exception.getMessage()).type(MediaType
				.TEXT_PLAIN_TYPE).build();
	}
}
