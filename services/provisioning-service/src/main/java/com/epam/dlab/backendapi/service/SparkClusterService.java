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

package com.epam.dlab.backendapi.service;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.core.Directories;
import com.epam.dlab.backendapi.core.FileHandlerCallback;
import com.epam.dlab.backendapi.core.commands.*;
import com.epam.dlab.backendapi.core.response.handlers.ComputationalCallbackHandler;
import com.epam.dlab.backendapi.core.response.handlers.ComputationalConfigure;
import com.epam.dlab.command.DockerAction;
import com.epam.dlab.dto.base.DataEngineType;
import com.epam.dlab.dto.base.computational.ComputationalBase;
import com.epam.dlab.dto.computational.ComputationalTerminateDTO;
import com.epam.dlab.exceptions.DlabException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import static com.epam.dlab.command.DockerAction.CREATE;
import static com.epam.dlab.command.DockerAction.TERMINATE;

@Singleton
public class SparkClusterService extends DockerService implements DockerCommands {

    private static final DataEngineType SPARK_ENGINE = DataEngineType.SPARK_STANDALONE;

    @Inject
    private ComputationalConfigure computationalConfigure;

    public String create(UserInfo ui, ComputationalBase<?> dto) {
        String uuid = DockerCommands.generateUUID();
        folderListenerExecutor.start(configuration.getImagesDirectory(),
                configuration.getResourceStatusPollTimeout(),
                getFileHandlerCallback(CREATE, uuid, dto));

        try {
            commandExecutor.executeAsync(
                    ui.getName(),
                    uuid,
                    commandBuilder.buildCommand(
                            new RunDockerCommand()
                                    .withInteractive()
                                    .withName(nameContainer(dto.getEdgeUserName(), CREATE, dto.getComputationalName()))
                                    .withVolumeForRootKeys(configuration.getKeyDirectory())
                                    .withVolumeForResponse(configuration.getImagesDirectory())
                                    .withVolumeForLog(configuration.getDockerLogDirectory(), SPARK_ENGINE.getName())
                                    .withResource(SPARK_ENGINE.getName())
                                    .withRequestId(uuid)
                                    .withConfKeyName(configuration.getAdminKey())
                                    .withActionCreate(DataEngineType.getDockerImageName(SPARK_ENGINE)),
                            dto
                    )
            );
        } catch (JsonProcessingException e) {
            throw new DlabException("Could not create computational resource cluster", e);
        }
        return uuid;

    }

    public String terminate(UserInfo ui, ComputationalTerminateDTO dto) {
        String uuid = DockerCommands.generateUUID();
        folderListenerExecutor.start(configuration.getImagesDirectory(),
                configuration.getResourceStatusPollTimeout(),
                getFileHandlerCallback(TERMINATE, uuid, dto));
        try {
            commandExecutor.executeAsync(
                    ui.getName(),
                    uuid,
                    commandBuilder.buildCommand(
                            new RunDockerCommand()
                                    .withInteractive()
                                    .withName(nameContainer(dto.getEdgeUserName(), TERMINATE, dto.getComputationalName()))
                                    .withVolumeForRootKeys(configuration.getKeyDirectory())
                                    .withVolumeForResponse(configuration.getImagesDirectory())
                                    .withVolumeForLog(configuration.getDockerLogDirectory(), SPARK_ENGINE.getName())
                                    .withResource(SPARK_ENGINE.getName())
                                    .withRequestId(uuid)
                                    .withConfKeyName(configuration.getAdminKey())
                                    .withActionTerminate(DataEngineType.getDockerImageName(SPARK_ENGINE)),
                            dto
                    )
            );
        } catch (JsonProcessingException e) {
            throw new DlabException("Could not terminate computational resources cluster", e);
        }

        return uuid;
    }

    private FileHandlerCallback getFileHandlerCallback(DockerAction action, String uuid, ComputationalBase<?> dto) {
        return new ComputationalCallbackHandler(computationalConfigure, selfService, action, uuid, dto);
    }

    private String nameContainer(String user, DockerAction action, String name) {
        return nameContainer(user, action.toString(), "computational", name);
    }

    @Override
    public String getResourceType() {
        return Directories.DATA_ENGINE_LOG_DIRECTORY;
    }
}
