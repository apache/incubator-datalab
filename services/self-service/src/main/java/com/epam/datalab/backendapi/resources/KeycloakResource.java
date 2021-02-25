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
import com.epam.datalab.backendapi.conf.KeycloakConfiguration;
import com.epam.datalab.backendapi.conf.SelfServiceApplicationConfiguration;
import com.epam.datalab.backendapi.dao.SecurityDAO;
import com.epam.datalab.backendapi.roles.UserRoles;
import com.epam.datalab.backendapi.service.KeycloakService;
import com.epam.datalab.backendapi.service.SecurityService;
import com.epam.datalab.exceptions.DatalabException;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;
import org.keycloak.representations.AccessTokenResponse;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.net.URI;
import java.net.URISyntaxException;

import static java.lang.String.format;

@Path("/oauth")
@Slf4j
public class KeycloakResource {
    private static final String LOGIN_URI_FORMAT = "%s/realms/%s/protocol/openid-connect/auth?client_id=%s" +
            "&redirect_uri=%s&response_type=code";
    private static final String KEYCLOAK_LOGOUT_URI_FORMAT = "%s/realms/%s/protocol/openid-connect/logout" +
            "?redirect_uri=%s";
    private final SecurityService securityService;
    private final KeycloakService keycloakService;
    private final SecurityDAO securityDAO;
    private final String loginUri;
    private final String logoutUri;
    private final String redirectUri;
    private final boolean defaultAccess;

    @Inject
    public KeycloakResource(SecurityService securityService, SelfServiceApplicationConfiguration configuration,
                            SecurityDAO securityDAO, KeycloakService keycloakService) {
        this.securityDAO = securityDAO;
        this.defaultAccess = configuration.getRoleDefaultAccess();
        final KeycloakConfiguration keycloakConfiguration = configuration.getKeycloakConfiguration();
        this.redirectUri = keycloakConfiguration.getRedirectUri();
        this.securityService = securityService;
        this.keycloakService = keycloakService;

        loginUri = format(LOGIN_URI_FORMAT,
                keycloakConfiguration.getAuthServerUrl(),
                keycloakConfiguration.getRealm(),
                keycloakConfiguration.getResource(),
                redirectUri);
        logoutUri = format(KEYCLOAK_LOGOUT_URI_FORMAT,
                keycloakConfiguration.getAuthServerUrl(),
                keycloakConfiguration.getRealm(),
                redirectUri);
    }

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public Response getLoginUri() throws URISyntaxException {
        return Response.ok(new URI(loginUri).toString())
                .build();
    }

    @POST
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUser(@QueryParam("code") String code) {
        return Response.ok(securityService.getUserInfo(code)).build();
    }

    @POST
    @Path("/authorize")
    public Response authorize(@Auth UserInfo userInfo) {
        UserRoles.initialize(securityDAO, defaultAccess);
        return Response.ok().build();
    }

    @GET
    @Path("/logout")
    public Response getLogoutUrl() throws URISyntaxException {
        return Response.noContent()
                .location(new URI(logoutUri))
                .build();
    }

    @POST
    @Path("/refresh/{refresh_token}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response refreshAccessToken(@PathParam("refresh_token") String refreshToken) throws URISyntaxException {
        AccessTokenResponse tokenResponse;
        try {
                tokenResponse = keycloakService.generateAccessToken(refreshToken);
        } catch (DatalabException e) {
            log.error("Cannot refresh token due to: {}", e.getMessage(), e);
            return Response.status(Response.Status.BAD_REQUEST)
                    .location(new URI(logoutUri))
                    .build();
        }
        return Response.ok(new TokenInfo(tokenResponse.getToken(), tokenResponse.getRefreshToken())).build();
    }

    class TokenInfo {
        @JsonProperty("access_token")
        private final String accessToken;
        @JsonProperty("refresh_token")
        private final String refreshToken;

        TokenInfo(String accessToken, String refreshToken) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }
    }
}
