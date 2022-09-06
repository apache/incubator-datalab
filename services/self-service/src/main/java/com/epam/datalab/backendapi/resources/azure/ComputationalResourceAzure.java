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

package com.epam.datalab.backendapi.resources.azure;

import com.epam.datalab.auth.UserInfo;
import com.epam.datalab.backendapi.conf.SelfServiceApplicationConfiguration;
import com.epam.datalab.backendapi.resources.dto.SparkStandaloneClusterCreateForm;
import com.epam.datalab.backendapi.resources.dto.azure.AzureComputationalCreateForm;
import com.epam.datalab.backendapi.roles.RoleType;
import com.epam.datalab.backendapi.roles.UserRoles;
import com.epam.datalab.backendapi.service.ComputationalService;
import com.epam.datalab.dto.aws.computational.ClusterConfig;
import com.epam.datalab.dto.azure.computational.AzureComputationalResource;
import com.epam.datalab.dto.base.DataEngineType;
import com.epam.datalab.exceptions.DatalabException;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.extern.slf4j.Slf4j;

import javax.management.relation.Role;
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
import static com.epam.datalab.rest.contracts.ComputationalAPI.AUDIT_COMPUTATIONAL_RECONFIGURE_MESSAGE;
import static com.epam.datalab.rest.contracts.ComputationalAPI.AUDIT_MESSAGE;

/**
 * Provides the REST API for the computational resource on Azure.
 */
@Path("/azure/infrastructure_provision/computational_resources")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class ComputationalResourceAzure {

    private final SelfServiceApplicationConfiguration configuration;

    private final ComputationalService computationalService;

    @Inject
    public ComputationalResourceAzure(ComputationalService computationalService,
                                      SelfServiceApplicationConfiguration configuration) {
        this.computationalService = computationalService;
        this.configuration = configuration;
    }

    @GET
    @Path("/{project}/{endpoint}/templates")
    public Response getTemplates(@Auth @Parameter(hidden = true) UserInfo userInfo, @PathParam("project") String project,
                                 @PathParam("endpoint") String endpoint) {
        return Response.ok(computationalService.getComputationalNamesAndTemplates(userInfo, project, endpoint)).build();
    }

    @PUT
    @Path("dataengine-service")
    public Response createDataEngineService(@Auth @Parameter(hidden = true) UserInfo userInfo,
                                            @Parameter @Valid @NotNull AzureComputationalCreateForm form) {
        log.debug("Create computational resources for {} | form is {}", userInfo.getName(), form);

        if (DataEngineType.CLOUD_SERVICE == DataEngineType.fromDockerImageName(form.getImage())) {

            validate(userInfo, form);

            AzureComputationalResource azureComputationalResource = AzureComputationalResource.builder()
                    .computationalName(form.getName())
                    .imageName(form.getImage())
                    .templateName(form.getTemplateName())
                    .status(CREATING.toString())
                    .masterShape(form.getMasterInstanceType())
                    .slaveShape(form.getSlaveInstanceType())
                    .totalInstanceCount(Integer.parseInt(form.getInstanceCount()))
                    .config(form.getConfig())
                    .version(form.getVersion())
                    .build();

            boolean resourceAdded = computationalService.createDataEngineService(userInfo, form.getName(), form, azureComputationalResource
                    , form.getProject(), getAuditInfo(form.getNotebookName()));
            return resourceAdded ? Response.ok().build() : Response.status(Response.Status.FOUND).build();
        }

        throw new IllegalArgumentException("Malformed image " + form.getImage());
    }


    /**
     * Asynchronously creates computational Spark cluster.
     *
     * @param userInfo user info.
     * @param form     user info about creation of the computational resource.
     * @return 200 OK if request success, 302 Found - for duplicates, otherwise throws exception.
     * @throws IllegalArgumentException if input is not valid or exceeds configuration limits
     */
    @PUT
    @Path("dataengine")
    public Response createDataEngine(@Auth UserInfo userInfo,
                                     @Valid @NotNull SparkStandaloneClusterCreateForm form) {
        log.debug("Create computational resources for {} | form is {}", userInfo.getName(), form);
        if (!UserRoles.checkAccess(userInfo, RoleType.COMPUTATIONAL, form.getImage(), userInfo.getRoles())) {
            log.warn("Unauthorized attempt to create a {} by user {}", form.getImage(), userInfo.getName());
            throw new DatalabException("You do not have the privileges to create a " + form.getTemplateName());
        }

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

    private void validate(UserInfo userInfo, AzureComputationalCreateForm formDTO) {
        if (!UserRoles.checkAccess(userInfo, RoleType.COMPUTATIONAL, formDTO.getImage(), userInfo.getRoles())) {
            log.warn("Unauthorized attempt to create a {} by user {}", formDTO.getImage(), userInfo.getName());
            throw new DatalabException("You do not have the privileges to create a " + formDTO.getTemplateName());
        }

        int slaveInstanceCount = Integer.parseInt(formDTO.getInstanceCount());
        if (slaveInstanceCount < configuration.getMinHDInsightInstanceCount() || slaveInstanceCount > configuration.getMaxHDInsightInstanceCount()) {

            log.debug("Creating computational resource {} for user {} fail: Limit exceeded to creation slave " +
                            "instances. Minimum is {}, maximum is {}",
                    formDTO.getName(), userInfo.getName(), configuration.getMinHDInsightInstanceCount(),
                    configuration.getMaxHDInsightInstanceCount());
            throw new DatalabException("Limit exceeded to creation slave instances. Minimum is " +
                    configuration.getMinHDInsightInstanceCount() + ", maximum is " + configuration.getMaxHDInsightInstanceCount() +
                    ".");
        }

    }

    private String getAuditInfo(String exploratoryName) {
        return String.format(AUDIT_MESSAGE, exploratoryName);
    }
}
