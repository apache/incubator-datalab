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

package com.epam.datalab.backendapi.dao;

/**
 * Names of Mongo collections.
 */
public class MongoCollections {
    /**
     * Environment settings.
     */
    public static final String SETTINGS = "settings";
    /**
     * Attempts of the user login into DataLab.
     */
    static final String LOGIN_ATTEMPTS = "loginAttempts";
    /**
     * Attempts the actions of docker.
     */
    static final String DOCKER_ATTEMPTS = "dockerAttempts";
    /**
     * User keys and credentials.
     */
    static final String USER_KEYS = "userKeys";
    /**
     * User AWS credentials.
     */
    public static final String USER_EDGE = "userCloudCredentials";
    /**
     * Instances of user.
     */
    public static final String USER_INSTANCES = "userInstances";
    /**
     * Name of shapes.
     */
    public static final String SHAPES = "shapes";
    static final String USER_SETTINGS = "userSettings";
    /* Billing data. */
    public static final String BILLING = "billing";
    /**
     * User roles.
     */
    static final String ROLES = "roles";
    /**
     * GIT credentials of user.
     */
    public static final String GIT_CREDS = "gitCreds";
    /**
     * RequestId
     */
    static final String REQUEST_ID = "requestId";
    /**
     * Images
     */
    public static final String IMAGES = "images";
    /**
     * Backup
     */
    public static final String BACKUPS = "backup";

    public static final String USER_GROUPS = "userGroups";

    private MongoCollections() {
    }
}
