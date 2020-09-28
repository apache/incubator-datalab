/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.epam.datalab.rest.mappers;

import com.epam.datalab.rest.dto.ErrorDTO;
import io.dropwizard.jersey.validation.ConstraintMessage;
import io.dropwizard.jersey.validation.JerseyViolationException;
import org.glassfish.jersey.server.model.Invocable;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import java.util.stream.Collectors;

public class ValidationExceptionMapper implements ExceptionMapper<JerseyViolationException> {
    @Override
    public Response toResponse(JerseyViolationException exception) {
        Invocable invocable = exception.getInvocable();
        final String errors =
                exception.getConstraintViolations()
                        .stream().map(violation -> ConstraintMessage.getMessage(violation, invocable))
                        .collect(Collectors.joining(","));
        return Response.status(Response.Status.BAD_REQUEST)
                .entity(new ErrorDTO(Response.Status.BAD_REQUEST.getStatusCode(), errors))
                .type(MediaType.APPLICATION_JSON)
                .build();
    }
}
