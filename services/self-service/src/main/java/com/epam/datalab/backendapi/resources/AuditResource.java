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

package com.epam.datalab.backendapi.resources;

import com.epam.datalab.auth.UserInfo;
import com.epam.datalab.backendapi.domain.AuditCreateDTO;
import com.epam.datalab.backendapi.resources.dto.AuditFilter;
import com.epam.datalab.backendapi.resources.dto.BillingFilter;
import com.epam.datalab.backendapi.resources.dto.ExportBillingFilter;
import com.epam.datalab.backendapi.service.AuditService;
import com.epam.datalab.model.StringList;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.HttpHeaders;
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
    public Response saveAudit(@Auth UserInfo userInfo, @Valid AuditCreateDTO auditCreateDTO) {
        auditService.save(userInfo.getName(), auditCreateDTO);
        return Response.ok().build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAudit(@Auth UserInfo userInfo,
                             @QueryParam("users") StringList users,
                             @QueryParam("projects") StringList projects,
                             @QueryParam("resource-names") StringList resourceNames,
                             @QueryParam("resource-types") StringList resourceTypes,
                             @QueryParam("date-start") String dateStart,
                             @QueryParam("date-end") String dateEnd,
                             @QueryParam("page-number") int pageNumber,
                             @QueryParam("page-size") int pageSize) {
        return Response
                .ok(auditService.getAudit(users, projects, resourceNames, resourceTypes, dateStart, dateEnd, pageNumber, pageSize))
                .build();
    }

    @POST
    @Path("/report")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getAuditReport(@Auth UserInfo userInfo, AuditFilter filter) {
        return Response.ok(auditService.getAuditReport(filter)).build();
    }

    @POST
    @Path("/report/download")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadAuditReport(@Auth UserInfo userInfo, AuditFilter filter) {
        return Response.ok(auditService.downloadAuditReport(filter))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"audit-report.csv\"")
                .build();
    }
}
