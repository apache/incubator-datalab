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
package com.epam.datalab.backendapi.resources.azure;

import com.epam.datalab.auth.contract.SecurityAPI;
import com.epam.datalab.constants.ServiceConsts;
import com.epam.datalab.dto.azure.auth.AuthorizationCodeFlowResponse;
import com.epam.datalab.rest.client.RESTService;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;

@Path("/user/azure")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AzureOauthResource {

    @Inject
    @Named(ServiceConsts.SECURITY_SERVICE_NAME)
    private RESTService securityService;


    @GET
    @Path("/init")
    public Response redirectedUrl() {
        return Response.seeOther(URI.create(securityService.get(SecurityAPI.INIT_LOGIN_OAUTH_AZURE, String.class)))
                .build();
    }

    @POST
    @Path("/oauth")
    public Response login(AuthorizationCodeFlowResponse codeFlowResponse) {
        return securityService.post(SecurityAPI.LOGIN_OAUTH_AZURE, codeFlowResponse, Response.class);
    }

}
