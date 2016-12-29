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

import com.epam.dlab.backendapi.dao.databind.IsoDateModule;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.mongo.MongoService;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.model.InsertOneOptions;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.io.IOException;
import java.util.*;
import java.util.function.Supplier;

import static com.mongodb.client.model.Aggregates.unwind;

class BaseDAO implements MongoCollections {
    protected static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true)
            .registerModule(new IsoDateModule());
    public static final String FIELD_DELIMETER = ".";
    public static final String FIELD_SET_DELIMETER = ".$.";
    public static final String FIELD_PROJECTION_DELIMITER = "$";
    public static final String ID = "_id";
    public static final String USER = "user";
    public static final String STATUS = "status";
    public static final String TIMESTAMP = "timestamp";

    @Inject
    protected MongoService mongoService;

    protected void insertOne(String collection, Supplier<Document> supplier) {
        insertOne(collection, supplier, generateUUID());
    }

    protected void insertOne(String collection, Supplier<Document> supplier, String uuid) {
        mongoService.getCollection(collection).insertOne(supplier.get()
                .append(ID, uuid)
                .append(TIMESTAMP, new Date()));
    }

    protected void insertOne(String collection, Object object) {
        insertOne(collection, object, generateUUID());
    }

    protected void insertOne(String collection, Object object, String uuid) {
        mongoService.getCollection(collection).insertOne(convertToBson(object)
                .append(ID, uuid)
                .append(TIMESTAMP, new Date()));
    }

    protected UpdateResult updateOne(String collection, Bson condition, Bson value) {
        return mongoService.getCollection(collection).updateOne(condition, value);
    }

    protected UpdateResult updateMany(String collection, Bson condition, Bson value) {
        return mongoService.getCollection(collection).updateMany(condition, value);
    }

    protected FindIterable<Document> find(String collection,
                                          Bson condition) {
        return mongoService.getCollection(collection)
                .find(condition);
    }

    protected FindIterable<Document> find(String collection,
                                          Bson condition,
                                          Bson projection) {
        return mongoService.getCollection(collection)
                .find(condition)
                .projection(projection);
    }

    AggregateIterable<Document> aggregate(String collection,
                                          List<? extends Bson> pipeline) {
        return mongoService.getCollection(collection)
                .aggregate(pipeline);
    }

    private Optional<Document> limitOne(MongoIterable<Document> documents) {
        Document first = documents.first();
        MongoCursor<Document> iterator = null;
        try {
            iterator = documents.iterator();
            if(iterator.hasNext()) {
                iterator.next();
                if(iterator.hasNext()) {
                    throw new DlabException("too many items found while one is expected");
                }
            }
        }
        finally {
            if(iterator != null) {
                iterator.close();
            }
        }
        return first == null ? Optional.empty() : Optional.ofNullable(first);
    }

    protected Optional<Document> findOne(String collection,
                               Bson condition) {
        FindIterable<Document> found = find(collection, condition);
        return limitOne(found);
    }

    protected Optional<Document> findOne(String collection,
                               Bson condition,
                               Bson projection) {
        FindIterable<Document> found = find(collection, condition, projection);
        return limitOne(found);
    }

    protected Document convertToBson(Object object) {
        try {
            return Document.parse(MAPPER.writeValueAsString(object));
        } catch (IOException e) {
            throw new DlabException("error converting to bson", e);
        }
    }

    protected <T> Optional<T> findOne(String collection, Bson eq, Class<T> clazz) {
        Optional<Document> doc = findOne(collection, eq);
        return doc.isPresent() ? Optional.ofNullable(convertFromDocument(doc.get(), clazz)) : Optional.empty();
    }

    protected <T> Optional<T> findOne(String collection, Bson eq, Bson projection, Class<T> clazz) {
        Optional<Document> doc = findOne(collection, eq, projection);
        return doc.isPresent() ? Optional.ofNullable(convertFromDocument(doc.get(), clazz)) : Optional.empty();
    }

    Optional<Document> aggregateOne(String collection,
                                          List<? extends Bson> pipeline) {
        MongoIterable<Document> found = aggregate(collection, pipeline);
        return limitOne(found);
    }

    private <T> T convertFromDocument(Document document, Class<T> clazz) {
        try {
            String json = document.toJson();
            return MAPPER.readValue(json, clazz);
        } catch (IOException e) {
            throw new DlabException("error converting from document with id " + document.get(ID), e);
        }
    }

    private String generateUUID() {
        return UUID.randomUUID().toString();
    }

    static Bson unwindField(String fieldName) {
        return unwind(FIELD_PROJECTION_DELIMITER + fieldName);
    }

    static Object getDotted(Document d, String fieldName) {
        if(fieldName.isEmpty()) {
            return null;
        }
        final String[] fieldParts = StringUtils.split(fieldName, '.');
        Object val = d.get(fieldParts[0]);
        for(int i = 1; i < fieldParts.length; ++i) {
            if(fieldParts[i].equals("$")
                    && val instanceof ArrayList) {
                 ArrayList array = (ArrayList) val;
                if(array.isEmpty()) {
                    break;
                }
                else{
                    val = array.get(0);
                }
            } else if (val instanceof Document) {
                val = ((Document)val).get(fieldParts[i]);
            } else {
                break;
            }
        }
        return val;
    }

    static Object getDottedOrDefault(Document d, String fieldName, Object defaultValue) {
        Object result = getDotted(d, fieldName);
        return result == null ? defaultValue : result;
    }
}
