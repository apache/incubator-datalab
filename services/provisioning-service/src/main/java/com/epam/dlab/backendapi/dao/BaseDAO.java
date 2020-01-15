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

package com.epam.dlab.backendapi.dao;

import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.mongo.MongoService;
import com.epam.dlab.util.mongo.modules.IsoDateModule;
import com.epam.dlab.util.mongo.modules.JavaPrimitiveModule;
import com.epam.dlab.util.mongo.modules.MongoModule;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.mongodb.MongoException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoIterable;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.io.IOException;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

@Slf4j
public class BaseDAO {
    public static final String ID = "_id";
    public static final String TIMESTAMP = "timestamp";

    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true)
            .registerModule(new IsoDateModule())
            .registerModule(new JavaPrimitiveModule())
            .registerModule(new MongoModule());

    @Inject
    protected MongoService mongoService;

    protected Optional<Document> findOne(String collection, Bson condition) {
        FindIterable<Document> found = find(collection, condition);
        return limitOne(found);
    }

    protected void insertOne(String collection, Object object) {
        insertOne(collection, object, generateUUID());
    }

    protected FindIterable<Document> find(String collection) {
        return mongoService.getCollection(collection).find();
    }

    private FindIterable<Document> find(String collection, Bson condition) {
        return mongoService.getCollection(collection).find(condition);
    }

    private Optional<Document> limitOne(MongoIterable<Document> documents) {
        Document first = documents.first();
        try (MongoCursor<Document> iterator = documents.iterator()) {
            if (iterator.hasNext()) {
                iterator.next();
                if (iterator.hasNext()) {
                    log.error("Too many items found while one is expected");
                    throw new DlabException("Too many items found while one is expected");
                }
            }
        }
        return Optional.ofNullable(first);
    }

    private void insertOne(String collection, Object object, String uuid) {
        try {
            mongoService.getCollection(collection)
                    .insertOne(convertToBson(object)
                            .append(ID, uuid)
                            .append(TIMESTAMP, new Date()));
        } catch (MongoException e) {
            log.error("Insert to Mongo DB fails: {}", e.getLocalizedMessage(), e);
            throw new DlabException("Insert to Mongo DB fails: " + e.getLocalizedMessage(), e);
        }
    }

    private Document convertToBson(Object object) {
        try {
            return Document.parse(MAPPER.writeValueAsString(object));
        } catch (IOException e) {
            throw new DlabException("error converting to bson", e);
        }
    }

    private String generateUUID() {
        return UUID.randomUUID().toString();
    }
}
