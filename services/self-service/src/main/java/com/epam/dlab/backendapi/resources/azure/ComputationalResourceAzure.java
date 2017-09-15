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
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Provides the REST API for the computational resource on AWS.
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
}
