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
import com.epam.datalab.backendapi.core.response.folderlistener.WatchItem;
import com.epam.datalab.properties.ChangePropertiesConst;
import com.epam.datalab.properties.ChangePropertiesService;
import com.epam.datalab.properties.RestartForm;
import com.epam.datalab.properties.YmlDTO;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Arrays;
import java.util.List;

import static com.epam.datalab.backendapi.core.response.folderlistener.WatchItem.ItemStatus.INPROGRESS;
import static com.epam.datalab.backendapi.core.response.folderlistener.WatchItem.ItemStatus.WAIT_FOR_FILE;

@Path("/config")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Slf4j
public class ChangePropertiesResource implements ChangePropertiesConst {

    private final ChangePropertiesService changePropertiesService;

    @Inject
    public ChangePropertiesResource(ChangePropertiesService changePropertiesService) {
        this.changePropertiesService = changePropertiesService;
    }

    @GET
    @Path("/provisioning-service")
    public Response getProvisioningServiceProperties(@Auth UserInfo userInfo) {
        return Response
                .ok(changePropertiesService.readFileAsString(PROVISIONING_SERVICE_PROP_PATH, PROVISIONING_SERVICE))
                .build();
    }

    @GET
    @Path("/billing")
    public Response getBillingServiceProperties(@Auth UserInfo userInfo) {
        return Response
                .ok(changePropertiesService.readFileAsString(BILLING_SERVICE_PROP_PATH, BILLING_SERVICE))
                .build();
    }

    @POST
    @Path("/provisioning-service")
    public Response overwriteProvisioningServiceProperties(@Auth UserInfo userInfo, YmlDTO ymlDTO) {
        changePropertiesService.writeFileFromString(ymlDTO.getYmlString(), PROVISIONING_SERVICE, PROVISIONING_SERVICE_PROP_PATH);
        return Response.ok().build();
    }

    @POST
    @Path("/billing")
    public Response overwriteBillingServiceProperties(@Auth UserInfo userInfo, YmlDTO ymlDTO) {
        changePropertiesService.writeFileFromString(ymlDTO.getYmlString(), BILLING_SERVICE, BILLING_SERVICE_PROP_PATH);
        return Response.ok().build();
    }

    @POST
    @Path("/restart")
    public Response restart(@Auth UserInfo userInfo, RestartForm restartForm) {
        return Response.ok(changePropertiesService.restart(restartForm))
                .build();
    }
}
