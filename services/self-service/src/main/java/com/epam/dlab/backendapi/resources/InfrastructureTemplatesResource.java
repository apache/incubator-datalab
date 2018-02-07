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

package com.epam.dlab.backendapi.resources;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.SelfServiceApplicationConfiguration;
import com.epam.dlab.backendapi.dao.SettingsDAO;
import com.epam.dlab.backendapi.service.InfrastructureTemplatesService;
import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.dto.base.computational.FullComputationalTemplate;
import com.epam.dlab.dto.imagemetadata.ExploratoryMetadataDTO;
import com.epam.dlab.rest.client.RESTService;
import com.epam.dlab.rest.contracts.DockerAPI;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.dropwizard.auth.Auth;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

/**
 * Provides the REST API to retrieve exploratory/computational templates.
 */
@Path("/infrastructure_templates")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class InfrastructureTemplatesResource implements DockerAPI {

    @Inject
    private SelfServiceApplicationConfiguration configuration;

    @Inject
    private SettingsDAO settingsDAO;

    @Inject
    private InfrastructureTemplatesService infrastructureTemplatesService;


    @Inject
    @Named(ServiceConsts.PROVISIONING_SERVICE_NAME)
    private RESTService provisioningService;

    /**
     * Returns the list of the computational resources templates for user.
     *
     * @param userInfo user info.
     */
    @GET
    @Path("/computational_templates")
    public Iterable<FullComputationalTemplate> getComputationalTemplates(@Auth UserInfo userInfo) {
        return infrastructureTemplatesService.getComputationalTemplates(userInfo);
    }

    /**
     * Returns the list of the exploratory environment templates for user.
     *
     * @param userInfo user info.
     */
    @GET
    @Path("/exploratory_templates")
    public Iterable<ExploratoryMetadataDTO> getExploratoryTemplates(@Auth UserInfo userInfo) {
        return infrastructureTemplatesService.getExploratoryTemplates(userInfo);
    }
}

