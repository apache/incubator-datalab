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

import com.epam.datalab.exceptions.DatalabException;
import com.google.inject.Singleton;
import org.bson.Document;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.epam.datalab.backendapi.dao.MongoCollections.USER_GROUPS;
import static com.mongodb.client.model.Filters.*;

@Singleton
public class UserGroupDAOImpl extends BaseDAO implements UserGroupDAO {

    private static final String USERS_FIELD = "users";

    @Override
    public void addUsers(String group, Set<String> users) {
        updateOne(USER_GROUPS, eq(ID, group), addToSet(USERS_FIELD, users), true);
    }

    @Override
    public void updateUsers(String group, Set<String> users) {
        updateOne(USER_GROUPS, eq(ID, group), new Document(SET, new Document(USERS_FIELD, users)), true);
    }

    @Override
    public void removeGroup(String groupId) {
        deleteOne(USER_GROUPS, eq(ID, groupId));
    }

    @Override
    public Set<String> getUserGroups(String user) {
        return stream(find(USER_GROUPS, elemMatch(USERS_FIELD, new Document("$regex", "^" + user + "$")
                .append("$options", "i"))))
                .map(document -> document.getString(ID))
                .collect(Collectors.toSet());
    }

    @Override
    public Set<String> getUsers(String group) {
        return new HashSet<>(findOne(USER_GROUPS, eq(ID, group))
                .map(document -> (List<String>) document.get(USERS_FIELD))
                .orElseThrow(() -> new DatalabException(String.format("Group %s not found", group))));
    }

    @Override
    public Set<String> getGroupNames(String name) {
        return stream(find(USER_GROUPS, regex(ID, name,"i")))
                .map(document -> document.getString(ID))
                .collect(Collectors.toSet());
    }
}
