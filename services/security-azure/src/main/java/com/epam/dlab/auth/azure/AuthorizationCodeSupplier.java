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

package com.epam.dlab.auth.azure;

import com.epam.dlab.auth.conf.AzureLoginConfiguration;
import com.epam.dlab.dto.azure.auth.AuthorizationCodeFlowResponse;
import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationResult;

import java.net.URI;
import java.util.concurrent.Future;

class AuthorizationCodeSupplier extends AuthorizationSupplier {
    private final AuthorizationCodeFlowResponse response;

    AuthorizationCodeSupplier(AzureLoginConfiguration azureLoginConfiguration,
                              AuthorizationCodeFlowResponse response) {
        super(azureLoginConfiguration);
        this.response = response;
    }

    public Future<AuthenticationResult> get(AuthenticationContext context, String resource) {
        return context
                .acquireTokenByAuthorizationCode(response.getCode(), resource, azureLoginConfiguration.getClientId(),
                        URI.create(azureLoginConfiguration.getRedirectUrl()), null);
    }
}