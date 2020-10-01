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
import io.dropwizard.auth.Authorizer;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Date;

@Slf4j
public final class UserSessionDurationAuthorizer implements Authorizer<UserInfo> {
    public static final String SHORT_USER_SESSION_DURATION = "SHORT_USER_SESSION";
    private final long maxSessionDurabilityMilliseconds;
    private UserSessionDurationCallback callback;

    public UserSessionDurationAuthorizer(UserSessionDurationCallback callback, long maxSessionDurabilityMilliseconds) {
        this.callback = callback;
        this.maxSessionDurabilityMilliseconds = maxSessionDurabilityMilliseconds;
    }

    @Override
    public boolean authorize(UserInfo principal, String role) {
        if (SHORT_USER_SESSION_DURATION.equalsIgnoreCase(role)) {
            try {
                String refreshToken = principal.getKeys().get("refresh_token");
                String createdDateOfRefreshToken = principal.getKeys().get("created_date_of_refresh_token");

                if (StringUtils.isEmpty(refreshToken)) {
                    log.info("Refresh token is empty for user {}", principal.getName());
                    return false;
                }

                if (StringUtils.isEmpty(createdDateOfRefreshToken)) {
                    log.info("Created date for refresh token is empty for user {}", principal.getName());
                    return false;
                }

                log.debug("refresh token requested {} and current date is {}",
                        new Date(Long.valueOf(createdDateOfRefreshToken)), new Date());

                long passedTime = System.currentTimeMillis() - Long.valueOf(createdDateOfRefreshToken);

                log.info("Passed time of session for user {} is {} milliseconds", principal.getName(), passedTime);
                if (passedTime > maxSessionDurabilityMilliseconds) {

                    silentCallbackExecution(principal);

                    log.info("Re-login required for user {}", principal.getName());
                    return false;
                }

                return true;
            } catch (RuntimeException e) {
                log.error("Cannot verify durability of session for user {}", principal.getName(), e);
                return false;
            }

        }

        return true;
    }

    private void silentCallbackExecution(UserInfo principal) {
        log.info("Log out expired user {}", principal.getName());
        try {
            callback.onSessionExpired(principal);
        } catch (RuntimeException e) {
            log.warn("Error during logout user {}", principal.getName(), e);
        }
    }
}
