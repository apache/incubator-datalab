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
import com.epam.dlab.backendapi.domain.CreateOdahuDTO;
import com.epam.dlab.backendapi.service.OdahuService;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import io.swagger.v3.oas.annotations.Parameter;

import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

@Path("odahu")
@Consumes(MediaType.APPLICATION_JSON)
public class OdahuResource {

    private final OdahuService odahuService;

    @Inject
    public OdahuResource(OdahuService odahuService) {
        this.odahuService = odahuService;
    }

    @POST
    public Response createOdahuCluster(@Parameter(hidden = true) @Auth UserInfo userInfo,
                                       @Parameter(hidden = true) @Context UriInfo uriInfo,
                                       @Valid CreateOdahuDTO createOdahuDTO) {
        odahuService.create(createOdahuDTO.getProject(), createOdahuDTO, userInfo);
        final URI uri = uriInfo.getRequestUriBuilder().path(createOdahuDTO.getName()).build();
        return Response.created(uri).build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getOdahuClusters(@Parameter(hidden = true) @Auth UserInfo userInfo) {
        return Response.ok(odahuService.findOdahu()).build();
    }
}
