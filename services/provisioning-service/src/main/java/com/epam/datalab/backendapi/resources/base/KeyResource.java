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

package com.epam.datalab.backendapi.resources.base;

import com.epam.datalab.auth.UserInfo;
import com.epam.datalab.backendapi.ProvisioningServiceApplicationConfiguration;
import com.epam.datalab.backendapi.core.commands.DockerAction;
import com.epam.datalab.backendapi.service.impl.KeyService;
import com.epam.datalab.dto.reuploadkey.ReuploadKeyDTO;
import com.epam.datalab.rest.contracts.KeyAPI;
import com.epam.datalab.util.FileUtils;
import com.epam.datalab.util.UsernameUtils;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;

import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.util.UUID;

/**
 * Provides API for reuploading keys
 */
@Path("key")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class KeyResource {

    private final KeyService keyService;
    private final ProvisioningServiceApplicationConfiguration configuration;

    @Inject
    public KeyResource(KeyService keyService, ProvisioningServiceApplicationConfiguration configuration) {
        this.keyService = keyService;
        this.configuration = configuration;
    }


    @Path("/reupload")
    @POST
    public String reuploadKey(@Auth UserInfo ui, @DefaultValue("true") @QueryParam("is_primary_reuploading")
            boolean isPrimaryReuploading, ReuploadKeyDTO dto) throws IOException {
        if (isPrimaryReuploading) {
            replaceKeyfile(dto);
        }
        keyService.reuploadKeyAction(ui.getName(), dto, DockerAction.REUPLOAD_KEY);
        return UUID.randomUUID().toString();
    }

    @GET
    public String getAdminKey(@Auth UserInfo userInfo) {
        return keyService.getAdminKey();
    }

    private void replaceKeyfile(ReuploadKeyDTO dto) throws IOException {
        String edgeUserName = dto.getEdgeUserName();
        String filename = UsernameUtils.replaceWhitespaces(edgeUserName) + KeyAPI.KEY_EXTENTION;
        FileUtils.deleteFile(filename, configuration.getKeyDirectory());
        FileUtils.saveToFile(filename, configuration.getKeyDirectory(), dto.getContent());
    }

}
