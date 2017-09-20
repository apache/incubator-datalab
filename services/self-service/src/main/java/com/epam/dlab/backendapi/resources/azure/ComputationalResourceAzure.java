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

import com.epam.dlab.UserInstanceStatus;
import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.SelfServiceApplicationConfiguration;
import com.epam.dlab.backendapi.core.UserInstanceDTO;
import com.epam.dlab.backendapi.dao.ComputationalDAO;
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.resources.dto.SparkStandaloneConfiguration;
import com.epam.dlab.backendapi.resources.dto.azure.AzureComputationalCreateForm;
import com.epam.dlab.backendapi.resources.dto.azure.AzureComputationalResource;
import com.epam.dlab.backendapi.roles.RoleType;
import com.epam.dlab.backendapi.roles.UserRoles;
import com.epam.dlab.backendapi.util.RequestBuilder;
import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.dto.azure.computational.ComputationalCreateAzure;
import com.epam.dlab.dto.base.DataEngineType;
import com.epam.dlab.dto.computational.ComputationalStatusDTO;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.client.RESTService;
import com.epam.dlab.rest.contracts.ComputationalAPI;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static com.epam.dlab.UserInstanceStatus.CREATING;
import static com.epam.dlab.UserInstanceStatus.FAILED;

/**
 * Provides the REST API for the computational resource on Azure.
 */
@Path("/infrastructure_provision/computational_resources")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class ComputationalResourceAzure {

    @Inject
    private ExploratoryDAO exploratoryDAO;

    @Inject
    private ComputationalDAO computationalDAO;

    @Inject
    @Named(ServiceConsts.PROVISIONING_SERVICE_NAME)
    private RESTService provisioningService;

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
     * @param form     user info about creation of the computational resource.
     * @return 200 OK - if request success, 302 Found - for duplicates, otherwise throws exception.
     * @throws IllegalArgumentException if input is not valid or exceeds configuration limits
     */
    @PUT
    public Response create(@Auth UserInfo userInfo, @Valid @NotNull AzureComputationalCreateForm form) {
        log.debug("Create computational resources for {} | form is {}", userInfo.getName(), form);

        if (!UserRoles.checkAccess(userInfo, RoleType.COMPUTATIONAL, form.getImage())) {
            log.warn("Unauthorized attempt to create a {} by user {}", form.getImage(), userInfo.getName());
            throw new DlabException("You do not have the privileges to create a " + form.getTemplateName());
        }

        validateForm(form);

        if (computationalDAO.addComputational(userInfo.getName(), form.getNotebookName(),
                createInitialComputationalResource(form))) {

            try {
                UserInstanceDTO instance = exploratoryDAO.fetchExploratoryFields(userInfo.getName(), form.getNotebookName());

                ComputationalCreateAzure dto = RequestBuilder.newComputationalCreate(userInfo, instance, form);

                String uuid = provisioningService.post(ComputationalAPI.COMPUTATIONAL_CREATE, userInfo.getAccessToken(), dto, String.class);
                RequestId.put(userInfo.getName(), uuid);
                return Response.ok().build();
            } catch (RuntimeException e) {
                try {
                    updateComputationalStatus(userInfo.getName(), form.getNotebookName(), form.getName(), FAILED);
                } catch (DlabException d) {
                    log.error("Could not update the status of computational resource {} for user {}",
                            form.getName(), userInfo.getName(), d);
                }
                throw e;
            }
        } else {
            log.debug("Computational with name {} is already existing for user {}", form.getName(), userInfo.getName());
            return Response.status(Response.Status.FOUND).build();

        }
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

    /**
     * Validates if input form is correct
     *
     * @param form user input form
     * @throws IllegalArgumentException if user typed wrong arguments
     */

    private void validateForm(AzureComputationalCreateForm form) {

        int instanceCount = Integer.parseInt(form.getDataEngineInstanceCount());

        if (instanceCount < configuration.getMinSparkInstanceCount()
                || instanceCount > configuration.getMaxSparkInstanceCount()) {
            throw new IllegalArgumentException(String.format("Instance count should be in range [%d..%d]",
                    configuration.getMinSparkInstanceCount(), configuration.getMaxSparkInstanceCount()));
        }

        if (DataEngineType.fromString(form.getImage()) != DataEngineType.SPARK_STANDALONE) {
            throw new IllegalArgumentException(String.format("Unknown data engine %s", form.getImage()));
        }
    }

    private AzureComputationalResource createInitialComputationalResource(AzureComputationalCreateForm form) {

        return AzureComputationalResource.builder()
                .computationalName(form.getName())
                .imageName(form.getImage())
                .templateName(form.getTemplateName())
                .status(CREATING.toString())
                .dataEngineInstanceCount(form.getDataEngineInstanceCount())
                .dataEngineMasterSize(form.getDataEngineMasterSize())
                .dataEngineSlaveSize(form.getDataEngineSlaveSize())
                .build();
    }

    /**
     * Updates the status of computational resource in database.
     *
     * @param user              user name.
     * @param exploratoryName   name of exploratory.
     * @param computationalName name of computational resource.
     * @param status            status
     */
    private void updateComputationalStatus(String user, String exploratoryName, String computationalName, UserInstanceStatus status) {
        ComputationalStatusDTO computationalStatus = new ComputationalStatusDTO()
                .withUser(user)
                .withExploratoryName(exploratoryName)
                .withComputationalName(computationalName)
                .withStatus(status);

        computationalDAO.updateComputationalStatus(computationalStatus);
    }
}
