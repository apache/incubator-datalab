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
import com.epam.dlab.backendapi.roles.RoleType;
import com.epam.dlab.backendapi.roles.UserRoles;
import com.epam.dlab.cloud.CloudProvider;
import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.dto.imagemetadata.ComputationalMetadataDTO;
import com.epam.dlab.dto.imagemetadata.ExploratoryMetadataDTO;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.client.RESTService;
import com.epam.dlab.rest.contracts.DockerAPI;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Provides the REST API to retrieve exploratory/computational templates.
 */
@Path("/infrastructure_templates")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class InfrastructureTemplatesResource implements DockerAPI {


    @Inject
    @Named(ServiceConsts.PROVISIONING_SERVICE_NAME)
    private RESTService provisioningService;

    @Inject
    private SettingsDAO settingsDAO;

    @Inject
    private SelfServiceApplicationConfiguration configuration;

    /**
     * Returns the list of the computational resources templates for user.
     *
     * @param userInfo user info.
     */
    @GET
    @Path("/computational_templates")
    public Iterable<ComputationalMetadataDTO> getComputationalTemplates(@Auth UserInfo userInfo) {
        log.debug("Loading list of computational templates for user {}", userInfo.getName());
        try {
            ComputationalMetadataDTO[] array = provisioningService.get(DOCKER_COMPUTATIONAL, userInfo.getAccessToken(), ComputationalMetadataDTO[].class);
            List<ComputationalMetadataDTO> list = new ArrayList<>();
            for (int i = 0; i < array.length; i++) {
                array[i].setImage(getSimpleImageName(array[i].getImage()));
                if (UserRoles.checkAccess(userInfo, RoleType.COMPUTATIONAL, array[i].getImage())) {
                    list.add(array[i]);
                }
            }
            return list;
        } catch (DlabException e) {
            log.error("Could not load list of computational templates for user: {}", userInfo.getName(), e);
            throw e;
        }
    }

    /**
     * Returns the list of the exploratory environment templates for user.
     *
     * @param userInfo user info.
     */
    @GET
    @Path("/exploratory_templates")
    public Iterable<ExploratoryMetadataDTO> getExploratoryTemplates(@Auth UserInfo userInfo) {
        log.debug("Loading list of exploratory templates for user {}", userInfo.getName());
        try {
            ExploratoryMetadataDTO[] array = provisioningService.get(DOCKER_EXPLORATORY, userInfo.getAccessToken(), ExploratoryMetadataDTO[].class);
            List<ExploratoryMetadataDTO> list = new ArrayList<>();
            for (int i = 0; i < array.length; i++) {
                array[i].setImage(getSimpleImageName(array[i].getImage()));
                if (UserRoles.checkAccess(userInfo, RoleType.EXPLORATORY, array[i].getImage())) {
                    list.add(array[i]);
                }
            }

            if (settingsDAO.getConfOsFamily().equals("redhat") && configuration.getCloudProvider() == CloudProvider.AZURE) {
                return list.stream().filter(e -> !e.getImage().endsWith("deeplearning")
                        && !e.getImage().endsWith("tensor")).collect(Collectors.toList());
            }

            return list;
        } catch (DlabException e) {
            log.error("Could not load list of exploratory templates for user: {}", userInfo.getName(), e);
            throw e;
        }
    }

    /**
     * Return the image name without suffix version.
     *
     * @param imageName the name of image.
     */
    private String getSimpleImageName(String imageName) {
        int separatorIndex = imageName.indexOf(":");
        return (separatorIndex > 0 ? imageName.substring(0, separatorIndex) : imageName);
    }
}

