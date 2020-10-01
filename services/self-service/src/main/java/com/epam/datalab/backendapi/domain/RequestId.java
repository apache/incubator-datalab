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

package com.epam.datalab.backendapi.domain;

import com.epam.datalab.backendapi.dao.RequestIdDAO;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.dropwizard.util.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.UUID;

/**
 * Stores and checks the id of requests for Provisioning Service.
 */
@Singleton
public class RequestId {
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestId.class);

    /**
     * Timeout in milliseconds when the request id is out of date.
     */
    private static final long EXPIRED_TIMEOUT_MILLIS = Duration.hours(12).toMilliseconds();

    @Inject
    private RequestIdDAO dao;

    /**
     * Add the request id for user.
     *
     * @param username the name of user.
     * @param uuid     UUID.
     */
    public String put(String username, String uuid) {
        LOGGER.trace("Register request id {} for user {}", uuid, username);
        dao.put(new RequestIdDTO()
                .withId(uuid)
                .withUser(username)
                .withRequestTime(new Date())
                .withExpirationTime(new Date(System.currentTimeMillis() + EXPIRED_TIMEOUT_MILLIS)));
        return uuid;
    }

    /**
     * Generate, add and return new UUID.
     *
     * @param username the name of user.
     * @return new UUID
     */
    public String get(String username) {
        return put(UUID.randomUUID().toString(), username);
    }

    /**
     * Remove UUID if it exist.
     *
     * @param uuid UUID.
     */
    public void remove(String uuid) {
        LOGGER.trace("Unregister request id {}", uuid);
        dao.delete(uuid);
    }

    /**
     * Check and remove UUID, if it not exists throw exception.
     *
     * @param uuid UUID.
     * @return username
     */
    public String checkAndRemove(String uuid) {
        String username = dao.get(uuid).getUser();
        LOGGER.trace("Unregister request id {} for user {}", uuid, username);
        dao.delete(uuid);
        return username;
    }
}
