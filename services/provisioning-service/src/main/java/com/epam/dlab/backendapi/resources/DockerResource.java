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

package com.epam.dlab.backendapi.resources;

import com.epam.dlab.backendapi.ProvisioningServiceApplicationConfiguration;
import com.epam.dlab.backendapi.core.ICommandExecutor;
import com.epam.dlab.backendapi.core.MetadataHolder;
import com.epam.dlab.backendapi.core.commands.CommandBuilder;
import com.epam.dlab.backendapi.core.commands.CommandExecutor;
import com.epam.dlab.backendapi.core.commands.DockerCommands;
import com.epam.dlab.backendapi.core.commands.RunDockerCommand;
import com.epam.dlab.dto.imagemetadata.ImageMetadataDTO;
import com.epam.dlab.dto.imagemetadata.ImageType;
import com.google.inject.Inject;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.Set;

@Path("/docker")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class DockerResource implements DockerCommands {
    private static final Logger LOGGER = LoggerFactory.getLogger(DockerResource.class);

    @Inject
    private ProvisioningServiceApplicationConfiguration configuration;
    @Inject
    private MetadataHolder metadataHolder;
    @Inject
    private ICommandExecutor commandExecuter;

    @Inject
    private CommandBuilder commandBuilder;

    @GET
    @Path("{type}")
    public Set<ImageMetadataDTO> getDockerImages(@PathParam("type") String type) throws
            IOException, InterruptedException {
        LOGGER.debug("docker statuses asked for {}", type);
        return metadataHolder
                .getMetadata(ImageType.valueOf(type.toUpperCase()));
    }

    @Path("/run")
    @POST
    public String run(String image) throws IOException, InterruptedException {
        LOGGER.debug("run docker image {}", image);
        String uuid = DockerCommands.generateUUID();
        commandExecuter.executeAsync(
                new RunDockerCommand()
                        .withName(nameContainer("image", "runner"))
                        .withVolumeForRootKeys(configuration.getKeyDirectory())
                        .withVolumeForResponse(configuration.getImagesDirectory())
                        .withRequestId(uuid)
                        .withDryRun()
                        .withActionRun(image)
                        .toCMD()
        );
        return uuid;
    }

    public String getResourceType() {
        throw new NotImplementedException("General commands haven't a pre-defined log path");
    }
}
