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
import com.epam.dlab.backendapi.SelfServiceApplicationConfiguration;
import com.epam.dlab.backendapi.resources.dto.SparkStandaloneConfiguration;
import com.epam.dlab.backendapi.resources.dto.azure.AzureComputationalCreateForm;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Provides the REST API for the computational resource on Azure.
 */
@Path("/infrastructure_provision/computational_resources")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class ComputationalResourceAzure {

    @Inject
    private SelfServiceApplicationConfiguration configuration;


    /**
     * Returns the limits for creation the computational resources.
     *
     * @param userInfo user info.
     */
    @GET
    @Path("/configuration")
    public SparkStandaloneConfiguration getConfiguration(@Auth UserInfo userInfo) {

        SparkStandaloneConfiguration sparkStandaloneConfiguration = SparkStandaloneConfiguration.builder()
                .maxSparkInstanceCount(configuration.getMaxSparkInstanceCount())
                .minSparkInstanceCount(configuration.getMinSparkInstanceCount())
                .build();


        log.debug("Returns limits for user {}: {}", userInfo.getName(), sparkStandaloneConfiguration);
        return sparkStandaloneConfiguration;
    }

    /**
     * Sends request to provisioning service for creation the computational resource for user.
     *
     * @param userInfo user info.
     * @param form     DTO info about creation of the computational resource.
     * @return 200 OK - if request success, 302 Found - for duplicates, otherwise throws exception.
     */
    @PUT
    public Response create(@Auth UserInfo userInfo, @Valid @NotNull AzureComputationalCreateForm form) {
        throw new UnsupportedOperationException("Operation is not implemented yet");
    }

    /**
     * Sends request to provisioning service for termination the computational resource for user.
     *
     * @param userInfo          user info.
     * @param exploratoryName   name of exploratory.
     * @param computationalName name of computational resource.
     * @return 200 OK - if request success, otherwise fails.
     */
    @DELETE
    @Path("/{exploratoryName}/{computationalName}/terminate")
    public Response terminate(@Auth UserInfo userInfo,
                              @PathParam("exploratoryName") String exploratoryName,
                              @PathParam("computationalName") String computationalName) {

        throw new UnsupportedOperationException("Operation is not implemented yet");
    }


}
