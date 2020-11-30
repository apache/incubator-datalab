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
import com.epam.datalab.backendapi.resources.dto.UserRoleDTO;
import com.epam.datalab.backendapi.service.UserRoleService;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.security.RolesAllowed;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Slf4j
@Path("role")
@RolesAllowed("/roleManagement")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UserRoleResource {

    private final UserRoleService userRoleService;

    @Inject
    public UserRoleResource(UserRoleService roleService) {
        this.userRoleService = roleService;
    }

    @GET
    public Response getRoles(@Auth UserInfo userInfo) {
        log.debug("Getting all roles for admin {}...", userInfo.getName());
        return Response.ok(userRoleService.getUserRoles()).build();
    }

    @POST
    public Response createRole(@Auth UserInfo userInfo, UserRoleDTO dto) {
        log.info("Creating new role {} on behalf of admin {}...", dto, userInfo.getName());
        userRoleService.createRole(dto);
        return Response.ok().build();
    }
}
