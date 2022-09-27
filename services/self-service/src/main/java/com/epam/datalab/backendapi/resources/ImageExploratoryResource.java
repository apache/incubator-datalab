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

package com.epam.datalab.backendapi.resources;

import com.epam.datalab.auth.UserInfo;
import com.epam.datalab.backendapi.domain.RequestId;
import com.epam.datalab.backendapi.resources.dto.*;
import com.epam.datalab.backendapi.service.ImageExploratoryService;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
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
                formDTO.getName(), formDTO.getDescription());
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
        return Response.ok(imageExploratoryService.getNotFailedImages(ui, dockerImage,
                project, endpoint)).build();
    }

    @GET
    @Path("all")
    public Response getAllImagesForProject(@Auth UserInfo ui, @NotNull @QueryParam("project") String project) {
        log.debug("Getting images for user {}, project {}", ui.getName(), project);
        final List<ImageInfoRecord> images = imageExploratoryService.getImagesForProject(project);
        return Response.ok(images).build();
    }


    @GET
    @Path("user")
    public Response getImagesForUser(@Auth UserInfo ui) {
        log.debug("Getting images for user {}", ui.getName());
        final ImagesPageInfo images = imageExploratoryService.getImagesOfUser(ui,null);
        return Response.ok(images).build();
    }

    @POST
    @Path("user")
    public Response getImagesForUser(@Auth UserInfo ui, @Valid @NotNull ImageFilter imageFilter) {
        log.debug("Getting images for user {} with filter {}", ui.getName(), imageFilter);
        final ImagesPageInfo images = imageExploratoryService.getImagesOfUser(ui, imageFilter);
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

    @RolesAllowed("/api/image/share")
    @POST
    @Path("share")
    public Response shareImage(@Auth UserInfo ui, @Valid @NotNull ImageShareDTO dto) {
        log.debug("Sharing user image {} with project {} groups", dto.getImageName(), dto.getProjectName());
        imageExploratoryService.updateImageSharing(ui, dto);
        return Response.ok(imageExploratoryService.getImagesOfUser(ui,null)).build();
    }

    @RolesAllowed("/api/image/terminate")
    @DELETE
    @Path("/{projectName}/{endpoint}/{imageName}/terminate")
    public Response terminateUserImage(@Auth UserInfo ui,
                                       @PathParam("imageName") String imageName,
                                       @PathParam("projectName") String projectName,
                                       @PathParam("endpoint") String endpoint) {
        log.debug("Terminating  image {} of user {} groups", imageName, ui.getName());
        imageExploratoryService.terminateImage(ui,projectName,endpoint,imageName);
        return Response.ok(imageExploratoryService.getImagesOfUser(ui,null)).build();
    }

    @GET
    @Path("sharing_info/{imageName}/{projectName}/{endpoint}")
    public Response getSharingInfo(@Auth UserInfo ui,
                                   @PathParam("imageName") String imageName,
                                   @PathParam("projectName") String projectName,
                                   @PathParam("endpoint") String endpoint){
        return Response.ok(imageExploratoryService.getSharingInfo(ui.getName(),imageName,projectName, endpoint)).build();
    }

    @GET
    @Path("share_autocomplete/{imageName}/{projectName}/{endpoint}")
    public Response getUsersAndGroups(@Auth UserInfo userInfo,
                                      @PathParam("imageName") String imageName,
                                      @PathParam("projectName") String projectName,
                                      @PathParam("endpoint") String endpoint,
                                      @NotNull @QueryParam("value") String value){
        return Response.ok(imageExploratoryService.getUsersAndGroupsForSharing(userInfo.getName(),imageName, projectName, endpoint, value)).build();
    }
}
