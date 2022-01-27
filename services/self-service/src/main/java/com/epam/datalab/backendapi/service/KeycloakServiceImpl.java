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

package com.epam.datalab.backendapi.service;

import com.epam.datalab.backendapi.conf.SelfServiceApplicationConfiguration;
import com.epam.datalab.backendapi.dao.SecurityDAO;
import com.epam.datalab.backendapi.util.KeycloakUtil;
import com.epam.datalab.exceptions.DatalabException;
import com.google.inject.Inject;
import de.ahus1.keycloak.dropwizard.KeycloakConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.internal.util.Base64;
import org.keycloak.representations.AccessTokenResponse;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;

@Slf4j
public class KeycloakServiceImpl implements KeycloakService {

    private static final String URI = "/realms/%s/protocol/openid-connect/token";
    private static final String GRANT_TYPE = "grant_type";
    private final Client httpClient;
    private final KeycloakConfiguration conf;
    private final SecurityDAO securityDAO;
    private final String redirectUri;

    @Inject
    public KeycloakServiceImpl(Client httpClient, SelfServiceApplicationConfiguration conf, SecurityDAO securityDAO) {
        this.httpClient = httpClient;
        this.conf = conf.getKeycloakConfiguration();
        this.securityDAO = securityDAO;
        this.redirectUri = conf.getKeycloakConfiguration().getRedirectUri();
    }

    @Override
    public AccessTokenResponse getToken(String code) {
        return requestToken(accessTokenRequestForm(code));
    }

    @Override
    public AccessTokenResponse refreshToken(String refreshToken) {
        return requestToken(refreshTokenRequestForm(refreshToken));
    }

    @Override
    public AccessTokenResponse generateAccessToken(String refreshToken) {
        AccessTokenResponse tokenResponse = refreshToken(refreshToken);
        final String username = KeycloakUtil.parseToken(tokenResponse.getToken()).getPreferredUsername();
        if (username.contains("auto")) {
            throw new DatalabException("can not generate Access token for user with: auto, in username");
        }
        securityDAO.updateUser(username, tokenResponse);
        return tokenResponse;
    }

    @Override
    public AccessTokenResponse generateServiceAccountToken() {
        return requestToken(serviceAccountRequestForm());
    }

    private AccessTokenResponse requestToken(Form requestForm) {
        final String credentials = Base64.encodeAsString(String.join(":", conf.getResource(),
                String.valueOf(conf.getCredentials().get("secret"))));
        String url = conf.getAuthServerUrl() + String.format(URI, conf.getRealm());
        String header = "Basic " + credentials;
        final Response response =
                httpClient.target(url)
                        .request()
                        .header(HttpHeaders.AUTHORIZATION, header)
                        .post(Entity.form(requestForm));
        if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {

            log.error("Error getting token:code {}, body {}", response.getStatus(), response.readEntity(String.class));
            throw new DatalabException("can not get token");
        }
        return response.readEntity(AccessTokenResponse.class);
    }

    private Form accessTokenRequestForm(String code) {
        return new Form()
                .param(GRANT_TYPE, "authorization_code")
                .param("code", code)
                .param("redirect_uri", redirectUri);
    }

    private Form refreshTokenRequestForm(String refreshToken) {
        return new Form()
                .param(GRANT_TYPE, "refresh_token")
                .param("refresh_token", refreshToken);
    }

    private Form serviceAccountRequestForm() {
        return new Form()
                .param(GRANT_TYPE, "client_credentials");
    }
}
