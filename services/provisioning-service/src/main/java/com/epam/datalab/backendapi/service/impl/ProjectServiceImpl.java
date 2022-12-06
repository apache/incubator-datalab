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

package com.epam.datalab.backendapi.service.impl;

import com.epam.datalab.auth.UserInfo;
import com.epam.datalab.backendapi.ProvisioningServiceApplicationConfiguration;
import com.epam.datalab.backendapi.core.commands.*;
import com.epam.datalab.backendapi.core.response.folderlistener.FolderListenerExecutor;
import com.epam.datalab.backendapi.core.response.handlers.ProjectCallbackHandler;
import com.epam.datalab.backendapi.service.ProjectService;
import com.epam.datalab.cloud.CloudProvider;
import com.epam.datalab.dto.ResourceBaseDTO;
import com.epam.datalab.dto.aws.edge.EdgeInfoAws;
import com.epam.datalab.dto.azure.edge.EdgeInfoAzure;
import com.epam.datalab.dto.gcp.edge.EdgeInfoGcp;
import com.epam.datalab.dto.project.ProjectActionDTO;
import com.epam.datalab.dto.project.ProjectCreateDTO;
import com.epam.datalab.rest.client.RESTService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.inject.Inject;
import io.dropwizard.util.Duration;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;

@Slf4j
public class ProjectServiceImpl implements ProjectService {
    private static final String PROJECT_IMAGE = "docker.datalab-project";
    private static final String EDGE_IMAGE = "docker.datalab-edge";
    private static final String CALLBACK_URI = "/api/project/status";
    private static final String PROJECT_RESOURCE_TYPE = "project";
    private static final String EDGE_RESOURCE_TYPE = "edge";

    protected final RESTService selfService;
    private final ProvisioningServiceApplicationConfiguration configuration;
    private final FolderListenerExecutor folderListenerExecutor;
    private final ICommandExecutor commandExecutor;
    private final CommandBuilder commandBuilder;

    @Inject
    public ProjectServiceImpl(RESTService selfService, ProvisioningServiceApplicationConfiguration configuration,
                              FolderListenerExecutor folderListenerExecutor, ICommandExecutor commandExecutor, CommandBuilder commandBuilder) {
        this.selfService = selfService;
        this.configuration = configuration;
        this.folderListenerExecutor = folderListenerExecutor;
        this.commandExecutor = commandExecutor;
        this.commandBuilder = commandBuilder;
    }

    @Override
    public String create(UserInfo userInfo, ProjectCreateDTO dto) {
        log.info("Trying to create project: {}", dto);
        return executeDocker(userInfo, dto, DockerAction.CREATE, dto.getName(), PROJECT_RESOURCE_TYPE, PROJECT_IMAGE, dto.getEndpoint());
    }

    @Override
    public String recreate(UserInfo userInfo, ProjectCreateDTO dto) {
        log.info("Trying to recreate project: {}", dto);
        return executeDocker(userInfo, dto, DockerAction.RECREATE, dto.getName(), PROJECT_RESOURCE_TYPE, PROJECT_IMAGE, dto.getEndpoint());
    }

    @Override
    public String terminate(UserInfo userInfo, ProjectActionDTO dto) {
        log.info("Trying to terminate project: {}", dto);
        return executeDocker(userInfo, dto, DockerAction.TERMINATE, dto.getName(), PROJECT_RESOURCE_TYPE, PROJECT_IMAGE, dto.getEndpoint());
    }

    @Override
    public String start(UserInfo userInfo, ProjectActionDTO dto) {
        log.info("Trying to start project: {}", dto);
        return executeDocker(userInfo, dto, DockerAction.START, dto.getName(), EDGE_RESOURCE_TYPE, EDGE_IMAGE, dto.getEndpoint());
    }

    @Override
    public String stop(UserInfo userInfo, ProjectActionDTO dto) {
        log.info("Trying to stop project: {}", dto);
        return executeDocker(userInfo, dto, DockerAction.STOP, dto.getName(), EDGE_RESOURCE_TYPE, EDGE_IMAGE, dto.getEndpoint());
    }

    private String executeDocker(UserInfo userInfo, ResourceBaseDTO dto, DockerAction action, String projectName,
                                 String resourceType, String image, String endpoint) {
        String uuid = DockerCommands.generateUUID();
        Duration timeout = configuration.getKeyLoaderPollTimeout();
        if(action == DockerAction.CREATE){
            timeout = Duration.minutes(timeout.toMinutes() + 30);
        }

        folderListenerExecutor.start(configuration.getKeyLoaderDirectory(),
                timeout,
                new ProjectCallbackHandler(selfService, userInfo.getName(), uuid,
                        action, CALLBACK_URI, projectName, getEdgeClass(), endpoint));

        RunDockerCommand runDockerCommand = new RunDockerCommand()
                .withInteractive()
                .withName(String.join("_", userInfo.getSimpleName(), projectName, resourceType, action.toString(),
                        Long.toString(System.currentTimeMillis())))
                .withVolumeForRootKeys(configuration.getKeyDirectory())
                .withVolumeForResponse(configuration.getKeyLoaderDirectory())
                .withVolumeForLog(configuration.getDockerLogDirectory(), resourceType)
                .withResource(resourceType)
                .withRequestId(uuid)
                .withConfKeyName(configuration.getAdminKey())
                .withImage(image)
                .withAction(action);
        log.info("Docker command : {}", runDockerCommand);
        if (configuration.getCloudProvider() == CloudProvider.AZURE &&
                Objects.nonNull(configuration.getCloudConfiguration().getAzureAuthFile()) &&
                !configuration.getCloudConfiguration().getAzureAuthFile().isEmpty()) {
            runDockerCommand.withVolumeFoAzureAuthFile(configuration.getCloudConfiguration().getAzureAuthFile());
        }

        try {
            commandExecutor.executeAsync(userInfo.getName(), uuid, commandBuilder.buildCommand(runDockerCommand, dto));
        } catch (JsonProcessingException e) {
            log.error("Something went wrong. Reason {}", e.getMessage(), e);
        }
        return uuid;
    }

    private <T> Class<T> getEdgeClass() {
        if (configuration.getCloudProvider() == CloudProvider.AWS) {
            return (Class<T>) EdgeInfoAws.class;
        } else if (configuration.getCloudProvider() == CloudProvider.AZURE) {
            return (Class<T>) EdgeInfoAzure.class;
        } else if (configuration.getCloudProvider() == CloudProvider.GCP) {
            return (Class<T>) EdgeInfoGcp.class;
        }
        throw new IllegalArgumentException();
    }
}
