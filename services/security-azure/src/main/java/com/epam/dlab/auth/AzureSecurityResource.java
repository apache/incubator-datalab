/*
 * Copyright (c) 2017, EPAM SYSTEMS INC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.dlab.auth;

import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.rest.client.RESTService;
import com.epam.dlab.rest.contracts.SecurityAPI;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Path("/user/azure")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class AzureSecurityResource {

    private static final Object DUMMY = new Object();
    private final Cache<String, Object> cache = CacheBuilder.newBuilder().expireAfterWrite(4, TimeUnit.HOURS).maximumSize(10000).build();

    @Inject
    @Named(ServiceConsts.SECURITY_SERVICE_NAME)
    private RESTService securityService;

    @Inject
    private AzureLoginUrlBuilder azureLoginUrlBuilder;

    @GET
    @Path("/init")
    public Response login() {

        log.debug("Init oauth silent login flow");
        String uuid = UUID.randomUUID().toString();
        log.info("Register oauth state {}", uuid);
        cache.put(uuid, DUMMY);

        return Response.seeOther(URI.create(azureLoginUrlBuilder.buildSilentLoginUrl(uuid))).build();
    }

    @POST
    @Path("/oauth")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    public Response login(@BeanParam AuthorizationCodeFlowResponse authorizationCodeFlowResponse) {
        log.info("Authenticate client {}", authorizationCodeFlowResponse);
        if (authorizationCodeFlowResponse.isSuccessful()) {
            log.debug("Successfully received auth code {}", authorizationCodeFlowResponse);
            if (cache.getIfPresent(authorizationCodeFlowResponse.getState()) != null) {
                log.debug("Retrieving token from {}", authorizationCodeFlowResponse);
                Response response = securityService.post(SecurityAPI.LOGIN_OAUTH, authorizationCodeFlowResponse, Response.class);
                log.debug("Token retrieve response {}", response);
                if (response.getStatus() == Response.Status.OK.getStatusCode()
                        || response.getStatus() == Response.Status.UNAUTHORIZED.getStatusCode()) {

                    AzureLocalAuthResponse localAuthResponse = response.readEntity(AzureLocalAuthResponse.class);
                    log.debug("Token retrieve response {}", localAuthResponse);
                    return Response.ok(localAuthResponse).build();
                }
                return response;
            } else {
                log.warn("Malformed authorization code is retrieved for state {}", authorizationCodeFlowResponse);
            }
        } else {
            log.info("Check if silent authentication {}", authorizationCodeFlowResponse);
            if (cache.getIfPresent(authorizationCodeFlowResponse.getState()) != null
                    && "login_required".equals(authorizationCodeFlowResponse.getError())) {

                log.debug("Silent authentication detected {}", authorizationCodeFlowResponse);
                return Response.seeOther(URI.create(
                        azureLoginUrlBuilder.buildLoginUrl(authorizationCodeFlowResponse.getState()))).build();
            }
        }
        log.info("Try to log in one more time");
        return Response.seeOther(URI.create(azureLoginUrlBuilder.buildLoginUrl())).build();
    }
}

