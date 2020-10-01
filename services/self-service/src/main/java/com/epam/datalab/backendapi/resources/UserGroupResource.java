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
import com.epam.datalab.backendapi.resources.dto.GroupDTO;
import com.epam.datalab.backendapi.resources.dto.UpdateGroupDTO;
import com.epam.datalab.backendapi.service.UserGroupService;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Slf4j
@Path("group")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UserGroupResource {

    private final UserGroupService userGroupService;

    @Inject
    public UserGroupResource(UserGroupService userGroupService) {
        this.userGroupService = userGroupService;
    }


    @POST
    @RolesAllowed("/roleManagement/create")
    public Response createGroup(@Auth UserInfo userInfo, @Valid GroupDTO dto) {
        log.debug("Creating new group {}", dto.getName());
        userGroupService.createGroup(userInfo, dto.getName(), dto.getRoleIds().keySet(), dto.getUsers());
        return Response.ok().build();
    }

    @PUT
    @RolesAllowed("/roleManagement")
    public Response updateGroup(@Auth UserInfo userInfo, @Valid UpdateGroupDTO dto) {
        log.debug("Updating group {}", dto.getName());
        userGroupService.updateGroup(userInfo, dto.getName(), dto.getRoles(), dto.getUsers());
        return Response.ok().build();
    }

    @GET
    @RolesAllowed("/roleManagement")
    public Response getGroups(@Auth UserInfo userInfo) {
        log.debug("Getting all groups for admin {}...", userInfo.getName());
        return Response.ok(userGroupService.getAggregatedRolesByGroup(userInfo)).build();
    }

    @DELETE
    @Path("{id}")
    @RolesAllowed("/roleManagement/delete")
    public Response deleteGroup(@Auth UserInfo userInfo, @PathParam("id") String group) {
        log.info("Admin {} is trying to delete group {} from application", userInfo.getName(), group);
        userGroupService.removeGroup(userInfo, group);
        return Response.ok().build();
    }
}
