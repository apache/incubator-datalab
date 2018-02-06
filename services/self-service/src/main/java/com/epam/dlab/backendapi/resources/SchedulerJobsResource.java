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
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.service.SchedulerJobsService;
import com.epam.dlab.dto.SchedulerJobDTO;
import com.epam.dlab.exceptions.DlabException;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
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
    private ExploratoryDAO exploratoryDAO;

    @Inject
    private SchedulerJobsService schedulerJobsService;


    @POST
    @Path("/{exploratoryName}")
    public Response create(@Auth UserInfo userInfo, @PathParam("exploratoryName") String exploratoryName,
                           SchedulerJobDTO dto) {
        LOGGER.info("Adding {} to Mongo DB ...", dto);
        exploratoryDAO.updateSchedulerDataForUserAndExploratory(userInfo.getName(), exploratoryName, dto);
        return Response.ok().build();
    }

    /**
     * Returns scheduler job for dlab resource <code>exploratoryName<code/>
     *
     * @param userInfo          user info
     * @param exploratoryName   name of exploratory resource
     * @return scheduler job data
     */
    @GET
    @Path("/{exploratoryName}")
    public SchedulerJobDTO fetchSchedulerJobForUserAndExploratory(@Auth UserInfo userInfo,
                                                             @PathParam("exploratoryName") String exploratoryName) {

        LOGGER.debug("Loading scheduler job for user {} and exploratory {}",
                userInfo.getName(), exploratoryName);
        try {
            SchedulerJobDTO job = schedulerJobsService.fetchSchedulerJobForUserAndExploratory(userInfo.getName(), exploratoryName);
            LOGGER.info("Scheduler job data: {}", job);
            return job;

        } catch (Exception t) {
            LOGGER.error("Cannot load scheduler job for user {} and exploratory {} an", userInfo.getName(), exploratoryName, t);
            throw new DlabException("Cannot load scheduler job: " + t.getLocalizedMessage(), t);
        }
    }

}

