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

import com.epam.dlab.mongo.MongoService;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.WriteConcern;

import java.util.Collections;
import java.util.List;

abstract class DAOTestBase {
    private final static String DBNAME = "dlabdbtest";

    private static MongoClient mongoClient;
    static MongoService mongoService;
    static Injector testInjector;

    private List<String> affectedCollections;

    DAOTestBase(List<String> cols) {
        affectedCollections = cols;
    }

    static void setupAll() {
        connectMongo();
        createInjector();
    }

    static void teardownAll() {
        disconnectMongo();
    }

    // TODO: Make Mongo test instance configurable
    private static void connectMongo() {
        if(mongoClient == null) {
            mongoClient = new MongoClient(
                    new ServerAddress("localhost", 27017),
                    Collections.singletonList(
                    MongoCredential.createCredential("admin", "dlabdb", "XS3ms9R3tP".toCharArray())));
        }
        mongoService = new MongoService(mongoClient, DBNAME, WriteConcern.ACKNOWLEDGED);
    }

    private static void disconnectMongo() {
        if(mongoClient != null) {
            mongoService = null;
            mongoClient.close();
        }
    }

    void cleanup() {
        for(String collection : affectedCollections) {
            mongoService.getCollection(collection).drop();
        }
    }

    private static void createInjector() {
        testInjector = Guice.createInjector(new AbstractModule() {
            @Override
            protected void configure() {
                bind(MongoService.class).toInstance(mongoService);
            }
        });
    }
}
