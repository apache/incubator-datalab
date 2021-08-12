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

package com.epam.datalab.backendapi.resources.aws;

import com.epam.datalab.auth.UserInfo;
import com.epam.datalab.backendapi.conf.SelfServiceApplicationConfiguration;
import com.epam.datalab.backendapi.resources.dto.SparkStandaloneClusterCreateForm;
import com.epam.datalab.backendapi.resources.dto.aws.AwsComputationalCreateForm;
import com.epam.datalab.backendapi.roles.RoleType;
import com.epam.datalab.backendapi.roles.UserRoles;
import com.epam.datalab.backendapi.service.ComputationalService;
import com.epam.datalab.dto.aws.computational.AwsComputationalResource;
import com.epam.datalab.dto.aws.computational.ClusterConfig;
import com.epam.datalab.dto.base.DataEngineType;
import com.epam.datalab.exceptions.DatalabException;
import com.epam.datalab.rest.contracts.ComputationalAPI;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;

import static com.epam.datalab.dto.UserInstanceStatus.CREATING;
import static com.epam.datalab.dto.base.DataEngineType.SPARK_STANDALONE;


/**
 * Provides the REST API for the computational resource on AWS.
 */
@Path("/aws/infrastructure_provision/computational_resources")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class ComputationalResourceAws implements ComputationalAPI {
    @Inject
    private SelfServiceApplicationConfiguration configuration;
    @Inject
    private ComputationalService computationalService;

    @GET
    @Path("/{project}/{endpoint}/templates")
    public Response getTemplates(@Auth @Parameter(hidden = true) UserInfo userInfo, @PathParam("project") String project,
                                 @PathParam("endpoint") String endpoint) {
        return Response.ok(computationalService.getComputationalNamesAndTemplates(userInfo, project, endpoint)).build();
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
    public Response createDataEngineService(@Auth @Parameter(hidden = true) UserInfo userInfo,
                                            @Parameter @Valid @NotNull AwsComputationalCreateForm form) {
        log.debug("Create computational resources for {} | form is {}", userInfo.getName(), form);

        if (DataEngineType.CLOUD_SERVICE == DataEngineType.fromDockerImageName(form.getImage())) {

            validate(userInfo, form);

            AwsComputationalResource awsComputationalResource = AwsComputationalResource.builder()
                    .computationalName(form.getName())
                    .imageName(form.getImage())
                    .templateName(form.getTemplateName())
                    .status(CREATING.toString())
                    .masterShape(form.getMasterInstanceType())
                    .slaveShape(form.getSlaveInstanceType())
                    .slaveSpot(form.getSlaveInstanceSpot())
                    .slaveSpotPctPrice(form.getSlaveInstanceSpotPctPrice())
                    .slaveNumber(form.getInstanceCount())
                    .config(form.getConfig())
                    .version(form.getVersion())
                    .totalInstanceCount(Integer.parseInt(form.getInstanceCount()))
                    .build();
            boolean resourceAdded = computationalService.createDataEngineService(userInfo, form.getName(), form, awsComputationalResource,
                    form.getProject(), getAuditInfo(form.getNotebookName()));
            return resourceAdded ? Response.ok().build() : Response.status(Response.Status.FOUND).build();
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
    public Response createDataEngine(@Auth UserInfo userInfo,
                                     @Valid @NotNull SparkStandaloneClusterCreateForm form) {
        log.debug("Create computational resources for {} | form is {}", userInfo.getName(), form);

        validate(form);
        return computationalService.createSparkCluster(userInfo, form.getName(), form, form.getProject(), getAuditInfo(form.getNotebookName()))
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
    @Path("/{projectName}/{exploratoryName}/{computationalName}/terminate")
    public Response terminate(@Auth UserInfo userInfo,
                              @PathParam("projectName") String projectName,
                              @PathParam("exploratoryName") String exploratoryName,
                              @PathParam("computationalName") String computationalName) {
        log.debug("Terminating computational resource {} for user {}", computationalName, userInfo.getName());

        computationalService.terminateComputational(userInfo, userInfo.getName(), projectName, exploratoryName, computationalName, getAuditInfo(exploratoryName));

        return Response.ok().build();
    }

    /**
     * Sends request to provisioning service for stopping the computational resource for user.
     *
     * @param userInfo          user info.
     * @param exploratoryName   name of exploratory.
     * @param computationalName name of computational resource.
     * @return 200 OK if operation is successfully triggered
     */
    @DELETE
    @Path("/{project}/{exploratoryName}/{computationalName}/stop")
    public Response stop(@Auth UserInfo userInfo,
                         @PathParam("project") String project,
                         @PathParam("exploratoryName") String exploratoryName,
                         @PathParam("computationalName") String computationalName) {
        log.debug("Stopping computational resource {} for user {}", computationalName, userInfo.getName());

        computationalService.stopSparkCluster(userInfo, userInfo.getName(), project, exploratoryName, computationalName, getAuditInfo(exploratoryName));
        return Response.ok().build();
    }

    /**
     * Sends request to provisioning service for starting the computational resource for user.
     *
     * @param userInfo          user info.
     * @param exploratoryName   name of exploratory.
     * @param computationalName name of computational resource.
     * @return 200 OK if operation is successfully triggered
     */
    @PUT
    @Path("/{project}/{exploratoryName}/{computationalName}/start")
    public Response start(@Auth UserInfo userInfo,
                          @PathParam("exploratoryName") String exploratoryName,
                          @PathParam("computationalName") String computationalName,
                          @PathParam("project") String project) {
        log.debug("Starting computational resource {} for user {}", computationalName, userInfo.getName());

        computationalService.startSparkCluster(userInfo, exploratoryName, computationalName, project, getAuditInfo(exploratoryName));
        return Response.ok().build();
    }

    @PUT
    @Path("dataengine/{projectName}/{exploratoryName}/{computationalName}/config")
    public Response updateDataEngineConfig(@Auth UserInfo userInfo,
                                           @PathParam("projectName") String projectName,
                                           @PathParam("exploratoryName") String exploratoryName,
                                           @PathParam("computationalName") String computationalName,
                                           @Valid @NotNull List<ClusterConfig> config) {
        computationalService.updateSparkClusterConfig(userInfo, projectName, exploratoryName, computationalName, config,
                String.format(AUDIT_COMPUTATIONAL_RECONFIGURE_MESSAGE, computationalName, exploratoryName));
        return Response.ok().build();
    }

    @GET
    @Path("/{projectName}/{exploratoryName}/{computationalName}/config")
    public Response getClusterConfig(@Auth UserInfo userInfo,
                                     @PathParam("projectName") String projectName,
                                     @PathParam("exploratoryName") String exploratoryName,
                                     @PathParam("computationalName") String computationalName) {
        return Response.ok(computationalService.getClusterConfig(userInfo, projectName, exploratoryName, computationalName)).build();
    }

    private void validate(SparkStandaloneClusterCreateForm form) {

        int instanceCount = Integer.parseInt(form.getDataEngineInstanceCount());

        if (instanceCount < configuration.getMinSparkInstanceCount()
                || instanceCount > configuration.getMaxSparkInstanceCount()) {
            throw new IllegalArgumentException(String.format("Instance count should be in range [%d..%d]",
                    configuration.getMinSparkInstanceCount(), configuration.getMaxSparkInstanceCount()));
        }

        if (DataEngineType.fromDockerImageName(form.getImage()) != SPARK_STANDALONE) {
            throw new IllegalArgumentException(String.format("Unknown data engine %s", form.getImage()));
        }
    }

    private void validate(UserInfo userInfo, AwsComputationalCreateForm formDTO) {
        if (!UserRoles.checkAccess(userInfo, RoleType.COMPUTATIONAL, formDTO.getImage(), userInfo.getRoles())) {
            log.warn("Unauthorized attempt to create a {} by user {}", formDTO.getImage(), userInfo.getName());
            throw new DatalabException("You do not have the privileges to create a " + formDTO.getTemplateName());
        }

        int slaveInstanceCount = Integer.parseInt(formDTO.getInstanceCount());
        if (slaveInstanceCount < configuration.getMinEmrInstanceCount() || slaveInstanceCount >
                configuration.getMaxEmrInstanceCount()) {
            log.debug("Creating computational resource {} for user {} fail: Limit exceeded to creation slave " +
                            "instances. Minimum is {}, maximum is {}",
                    formDTO.getName(), userInfo.getName(), configuration.getMinEmrInstanceCount(),
                    configuration.getMaxEmrInstanceCount());
            throw new DatalabException("Limit exceeded to creation slave instances. Minimum is " +
                    configuration.getMinEmrInstanceCount() + ", maximum is " + configuration.getMaxEmrInstanceCount() +
                    ".");
        }

        if (formDTO.getSlaveInstanceSpotPctPrice() != null) {
            int slaveSpotInstanceBidPct = formDTO.getSlaveInstanceSpotPctPrice();
            if (formDTO.getSlaveInstanceSpot() && (slaveSpotInstanceBidPct < configuration.getMinEmrSpotInstanceBidPct()
                    || slaveSpotInstanceBidPct > configuration.getMaxEmrSpotInstanceBidPct())) {
                log.debug("Creating computational resource {} for user {} fail: Spot instances bidding percentage " +
                                "value " +
                                "out of the boundaries. Minimum is {}, maximum is {}",
                        formDTO.getName(), userInfo.getName(), configuration.getMinEmrSpotInstanceBidPct(),
                        configuration.getMaxEmrSpotInstanceBidPct());
                throw new DatalabException("Spot instances bidding percentage value out of the boundaries. Minimum is " +
                        configuration.getMinEmrSpotInstanceBidPct() + ", maximum is " +
                        configuration.getMaxEmrSpotInstanceBidPct() + ".");
            }
        }
    }

    private String getAuditInfo(String exploratoryName) {
        return String.format(AUDIT_MESSAGE, exploratoryName);
    }
}
