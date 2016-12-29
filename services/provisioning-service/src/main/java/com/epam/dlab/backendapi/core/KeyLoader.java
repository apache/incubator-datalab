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

package com.epam.dlab.backendapi.core;

import com.epam.dlab.backendapi.ProvisioningServiceApplicationConfiguration;
import com.epam.dlab.backendapi.core.commands.CommandBuilder;
import com.epam.dlab.backendapi.core.commands.CommandExecutor;
import com.epam.dlab.backendapi.core.commands.DockerCommands;
import com.epam.dlab.backendapi.core.commands.RunDockerCommand;
import com.epam.dlab.backendapi.core.response.folderlistener.FolderListenerExecutor;
import com.epam.dlab.dto.edge.EdgeCreateDTO;
import com.epam.dlab.dto.keyload.KeyLoadStatus;
import com.epam.dlab.dto.keyload.UploadFileDTO;
import com.epam.dlab.dto.keyload.UploadFileResultDTO;
import com.epam.dlab.dto.keyload.UserAWSCredentialDTO;
import com.epam.dlab.rest.client.RESTService;
import com.epam.dlab.rest.contracts.SelfServiceAPI;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Singleton
public class KeyLoader implements DockerCommands, SelfServiceAPI {
    private static final Logger LOGGER = LoggerFactory.getLogger(KeyLoader.class);
    private static final String KEY_EXTENTION = ".pub";
    private static final String STATUS_FIELD = "status";
    private static final String RESPONSE_NODE = "response";
    private static final String RESULT_NODE = "result";

    @Inject
    private ProvisioningServiceApplicationConfiguration configuration;
    @Inject
    private FolderListenerExecutor folderListenerExecutor;
    @Inject
    private ICommandExecutor commandExecuter;
    @Inject
    private CommandBuilder commandBuilder;
    @Inject
    private RESTService selfService;

    public String uploadKey(UploadFileDTO dto) throws IOException, InterruptedException {
        saveKeyToFile(dto);
        String uuid = DockerCommands.generateUUID();
        EdgeCreateDTO edgeDto = dto.getEdge();
        folderListenerExecutor.start(configuration.getKeyLoaderDirectory(),
                                     configuration.getKeyLoaderPollTimeout(),
                                     getFileHandlerCallback(edgeDto.getIamUser(), uuid));
        commandExecuter.executeAsync(
                commandBuilder.buildCommand(
                        new RunDockerCommand()
                                .withName(nameContainer(edgeDto.getEdgeUserName(), "create", "edge"))
                                .withVolumeForRootKeys(configuration.getKeyDirectory())
                                .withVolumeForResponse(configuration.getKeyLoaderDirectory())
                                .withVolumeForLog(configuration.getDockerLogDirectory(), getResourceType())
                                .withResource(getResourceType())
                                .withRequestId(uuid)
                                .withCredsKeyName(configuration.getAdminKey())
                                .withActionCreate(configuration.getEdgeImage())
                                .withConfServiceBaseName(edgeDto.getServiceBaseName())
                                .withCredsRegion(edgeDto.getRegion())
                                .withCredsSecurityGroupsIds(edgeDto.getSecurityGroupIds())
                                .withVpcId(edgeDto.getVpcId())
                                .withEdgeSubnetId(edgeDto.getSubnetId())
                                .withUserKeyName(edgeDto.getEdgeUserName()), edgeDto
                )
        );

        return uuid;
    }

    private void saveKeyToFile(UploadFileDTO dto) throws IOException {
        Path keyFilePath = Paths.get(configuration.getKeyDirectory(), dto.getEdge().getEdgeUserName() + KEY_EXTENTION);
        LOGGER.debug("saving key to {}", keyFilePath.toString());
        Files.write(keyFilePath, dto.getContent().getBytes());
    }

    private FileHandlerCallback getFileHandlerCallback(String user, String originalUuid) {
        return new FileHandlerCallback() {
        	
        	private final String uuid = originalUuid;
        	
        	@Override
        	public String getUUID() {
        		return uuid;
        	}
        	
            @Override
            public boolean checkUUID(String uuid) {
                return this.uuid.equals(uuid);
            }

            @Override
            public boolean handle(String fileName, byte[] content) throws Exception {
                LOGGER.debug("Expected {}; processing response {} with content: {}", uuid, fileName, new String(content));
                JsonNode document = MAPPER.readTree(content);
                UploadFileResultDTO result = new UploadFileResultDTO(user);
                if (KeyLoadStatus.isSuccess(document.get(STATUS_FIELD).textValue())) {
                    result.setSuccessAndCredential(extractCredential(document));
                }
                selfService.post(KEY_LOADER, result, UploadFileResultDTO.class);
                return result.isSuccess();
            }

            @Override
            public void handleError() {
                selfService.post(KEY_LOADER, new UploadFileResultDTO(user), UploadFileResultDTO.class);
            }
        };

    }

    private UserAWSCredentialDTO extractCredential(JsonNode document) throws IOException {
        JsonNode node = document.get(RESPONSE_NODE).get(RESULT_NODE);
        return MAPPER.readValue(node.toString(), UserAWSCredentialDTO.class);
    }

    @Override
    public String getResourceType() {
        return Directories.EDGE_LOG_DIRECTORY;}
}
