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
import com.epam.dlab.backendapi.dao.SchedulerJobsDAO;
import com.epam.dlab.backendapi.service.SchedulerJobsService;
import com.epam.dlab.dto.SchedulerJobDTO;
import com.epam.dlab.exceptions.DlabException;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.hibernate.validator.constraints.NotBlank;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

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
    private SchedulerJobsDAO schedulerJobsDAO;

    @Inject
    private SchedulerJobsService schedulerJobsService;


    @POST
    @Path("/{exploratoryName}")
    public Response create(@Auth UserInfo userInfo, @PathParam("exploratoryName") String exploratoryName,
                           SchedulerJobDTO dto) {
        LOGGER.info("Adding {} to Mongo DB ...", dto);
        exploratoryDAO.updateSchedulerData(userInfo.getName(), exploratoryName, dto);
        return Response.ok().build();
    }

    /**
     * Returns document of single scheduler job for dlab resource <code>exploratoryName<code/>
     *
     * @param userInfo          user info
     * @param exploratoryName   name of exploratory resource
     * @return document of single scheduler job
     */
    @GET
    @Path("/{exploratoryName}/documents")
    public Document getSchedulerJobForExploratory(@Auth UserInfo userInfo,
                                     @PathParam("exploratoryName") String exploratoryName) {

        LOGGER.debug("Loading document of single scheduler job for user {} and exploratory {}",
                userInfo.getName(), exploratoryName);
        try {
            Document doc = schedulerJobsService.getSingleSchedulerJobForExploratory(userInfo.getName(), exploratoryName);
            LOGGER.info("Document of job: {}", doc);
            return doc;

        } catch (Exception t) {
            LOGGER.error("Cannot load single scheduler job for user {} and exploratory {} an", userInfo.getName(), exploratoryName, t);
            throw new DlabException("Cannot load single scheduler job: " + t.getLocalizedMessage(), t);
        }
    }

    /**
     * Returns DTO list of scheduler jobs for dlab resource <code>exploratoryName<code/>
     *
     * @param userInfo          user info
     * @param exploratoryName   name of exploratory resource
     * @return list of scheduler jobs
     */
    @GET
    @Path("/{exploratoryName}/dtolist")
    public List<SchedulerJobDTO> fetchSchedulerJobsListForExploratory(@Auth UserInfo userInfo,
                                                             @PathParam("exploratoryName") String exploratoryName) {

        LOGGER.debug("Loading list of scheduler jobs for user {} and exploratory {}",
                userInfo.getName(), exploratoryName);
        try {
            List<SchedulerJobDTO> listOfJobs = schedulerJobsService.fetchSchedulerJobsForExploratory(userInfo.getName(), exploratoryName);
            LOGGER.info("List of jobs: {}", listOfJobs);
            return listOfJobs;

        } catch (Exception t) {
            LOGGER.error("Cannot load scheduler jobs for user {} and exploratory {} an", userInfo.getName(), exploratoryName, t);
            throw new DlabException("Cannot load scheduler jobs: " + t.getLocalizedMessage(), t);
        }
    }

}

