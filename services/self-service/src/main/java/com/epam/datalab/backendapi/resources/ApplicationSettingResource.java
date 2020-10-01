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
import com.epam.datalab.backendapi.service.ApplicationSettingService;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.security.RolesAllowed;
import javax.validation.constraints.Min;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Slf4j
@Path("/settings")
@RolesAllowed("/api/settings")
public class ApplicationSettingResource {


    private final ApplicationSettingService settingService;

    @Inject
    public ApplicationSettingResource(ApplicationSettingService settingService) {
        this.settingService = settingService;
    }

    @PUT
    @Path("budget/{maxBudgetAllowed}")
    public Response setMaxBudget(@Auth UserInfo userInfo,
                                 @PathParam("maxBudgetAllowed") @Min(1) Long maxBudget) {
        settingService.setMaxBudget(maxBudget);
        return Response.noContent().build();
    }

    @DELETE
    @Path("budget")
    public Response removeAllowedBudget(@Auth UserInfo userInfo) {
        log.debug("User {} is removing max budget application setting", userInfo.getName());
        settingService.removeMaxBudget();
        return Response.noContent().build();
    }

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSettings(@Auth UserInfo userInfo) {
        return Response.ok(settingService.getSettings()).build();

    }
}
