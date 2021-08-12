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
import com.epam.datalab.backendapi.service.CheckInactivityService;
import com.epam.datalab.dto.computational.ComputationalCheckInactivityDTO;
import com.epam.datalab.dto.exploratory.ExploratoryCheckInactivityAction;
import com.epam.datalab.rest.contracts.InfrasctructureAPI;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path(InfrasctructureAPI.INFRASTRUCTURE)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class InfrastructureResource {

    @Inject
    private CheckInactivityService checkInactivityService;

    /**
     * Return status of provisioning service.
     */
    @GET
    public Response status(@Auth UserInfo ui) {
        return Response.status(Response.Status.OK).build();
    }

    @POST
    @Path("/exploratory/check_inactivity")
    public String checkExploratoryInactivity(@Auth UserInfo ui, ExploratoryCheckInactivityAction dto) {
        return checkInactivityService.checkExploratoryInactivity(ui.getName(), dto);
    }

    @POST
    @Path("/computational/check_inactivity")
    public String checkComputationalInactivity(@Auth UserInfo ui, ComputationalCheckInactivityDTO dto) {
        return checkInactivityService.checkComputationalInactivity(ui.getName(), dto);
    }
}
