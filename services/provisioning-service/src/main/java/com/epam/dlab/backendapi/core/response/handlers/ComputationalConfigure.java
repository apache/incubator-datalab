/***************************************************************************

Copyright (c) 2016, EPAM SYSTEMS INC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

****************************************************************************/


package com.epam.dlab.backendapi.core.response.handlers;

import static com.epam.dlab.backendapi.core.commands.DockerAction.CONFIGURE;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epam.dlab.backendapi.ProvisioningServiceApplication;
import com.epam.dlab.backendapi.ProvisioningServiceApplicationConfiguration;
import com.epam.dlab.backendapi.core.Directories;
import com.epam.dlab.backendapi.core.FileHandlerCallback;
import com.epam.dlab.backendapi.core.commands.CommandBuilder;
import com.epam.dlab.backendapi.core.commands.DockerAction;
import com.epam.dlab.backendapi.core.commands.DockerCommands;
import com.epam.dlab.backendapi.core.commands.ICommandExecutor;
import com.epam.dlab.backendapi.core.commands.RunDockerCommand;
import com.epam.dlab.backendapi.core.response.folderlistener.FolderListenerExecutor;
import com.epam.dlab.dto.computational.ComputationalBaseDTO;
import com.epam.dlab.dto.computational.ComputationalConfigDTO;
import com.epam.dlab.dto.computational.ComputationalCreateDTO;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.client.RESTService;
import com.google.inject.Inject;

public class ComputationalConfigure implements DockerCommands {
    private static final Logger LOGGER = LoggerFactory.getLogger(ComputationalConfigure.class);

    @Inject
    private ProvisioningServiceApplicationConfiguration configuration;
    @Inject
    private FolderListenerExecutor folderListenerExecutor;
    @Inject
    private ICommandExecutor commandExecutor;
    @Inject
    private CommandBuilder commandBuilder;
    @Inject
    private RESTService selfService;

    public static String configure(String uuid, ComputationalCreateDTO dto) throws DlabException {
    	ComputationalConfigDTO dtoConf = new ComputationalConfigDTO()
                .withServiceBaseName(dto.getServiceBaseName())
                .withApplicationName(dto.getApplicationName())
                .withExploratoryName(dto.getExploratoryName())
                .withComputationalName(dto.getComputationalName())
                .withNotebookInstanceName(dto.getNotebookInstanceName())
                .withVersion(dto.getVersion())
                .withEdgeUserName(dto.getEdgeUserName())
                .withAwsIamUser(dto.getAwsIamUser())
                .withAwsRegion(dto.getAwsRegion())
                .withConfOsUser(dto.getConfOsUser());
    	ComputationalConfigure conf = new ComputationalConfigure();
    	ProvisioningServiceApplication
    		.getInjector()
    		.injectMembers(conf);
    	return conf.configure(uuid, dtoConf);
    }
    
    public String configure(String uuid, ComputationalConfigDTO dto) throws DlabException {
    	LOGGER.debug("Configure computational resources {} for user {}: {}", dto.getComputationalName(), dto.getEdgeUserName(), dto);
        folderListenerExecutor.start(
        		configuration.getImagesDirectory(),
                configuration.getResourceStatusPollTimeout(),
                getFileHandlerCallback(CONFIGURE, uuid, dto));
        try {
            commandExecutor.executeAsync(
            		dto.getEdgeUserName(),
                    uuid,
                    commandBuilder.buildCommand(
                            new RunDockerCommand()
                                    .withInteractive()
                                    .withName(nameContainer(dto.getEdgeUserName(), CONFIGURE, dto.getComputationalName()))
                                    .withVolumeForRootKeys(configuration.getKeyDirectory())
                                    .withVolumeForResponse(configuration.getImagesDirectory())
                                    .withVolumeForLog(configuration.getDockerLogDirectory(), getResourceType())
                                    .withResource(getResourceType())
                                    .withRequestId(uuid)
                                    .withConfKeyName(configuration.getAdminKey())
                                    .withActionConfigure(getImageConfigure(dto.getApplicationName())),
                            dto
                    )
            );
        } catch (Throwable t) {
            throw new DlabException("Could not configure computational resource cluster", t);
        }
    	return uuid;
    }

    private FileHandlerCallback getFileHandlerCallback(DockerAction action, String originalUuid, ComputationalBaseDTO<?> dto) {
        return new ComputationalCallbackHandler(selfService, action, originalUuid, dto);
    }
    
    private String nameContainer(String user, DockerAction action, String name) {
        return nameContainer(user, action.toString(), "computational", name);
    }

    private String getImageConfigure(String application) throws DlabException {
    	String imageName = configuration.getEmrImage();
    	int pos = imageName.lastIndexOf('-');
    	if (pos > 0) {
    		return imageName.substring(0, pos + 1) + application;
    	}
        throw new DlabException("Could not describe the image name for computational resources from image " + imageName + " and application " + application);
    }

    public String getResourceType() {
        return Directories.EMR_LOG_DIRECTORY;
    }
}
