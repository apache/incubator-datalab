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
import com.epam.datalab.backendapi.domain.EndpointDTO;
import com.epam.datalab.backendapi.domain.EndpointResourcesDTO;
import com.epam.datalab.backendapi.service.EndpointService;
import com.epam.datalab.rest.dto.ErrorDTO;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.headers.Header;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.net.URI;

@Path("endpoint")
@RolesAllowed("/api/endpoint")
public class EndpointResource {

    private final EndpointService endpointService;
    @Context
    private UriInfo uriInfo;

    @Inject
    public EndpointResource(EndpointService endpointService) {
        this.endpointService = endpointService;
    }

    @Operation(summary = "Create endpoint", tags = "endpoint")
    @ApiResponse(responseCode = "201", description = "Endpoint is successfully created",
            headers =
            @Header(required = true, name = "Location", description = "URI of created endpoint resource",
                    schema = @Schema(type = "string")))
    @ApiResponse(responseCode = "400", description = "Validation error", content = @Content(mediaType =
            MediaType.APPLICATION_JSON,
            schema = @Schema(implementation = ErrorDTO.class)))
    @ApiResponse(responseCode = "409", description = "Endpoint with passed name already exist in system",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ErrorDTO.class)))
    @Consumes(MediaType.APPLICATION_JSON)
    @POST
    public Response createEndpoint(@Parameter(hidden = true) @Auth UserInfo userInfo, @Valid EndpointDTO endpointDTO) {
        endpointService.create(userInfo, endpointDTO.getName(), endpointDTO);
        final URI uri = uriInfo.getRequestUriBuilder().path(endpointDTO.getName()).build();
        return Response
                .ok()
                .location(uri)
                .build();
    }

    @Operation(summary = "Get endpoint info", tags = "endpoint")
    @ApiResponse(responseCode = "200", description = "Return information about endpoint",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema =
            @Schema(implementation = EndpointDTO.class)))
    @ApiResponse(responseCode = "404", description = "Endpoint with passed name not found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ErrorDTO.class)))
    @GET
    @Path("{name}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEndpoint(@Parameter(hidden = true) @Auth UserInfo userInfo,
                                @Parameter(description = "Endpoint name")
                                @PathParam("name") String name) {
        return Response.ok(endpointService.get(name)).build();
    }

    @Operation(summary = "Get endpoints available in system", tags = "endpoint")
    @ApiResponse(responseCode = "200", description = "Return information about endpoints",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema =
            @Schema(implementation = EndpointDTO.class)))
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEndpoints(@Parameter(hidden = true) @Auth UserInfo userInfo) {
        return Response.ok(endpointService.getEndpoints()).build();
    }

    @Operation(summary = "Get resources related to the endpoint", tags = "endpoint")
    @ApiResponse(responseCode = "200", description = "Return information about resources of endpoint",
            content = @Content(mediaType = MediaType.APPLICATION_JSON, schema =
            @Schema(implementation = EndpointResourcesDTO.class)))
    @GET
    @Path("{name}/resources")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getEndpointResources(@Parameter(hidden = true) @Auth UserInfo userInfo,
                                         @Parameter(description = "Endpoint name")
                                         @PathParam("name") String name) {
        return Response.ok(endpointService.getEndpointResources(name)).build();
    }

    @Operation(summary = "Remove endpoint", tags = "endpoint")
    @ApiResponse(responseCode = "200", description = "Endpoint is successfully removed")
    @ApiResponse(responseCode = "404", description = "Endpoint with passed name not found",
            content = @Content(mediaType = MediaType.APPLICATION_JSON,
                    schema = @Schema(implementation = ErrorDTO.class)))
    @DELETE
    @Path("{name}")
    public Response removeEndpoint(@Parameter(hidden = true) @Auth UserInfo userInfo,
                                   @Parameter(description = "Endpoint name")
                                   @PathParam("name") String name) {
        endpointService.remove(userInfo, name);
        return Response.ok().build();
    }

    @Operation(summary = "Check whether endpoint url is valid", tags = "endpoint")
    @ApiResponse(responseCode = "200", description = "Valid endpoint url")
    @ApiResponse(responseCode = "404", description = "Endpoint url is not valid")
    @GET
    @Path("url/{url}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response checkEndpointUrl(@Parameter(hidden = true) @Auth UserInfo userInfo,
                                     @Parameter(description = "Endpoint url")
                                     @PathParam("url") String url) {
        endpointService.checkUrl(userInfo, url);
        return Response.ok().build();
    }
}
