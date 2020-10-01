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
import com.epam.datalab.backendapi.core.Directories;
import com.epam.datalab.backendapi.core.FileHandlerCallback;
import com.epam.datalab.backendapi.core.commands.DockerAction;
import com.epam.datalab.backendapi.core.commands.DockerCommands;
import com.epam.datalab.backendapi.core.commands.RunDockerCommand;
import com.epam.datalab.backendapi.core.response.handlers.LibInstallCallbackHandler;
import com.epam.datalab.backendapi.core.response.handlers.LibListCallbackHandler;
import com.epam.datalab.backendapi.service.impl.DockerService;
import com.epam.datalab.cloud.CloudProvider;
import com.epam.datalab.dto.LibListComputationalDTO;
import com.epam.datalab.dto.LibListExploratoryDTO;
import com.epam.datalab.dto.base.DataEngineType;
import com.epam.datalab.dto.exploratory.ExploratoryActionDTO;
import com.epam.datalab.dto.exploratory.ExploratoryBaseDTO;
import com.epam.datalab.dto.exploratory.LibraryInstallDTO;
import com.epam.datalab.rest.contracts.ComputationalAPI;
import com.epam.datalab.rest.contracts.ExploratoryAPI;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.Objects;

@Path("/library")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class LibraryResource extends DockerService implements DockerCommands {


    @POST
    @Path(ExploratoryAPI.EXPLORATORY + "/lib_list")
    public String getLibList(@Auth UserInfo ui, LibListExploratoryDTO dto) throws JsonProcessingException {
        return actionExploratory(ui.getName(), dto, DockerAction.LIB_LIST);
    }

    @POST
    @Path(ExploratoryAPI.EXPLORATORY + "/lib_install")
    public String libInstall(@Auth UserInfo ui, LibraryInstallDTO dto) throws JsonProcessingException {
        return actionExploratory(ui.getName(), dto, DockerAction.LIB_INSTALL);
    }

    @POST
    @Path(ComputationalAPI.COMPUTATIONAL + "/lib_list")
    public String getLibList(@Auth UserInfo ui, LibListComputationalDTO dto) throws JsonProcessingException {
        return actionComputational(ui.getName(), dto, DockerAction.LIB_LIST);
    }

    @POST
    @Path(ComputationalAPI.COMPUTATIONAL + "/lib_install")
    public String getLibList(@Auth UserInfo ui, LibraryInstallDTO dto) throws JsonProcessingException {
        return actionComputational(ui.getName(), dto, DockerAction.LIB_INSTALL);
    }

    private String actionExploratory(String username, ExploratoryBaseDTO<?> dto, DockerAction action) throws JsonProcessingException {
        log.debug("{} user {} exploratory environment {}", action, username, dto);
        String uuid = DockerCommands.generateUUID();
        folderListenerExecutor.start(configuration.getImagesDirectory(),
                configuration.getResourceStatusPollTimeout(),
                getFileHandlerCallbackExploratory(action, uuid, dto));

        RunDockerCommand runDockerCommand = getDockerCommandExploratory(dto, action, uuid);

        commandExecutor.executeAsync(username, uuid, commandBuilder.buildCommand(runDockerCommand, dto));
        return uuid;
    }

    private String actionComputational(String username, ExploratoryActionDTO<?> dto, DockerAction action) throws JsonProcessingException {
        log.debug("{} user {} exploratory environment {}", action, username, dto);
        String uuid = DockerCommands.generateUUID();
        folderListenerExecutor.start(configuration.getImagesDirectory(),
                configuration.getResourceStatusPollTimeout(),
                getFileHandlerCallbackComputational(action, uuid, dto));

        RunDockerCommand runDockerCommand = getDockerCommandComputational(dto, action, uuid);

        commandExecutor.executeAsync(username, uuid, commandBuilder.buildCommand(runDockerCommand, dto));
        return uuid;
    }

    private RunDockerCommand getDockerCommandExploratory(ExploratoryBaseDTO<?> dto, DockerAction action, String uuid) {
        return getDockerCommand(action, uuid)
                .withName(nameContainer(dto.getEdgeUserName(), action.toString(), "exploratory",
                        dto.getExploratoryName()))
                .withVolumeForLog(configuration.getDockerLogDirectory(), getResourceType())
                .withResource(getResourceType())
                .withImage(dto.getNotebookImage());
    }

    private RunDockerCommand getDockerCommandComputational(ExploratoryActionDTO<?> dto, DockerAction action,
                                                           String uuid) {
        RunDockerCommand runDockerCommand = getDockerCommand(action, uuid);
        if (dto instanceof LibraryInstallDTO) {
            LibraryInstallDTO newDTO = (LibraryInstallDTO) dto;
            runDockerCommand.withName(nameContainer(dto.getEdgeUserName(), action.toString(),
                    "computational", newDTO.getComputationalId()))
                    .withVolumeForLog(configuration.getDockerLogDirectory(),
                            DataEngineType.fromDockerImageName(newDTO.getComputationalImage()).getName())
                    .withResource(DataEngineType.fromDockerImageName(newDTO.getComputationalImage()).getName())

                    .withImage(newDTO.getComputationalImage());

        } else {
            LibListComputationalDTO newDTO = (LibListComputationalDTO) dto;

            runDockerCommand.withName(nameContainer(dto.getEdgeUserName(), action.toString(),
                    "computational", newDTO.getComputationalId()))
                    .withVolumeForLog(configuration.getDockerLogDirectory(),
                            DataEngineType.fromDockerImageName(newDTO.getComputationalImage()).getName())
                    .withResource(DataEngineType.fromDockerImageName(newDTO.getComputationalImage()).getName())
                    .withImage(newDTO.getComputationalImage());

        }
        return runDockerCommand;
    }

    private RunDockerCommand getDockerCommand(DockerAction action, String uuid) {
        RunDockerCommand runDockerCommand = new RunDockerCommand()
                .withInteractive()
                .withVolumeForRootKeys(configuration.getKeyDirectory())
                .withVolumeForResponse(configuration.getImagesDirectory())
                .withRequestId(uuid)
                .withConfKeyName(configuration.getAdminKey())
                .withAction(action);
        if (configuration.getCloudProvider() == CloudProvider.AZURE &&
                Objects.nonNull(configuration.getCloudConfiguration().getAzureAuthFile()) &&
                !configuration.getCloudConfiguration().getAzureAuthFile().isEmpty()) {
            runDockerCommand.withVolumeFoAzureAuthFile(configuration.getCloudConfiguration().getAzureAuthFile());
        }

        return runDockerCommand;
    }

    private FileHandlerCallback getFileHandlerCallbackExploratory(DockerAction action, String uuid,
                                                                  ExploratoryBaseDTO<?> dto) {
        switch (action) {
            case LIB_LIST:
                final String group = ((LibListExploratoryDTO) dto).getLibCacheKey();
                return new LibListCallbackHandler(selfService, DockerAction.LIB_LIST, uuid,
                        dto.getCloudSettings().getIamUser(), group);
            case LIB_INSTALL:
                return new LibInstallCallbackHandler(selfService, action, uuid,
                        dto.getCloudSettings().getIamUser(),
                        (LibraryInstallDTO) dto);
            default:
                throw new IllegalArgumentException("Unknown action " + action);
        }
    }

    private FileHandlerCallback getFileHandlerCallbackComputational(DockerAction action, String uuid,
                                                                    ExploratoryBaseDTO<?> dto) {
        switch (action) {
            case LIB_LIST:
                return new LibListCallbackHandler(selfService, action, uuid,
                        dto.getCloudSettings().getIamUser(), ((LibListComputationalDTO) dto).getLibCacheKey());
            case LIB_INSTALL:
                return new LibInstallCallbackHandler(selfService, action, uuid,
                        dto.getCloudSettings().getIamUser(), ((LibraryInstallDTO) dto));

            default:
                throw new IllegalArgumentException("Unknown action " + action);
        }
    }

    public String getResourceType() {
        return Directories.NOTEBOOK_LOG_DIRECTORY;
    }
}
