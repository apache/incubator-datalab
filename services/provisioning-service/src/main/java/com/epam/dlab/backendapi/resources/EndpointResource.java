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
import com.epam.dlab.backendapi.ProvisioningServiceApplicationConfiguration;
import com.epam.dlab.backendapi.service.EndpointService;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("infrastructure/endpoint")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class EndpointResource {
    private final EndpointService endpointService;
    private final ProvisioningServiceApplicationConfiguration configuration;

    @Inject
    public EndpointResource(EndpointService endpointService, ProvisioningServiceApplicationConfiguration configuration) {
        this.endpointService = endpointService;
        this.configuration = configuration;
    }

    @GET
    @Path("/healthcheck")
    public Response status(@Auth UserInfo userInfo) {
        return Response.ok().build();
    }

    @POST
    public Response connectEndpoint(@Auth UserInfo userInfo, @Context HttpServletRequest request, String name) {
        endpointService.create(name, request.getScheme() + "://" + request.getRemoteHost());
        return Response.ok(configuration.getCloudProvider()).build();
    }
}
