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

import com.epam.datalab.exceptions.DatalabException;
import org.bson.Document;

import static com.epam.datalab.backendapi.dao.MongoCollections.DOCKER_ATTEMPTS;

/**
 * DAO write attempt of Docker
 */
public class DockerDAO extends BaseDAO {
    public static final String RUN = "run";

    /**
     * Write the attempt of docker action.
     *
     * @param user   user name.
     * @param action action of docker.
     * @throws DatalabException may be thrown
     */
    public void writeDockerAttempt(String user, String action) {
        insertOne(DOCKER_ATTEMPTS, () -> new Document(USER, user).append("action", action));
    }
}
