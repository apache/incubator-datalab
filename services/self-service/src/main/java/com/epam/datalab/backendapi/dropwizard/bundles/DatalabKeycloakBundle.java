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

package com.epam.datalab.backendapi.dropwizard.bundles;

import com.epam.datalab.auth.UserInfo;
import com.epam.datalab.backendapi.auth.KeycloakAuthenticator;
import com.epam.datalab.backendapi.auth.SelfServiceSecurityAuthorizer;
import com.epam.datalab.backendapi.conf.SelfServiceApplicationConfiguration;
import com.google.inject.Inject;
import de.ahus1.keycloak.dropwizard.KeycloakBundle;
import de.ahus1.keycloak.dropwizard.KeycloakConfiguration;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.Authorizer;

import java.security.Principal;

public class DatalabKeycloakBundle extends KeycloakBundle<SelfServiceApplicationConfiguration> {

    @Inject
    private KeycloakAuthenticator authenticator;

    @Override
    protected KeycloakConfiguration getKeycloakConfiguration(SelfServiceApplicationConfiguration configuration) {
        return configuration.getKeycloakConfiguration();
    }

    @Override
    protected Class<? extends Principal> getUserClass() {
        return UserInfo.class;
    }

    @Override
    protected Authorizer createAuthorizer() {
        return new SelfServiceSecurityAuthorizer();
    }

    @Override
    protected Authenticator createAuthenticator(KeycloakConfiguration configuration) {
        return new KeycloakAuthenticator(configuration);
    }
}
