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
import com.epam.datalab.backendapi.resources.dto.ConnectedPlatformAddFrom;
import com.epam.datalab.backendapi.resources.dto.ConnectedPlatformType;
import com.epam.datalab.backendapi.service.ConnectedPlatformsService;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("connected_platforms")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class ConnectedPlatformResource {

    private final ConnectedPlatformsService connectedPlatformsService;

    @Inject
    public ConnectedPlatformResource(ConnectedPlatformsService connectedPlatformsService) {
        this.connectedPlatformsService = connectedPlatformsService;
    }

    @RolesAllowed("/api/connected_platforms/view")
    @GET
    @Path("/user")
    public Response getConnectedPlatforms(@Auth UserInfo ui) {
        return Response.ok(connectedPlatformsService.getUserPlatforms(ui.getName())).build();
    }

    @RolesAllowed("/api/connected_platforms/add")
    @POST
    public Response addConnectedPlatform(@Auth UserInfo ui, @Valid ConnectedPlatformAddFrom from) {
        connectedPlatformsService.addPlatform(ui, from.getName(), from.getType(), from.getUrl());
        return Response.ok().build();
    }

    @RolesAllowed("/api/connected_platforms/disconnect")
    @DELETE
    @Path("{name}")
    public Response disconnectPlatform(@Auth UserInfo ui, @PathParam("name") String platformName) {
        connectedPlatformsService.disconnect(ui, platformName);
        return Response.ok().build();
    }

}
