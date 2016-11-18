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

package com.epam.dlab.backendapi.core.response.warmup;

import com.epam.dlab.backendapi.ProvisioningServiceApplicationConfiguration;
import com.epam.dlab.backendapi.core.CommandExecutor;
import com.epam.dlab.backendapi.core.DockerCommands;
import com.epam.dlab.backendapi.core.docker.command.RunDockerCommand;
import com.epam.dlab.backendapi.core.response.folderlistener.FileHandlerCallback;
import com.epam.dlab.backendapi.core.response.folderlistener.FolderListenerExecutor;
import com.epam.dlab.dto.imagemetadata.ComputationalMetadataDTO;
import com.epam.dlab.dto.imagemetadata.ExploratoryMetadataDTO;
import com.epam.dlab.dto.imagemetadata.ImageMetadataDTO;
import com.epam.dlab.dto.imagemetadata.ImageType;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.dropwizard.lifecycle.Managed;
import org.eclipse.jetty.util.ConcurrentHashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Singleton
public class DockerWarmuper implements Managed, DockerCommands, MetadataHolder {
    private static final Logger LOGGER = LoggerFactory.getLogger(DockerWarmuper.class);

    @Inject
    private ProvisioningServiceApplicationConfiguration configuration;
    @Inject
    private FolderListenerExecutor folderListenerExecutor;
    @Inject
    private CommandExecutor commandExecutor;
    private Map<String, String> uuids = new ConcurrentHashMap<>();
    private Set<ImageMetadataDTO> metadataDTOs = new ConcurrentHashSet<>();


    @Override
    public void start() throws Exception {
        LOGGER.debug("docker warm up start");
        folderListenerExecutor.start(configuration.getWarmupDirectory(),
                configuration.getWarmupPollTimeout(),
                getFileHandlerCallback());
        List<String> images = commandExecutor.executeSync(GET_IMAGES);
        for (String image : images) {
            LOGGER.debug("image: {}", image);
            String uuid = UUID.randomUUID().toString();
            uuids.put(uuid, image);
            String command = new RunDockerCommand()
                    .withVolumeForRootKeys(configuration.getKeyDirectory())
                    .withVolumeForResponse(configuration.getWarmupDirectory())
                    .withRequestId(uuid)
                    .withActionDescribe(image)
                    .toCMD();
            commandExecutor.executeAsync(command);
        }
    }

    FileHandlerCallback getFileHandlerCallback() {
        return new FileHandlerCallback() {
            @Override
            public boolean checkUUID(String uuid) {
                return uuids.containsKey(uuid);
            }

            @Override
            public boolean handle(String fileName, byte[] content) throws Exception {
                String uuid = DockerCommands.extractUUID(fileName);
                if (uuids.containsKey(uuid)) {
                    LOGGER.debug("handle file {}", fileName);
                    addMetadata(content, uuid);
                    return true;
                }
                return false;
            }

            @Override
            public void handleError() {
                LOGGER.warn("docker warmuper returned no result");
            }
        };
    }

    private void addMetadata(byte[] content, String uuid) throws IOException {
        final JsonNode jsonNode = MAPPER.readTree(content);
        ImageMetadataDTO metadata;
        if (jsonNode.has("exploratory_environment_shapes")) {
            metadata = MAPPER.readValue(content, ExploratoryMetadataDTO.class);
            metadata.setImageType(ImageType.EXPLORATORY);
        } else {
            metadata = MAPPER
                    .readValue(content, ComputationalMetadataDTO.class);
            metadata.setImageType(ImageType.COMPUTATIONAL);
        }
        metadata.setImage(uuids.get(uuid));
        metadataDTOs.add(metadata);
    }

    @Override
    public void stop() throws Exception {
    }

    public Map<String, String> getUuids() {
        return Collections.unmodifiableMap(uuids);
    }

    public Set<ImageMetadataDTO> getMetadatas(ImageType type) {
        return metadataDTOs.stream().filter(m -> m.getImageType().equals(type))
                .collect(Collectors.toSet());
    }
}
