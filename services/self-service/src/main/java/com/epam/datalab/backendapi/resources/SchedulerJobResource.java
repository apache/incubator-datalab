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
import com.epam.datalab.backendapi.service.SchedulerJobService;
import com.epam.datalab.backendapi.validation.annotation.SchedulerJobDTOValid;
import com.epam.datalab.dto.SchedulerJobDTO;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Manages scheduler jobs for exploratory environment
 */
@Path("/infrastructure_provision/exploratory_environment/scheduler")
@Slf4j
public class SchedulerJobResource {

    private final SchedulerJobService schedulerJobService;

    @Inject
    public SchedulerJobResource(SchedulerJobService schedulerJobService) {
        this.schedulerJobService = schedulerJobService;
    }


    /**
     * Updates exploratory <code>exploratoryName<code/> for user <code>userInfo<code/> with new scheduler job data
     *
     * @param userInfo        user info
     * @param exploratoryName name of exploratory resource
     * @param dto             scheduler job data
     * @return response
     */
    @POST
    @Path("/{projectName}/{exploratoryName}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateExploratoryScheduler(@Auth UserInfo userInfo,
                                               @PathParam("projectName") String projectName,
                                               @PathParam("exploratoryName") String exploratoryName,
                                               @SchedulerJobDTOValid SchedulerJobDTO dto) {
        schedulerJobService.updateExploratorySchedulerData(userInfo, projectName, exploratoryName, dto);
        return Response.ok().build();
    }

    /**
     * Removes exploratory <code>exploratoryName<code/> for user <code>userInfo<code/>
     *
     * @param userInfo        user info
     * @param exploratoryName name of exploratory resource
     * @return response
     */
    @DELETE
    @Path("/{exploratoryName}")
    public Response removeExploratoryScheduler(@Auth UserInfo userInfo,
                                               @PathParam("exploratoryName") String exploratoryName) {
        log.debug("User {} is trying to remove scheduler for exploratory {}", userInfo.getName(), exploratoryName);
        schedulerJobService.removeScheduler(userInfo.getName(), exploratoryName);
        return Response.ok().build();
    }

    /**
     * Updates computational resource <code>computationalName<code/> affiliated with exploratory
     * <code>exploratoryName<code/> for user <code>userInfo<code/> with new scheduler job data
     *
     * @param userInfo          user info
     * @param exploratoryName   name of exploratory resource
     * @param computationalName name of computational resource
     * @param dto               scheduler job data
     * @return response
     */
    @POST
    @Path("/{projectName}/{exploratoryName}/{computationalName}")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response updateComputationalScheduler(@Auth UserInfo userInfo,
                                                 @PathParam("projectName") String projectName,
                                                 @PathParam("exploratoryName") String exploratoryName,
                                                 @PathParam("computationalName") String computationalName,
                                                 @SchedulerJobDTOValid SchedulerJobDTO dto) {
        schedulerJobService.updateComputationalSchedulerData(userInfo, projectName, exploratoryName, computationalName, dto);
        return Response.ok().build();
    }

    /**
     * Updates computational resource <code>computationalName<code/> affiliated with exploratory
     * <code>exploratoryName<code/> for user <code>userInfo<code/> with new scheduler job data
     *
     * @param userInfo          user info
     * @param exploratoryName   name of exploratory resource
     * @param computationalName name of computational resource
     * @return response
     */
    @DELETE
    @Path("/{exploratoryName}/{computationalName}")
    public Response removeComputationalScheduler(@Auth UserInfo userInfo,
                                                 @PathParam("exploratoryName") String exploratoryName,
                                                 @PathParam("computationalName") String computationalName) {
        log.debug("User {} is trying to remove scheduler for computational {} connected with exploratory {}",
                userInfo.getName(), computationalName, exploratoryName);
        schedulerJobService.removeScheduler(userInfo.getName(), exploratoryName, computationalName);
        return Response.ok().build();
    }


    /**
     * Returns scheduler job for exploratory resource <code>exploratoryName<code/>
     *
     * @param userInfo        user info
     * @param exploratoryName name of exploratory resource
     * @return scheduler job data
     */
    @GET
    @Path("/{projectName}/{exploratoryName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response fetchSchedulerJobForUserAndExploratory(@Auth UserInfo userInfo,
                                                           @PathParam("projectName") String projectName,
                                                           @PathParam("exploratoryName") String exploratoryName) {
        log.debug("Loading scheduler job for user {} and exploratory {}...", userInfo.getName(), exploratoryName);
        final SchedulerJobDTO schedulerJob =
                schedulerJobService.fetchSchedulerJobForUserAndExploratory(userInfo.getName(), projectName, exploratoryName);
        return Response.ok(schedulerJob).build();
    }

    /**
     * Returns scheduler job for computational resource <code>computationalName<code/> affiliated with
     * exploratory <code>exploratoryName<code/>
     *
     * @param userInfo          user info
     * @param exploratoryName   name of exploratory resource
     * @param computationalName name of computational resource
     * @return scheduler job data
     */
    @GET
    @Path("/{projectName}/{exploratoryName}/{computationalName}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response fetchSchedulerJobForComputationalResource(@Auth UserInfo userInfo,
                                                              @PathParam("exploratoryName") String exploratoryName,
                                                              @PathParam("projectName") String projectName,
                                                              @PathParam("computationalName") String computationalName) {
        log.debug("Loading scheduler job for user {}, exploratory {} and computational resource {}...",
                userInfo.getName(), exploratoryName, computationalName);
        final SchedulerJobDTO schedulerJob = schedulerJobService
                .fetchSchedulerJobForComputationalResource(userInfo.getName(), projectName, exploratoryName, computationalName);
        return Response.ok(schedulerJob).build();
    }

    @GET
    @Path("active")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getActiveSchedulers(@Auth UserInfo userInfo,
                                        @QueryParam("minuteOffset") long minuteOffset) {
        log.trace("Getting active schedulers for user {} and offset {}", userInfo.getName(), minuteOffset);
        return Response.ok(schedulerJobService.getActiveSchedulers(userInfo.getName(), minuteOffset)).build();
    }

}

