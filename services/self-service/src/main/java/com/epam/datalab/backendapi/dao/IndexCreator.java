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

import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.model.Indexes;
import io.dropwizard.lifecycle.Managed;

import static com.epam.datalab.backendapi.dao.ExploratoryDAO.EXPLORATORY_NAME;
import static com.epam.datalab.backendapi.dao.MongoCollections.USER_INSTANCES;

/**
 * Creates the indexes for mongo collections.
 */
public class IndexCreator extends BaseDAO implements Managed {
    private static final String PROJECT_FIELD = "project";

    @Override
    public void start() {
        mongoService.getCollection(USER_INSTANCES)
                .createIndex(Indexes.ascending(USER, EXPLORATORY_NAME, PROJECT_FIELD), new IndexOptions().unique(true));
    }

    @Override
    public void stop() {
        //Add some functionality if necessary
    }
}
