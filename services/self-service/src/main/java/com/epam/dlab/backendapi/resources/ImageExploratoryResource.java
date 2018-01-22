package com.epam.dlab.backendapi.resources;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.resources.dto.ExploratoryImageCreateFormDTO;
import com.epam.dlab.backendapi.service.ImageExploratoryService;
import com.epam.dlab.backendapi.util.RequestBuilder;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

/**
 * Manages images for exploratory and computational environment
 */
@Path("/infrastructure_provision/exploratory_environment/image")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class ImageExploratoryResource {

    @Inject
    private ImageExploratoryService imageExploratoryService;

    @POST
    public Response createImage(@Auth UserInfo ui, @Valid @NotNull ExploratoryImageCreateFormDTO formDTO) {
        log.info("Create image request: " + formDTO);
        String uuid = imageExploratoryService.createImage(ui, formDTO.getNotebookName(), RequestBuilder.newImageCreate(ui, formDTO));
        return Response.accepted(uuid).build();
    }


    @GET
    public Response getImages(@Auth UserInfo ui) {
        log.info("Getting images for user " + ui.getName());
        return Response.ok().build();
    }
}
