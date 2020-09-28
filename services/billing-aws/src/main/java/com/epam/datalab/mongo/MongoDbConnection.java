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

package com.epam.datalab.mongo;

import com.epam.datalab.core.aggregate.UsageDataList;
import com.epam.datalab.exceptions.AdapterException;
import com.epam.datalab.model.aws.ReportLine;
import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.IndexOptions;
import com.mongodb.client.result.DeleteResult;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;

/**
 * Provides operation with Mongo database and billing report.
 */
public class MongoDbConnection implements Closeable {
    private static final Logger LOGGER = LoggerFactory.getLogger(MongoDbConnection.class);

    /**
     * Mongo client.
     */
    private MongoClient client;

    /**
     * Mongo database.
     */
    private MongoDatabase database;


    /**
     * Instantiate the helper for Mongo database adapter.
     *
     * @param host         the host name.
     * @param port         the port.
     * @param databaseName the name of database.
     * @param username     the name of user.
     * @param password     the password.
     * @throws AdapterException
     */
    public MongoDbConnection(String host, int port, String databaseName, String username, String password) throws
            AdapterException {
        try {
            client = new MongoClient(
                    new ServerAddress(host, port),
                    Collections.singletonList(
                            MongoCredential.createCredential(username, databaseName, password.toCharArray())));
            database = client.getDatabase(databaseName).withWriteConcern(WriteConcern.ACKNOWLEDGED);
        } catch (Exception e) {
            throw new AdapterException("Cannot create connection to database " +
                    databaseName + ". " + e.getLocalizedMessage(), e);
        }
    }

    /**
     * Close connection to Mongo database.
     */
    @Override
    public void close() throws IOException {
        if (client != null) {
            try {
                client.close();
            } catch (Exception e) {
                throw new IOException(e.getLocalizedMessage(), e);
            } finally {
                client = null;
                database = null;
            }
        }
    }

    /**
     * Create index on billing collection.
     *
     * @param indexName the name of index.
     * @param index     the index options.
     */
    private void createBillingIndexes(String indexName, Bson index) {
        MongoCollection<Document> collection = database.getCollection(MongoConstants.COLLECTION_BILLING);
        IndexOptions options = new IndexOptions().name(MongoConstants.COLLECTION_BILLING + indexName);
        try {
            collection
                    .createIndex(index, options);
        } catch (Exception e) {
            LOGGER.warn("Cannot create index {} on collection {}. {}", options.getName(),
                    MongoConstants.COLLECTION_BILLING, e.getLocalizedMessage(), e);
        }
    }

    /**
     * Create index on Mongo collection for fast upsert operations.
     */
    public void createBillingIndexes() {
        createBillingIndexes("_IntervalIdx",
                new BasicDBObject()
                        .append(ReportLine.FIELD_USER_ID, 1)
                        .append(ReportLine.FIELD_USAGE_DATE, 2));
        createBillingIndexes("_ExploratoryIdx",
                new BasicDBObject()
                        .append(ReportLine.FIELD_USER_ID, 1)
                        .append(MongoConstants.FIELD_EXPLORATORY_NAME, 2));
    }

    /**
     * Return the collection of Mongo database.
     *
     * @param collectionName the name of collection.
     */
    public MongoCollection<Document> getCollection(String collectionName) {
        return database.getCollection(collectionName);
    }

    /**
     * Insert document to Mongo.
     *
     * @param collection the name of collection.
     * @param document   the document.
     * @throws AdapterException
     */
    public void insertOne(MongoCollection<Document> collection, Document document) throws AdapterException {
        try {
            collection.insertOne(document);
        } catch (Exception e) {
            throw new AdapterException("Cannot insert document into collection " +
                    collection.getNamespace() + ": " + e.getLocalizedMessage(), e);
        }
    }

    /**
     * Insert documents from list to Mongo collection and clear list.
     *
     * @param collection Mongo collection.
     * @param documents  the list of documents.
     * @throws AdapterException
     */
    public void insertRows(MongoCollection<Document> collection, List<Document> documents) throws AdapterException {
        try {
            if (!documents.isEmpty()) {
                collection.insertMany(documents);
                LOGGER.debug("{} documents has been inserted into collection {}",
                        documents.size(), collection.getNamespace());
                documents.clear();
            }
        } catch (Exception e) {
            throw new AdapterException("Cannot insert new documents into collection " +
                    collection.getNamespace() + ": " + e.getLocalizedMessage(), e);
        }
    }

    /**
     * Insert documents from list to Mongo collection and clear list.
     *
     * @param collection    Mongo collection.
     * @param documents     the list of documents.
     * @param usageDateList list of the data interval to deletion old data from Mongo.
     * @throws AdapterException
     */
    public void upsertRows(MongoCollection<Document> collection, List<Document> documents, UsageDataList usageDateList)
            throws AdapterException {
        deleteRows(collection, usageDateList);
        insertRows(collection, documents);
    }

    /**
     * Delete the documents from Mongo collection.
     *
     * @param collection    Mongo collection.
     * @param usageDateList list of the data interval to deletion data from Mongo.
     * @throws AdapterException
     */
    public void deleteRows(MongoCollection<Document> collection, UsageDataList usageDateList)
            throws AdapterException {
        try {
            long rowCount = 0;
            for (String date : usageDateList) {
                if (!usageDateList.get(date)) {
                    DeleteResult result = collection.deleteMany(eq(ReportLine.FIELD_USAGE_DATE, date));
                    rowCount += result.getDeletedCount();
                    usageDateList.set(date, true);
                }
            }
            if (rowCount > 0) {
                LOGGER.debug("{} documents has been deleted from collection {}",
                        rowCount, collection.getNamespace());
            }
        } catch (Exception e) {
            throw new AdapterException("Cannot delete old rows from collection " +
                    collection.getNamespace() + ": " + e.getLocalizedMessage(), e);
        }
    }
}
