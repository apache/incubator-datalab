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

import com.mongodb.MongoClient;
import com.mongodb.WriteConcern;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

public class MongoService {
    private MongoClient client;
    private String databaseName;
    private MongoDatabase database;

    private static final Document PING = new Document("ping", "1");

    public MongoService(MongoClient client,
                        String databaseName,
                        WriteConcern writeConcern) {
        this(client, databaseName);
        database = database.withWriteConcern(writeConcern);
    }

    public MongoService(MongoClient client, String databaseName) {
        this.client = client;
        this.databaseName = databaseName;
        this.database = client.getDatabase(databaseName);
    }

    public boolean collectionExists(String name) {
        for (String c : database.listCollectionNames()) {
            if (c.equals(name)) {
                return true;
            }
        }
        return false;
    }

    public MongoCollection<Document> getCollection(String name) {
        return database.getCollection(name, Document.class);
    }

    public <T> MongoCollection<T> getCollection(String name, Class<T> c) {
        return database.getCollection(name, c);
    }

    public void createCollection(String name) {
        database.createCollection(name);
    }

    public String getDatabaseName() {
        return databaseName;
    }

    public MongoClient getClient() {
        return client;
    }

    public Document ping() {
        return database.runCommand(PING);
    }
}
