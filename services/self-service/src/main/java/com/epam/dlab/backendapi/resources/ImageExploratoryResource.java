/*
 * Copyright (c) 2018, EPAM SYSTEMS INC
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

package com.epam.dlab.backendapi.resources;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.resources.dto.ExploratoryImageCreateFormDTO;
import com.epam.dlab.backendapi.resources.dto.ImageInfoRecord;
import com.epam.dlab.backendapi.resources.swagger.SwaggerSecurityInfo;
import com.epam.dlab.backendapi.service.ImageExploratoryService;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import io.swagger.annotations.*;
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
@Api(value = "Service for machine images", authorizations = @Authorization(SwaggerSecurityInfo.TOKEN_AUTH))
@Slf4j
public class ImageExploratoryResource {

	private ImageExploratoryService imageExploratoryService;
	private RequestId requestId;

	@Inject
	public ImageExploratoryResource(ImageExploratoryService imageExploratoryService, RequestId requestId) {
		this.imageExploratoryService = imageExploratoryService;
		this.requestId = requestId;
	}

	@POST
	@ApiOperation("Creates machine image from existing notebook")
	@ApiResponses(@ApiResponse(code = 202, message = "Machine image has been created"))
	public Response createImage(@ApiParam(hidden = true) @Auth UserInfo ui,
								@ApiParam(value = "Notebook image create form DTO", required = true)
								@Valid @NotNull ExploratoryImageCreateFormDTO formDTO,
								@ApiParam(hidden = true) @Context UriInfo uriInfo) {
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
	@ApiOperation("Fetches machine images created from specific Docker image")
	@ApiResponses(@ApiResponse(code = 200, message = "Machine images were fetched successfully"))
	public Response getImages(@ApiParam(hidden = true) @Auth UserInfo ui,
							  @ApiParam(value = "Docker image", required = true)
							  @QueryParam("docker_image") String dockerImage) {
		log.debug("Getting images for user " + ui.getName());
		final List<ImageInfoRecord> images = imageExploratoryService.getNotFailedImages(ui.getName(), dockerImage);
		return Response.ok(images).build();
	}

	@GET
	@Path("{name}")
	@ApiOperation("Fetches machine image by name")
	@ApiResponses({@ApiResponse(code = 400, message = "Invalid machine image's name"),
			@ApiResponse(code = 200, message = "Machine image fetched successfully")})
	public Response getImage(@ApiParam(hidden = true) @Auth UserInfo ui,
							 @ApiParam(value = "Image's name", required = true) @PathParam("name") String name) {
		log.debug("Getting image with name {} for user {}", name, ui.getName());
		return Response.ok(imageExploratoryService.getImage(ui.getName(), name)).build();
	}
}
