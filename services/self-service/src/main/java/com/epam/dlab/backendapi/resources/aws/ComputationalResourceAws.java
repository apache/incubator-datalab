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


package com.epam.dlab.backendapi.resources.aws;

import com.epam.dlab.UserInstanceStatus;
import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.SelfServiceApplicationConfiguration;
import com.epam.dlab.dto.UserInstanceDTO;
import com.epam.dlab.backendapi.dao.ComputationalDAO;
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.dao.SettingsDAO;
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.resources.dto.SparkStandaloneClusterCreateForm;
import com.epam.dlab.backendapi.resources.dto.aws.AwsComputationalCreateForm;
import com.epam.dlab.dto.aws.computational.AwsComputationalResource;
import com.epam.dlab.backendapi.roles.RoleType;
import com.epam.dlab.backendapi.roles.UserRoles;
import com.epam.dlab.backendapi.service.ComputationalService;
import com.epam.dlab.backendapi.util.RequestBuilder;
import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.dto.aws.computational.ComputationalCreateAws;
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
 * Provides the REST API for the computational resource on AWS.
 */
@Path("/infrastructure_provision/computational_resources")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class ComputationalResourceAws implements ComputationalAPI {

    @Inject
    private SettingsDAO settingsDAO;
    @Inject
    private ExploratoryDAO exploratoryDAO;
    @Inject
    private ComputationalDAO computationalDAO;
    @Inject
    @Named(ServiceConsts.PROVISIONING_SERVICE_NAME)
    private RESTService provisioningService;
    @Inject
    private SelfServiceApplicationConfiguration configuration;
    @Inject
    private ComputationalService computationalService;

    /**
     * Sends request to provisioning service for creation the computational resource for user.
     *
     * @param userInfo user info.
     * @param formDTO  DTO info about creation of the computational resource.
     * @return 200 OK - if request success, 302 Found - for duplicates, otherwise throws exception.
     */
    @PUT
    @Deprecated
    public Response create(@Auth UserInfo userInfo, @Valid @NotNull AwsComputationalCreateForm formDTO) {
        log.debug("Send request for creation the computational resource {} for user {}", formDTO.getName(), userInfo.getName());
        if (!UserRoles.checkAccess(userInfo, RoleType.COMPUTATIONAL, formDTO.getImage())) {
            log.warn("Unauthorized attempt to create a {} by user {}", formDTO.getImage(), userInfo.getName());
            throw new DlabException("You do not have the privileges to create a " + formDTO.getTemplateName());
        }

        int slaveInstanceCount = Integer.parseInt(formDTO.getInstanceCount());
        if (slaveInstanceCount < configuration.getMinEmrInstanceCount() || slaveInstanceCount > configuration.getMaxEmrInstanceCount()) {
            log.debug("Creating computational resource {} for user {} fail: Limit exceeded to creation slave instances. Minimum is {}, maximum is {}",
                    formDTO.getName(), userInfo.getName(), configuration.getMinEmrInstanceCount(), configuration.getMaxEmrInstanceCount());
            throw new DlabException("Limit exceeded to creation slave instances. Minimum is " + configuration.getMinEmrInstanceCount() +
                    ", maximum is " + configuration.getMaxEmrInstanceCount() + ".");
        }

        int slaveSpotInstanceBidPct = formDTO.getSlaveInstanceSpotPctPrice();
        if (formDTO.getSlaveInstanceSpot() && (slaveSpotInstanceBidPct < configuration.getMinEmrSpotInstanceBidPct() || slaveSpotInstanceBidPct > configuration.getMaxEmrSpotInstanceBidPct())) {
            log.debug("Creating computational resource {} for user {} fail: Spot instances bidding percentage value out of the boundaries. Minimum is {}, maximum is {}",
                    formDTO.getName(), userInfo.getName(), configuration.getMinEmrSpotInstanceBidPct(), configuration.getMaxEmrSpotInstanceBidPct());
            throw new DlabException("Spot instances bidding percentage value out of the boundaries. Minimum is " + configuration.getMinEmrSpotInstanceBidPct() +
                    ", maximum is " + configuration.getMaxEmrSpotInstanceBidPct() + ".");
        }

        boolean isAdded = computationalDAO.addComputational(userInfo.getName(), formDTO.getNotebookName(),
                AwsComputationalResource.builder()
                        .computationalName(formDTO.getName())
                        .imageName(formDTO.getImage())
                        .templateName(formDTO.getTemplateName())
                        .status(CREATING.toString())
                        .masterShape(formDTO.getMasterInstanceType())
                        .slaveShape(formDTO.getSlaveInstanceType())
                        .slaveSpot(formDTO.getSlaveInstanceSpot())
                        .slaveSpotPctPrice(formDTO.getSlaveInstanceSpotPctPrice())
                        .slaveNumber(formDTO.getInstanceCount())
                        .version(formDTO.getVersion()).build());
        if (isAdded) {
            try {
                UserInstanceDTO instance = exploratoryDAO.fetchExploratoryFields(userInfo.getName(), formDTO.getNotebookName());
                ComputationalCreateAws dto = RequestBuilder.newComputationalCreate(userInfo, instance, formDTO);
                String uuid = provisioningService.post(COMPUTATIONAL_CREATE_CLOUD_SPECIFIC, userInfo.getAccessToken(), dto, String.class);
                RequestId.put(userInfo.getName(), uuid);
                return Response.ok().build();
            } catch (Exception t) {
                try {
                    updateComputationalStatus(userInfo.getName(), formDTO.getNotebookName(), formDTO.getName(), FAILED);
                } catch (DlabException e) {
                    log.error("Could not update the status of computational resource {} for user {}", formDTO.getName(), userInfo.getName(), e);
                }
                throw new DlabException("Could not send request for creation the computational resource " + formDTO.getName() + ": " + t.getLocalizedMessage(), t);
            }
        } else {
            log.debug("Used existing computational resource {} for user {}", formDTO.getName(), userInfo.getName());
            return Response.status(Response.Status.FOUND).build();
        }
    }

    /**
     * Asynchronously creates EMR cluster
     *
     * @param userInfo user info.
     * @param form     DTO info about creation of the computational resource.
     * @return 200 OK - if request success, 302 Found - for duplicates.
     * @throws IllegalArgumentException if docker image name is malformed
     */
    @PUT
    @Path("dataengine-service")
    public Response createDataEngineService(@Auth UserInfo userInfo, @Valid @NotNull AwsComputationalCreateForm form) {

        log.debug("Create computational resources for {} | form is {}", userInfo.getName(), form);

        if (DataEngineType.CLOUD_SERVICE == DataEngineType.fromDockerImageName(form.getImage())) {
            return create(userInfo, form);
        }

        throw new IllegalArgumentException("Malformed image " + form.getImage());
    }

    /**
     * Asynchronously triggers creation of Spark cluster
     *
     * @param userInfo user info.
     * @param form     DTO info about creation of the computational resource.
     * @return 200 OK - if request success, 302 Found - for duplicates.
     */

    @PUT
    @Path("dataengine")
    public Response createDataEngine(@Auth UserInfo userInfo, @Valid @NotNull SparkStandaloneClusterCreateForm form) {
        log.debug("Create computational resources for {} | form is {}", userInfo.getName(), form);

        if (!UserRoles.checkAccess(userInfo, RoleType.COMPUTATIONAL, form.getImage())) {
            log.warn("Unauthorized attempt to create a {} by user {}", form.getImage(), userInfo.getName());
            throw new DlabException("You do not have the privileges to create a " + form.getTemplateName());
        }

        return computationalService.createSparkCluster(userInfo, form)
                ? Response.ok().build()
                : Response.status(Response.Status.FOUND).build();
    }

    /**
     * Sends request to provisioning service for termination the computational resource for user.
     *
     * @param userInfo          user info.
     * @param exploratoryName   name of exploratory.
     * @param computationalName name of computational resource.
     * @return 200 OK if operation is successfully triggered
     */
    @DELETE
    @Path("/{exploratoryName}/{computationalName}/terminate")
    public Response terminate(@Auth UserInfo userInfo,
                              @PathParam("exploratoryName") String exploratoryName,
                              @PathParam("computationalName") String computationalName) {
        log.debug("Terminating computational resource {} for user {}", computationalName, userInfo.getName());

        computationalService.terminateComputationalEnvironment(userInfo, exploratoryName, computationalName);

        return Response.ok().build();
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
