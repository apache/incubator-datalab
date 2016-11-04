/******************************************************************************************************

 Copyright (c) 2016 EPAM Systems Inc.

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 *****************************************************************************************************/

package com.epam.dlab.backendapi.core.response.keyloader;

import com.epam.dlab.backendapi.ProvisioningServiceApplicationConfiguration;
import com.epam.dlab.backendapi.client.rest.SelfAPI;
import com.epam.dlab.backendapi.core.CommandExecutor;
import com.epam.dlab.backendapi.core.DockerCommands;
import com.epam.dlab.backendapi.core.docker.command.RunDockerCommand;
import com.epam.dlab.backendapi.core.response.folderlistener.FileHandlerCallback;
import com.epam.dlab.backendapi.core.response.folderlistener.FolderListenerExecutor;
import com.epam.dlab.client.restclient.RESTService;
import com.epam.dlab.dto.keyload.KeyLoadStatus;
import com.epam.dlab.dto.keyload.UploadFileDTO;
import com.epam.dlab.dto.keyload.UploadFileResultDTO;
import com.epam.dlab.dto.keyload.UserAWSCredentialDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

@Singleton
public class KeyLoader implements DockerCommands, SelfAPI {
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
    private CommandExecutor commandExecuter;
    @Inject
    private RESTService selfService;

    public String uploadKey(UploadFileDTO dto) throws IOException, InterruptedException {
        saveKeyToFile(dto);
        String uuid = DockerCommands.generateUUID();
        folderListenerExecutor.start(configuration.getKeyLoaderDirectory(),
                configuration.getKeyLoaderPollTimeout(),
                getFileHandlerCallback(dto.getUser(), uuid));
        commandExecuter.executeAsync(
                new RunDockerCommand()
                        .withVolumeForRootKeys(configuration.getKeyDirectory())
                        .withVolumeForResponse(configuration.getKeyLoaderDirectory())
                        .withRequestId(uuid)
                        .withConfServiceBaseName(dto.getServiceBaseName())
                        .withCredsKeyName(configuration.getAdminKey())
                        .withCredsSecurityGroupsIds(dto.getSecurityGroup())
                        .withEdgeUserName(dto.getUser())
                        .withActionCreate(configuration.getEdgeImage())
                        .toCMD()
        );

        return uuid;
    }

    private void saveKeyToFile(UploadFileDTO dto) throws IOException {
        LOGGER.debug("save key");
        Files.write(Paths.get(configuration.getKeyDirectory(), dto.getUser() + KEY_EXTENTION), dto.getContent().getBytes());
    }

    private FileHandlerCallback getFileHandlerCallback(String user, String originalUuid) {
        return new FileHandlerCallback() {
            @Override
            public boolean checkUUID(String uuid) {
                return originalUuid.equals(uuid);
            }

            @Override
            public boolean handle(String fileName, byte[] content) throws Exception {
                LOGGER.debug("get file {} actually waited for {}", fileName, originalUuid);
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
}
