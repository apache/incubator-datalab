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
import com.epam.datalab.backendapi.domain.EndpointDTO;
import com.epam.datalab.cloud.CloudProvider;
import com.epam.datalab.rest.mappers.ResourceNotFoundExceptionMapper;
import io.dropwizard.auth.AuthDynamicFeature;
import io.dropwizard.auth.AuthValueFactoryProvider;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import io.dropwizard.auth.Authorizer;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;

import java.util.Optional;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestBase {

    protected final String TOKEN = "TOKEN";
    protected final String USER = "testUser";
    protected final String ENDPOINT_NAME = "local";
    protected final String ENDPOINT_URL = "http://localhost:8443/";
    protected final String ENDPOINT_ACCOUNT = "account";
    protected final String ENDPOINT_TAG = "tag";

    @SuppressWarnings("unchecked")
    private static Authenticator<String, UserInfo> authenticator = mock(Authenticator.class);
    @SuppressWarnings("unchecked")
    private static Authorizer<UserInfo> authorizer = mock(Authorizer.class);

    protected <T> ResourceTestRule getResourceTestRuleInstance(T resourceInstance) {
        return ResourceTestRule.builder()
                .bootstrapLogging(false)
                .setTestContainerFactory(new GrizzlyWebTestContainerFactory())
                .addProvider(new AuthDynamicFeature(new OAuthCredentialAuthFilter.Builder<UserInfo>()
                        .setAuthenticator(authenticator)
                        .setAuthorizer(authorizer)
                        .setRealm("SUPER SECRET STUFF")
                        .setPrefix("Bearer")
                        .buildAuthFilter()))
                .addProvider(RolesAllowedDynamicFeature.class)
                .addProvider(new ResourceNotFoundExceptionMapper())
                .addProvider(new AuthValueFactoryProvider.Binder<>(UserInfo.class))
                .addProvider(MultiPartFeature.class)
                .addResource(resourceInstance)
                .build();
    }

    protected void authSetup() throws AuthenticationException {
        when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(getUserInfo()));
        when(authorizer.authorize(any(), any())).thenReturn(true);
    }

    protected void authFailSetup() throws AuthenticationException {
        when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(getUserInfo()));
        when(authorizer.authorize(any(), any())).thenReturn(false);
    }

    protected UserInfo getUserInfo() {
        return new UserInfo(USER, TOKEN);
    }

    protected EndpointDTO getEndpointDTO() {
        return new EndpointDTO(ENDPOINT_NAME, ENDPOINT_URL, ENDPOINT_ACCOUNT, ENDPOINT_TAG, EndpointDTO.EndpointStatus.ACTIVE, CloudProvider.AWS);
    }
}
