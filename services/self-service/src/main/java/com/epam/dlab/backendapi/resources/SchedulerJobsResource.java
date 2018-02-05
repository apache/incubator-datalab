/*
 * Copyright (c) 2018, EPAM SYSTEMS INC
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


package com.epam.dlab.backendapi.resources;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.SchedulerJobsDAO;
import com.epam.dlab.dto.CreateSchedulerJobDTO;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Manages scheduler jobs for exploratory environment
 */
@Path("/infrastructure_provision/exploratory_environment/scheduler")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class SchedulerJobsResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchedulerJobsResource.class);

    @Inject
    private SchedulerJobsDAO schedulerJobsDAO;


    @POST
    public Response create(@Auth UserInfo userInfo, CreateSchedulerJobDTO dto) {
        LOGGER.info(dto.toString());
        return Response.ok().build();
    }

}

