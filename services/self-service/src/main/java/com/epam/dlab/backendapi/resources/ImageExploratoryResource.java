package com.epam.dlab.backendapi.resources;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.resources.dto.ExploratoryImageCreateFormDTO;
import com.epam.dlab.backendapi.resources.dto.ImageInfoRecord;
import com.epam.dlab.backendapi.service.ImageExploratoryService;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import java.net.URI;
import java.util.List;

/**
 * Manages images for exploratory and computational environment
 */
@Path("/infrastructure_provision/exploratory_environment/image")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class ImageExploratoryResource {

	private ImageExploratoryService imageExploratoryService;
	private RequestId requestId;

	@Inject
	ImageExploratoryResource(ImageExploratoryService imageExploratoryService, RequestId requestId) {
		this.imageExploratoryService = imageExploratoryService;
		this.requestId = requestId;
	}

	@POST
	public Response createImage(@Auth UserInfo ui, @Valid @NotNull ExploratoryImageCreateFormDTO formDTO,
								@Context UriInfo uriInfo) {
		log.debug("Creating an image {} for user {}", formDTO, ui.getName());
		String uuid = imageExploratoryService.createImage(ui, formDTO.getNotebookName(), formDTO.getName(), formDTO
				.getDescription());
		requestId.put(ui.getName(), uuid);

		final URI imageUri = UriBuilder.fromUri(uriInfo.getRequestUri())
				.path(formDTO.getName())
				.build();
		return Response.accepted(uuid).location(imageUri).build();
	}


	@GET
	public Response getImages(@Auth UserInfo ui, @QueryParam("docker_image") String dockerImage) {
		log.debug("Getting images for user " + ui.getName());
		final List<ImageInfoRecord> images = imageExploratoryService.getCreatedImages(ui.getName(), dockerImage);
		return Response.ok(images).build();
	}

	@GET
	@Path("{name}")
	public Response getImage(@Auth UserInfo ui, @PathParam("name") String name) {
		log.debug("Getting image with name {} for user {}", name, ui.getName());
		return Response.ok(imageExploratoryService.getImage(ui.getName(), name)).build();
	}
}
