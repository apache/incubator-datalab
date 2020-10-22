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
import com.epam.datalab.backendapi.resources.dto.BillingFilter;
import com.epam.datalab.backendapi.resources.dto.ExportBillingFilter;
import com.epam.datalab.backendapi.service.BillingService;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/billing")
@Consumes(MediaType.APPLICATION_JSON)
public class BillingResource {

    private final BillingService billingService;

    @Inject
    public BillingResource(BillingService billingService) {
        this.billingService = billingService;
    }

    @GET
    @Path("/quota")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getQuota(@Auth UserInfo userInfo) {
        return Response.ok(billingService.getQuotas(userInfo)).build();
    }

    @POST
    @Path("/report")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getBillingReport(@Auth UserInfo userInfo, @Valid @NotNull BillingFilter filter) {
        return Response.ok(billingService.getBillingReport(userInfo, filter)).build();
    }

    @POST
    @Path("/report/download")
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadBillingReport(@Auth UserInfo userInfo, @Valid @NotNull ExportBillingFilter filter) {
        return Response.ok(billingService.downloadReport(userInfo, filter, filter.getLocale()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"billing-report.csv\"")
                .build();
    }
}
