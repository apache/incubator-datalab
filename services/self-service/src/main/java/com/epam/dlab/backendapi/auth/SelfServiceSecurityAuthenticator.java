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

package com.epam.dlab.backendapi.auth;

import com.epam.dlab.auth.SecurityRestAuthenticator;
import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.domain.EnvStatusListener;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.dropwizard.auth.AuthenticationException;
import lombok.extern.slf4j.Slf4j;

import java.util.Optional;

@Slf4j
@Singleton
public class SelfServiceSecurityAuthenticator extends SecurityRestAuthenticator {
    private final EnvStatusListener envStatusListener;

    @Inject
    public SelfServiceSecurityAuthenticator(EnvStatusListener envStatusListener) {
        this.envStatusListener = envStatusListener;
    }

    @Override
    public Optional<UserInfo> authenticate(String credentials) throws AuthenticationException {
        Optional<UserInfo> userInfo = super.authenticate(credentials);

        if (userInfo.isPresent()) {
            UserInfo ui = userInfo.get();

            // Touch session
            UserInfo touched = envStatusListener.getSession(ui.getName());
            if (touched == null) {
                log.warn("Session does not exist for for env status listener {} {}", ui.getName(), ui.getAccessToken());
            }
        }

        return userInfo;
    }
}
