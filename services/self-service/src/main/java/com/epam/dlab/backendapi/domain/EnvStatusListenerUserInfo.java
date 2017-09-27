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

package com.epam.dlab.backendapi.domain;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.util.RequestBuilder;
import com.epam.dlab.dto.UserEnvironmentResources;

/**
 * Store info for the requests about the environment status of user.
 */
public class EnvStatusListenerUserInfo {
    /**
     * Time for the next check in milliseconds.
     */
    private long nextCheckTimeMillis;
    /**
     * Name of user.
     */
    private String username;
    /**
     * Access token for request provisioning service.
     */
    private String accessToken;
    private UserEnvironmentResources dto;

    EnvStatusListenerUserInfo(UserInfo userInfo) {
        this.nextCheckTimeMillis = System.currentTimeMillis();
        this.accessToken = userInfo.getAccessToken();
        this.username = userInfo.getName();
        this.dto = RequestBuilder.newUserEnvironmentStatus(userInfo);
    }

    /**
     * Return the time for next check of environment statuses.
     */
    long getNextCheckTimeMillis() {
        return nextCheckTimeMillis;
    }

    /**
     * Set the time for next check of environment statuses.
     *
     * @param nextCheckTimeMillis the time for next check.
     */
    void setNextCheckTimeMillis(long nextCheckTimeMillis) {
        this.nextCheckTimeMillis = nextCheckTimeMillis;
    }

    /**
     * Return the name of user.
     */
    public String getUsername() {
        return username;
    }

    /**
     * Return the access token for requests to Provisioning Service.
     */
    public String getAccessToken() {
        return accessToken;
    }

    /**
     * Return the DTO object for check of environment statuses.
     */
    UserEnvironmentResources getDTO() {
        return dto;
    }
}