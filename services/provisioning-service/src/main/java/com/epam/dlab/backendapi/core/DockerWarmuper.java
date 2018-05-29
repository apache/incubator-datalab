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
import com.epam.dlab.backendapi.core.commands.DockerCommands;
import com.epam.dlab.backendapi.core.commands.ICommandExecutor;
import com.epam.dlab.backendapi.core.commands.RunDockerCommand;
import com.epam.dlab.backendapi.core.response.folderlistener.FolderListenerExecutor;
import com.epam.dlab.dto.imagemetadata.ComputationalMetadataDTO;
import com.epam.dlab.dto.imagemetadata.ExploratoryMetadataDTO;
import com.epam.dlab.dto.imagemetadata.ImageMetadataDTO;
import com.epam.dlab.dto.imagemetadata.ImageType;
import com.epam.dlab.process.model.ProcessInfo;
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
    public static final String EXPLORATORY_RESPONSE_MARKER = "exploratory_environment_shapes";

    @Inject
    private ProvisioningServiceApplicationConfiguration configuration;
    @Inject
    private FolderListenerExecutor folderListenerExecutor;
    @Inject
    private ICommandExecutor commandExecutor;
    private Map<String, String> imageList = new ConcurrentHashMap<>();
    private Set<ImageMetadataDTO> metadataDTOs = new ConcurrentHashSet<>();


    @Override
    public void start() throws Exception {
        LOGGER.debug("warming up docker");
        final ProcessInfo processInfo = commandExecutor.executeSync("warmup", DockerCommands.generateUUID(), GET_IMAGES);
        List<String> images = Arrays.asList(processInfo.getStdOut().split("\n"));
        for (String image : images) {
            String uuid = UUID.randomUUID().toString();
            LOGGER.debug("warming up image: {} with uid {}", image, uuid);
            imageList.put(uuid, image);
            folderListenerExecutor.start(configuration.getWarmupDirectory(),
                    configuration.getWarmupPollTimeout(),
                    getFileHandlerCallback(uuid));
            String command = new RunDockerCommand()
                    .withVolumeForRootKeys(configuration.getKeyDirectory())
                    .withVolumeForResponse(configuration.getWarmupDirectory())
                    .withVolumeForLog(configuration.getDockerLogDirectory(), getResourceType())
                    .withResource(getResourceType())
                    .withRequestId(uuid)
                    .withActionDescribe(image)
                    .toCMD();
            commandExecutor.executeAsync("warmup", uuid, command);
        }
    }
    
    public class DockerFileHandlerCallback implements FileHandlerCallback {
    	private final String uuid;
    	
    	public DockerFileHandlerCallback(String uuid) {
    		this.uuid = uuid;
    	}
    	
    	@Override
    	public String getUUID() {
    		return uuid;
    	}
    	
        @Override
        public boolean checkUUID(String uuid) {
            return this.uuid.equals(uuid);
        }

        @Override
        public boolean handle(String fileName, byte[] content) {
            String uuid = DockerCommands.extractUUID(fileName);
            try {
                LOGGER.debug("processing response file {} with content {}", fileName, new String(content));
                addMetadata(content, uuid);
            } catch (IOException e) {
            	LOGGER.error("processing response file {} fails", fileName, e);
            	return false;
            }
            return true;
        }

        @Override
        public void handleError(String errorMessage) {
            LOGGER.warn("docker warmupper returned no result: {}", errorMessage);
        }
    }
    
    public DockerFileHandlerCallback getFileHandlerCallback(String uuid) {
    	return new DockerFileHandlerCallback(uuid);
    }

    private void addMetadata(byte[] content, String uuid) throws IOException {
        final JsonNode jsonNode = MAPPER.readTree(content);
        ImageMetadataDTO metadata;
        if (jsonNode.has(EXPLORATORY_RESPONSE_MARKER)) {
            metadata = MAPPER.readValue(content, ExploratoryMetadataDTO.class);
            metadata.setImageType(ImageType.EXPLORATORY);
        } else {
            metadata = MAPPER.readValue(content, ComputationalMetadataDTO.class);
            metadata.setImageType(ImageType.COMPUTATIONAL);
        }
        String image = imageList.get(uuid);
        metadata.setImage(image);
        LOGGER.debug("caching metadata for image {}: {}", image, metadata);
        metadataDTOs.add(metadata);
    }

    @Override
    public void stop() throws Exception {
        //do nothing
    }

    public Map<String, String> getUuids() {
        return Collections.unmodifiableMap(imageList);
    }

    public Set<ImageMetadataDTO> getMetadata(ImageType type) {
        return metadataDTOs.stream().filter(m -> m.getImageType().equals(type))
                .collect(Collectors.toSet());
    }

    @Override
    public String getResourceType() {
        return Directories.NOTEBOOK_LOG_DIRECTORY;
    }
}
