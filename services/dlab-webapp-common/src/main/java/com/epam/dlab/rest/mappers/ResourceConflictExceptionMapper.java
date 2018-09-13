/*
 * Copyright (c) 2018, EPAM SYSTEMS INC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.dlab.rest.mappers;

import com.epam.dlab.exceptions.ResourceConflictException;
import com.epam.dlab.rest.dto.ErrorDTO;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

@Provider
public class ResourceConflictExceptionMapper implements ExceptionMapper<ResourceConflictException> {
	@Override
	public Response toResponse(ResourceConflictException e) {
		final Response.Status conflict = Response.Status.CONFLICT;
		return Response.status(conflict)
				.type(MediaType.APPLICATION_JSON)
				.entity(new ErrorDTO(conflict.getStatusCode(), e.getMessage()))
				.build();
	}
}
