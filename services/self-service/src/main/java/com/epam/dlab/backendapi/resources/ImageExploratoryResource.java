/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

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
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
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
	private static final String AUDIT_MESSAGE = "Create image: %s";

	private final ImageExploratoryService imageExploratoryService;
	private final RequestId requestId;

	@Inject
	public ImageExploratoryResource(ImageExploratoryService imageExploratoryService, RequestId requestId) {
		this.imageExploratoryService = imageExploratoryService;
		this.requestId = requestId;
	}

	@POST
	public Response createImage(@Auth UserInfo ui,
								@Valid @NotNull ExploratoryImageCreateFormDTO formDTO,
								@Context UriInfo uriInfo) {
		log.debug("Creating an image {} for user {}", formDTO, ui.getName());
		String uuid = imageExploratoryService.createImage(ui, formDTO.getProjectName(), formDTO.getNotebookName(),
				formDTO.getName(), formDTO.getDescription(), String.format(AUDIT_MESSAGE, formDTO.getName()));
		requestId.put(ui.getName(), uuid);

		final URI imageUri = UriBuilder.fromUri(uriInfo.getRequestUri())
				.path(formDTO.getName())
				.build();
		return Response.accepted(uuid).location(imageUri).build();
	}

	@GET
	public Response getImages(@Auth UserInfo ui,
							  @QueryParam("docker_image") String dockerImage,
							  @QueryParam("project") String project,
							  @QueryParam("endpoint") String endpoint) {
		log.debug("Getting images for user {}, project {}", ui.getName(), project);
		final List<ImageInfoRecord> images = imageExploratoryService.getNotFailedImages(ui.getName(), dockerImage,
				project, endpoint);
		return Response.ok(images).build();
	}

	@GET
	@Path("all")
	public Response getAllImagesForProject(@Auth UserInfo ui, @NotNull @QueryParam("project") String project) {
		log.debug("Getting images for user {}, project {}", ui.getName(), project);
		final List<ImageInfoRecord> images = imageExploratoryService.getImagesForProject(project);
		return Response.ok(images).build();
	}

	@GET
	@Path("{name}")
	public Response getImage(@Auth UserInfo ui,
							 @PathParam("name") String name,
							 @QueryParam("project") String project,
							 @QueryParam("endpoint") String endpoint) {
		log.debug("Getting image with name {} for user {}", name, ui.getName());
		return Response.ok(imageExploratoryService.getImage(ui.getName(), name, project, endpoint)).build();
	}
}
