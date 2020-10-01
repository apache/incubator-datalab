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

import com.epam.datalab.dto.exploratory.ExploratoryGitCreds;
import com.epam.datalab.dto.exploratory.ExploratoryGitCredsDTO;
import org.bson.Document;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.epam.datalab.backendapi.dao.MongoCollections.GIT_CREDS;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Projections.excludeId;
import static com.mongodb.client.model.Projections.fields;
import static com.mongodb.client.model.Projections.include;

/**
 * DAO for user exploratory.
 */
public class GitCredsDAO extends BaseDAO {
    private static final String FIELD_GIT_CREDS = "git_creds";

    /**
     * Find and return the list of GIT credentials for user.
     *
     * @param user name.
     * @return GIT credentials DTO
     */
    public ExploratoryGitCredsDTO findGitCreds(String user) {
        return findGitCreds(user, false);
    }

    /**
     * Find and return the list of GIT credentials for user.
     *
     * @param user          name.
     * @param clearPassword clear user password if set to <b>true</b>.
     * @return GIT credentials DTO
     */
    public ExploratoryGitCredsDTO findGitCreds(String user, boolean clearPassword) {
        Optional<ExploratoryGitCredsDTO> opt = findOne(GIT_CREDS,
                eq(ID, user),
                fields(include(FIELD_GIT_CREDS), excludeId()),
                ExploratoryGitCredsDTO.class);
        ExploratoryGitCredsDTO creds = (opt.orElseGet(ExploratoryGitCredsDTO::new));
        List<ExploratoryGitCreds> list = creds.getGitCreds();
        if (clearPassword && list != null) {
            for (ExploratoryGitCreds cred : list) {
                cred.setPassword(null);
            }
        }

        return creds;
    }

    /**
     * Update the GIT credentials for user.
     *
     * @param user name.
     * @param dto  GIT credentials.
     */
    public void updateGitCreds(String user, ExploratoryGitCredsDTO dto) {
        List<ExploratoryGitCreds> list = findGitCreds(user).getGitCreds();
        if (list != null && dto.getGitCreds() != null) {
            Collections.sort(dto.getGitCreds());
            // Restore passwords from Mongo DB.
            for (ExploratoryGitCreds cred : dto.getGitCreds()) {
                if (cred.getPassword() == null) {
                    int index = Collections.binarySearch(list, cred);
                    if (index >= 0) {
                        cred.setPassword(list.get(index).getPassword());
                    }
                }
            }
        }

        Document d = new Document(SET, convertToBson(dto).append(ID, user));
        updateOne(GIT_CREDS, eq(ID, user), d, true);
    }
}