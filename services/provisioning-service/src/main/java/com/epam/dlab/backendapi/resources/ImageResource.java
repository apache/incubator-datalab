package com.epam.dlab.backendapi.resources;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.core.Directories;
import com.epam.dlab.backendapi.core.commands.DockerAction;
import com.epam.dlab.backendapi.core.commands.DockerCommands;
import com.epam.dlab.backendapi.core.commands.RunDockerCommand;
import com.epam.dlab.backendapi.core.response.handlers.ImageCreateCallbackHandler;
import com.epam.dlab.backendapi.service.DockerService;
import com.epam.dlab.dto.exploratory.ExploratoryImage;
import com.epam.dlab.rest.contracts.ExploratoryAPI;
import com.fasterxml.jackson.core.JsonProcessingException;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path(ExploratoryAPI.EXPLORATORY_IMAGE)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class ImageResource extends DockerService implements DockerCommands {

    @POST
    public Response createImage(@Auth UserInfo ui, ExploratoryImage image) throws JsonProcessingException {
        final String uuid = DockerCommands.generateUUID();

        folderListenerExecutor.start(configuration.getImagesDirectory(), configuration.getResourceStatusPollTimeout(),
                new ImageCreateCallbackHandler(selfService, image.getCloudSettings().getIamUser(), uuid,
                        DockerAction.IMAGE_CREATE, image));
        String command = commandBuilder.buildCommand(getDockerCommand(DockerAction.IMAGE_CREATE, uuid), image);
        //commandExecutor.executeAsync(ui.getName(), uuid, command);
        log.debug("Docker command: " + command);
        return Response.accepted(uuid).build();
    }


    @Override
    public String getResourceType() {
        return Directories.IMAGE_LOG_DIRECTORY;
    }

    private RunDockerCommand getDockerCommand(DockerAction action, String uuid) {
        return new RunDockerCommand()
                .withInteractive()
                .withVolumeForRootKeys(configuration.getKeyDirectory())
                .withVolumeForResponse(configuration.getImagesDirectory())
                .withRequestId(uuid)
                .withConfKeyName(configuration.getAdminKey())
                .withAction(action)
                .withName("TEST"); //TODO add specific values
    }
}
