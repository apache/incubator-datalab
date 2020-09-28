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

import com.epam.datalab.dto.UserInstanceStatus;
import com.epam.datalab.exceptions.DatalabException;
import com.epam.datalab.mongo.MongoService;
import com.epam.datalab.util.mongo.modules.IsoDateModule;
import com.epam.datalab.util.mongo.modules.JavaPrimitiveModule;
import com.epam.datalab.util.mongo.modules.MongoModule;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.google.inject.Inject;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoException;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoIterable;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.exists;
import static com.mongodb.client.model.Filters.ne;

/**
 * Implements the base API for Mongo database.
 */
public class BaseDAO {

    public static final String ID = "_id";
    public static final String USER = "user";
    public static final String STATUS = "status";
    public static final String ERROR_MESSAGE = "error_message";
    protected static final String INSTANCE_ID = "instance_id";
    protected static final String EDGE_STATUS = "edge_status";
    protected static final String ADD_TO_SET = "$addToSet";
    protected static final String UNSET_OPERATOR = "$unset";
    static final String FIELD_SET_DELIMETER = ".$.";
    static final String SET = "$set";
    static final String TIMESTAMP = "timestamp";
    static final String REUPLOAD_KEY_REQUIRED = "reupload_key_required";
    private static final Logger LOGGER = LoggerFactory.getLogger(BaseDAO.class);
    private static final String INSERT_ERROR_MESSAGE = "Insert to Mongo DB fails: ";
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true)
            .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
            .registerModule(new IsoDateModule())
            .registerModule(new JavaPrimitiveModule())
            .registerModule(new MongoModule());
    private static final String PULL = "$pull";
    private static final String PULL_ALL = "$pullAll";
    private static final String EACH = "$each";
    private static final String ELEMENT_AT_OPERATOR = "$arrayElemAt";

    @Inject
    protected MongoService mongoService;

    /**
     * Return <b>true</b> if collection exists.
     *
     * @param name collection name.
     */
    boolean collectionExists(String name) {
        return mongoService.collectionExists(name);
    }

    /**
     * Return Mongo collection.
     *
     * @param collection collection name.
     */
    public MongoCollection<Document> getCollection(String collection) {
        return mongoService.getCollection(collection);
    }

    /**
     * Inserts the document into the collection.
     *
     * @param collection collection name.
     * @param supplier   document.
     */
    protected void insertOne(String collection, Supplier<Document> supplier) {
        insertOne(collection, supplier, generateUUID());
    }

    /**
     * Inserts the document into the collection with given the unique id.
     *
     * @param collection collection name.
     * @param document   document.
     * @param uuid       unique id.
     */
    protected void insertOne(String collection, Supplier<Document> document, String uuid) {
        try {
            mongoService.getCollection(collection)
                    .insertOne(document.get()
                            .append(ID, uuid)
                            .append(TIMESTAMP, new Date()));
        } catch (MongoException e) {
            LOGGER.warn(INSERT_ERROR_MESSAGE + "{}", e.getLocalizedMessage(), e);
            throw new DatalabException("Insert to Mongo DB failed: " + e.getLocalizedMessage(), e);
        }
    }

    /**
     * Serializes the object and inserts into the collection.
     *
     * @param collection collection name.
     * @param object     for inserting to collection.
     */
    protected void insertOne(String collection, Object object) {
        insertOne(collection, object, generateUUID());
    }

    /**
     * Serializes the object and inserts into the collection.
     *
     * @param collection collection name.
     * @param object     for inserting to collection.
     * @param uuid       unique id.
     */
    protected void insertOne(String collection, Object object, String uuid) {
        try {
            mongoService.getCollection(collection)
                    .insertOne(convertToBson(object)
                            .append(ID, uuid)
                            .append(TIMESTAMP, new Date()));
        } catch (MongoException e) {
            LOGGER.warn(INSERT_ERROR_MESSAGE + "{}", e.getLocalizedMessage(), e);
            throw new DatalabException(INSERT_ERROR_MESSAGE + e.getLocalizedMessage(), e);
        }
    }

    /**
     * Serializes objects and inserts into the collection.
     *
     * @param collection collection name.
     * @param object     for inserting to collection.
     */
    protected void insertMany(String collection, List<Object> object) {
        try {
            mongoService.getCollection(collection)
                    .insertMany(convertToBson(object)
                            .stream()
                            .peek(o -> {
                                o.append(ID, generateUUID());
                                o.append(TIMESTAMP, new Date());
                            })
                            .collect(Collectors.toList())
                    );
        } catch (MongoException e) {
            LOGGER.warn(INSERT_ERROR_MESSAGE + "{}", e.getLocalizedMessage(), e);
            throw new DatalabException(INSERT_ERROR_MESSAGE + e.getLocalizedMessage(), e);
        }
    }

    /**
     * Updates single document in the collection by condition.
     *
     * @param collection collection name.
     * @param condition  condition for search documents in collection.
     * @param document   document.
     */
    protected UpdateResult updateOne(String collection, Bson condition, Bson document) {
        try {
            return mongoService.getCollection(collection)
                    .updateOne(condition, document);
        } catch (MongoException e) {
            LOGGER.warn("Update Mongo DB fails: {}", e.getLocalizedMessage(), e);
            throw new DatalabException("Update to Mongo DB fails: " + e.getLocalizedMessage(), e);
        }
    }

    /**
     * Update or insert single document in the collection by condition.
     *
     * @param collection collection name.
     * @param condition  condition for search documents in collection.
     * @param document   document.
     * @param isUpsert   if <b>true</b> document will be updated or inserted.
     */
    protected void updateOne(String collection, Bson condition, Bson document, boolean isUpsert) {
        try {
            if (isUpsert) {
                mongoService.getCollection(collection).updateOne(condition, document,
                        new UpdateOptions().upsert(true));
            } else {
                mongoService.getCollection(collection).updateOne(condition, document);
            }
        } catch (MongoException e) {
            LOGGER.warn("Upsert Mongo DB fails: {}", e.getLocalizedMessage(), e);
            throw new DatalabException("Upsert to Mongo DB fails: " + e.getLocalizedMessage(), e);
        }
    }

    /**
     * Updates all documents in the collection by condition.
     *
     * @param collection collection name.
     * @param condition  condition for search documents in collection.
     * @param document   document.
     */
    UpdateResult updateMany(String collection, Bson condition, Bson document) {
        try {
            return mongoService.getCollection(collection)
                    .updateMany(condition, document);
        } catch (MongoException e) {
            LOGGER.warn("Update Mongo DB fails: {}", e.getLocalizedMessage(), e);
            throw new DatalabException("Update to Mongo DB fails: " + e.getLocalizedMessage(), e);
        }
    }

    /**
     * Removes single document in the collection by condition.
     *
     * @param collection collection name.
     * @param condition  condition for search documents in collection.
     */
    protected DeleteResult deleteOne(String collection, Bson condition) {
        try {
            return mongoService.getCollection(collection)
                    .deleteOne(condition);
        } catch (MongoException e) {
            LOGGER.warn("Removing document from Mongo DB fails: {}", e.getLocalizedMessage(), e);
            throw new DatalabException("Removing document from Mongo DB fails: " + e.getLocalizedMessage(), e);
        }
    }

    /**
     * Removes many documents in the collection by condition.
     *
     * @param collection collection name.
     * @param condition  condition for search documents in collection.
     */
    protected DeleteResult deleteMany(String collection, Bson condition) {
        try {
            return mongoService.getCollection(collection)
                    .deleteMany(condition);
        } catch (MongoException e) {
            LOGGER.warn("Removing document from Mongo DB fails: {}", e.getLocalizedMessage(), e);
            throw new DatalabException("Removing document from Mongo DB fails: " + e.getLocalizedMessage(), e);
        }
    }

    /**
     * Finds and returns all documents from the collection.
     *
     * @param collection collection name.
     */
    protected FindIterable<Document> find(String collection) {
        return mongoService.getCollection(collection).find();
    }

    /**
     * Finds and returns documents from the collection by condition.
     *
     * @param collection collection name.
     * @param condition  condition for search documents in collection.
     */
    protected FindIterable<Document> find(String collection, Bson condition) {
        return mongoService.getCollection(collection)
                .find(condition);
    }

    /**
     * Finds and returns all documents from the collection converted to resulted type.
     *
     * @param collection    collection name.
     * @param resultedClass type of class for deserialization.
     */
    protected <T> List<T> find(String collection, Class<T> resultedClass) {
        return find(collection)
                .into(new ArrayList<>())
                .stream()
                .map(d -> convertFromDocument(d, resultedClass))
                .collect(Collectors.toList());
    }

    /**
     * Finds and returns documents from the collection by condition.
     *
     * @param collection    collection name.
     * @param condition     condition for search documents in collection.
     * @param resultedClass type of class for deserialization.
     */
    protected <T> List<T> find(String collection, Bson condition, Class<T> resultedClass) {
        return mongoService.getCollection(collection)
                .find(condition)
                .into(new ArrayList<>())
                .stream()
                .map(d -> convertFromDocument(d, resultedClass))
                .collect(Collectors.toList());
    }

    /**
     * Finds and returns documents with the specified fields from the collection by condition.
     *
     * @param collection collection name.
     * @param condition  condition for search documents in collection.
     * @param projection document describing the fields in the collection to return.
     */
    protected FindIterable<Document> find(String collection, Bson condition, Bson projection) {
        return mongoService.getCollection(collection)
                .find(condition)
                .projection(projection);
    }

    /**
     * Aggregates and returns documents according to the specified aggregation pipeline.
     *
     * @param collection collection name.
     * @param pipeline   the aggregate pipeline.
     */
    public AggregateIterable<Document> aggregate(String collection, List<? extends Bson> pipeline) {
        return mongoService.getCollection(collection)
                .aggregate(pipeline);
    }

    /**
     * Checks that the documents iterator have one document only.
     *
     * @param documents documents
     */
    private Optional<Document> limitOne(MongoIterable<Document> documents) {
        Document first = documents.first();
        try (MongoCursor<Document> iterator = documents.iterator()) {
            if (iterator.hasNext()) {
                iterator.next();
                if (iterator.hasNext()) {
                    throw new DatalabException("too many items found while one is expected");
                }
            }
        }
        return Optional.ofNullable(first);
    }

    /**
     * Finds and returns one document from the collection by condition.
     *
     * @param collection collection name.
     * @param condition  condition for search documents in collection.
     * @throws DatalabException if documents iterator have more than one document.
     */
    protected Optional<Document> findOne(String collection, Bson condition) {
        FindIterable<Document> found = find(collection, condition);
        return limitOne(found);
    }

    /**
     * Finds and returns one document with the specified fields from the collection by condition.
     *
     * @param collection collection name.
     * @param condition  condition for search documents in collection.
     * @param projection document describing the fields in the collection to return.
     * @throws DatalabException if documents iterator have more than one document.
     */
    protected Optional<Document> findOne(String collection, Bson condition, Bson projection) {
        FindIterable<Document> found = find(collection, condition, projection);
        return limitOne(found);
    }

    /**
     * Serializes given object to document and returns it.
     *
     * @param object object
     */
    Document convertToBson(Object object) {
        try {
            return Document.parse(MAPPER.writeValueAsString(object));
        } catch (IOException e) {
            throw new DatalabException("error converting to bson", e);
        }
    }

    List<Document> convertToBson(List<Object> objects) {
        return objects
                .stream()
                .map(this::convertToBson)
                .collect(Collectors.toList());
    }

    /**
     * Finds and returns one object as given class from the collection by condition.
     *
     * @param collection collection name.
     * @param condition  condition for search documents in collection.
     * @param clazz      type of class for deserialization.
     */
    protected <T> Optional<T> findOne(String collection, Bson condition, Class<T> clazz) {
        Optional<Document> doc = findOne(collection, condition);
        return doc.map(document -> convertFromDocument(document, clazz));
    }

    /**
     * Finds and returns one object as given class and with the specified fields from the collection by condition.
     *
     * @param collection collection name.
     * @param condition  condition for search documents in collection.
     * @param projection document describing the fields in the collection to return.
     * @param clazz      type of class for deserialization.
     */
    protected <T> Optional<T> findOne(String collection, Bson condition, Bson projection, Class<T> clazz) {
        Optional<Document> doc = findOne(collection, condition, projection);
        return doc.map(document -> convertFromDocument(document, clazz));
    }

    /**
     * Deserializes given document to object and returns it.
     *
     * @param document element from database
     */
    <T> T convertFromDocument(Document document, Class<T> clazz) {
        try {
            String json = document.toJson();
            return MAPPER.readValue(json, clazz);
        } catch (IOException e) {
            throw new DatalabException("error converting from document with id " + document.get(ID), e);
        }
    }

    <T> T convertFromDocument(List<Document> documents, TypeReference<T> valueTypeRef) {
        final String jsonArray = documents.stream()
                .map(Document::toJson)
                .collect(Collectors.joining(",", "[", "]"));
        try {
            return MAPPER.readValue(jsonArray, valueTypeRef);
        } catch (IOException e) {
            throw new DatalabException("error converting array " + jsonArray, e);
        }
    }

    protected Document getGroupingFields(String... fieldNames) {
        Document d = new Document();
        for (String name : fieldNames) {
            d.put(name, "$" + name);
        }
        return d;
    }

    protected Stream<Document> stream(Iterable<Document> iterable) {
        return StreamSupport.stream(iterable.spliterator(), false);
    }

    List<String> statusList(UserInstanceStatus[] statuses) {
        return Arrays.stream(statuses).map(UserInstanceStatus::toString).collect(Collectors.toList());
    }

    List<String> statusList(List<UserInstanceStatus> statuses) {
        return statuses.stream().map(UserInstanceStatus::toString).collect(Collectors.toList());
    }

    /**
     * Returns a unique id.
     */
    private String generateUUID() {
        return UUID.randomUUID().toString();
    }

    protected BasicDBObject addToSet(String columnName, Set<String> values) {
        return new BasicDBObject(ADD_TO_SET, new BasicDBObject(columnName, new BasicDBObject(EACH, values)));
    }

    protected Bson unset(String columnName, String value) {
        return new BasicDBObject(UNSET_OPERATOR, new BasicDBObject(columnName, value));
    }

    protected BasicDBObject pull(String columnName, String value) {
        return new BasicDBObject(PULL, new BasicDBObject(columnName, value));
    }

    protected BasicDBObject pullAll(String columnName, Set<String> values) {
        return new BasicDBObject(PULL_ALL, new BasicDBObject(columnName, values));
    }

    protected Document elementAt(String arrayColumnName, int index) {
        return new Document(ELEMENT_AT_OPERATOR, Arrays.asList("$" + arrayColumnName, index));
    }

    protected Document elementAt(Bson bson, int index) {
        return new Document(ELEMENT_AT_OPERATOR, Arrays.asList(bson, index));
    }

    protected Bson notNull(String fieldName) {
        return and(exists(fieldName), ne(fieldName, null));
    }
}
