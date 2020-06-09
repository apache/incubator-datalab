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

package com.epam.dlab.backendapi.resources;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.domain.AuditCreateDTO;
import com.epam.dlab.backendapi.service.AuditService;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/audit")
public class AuditResource {
    private final AuditService auditService;

    @Inject
    public AuditResource(AuditService auditService) {
        this.auditService = auditService;
    }

    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    public Response createEndpoint(@Auth UserInfo userInfo, @Valid AuditCreateDTO auditCreateDTO) {
        auditService.save(userInfo.getName(), auditCreateDTO);
        return Response.ok().build();
    }
}
