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

import com.epam.datalab.auth.UserInfo;
import com.epam.datalab.backendapi.conf.SelfServiceApplicationConfiguration;
import com.epam.datalab.exceptions.DatalabException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Projections;
import org.bson.Document;
import org.keycloak.representations.AccessTokenResponse;

import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.epam.datalab.backendapi.dao.MongoCollections.ROLES;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Projections.exclude;
import static com.mongodb.client.model.Projections.fields;
import static com.mongodb.client.model.Projections.include;

/**
 * DAO write the attempt of user login into DataLab.
 */
@Singleton
public class SecurityDAO extends BaseDAO {
    private static final String SECURITY_COLLECTION = "security";
    private static final String TOKEN_RESPONSE = "tokenResponse";
    private static final String LAST_ACCESS = "last_access";

    @Inject
    private SelfServiceApplicationConfiguration conf;

    /**
     * Return the roles or throw exception if roles collection does not exists.
     */
    public FindIterable<Document> getRoles() {
        if (!collectionExists(ROLES)) {
            throw new DatalabException("Collection \"" + ROLES + "\" does not exists.");
        }
        return find(ROLES, ne(ID, "_Example"), fields(exclude("description")));
    }

    public Map<String, Set<String>> getGroups() {
        return stream(find("userGroups"))
                .collect(Collectors.toMap(d -> d.getString(ID).toLowerCase(), this::toUsers));

    }

    public void saveUser(String userName, AccessTokenResponse accessTokenResponse) {
        updateOne(SECURITY_COLLECTION, eq(ID, userName),
                new Document(SET,
                        new Document()
                                .append(ID, userName)
                                .append("created", new Date())
                                .append(LAST_ACCESS, new Date())
                                .append(TOKEN_RESPONSE, convertToBson(accessTokenResponse))),
                true);
    }

    public void updateUser(String userName, AccessTokenResponse accessTokenResponse) {
        updateOne(SECURITY_COLLECTION, eq(ID, userName),
                new Document(SET,
                        new Document()
                                .append(ID, userName)
                                .append(LAST_ACCESS, new Date())
                                .append(TOKEN_RESPONSE, convertToBson(accessTokenResponse))));
    }

    public Optional<UserInfo> getUser(String token) {
        return Optional.ofNullable(mongoService.getCollection(SECURITY_COLLECTION)
                .findOneAndUpdate(and(eq(TOKEN_RESPONSE + ".access_token", token), gte(LAST_ACCESS,
                        new Date(new Date().getTime() - conf.getInactiveUserTimeoutMillSec()))), new Document("$set",
                        new Document(LAST_ACCESS, new Date()))))
                .map(d -> new UserInfo(d.getString(ID), token));
    }


    public Optional<AccessTokenResponse> getTokenResponse(String user) {
        return findOne(SECURITY_COLLECTION, eq(ID, user), Projections.fields(include(TOKEN_RESPONSE)))
                .map(d -> convertFromDocument((Document) d.get(TOKEN_RESPONSE), AccessTokenResponse.class));
    }

    public Set<String> getUserNames(String name){
        return stream(find(SECURITY_COLLECTION, regex(ID, name,"i")))
                .map(document -> document.getString(ID))
                .collect(Collectors.toSet());
    }

    @SuppressWarnings("unchecked")
    private Set<String> toUsers(Document d) {
        final Object users = d.get("users");
        return users == null ? Collections.emptySet() :
                new HashSet<>(((List<String>) users).stream().map(String::toLowerCase).collect(Collectors.toList()));
    }
}
