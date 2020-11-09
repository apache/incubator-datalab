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
import com.epam.datalab.backendapi.resources.dto.HealthStatusPageDTO;
import com.epam.datalab.backendapi.resources.dto.ProjectInfrastructureInfo;
import com.epam.datalab.backendapi.service.InfrastructureInfoService;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

/**
 * Provides the REST API for the basic information about infrastructure.
 */
@Path("/infrastructure")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class InfrastructureInfoResource {

    private final InfrastructureInfoService infrastructureInfoService;

    @Inject
    public InfrastructureInfoResource(InfrastructureInfoService infrastructureInfoService) {
        this.infrastructureInfoService = infrastructureInfoService;
    }

    /**
     * Return status of self-service.
     */
    @GET
    public Response status() {
        return Response.status(Response.Status.OK).build();
    }

    /**
     * Returns the status of infrastructure: edge.
     *
     * @param userInfo user info.
     */
    @GET
    @Path("/status")
    public HealthStatusPageDTO status(@Auth UserInfo userInfo,
                                      @QueryParam("full") @DefaultValue("0") int fullReport) {
        return infrastructureInfoService.getHeathStatus(userInfo);
    }

    /**
     * Returns the list of the provisioned user resources.
     *
     * @param userInfo user info.
     */
    @GET
    @Path("/info")
    public List<ProjectInfrastructureInfo> getUserResources(@Auth UserInfo userInfo) {
        return infrastructureInfoService.getUserResources(userInfo);
    }

    @GET
    @Path("/meta")
    public Response getVersion(@Auth UserInfo userInfo) {
        return Response.ok(infrastructureInfoService.getInfrastructureMetaInfo())
                .build();
    }
}
