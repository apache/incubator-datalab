/******************************************************************************************************

 Copyright (c) 2016 EPAM Systems Inc.

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 *****************************************************************************************************/

package com.epam.dlab.backendapi.dao;

import com.epam.dlab.client.mongo.MongoService;
import com.epam.dlab.exceptions.DlabException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.io.IOException;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

class BaseDAO implements MongoCollections {
    protected static final ObjectMapper MAPPER = new ObjectMapper().configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true);
    public static final String FIELD_DELIMETER = ".";
    public static final String FIELD_SET_DELIMETER = ".$.";
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

    protected void update(String collection, Bson condition, Bson value) {
        mongoService.getCollection(collection).updateOne(condition, value);
    }

    protected Document convertToBson(Object object) {
        try {
            return Document.parse(MAPPER.writeValueAsString(object));
        } catch (IOException e) {
            throw new DlabException("error converting to bson", e);
        }
    }

    protected <T> Optional<T> find(String collection, Bson eq, Class<T> clazz) {
        return Optional.ofNullable(mongoService.getCollection(collection).find(eq).first())
                .flatMap(document -> Optional.of(convertFromDocument(document, clazz)));
    }

    private <T> T convertFromDocument(Document document, Class<T> clazz) {
        try {
            return MAPPER.readValue(document.toJson(), clazz);
        } catch (IOException e) {
            throw new DlabException("error converting from document with id " + document.get(ID), e);
        }
    }

    private String generateUUID() {
        return UUID.randomUUID().toString();
    }
}
