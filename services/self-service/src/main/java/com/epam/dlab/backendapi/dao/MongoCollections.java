/***************************************************************************

Copyright (c) 2016, EPAM SYSTEMS INC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

****************************************************************************/

package com.epam.dlab.backendapi.dao;

/** Names of Mongo collections. */
public interface MongoCollections {
	/** Environment settings. */
    String SETTINGS = "settings";
    /** Attempts of the user login into DLab. */
    String LOGIN_ATTEMPTS = "loginAttempts";
    /** Attempts the actions of docker. */
    String DOCKER_ATTEMPTS = "dockerAttempts";
    /** User keys and credentials. */
    String USER_KEYS = "userKeys";
    /** User AWS credentials. */
    String USER_EDGE = "userCloudCredentials";
    /** Instances of user. */
    String USER_INSTANCES = "userInstances";
    /** Name of shapes. */
    String SHAPES = "shapes";
    String USER_UI_SETTINGS = "userUISettings";
    /* Billing data. */
    String BILLING = "billing";
    /** User roles. */
    String ROLES = "roles";
    /** GIT credentials of user. */
    String GIT_CREDS = "gitCreds";
    /** RequestId */
    String REQUEST_ID = "requestId";
    /** Images */
    String IMAGES = "images";
	/**
	 * Backup
	 */
    String BACKUPS = "backup";
}
