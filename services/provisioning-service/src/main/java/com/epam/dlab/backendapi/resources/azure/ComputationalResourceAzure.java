/*
 * Copyright (c) 2017, EPAM SYSTEMS INC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.dlab.backendapi.resources.azure;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.service.SparkClusterService;
import com.epam.dlab.dto.SparkComputationalCreate;
import com.epam.dlab.dto.computational.ComputationalTerminateDTO;
import com.epam.dlab.rest.contracts.ComputationalAPI;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/computational")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class ComputationalResourceAzure {

    @Inject
    private SparkClusterService sparkClusterService;

    @POST
    @Path(ComputationalAPI.COMPUTATIONAL_CREATE_SPARK)
    public String create(@Auth UserInfo ui, SparkComputationalCreate dto) {
        log.debug("Create computational Spark resources {} for user {}: {}", dto.getComputationalName(), ui.getName(), dto);

        return sparkClusterService.create(ui, dto);
    }


    @POST
    @Path(ComputationalAPI.COMPUTATIONAL_TERMINATE_SPARK)
    public String terminate(@Auth UserInfo ui, ComputationalTerminateDTO dto) {
        log.debug("Terminate computational Spark resources {} for user {}: {}", dto.getComputationalName(), ui.getName(), dto);

        return sparkClusterService.terminate(ui, dto);
    }
}
