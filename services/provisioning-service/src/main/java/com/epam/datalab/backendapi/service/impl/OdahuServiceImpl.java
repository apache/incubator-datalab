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
import com.epam.datalab.backendapi.core.commands.CommandBuilder;
import com.epam.datalab.backendapi.core.commands.DockerAction;
import com.epam.datalab.backendapi.core.commands.DockerCommands;
import com.epam.datalab.backendapi.core.commands.ICommandExecutor;
import com.epam.datalab.backendapi.core.commands.RunDockerCommand;
import com.epam.datalab.backendapi.core.response.folderlistener.FolderListenerExecutor;
import com.epam.datalab.backendapi.core.response.handlers.OdahuCallbackHandler;
import com.epam.datalab.backendapi.service.OdahuService;
import com.epam.datalab.dto.ResourceBaseDTO;
import com.epam.datalab.dto.odahu.ActionOdahuDTO;
import com.epam.datalab.dto.odahu.CreateOdahuDTO;
import com.epam.datalab.rest.client.RESTService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.inject.Inject;

public class OdahuServiceImpl implements OdahuService {

    private static final String CALLBACK_URI = "/api/odahu/status";
    private static final String ODAHU_RESOURCE_TYPE = "odahu";
    private static final String ODAHU_IMAGE = "docker.datalab-odahu";

    private final ProvisioningServiceApplicationConfiguration configuration;
    private final FolderListenerExecutor folderListenerExecutor;
    private final CommandBuilder commandBuilder;
    private final ICommandExecutor commandExecutor;
    private final RESTService selfService;

    @Inject
    public OdahuServiceImpl(ProvisioningServiceApplicationConfiguration configuration,
                            FolderListenerExecutor folderListenerExecutor, CommandBuilder commandBuilder,
                            ICommandExecutor commandExecutor, RESTService selfService) {
        this.configuration = configuration;
        this.folderListenerExecutor = folderListenerExecutor;
        this.commandBuilder = commandBuilder;
        this.commandExecutor = commandExecutor;
        this.selfService = selfService;
    }

    @Override
    public String create(UserInfo userInfo, CreateOdahuDTO dto) {
        return executeDocker(userInfo, dto, DockerAction.CREATE, ODAHU_RESOURCE_TYPE, ODAHU_IMAGE, dto.getName(),
                dto.getProject(), dto.getEndpoint());
    }

    @Override
    public String start(UserInfo userInfo, ActionOdahuDTO dto) {
        return executeDocker(userInfo, dto, DockerAction.START, ODAHU_RESOURCE_TYPE, ODAHU_IMAGE, dto.getName(),
                dto.getProject(), dto.getEndpoint());
    }

    @Override
    public String stop(UserInfo userInfo, ActionOdahuDTO dto) {
        return executeDocker(userInfo, dto, DockerAction.STOP, ODAHU_RESOURCE_TYPE, ODAHU_IMAGE, dto.getName(),
                dto.getProject(), dto.getEndpoint());
    }

    @Override
    public String terminate(UserInfo userInfo, ActionOdahuDTO dto) {
        return executeDocker(userInfo, dto, DockerAction.TERMINATE, ODAHU_RESOURCE_TYPE, ODAHU_IMAGE, dto.getName(),
                dto.getProject(), dto.getEndpoint());
    }


    private String executeDocker(UserInfo userInfo, ResourceBaseDTO dto, DockerAction action, String resourceType,
                                 String image, String name, String project, String endpoint) {
        String uuid = DockerCommands.generateUUID();

        folderListenerExecutor.start(configuration.getKeyLoaderDirectory(),
                configuration.getKeyLoaderPollTimeout(),
                new OdahuCallbackHandler(selfService, userInfo.getName(), uuid, action, CALLBACK_URI, name, project, endpoint));

        RunDockerCommand runDockerCommand = new RunDockerCommand()
                .withInteractive()
                .withName(String.join("_", userInfo.getSimpleName(), name, resourceType, action.toString(),
                        Long.toString(System.currentTimeMillis())))
                .withVolumeForRootKeys(configuration.getKeyDirectory())
                .withVolumeForResponse(configuration.getKeyLoaderDirectory())
                .withVolumeForLog(configuration.getDockerLogDirectory(), resourceType)
                .withResource(resourceType)
                .withRequestId(uuid)
                .withConfKeyName(configuration.getAdminKey())
                .withImage(image)
                .withAction(action);

        try {
            commandExecutor.executeAsync(userInfo.getName(), uuid, commandBuilder.buildCommand(runDockerCommand, dto));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        return uuid;
    }
}
