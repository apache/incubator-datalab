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

package com.epam.dlab.auth.azure.service;

import com.epam.dlab.auth.azure.AuthorizationSupplier;
import com.epam.dlab.auth.azure.AzureLocalAuthResponse;
import com.microsoft.aad.adal4j.AuthenticationResult;

import javax.ws.rs.core.Response;

public interface AzureAuthorizationCodeService {

    /**
     * Authenticates user that provided by <code>authorizationSupplier</code>
     *
     * @param authorizationSupplier contains user info that is used for authentication
     * @return response {@link Response} with proper status {@link Response.Status} that means result of
     * the user authentication.
     */
    AzureLocalAuthResponse authenticateAndLogin(AuthorizationSupplier authorizationSupplier);

    /**
     * Verifies if user has permissions to configured scope in configuration file
     *
     * @param authenticationResult result retrieved after authentication against Azure platform
     * @return <code>true</code> if user is allowed, <code>false</code> otherwise
     */
    boolean validatePermissions(AuthenticationResult authenticationResult);
}
