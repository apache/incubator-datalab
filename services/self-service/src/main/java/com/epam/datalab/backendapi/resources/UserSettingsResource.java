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
import com.epam.datalab.backendapi.resources.dto.UserDTO;
import com.epam.datalab.backendapi.service.UserSettingService;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.security.RolesAllowed;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;


@Path("/user/settings")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class UserSettingsResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(UserSettingsResource.class);

    private final UserSettingService userSettingService;

    @Inject
    public UserSettingsResource(UserSettingService userSettingService) {
        this.userSettingService = userSettingService;
    }

    @GET
    public String getSettings(@Auth UserInfo userInfo) {
        String settings = userSettingService.getUISettings(userInfo);
        LOGGER.debug("Returns settings for user {}, content is {}", userInfo.getName(), settings);
        return settings;
    }

    @POST
    public Response saveSettings(@Auth UserInfo userInfo,
                                 @NotBlank String settings) {
        LOGGER.debug("Saves settings for user {}, content is {}", userInfo.getName(), settings);
        userSettingService.saveUISettings(userInfo, settings);
        return Response.ok().build();
    }

    @PUT
    @Path("budget")
    @RolesAllowed("/user/settings")
    public Response updateUsersBudget(@Auth UserInfo userInfo,
                                      @Valid @NotEmpty List<UserDTO> budgets) {
        LOGGER.debug("User {} is updating allowed budget for users: {}", userInfo.getName(), budgets);
        userSettingService.updateUsersBudget(budgets);
        return Response.ok().build();
    }


}
