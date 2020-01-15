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

package com.epam.dlab.backendapi.dao.impl;

import com.epam.dlab.backendapi.dao.BaseDAO;
import com.epam.dlab.backendapi.dao.EndpointDAO;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.mongodb.client.model.Filters.eq;

public class EndpointDAOImpl extends BaseDAO implements EndpointDAO {
    private static final String SELFSERVICE_COLLECTION = "selfservice";
    private static final String NAME = "name";
    private static final String URL = "url";


    @Override
    public Optional<Document> findOne(String name) {
        return findOne(SELFSERVICE_COLLECTION, eq(NAME, name));
    }

    @Override
    public List<Document> findAll() {
        return find(SELFSERVICE_COLLECTION).into(new ArrayList<>());
    }

    @Override
    public void create(String name, String url) {
        Document document = new Document(URL, url);
        document.append(NAME, name);
        insertOne(SELFSERVICE_COLLECTION, document);
    }
}
