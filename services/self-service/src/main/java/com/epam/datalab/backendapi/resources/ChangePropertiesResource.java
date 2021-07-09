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
import com.epam.datalab.backendapi.conf.SelfServiceApplicationConfiguration;
import com.epam.datalab.backendapi.dao.EndpointDAO;
import com.epam.datalab.backendapi.roles.UserRoles;
import com.epam.datalab.exceptions.ResourceNotFoundException;
import com.epam.datalab.properties.ChangePropertiesConst;
import com.epam.datalab.properties.ExternalChangeProperties;
import com.epam.datalab.properties.RestartForm;
import com.epam.datalab.properties.YmlDTO;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/config")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Slf4j
public class ChangePropertiesResource implements ChangePropertiesConst {

    private final EndpointDAO endpointDAO;
    private final ExternalChangeProperties externalChangeProperties;
    private final String deployedOn;

    @Inject
    public ChangePropertiesResource(EndpointDAO endpointDAO, ExternalChangeProperties externalChangeProperties,
                                    SelfServiceApplicationConfiguration selfServiceApplicationConfiguration) {
        this.endpointDAO = endpointDAO;
        this.externalChangeProperties = externalChangeProperties;
        deployedOn = selfServiceApplicationConfiguration.getDeployed();
    }

    @GET
    @Path("/multiple")
    public Response getAllPropertiesForEndpoint(@Auth UserInfo userInfo, @QueryParam("endpoint") String endpoint) {
        if (UserRoles.isAdmin(userInfo)) {
            String url = findEndpointDTOUrl(endpoint) + ChangePropertiesConst.BASE_CONFIG_URL;
            return Response
                    .ok(externalChangeProperties.getPropertiesWithExternal(endpoint, userInfo, url))
                    .build();
        } else {
            return Response
                    .status(Response.Status.FORBIDDEN)
                    .build();
        }
    }

    @POST
    @Path("/multiple/self-service")
    public Response overwriteExternalSelfServiceProperties(@Auth UserInfo userInfo, YmlDTO ymlDTO) {
        if (UserRoles.isAdmin(userInfo)) {
            if (deployedOn.equals("GKE")) {
                externalChangeProperties.overwritePropertiesWithExternal(GKE_SELF_SERVICE_PATH, GKE_SELF_SERVICE,
                        ymlDTO, userInfo, null);
            } else {
                externalChangeProperties.overwritePropertiesWithExternal(SELF_SERVICE_PROP_PATH, SELF_SERVICE,
                        ymlDTO, userInfo, null);
            }
            return Response.status(Response.Status.OK).build();
        } else {
            return Response
                    .status(Response.Status.FORBIDDEN)
                    .build();
        }
    }

    @POST
    @Path("/multiple/provisioning-service")
    public Response overwriteExternalProvisioningServiceProperties(@Auth UserInfo userInfo, YmlDTO ymlDTO) {
        if (UserRoles.isAdmin(userInfo)) {
            String url = findEndpointDTOUrl(ymlDTO.getEndpointName()) + BASE_CONFIG_URL;
            externalChangeProperties.overwritePropertiesWithExternal(PROVISIONING_SERVICE_PROP_PATH, PROVISIONING_SERVICE,
                    ymlDTO, userInfo, url);
            return Response.status(Response.Status.OK).build();
        } else {
            return Response
                    .status(Response.Status.FORBIDDEN)
                    .build();
        }
    }

    @POST
    @Path("/multiple/billing")
    public Response overwriteExternalBillingProperties(@Auth UserInfo userInfo, YmlDTO ymlDTO) {
        if (UserRoles.isAdmin(userInfo)) {
            String url = findEndpointDTOUrl(ymlDTO.getEndpointName()) + BASE_CONFIG_URL;
            externalChangeProperties.overwritePropertiesWithExternal(BILLING_SERVICE_PROP_PATH, BILLING_SERVICE,
                    ymlDTO, userInfo, url);
            return Response.status(Response.Status.OK).build();
        } else {
            return Response
                    .status(Response.Status.FORBIDDEN)
                    .build();
        }
    }


    @POST
    @Path("/multiple/restart")
    public Response restartWithExternal(@Auth UserInfo userInfo, RestartForm restartForm) {
        if (UserRoles.isAdmin(userInfo)) {
            if (deployedOn.equals("GKE")) {
                externalChangeProperties.restartForExternalForGKE(userInfo, restartForm);
            } else {
                String url = findEndpointDTOUrl(restartForm.getEndpoint())
                        + ChangePropertiesConst.RESTART_URL;
                externalChangeProperties.restartForExternal(restartForm, userInfo, url);
            }
            return Response.ok().build();
        } else {
            return Response
                    .status(Response.Status.FORBIDDEN)
                    .build();
        }
    }

    private String findEndpointDTOUrl(String endpointName) {
        return endpointDAO.get(endpointName)
                .orElseThrow(() -> new ResourceNotFoundException("Endpoint with name " + endpointName
                        + " not found")).getUrl();
    }
}