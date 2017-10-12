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

package com.epam.dlab.backendapi.resources.base;

import com.epam.dlab.backendapi.ProvisioningServiceApplicationConfiguration;
import com.epam.dlab.backendapi.core.Directories;
import com.epam.dlab.backendapi.core.FileHandlerCallback;
import com.epam.dlab.backendapi.core.commands.*;
import com.epam.dlab.backendapi.core.response.folderlistener.FolderListenerExecutor;
import com.epam.dlab.backendapi.core.response.handlers.ResourcesStatusCallbackHandler;
import com.epam.dlab.dto.ResourceBaseDTO;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.client.RESTService;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import static com.epam.dlab.backendapi.core.commands.DockerAction.STATUS;

@Slf4j
public abstract class InfrastructureService implements DockerCommands {
    @Inject
    private RESTService selfService;
    @Inject
    private ProvisioningServiceApplicationConfiguration configuration;
    @Inject
    private FolderListenerExecutor folderListenerExecutor;
    @Inject
    private ICommandExecutor commandExecutor;
    @Inject
    private CommandBuilder commandBuilder;

    public String action(String username, ResourceBaseDTO<?> dto, String iamUser, DockerAction dockerAction) {
        log.trace("Request the status of resources for user {}: {}", username, dto);
        String uuid = DockerCommands.generateUUID();
        folderListenerExecutor.start(configuration.getImagesDirectory(),
                configuration.getRequestEnvStatusTimeout(),
                getFileHandlerCallback(dockerAction, uuid, iamUser));
        try {
            commandExecutor.executeAsync(
                    username,
                    uuid,
                    commandBuilder.buildCommand(
                            new RunDockerCommand()
                                    .withInteractive()
                                    .withName(nameContainer(dto.getEdgeUserName(), STATUS, "resources"))
                                    .withVolumeForRootKeys(configuration.getKeyDirectory())
                                    .withVolumeForResponse(configuration.getImagesDirectory())
                                    .withVolumeForLog(configuration.getDockerLogDirectory(), Directories.EDGE_LOG_DIRECTORY)
                                    .withResource(getResourceType())
                                    .withRequestId(uuid)
                                    .withConfKeyName(configuration.getAdminKey())
                                    .withActionStatus(configuration.getEdgeImage()),
                            dto
                    )
            );
        } catch (Exception e) {
            throw new DlabException("Docker's command \"" + getResourceType() + "\" is fail: " + e.getLocalizedMessage(), e);
        }
        return uuid;
    }

    protected FileHandlerCallback getFileHandlerCallback(DockerAction action, String uuid, String user) {
        return new ResourcesStatusCallbackHandler(selfService, action, uuid, user);
    }

    private String nameContainer(String user, DockerAction action, String name) {
        return nameContainer(user, action.toString(), name);
    }

    public String getResourceType() {
        return "status";
    }
}
