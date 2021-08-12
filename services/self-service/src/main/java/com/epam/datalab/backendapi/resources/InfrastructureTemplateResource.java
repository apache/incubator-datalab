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
import com.epam.datalab.backendapi.service.InfrastructureTemplateService;
import com.epam.datalab.dto.base.computational.FullComputationalTemplate;
import com.epam.datalab.dto.imagemetadata.ExploratoryMetadataDTO;
import com.epam.datalab.rest.contracts.DockerAPI;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Provides the REST API to retrieve exploratory/computational templates.
 */
@Path("/infrastructure_templates")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class InfrastructureTemplateResource implements DockerAPI {

    private final InfrastructureTemplateService infrastructureTemplateService;

    @Inject
    public InfrastructureTemplateResource(InfrastructureTemplateService infrastructureTemplateService) {
        this.infrastructureTemplateService = infrastructureTemplateService;
    }

    /**
     * Returns the list of the computational resources templates for user.
     *
     * @param userInfo user info.
     */
    @GET
    @Path("/{project}/{endpoint}/computational_templates")
    public Iterable<FullComputationalTemplate> getComputationalTemplates(@Auth UserInfo userInfo,
                                                                         @PathParam("project") String project,
                                                                         @PathParam("endpoint") String endpoint) {
        return infrastructureTemplateService.getComputationalTemplates(userInfo, project, endpoint);
    }

    /**
     * Returns the list of the exploratory environment templates for user.
     *
     * @param userInfo user info.
     */
    @GET
    @Path("/{project}/{endpoint}/exploratory_templates")
    public Iterable<ExploratoryMetadataDTO> getExploratoryTemplates(@Auth UserInfo userInfo,
                                                                    @PathParam("project") String project,
                                                                    @PathParam("endpoint") String endpoint) {
        return infrastructureTemplateService.getExploratoryTemplates(userInfo, project, endpoint);
    }
}

