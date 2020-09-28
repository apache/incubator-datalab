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

package com.epam.datalab.backendapi.auth;

import com.epam.datalab.auth.UserInfo;
import de.ahus1.keycloak.dropwizard.AbstractKeycloakAuthenticator;
import de.ahus1.keycloak.dropwizard.KeycloakConfiguration;
import org.keycloak.KeycloakSecurityContext;
import org.keycloak.representations.AccessToken;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static java.util.Collections.emptyList;

public class KeycloakAuthenticator extends AbstractKeycloakAuthenticator<UserInfo> {

    private static final String GROUPS_CLAIM = "groups";

    public KeycloakAuthenticator(KeycloakConfiguration keycloakConfiguration) {
        super(keycloakConfiguration);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected UserInfo prepareAuthentication(KeycloakSecurityContext keycloakSecurityContext,
                                             HttpServletRequest httpServletRequest,
                                             KeycloakConfiguration keycloakConfiguration) {
        final AccessToken token = keycloakSecurityContext.getToken();
        final UserInfo userInfo = new UserInfo(token.getPreferredUsername(),
                keycloakSecurityContext.getTokenString());
        userInfo.addRoles((List<String>) token.getOtherClaims().getOrDefault(GROUPS_CLAIM, emptyList()));
        return userInfo;
    }
}
