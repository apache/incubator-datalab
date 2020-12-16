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
import com.epam.datalab.backendapi.service.OdahuService;
import com.epam.datalab.dto.odahu.ActionOdahuDTO;
import com.epam.datalab.dto.odahu.CreateOdahuDTO;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("infrastructure/odahu")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class OdahuResource {

    private final OdahuService odahuService;

    @Inject
    public OdahuResource(OdahuService odahuService) {
        this.odahuService = odahuService;
    }

    @POST
    public Response createProject(@Auth UserInfo userInfo, CreateOdahuDTO dto) {
        return Response.ok(odahuService.create(userInfo, dto)).build();
    }

    @Path("start")
    @POST
    public Response startProject(@Auth UserInfo userInfo, ActionOdahuDTO dto) {
        return Response.ok(odahuService.start(userInfo, dto)).build();
    }

    @Path("stop")
    @POST
    public Response stopProject(@Auth UserInfo userInfo, ActionOdahuDTO dto) {
        return Response.ok(odahuService.stop(userInfo, dto)).build();
    }

    @Path("terminate")
    @POST
    public Response terminateProject(@Auth UserInfo userInfo, ActionOdahuDTO dto) {
        return Response.ok(odahuService.terminate(userInfo, dto)).build();
    }
}
