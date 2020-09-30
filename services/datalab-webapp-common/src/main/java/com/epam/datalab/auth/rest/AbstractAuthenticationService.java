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


package com.epam.datalab.auth.rest;

import com.epam.datalab.auth.UserInfo;
import com.epam.datalab.auth.dto.UserCredentialDTO;
import io.dropwizard.Configuration;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.Response;
import java.util.UUID;

public abstract class AbstractAuthenticationService<C extends Configuration> extends ConfigurableResource<C> {

    public AbstractAuthenticationService(C config) {
        super(config);
    }

    public static String getRandomToken() {
        return UUID.randomUUID().toString();
    }

    public abstract Response login(UserCredentialDTO credential, HttpServletRequest request);

    public abstract UserInfo getUserInfo(String accessToken, HttpServletRequest request);

    public abstract Response logout(String accessToken);

}
