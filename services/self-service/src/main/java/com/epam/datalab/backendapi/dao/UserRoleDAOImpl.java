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

import com.epam.datalab.backendapi.resources.dto.UserGroupDto;
import com.epam.datalab.backendapi.resources.dto.UserRoleDTO;
import com.epam.datalab.cloud.CloudProvider;
import com.epam.datalab.exceptions.DatalabException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Singleton;
import com.mongodb.client.model.BsonField;
import com.mongodb.client.result.UpdateResult;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.epam.datalab.backendapi.dao.MongoCollections.USER_GROUPS;
import static com.mongodb.client.model.Aggregates.group;
import static com.mongodb.client.model.Aggregates.lookup;
import static com.mongodb.client.model.Aggregates.project;
import static com.mongodb.client.model.Aggregates.unwind;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Filters.not;
import static java.lang.String.format;
import static java.util.stream.Collectors.toList;

@Singleton
@Slf4j
public class UserRoleDAOImpl extends BaseDAO implements UserRoleDAO {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String ROLES_FILE_FORMAT = "/mongo/%s/mongo_roles.json";
    private static final String USERS_FIELD = "users";
    private static final String GROUPS_FIELD = "groups";
    private static final String DESCRIPTION = "description";
    private static final String TYPE = "type";
    private static final String CLOUD = "cloud";
    private static final String ROLES = "roles";
    private static final String GROUPS = "$groups";
    private static final String GROUP = "group";
    private static final String EXPLORATORY_SHAPES_FIELD = "exploratory_shapes";
    private static final String PAGES_FIELD = "pages";
    private static final String EXPLORATORIES_FIELD = "exploratories";
    private static final String COMPUTATIONALS_FIELD = "computationals";
    private static final String GROUP_INFO = "groupInfo";


    @Override
    public List<UserRoleDTO> findAll() {
        return find(MongoCollections.ROLES, UserRoleDTO.class);
    }

    @Override
    public UserRoleDTO findById(String roleId) {
        return findOne(MongoCollections.ROLES, in(ID, roleId),UserRoleDTO.class).orElse(null);
    }

    @Override
    public void insert(UserRoleDTO dto) {
        insertOne(MongoCollections.ROLES, dto, dto.getId());
    }

    @Override
    public void insert(List<UserRoleDTO> roles) {
        roles.forEach(this::insert);
    }

    @Override
    public boolean update(UserRoleDTO dto) {
        final Document userRoleDocument = convertToBson(dto).append(TIMESTAMP, new Date());
        return conditionMatched(updateOne(MongoCollections.ROLES,
                eq(ID, dto.getId()),
                new Document(SET, userRoleDocument)));
    }

    @Override
    public void updateMissingRoles(CloudProvider cloudProvider) {
        getUserRoleFromFile(cloudProvider)
                .stream()
                .peek(u -> u.setGroups(Collections.emptySet()))
                .filter(u -> findAll()
                        .stream()
                        .map(UserRoleDTO::getId)
                        .noneMatch(id -> id.equals(u.getId())))
                .forEach(this::insert);

        addGroupToRole(aggregateRolesByGroup()
                        .stream()
                        .map(UserGroupDto::getGroup)
                        .collect(Collectors.toSet()),
                getAll(cloudProvider));
    }

    private Set<String> getAll(CloudProvider cloudProvider) {
        return getUserRoleFromFile(cloudProvider)
                .stream()
                .map(UserRoleDTO::getId)
                .collect(Collectors.toSet());
    }

    @Override
    public boolean addGroupToRole(Set<String> groups, Set<String> roleIds) {
        return conditionMatched(updateMany(MongoCollections.ROLES, in(ID, roleIds), addToSet(GROUPS_FIELD,
                groups)));
    }

    @Override
    public void removeGroupWhenRoleNotIn(String group, Set<String> roleIds) {
        updateMany(MongoCollections.ROLES, not(in(ID, roleIds)), pull(GROUPS_FIELD, group));
    }

    @Override
    public void removeUnnecessaryRoles(CloudProvider cloudProviderToBeRemoved, List<CloudProvider> remainingProviders) {
        if (remainingProviders.contains(cloudProviderToBeRemoved)) {
            return;
        }
        List<UserRoleDTO> remainingRoles = new ArrayList<>();
        remainingProviders.forEach(p -> remainingRoles.addAll(getUserRoleFromFile(p)));

        getUserRoleFromFile(cloudProviderToBeRemoved)
                .stream()
                .filter(role -> UserRoleDTO.cloudSpecificTypes().contains(role.getType()))
                .map(UserRoleDTO::getId)
                .filter(u -> remainingRoles
                        .stream()
                        .map(UserRoleDTO::getId)
                        .noneMatch(id -> id.equals(u)))
                .forEach(this::remove);
    }

    @Override
    public void remove(String roleId) {
        deleteOne(MongoCollections.ROLES, eq(ID, roleId));
    }

    @Override
    public boolean removeGroup(String groupId) {
        return conditionMatched(updateMany(MongoCollections.ROLES, in(GROUPS_FIELD, groupId), pull(GROUPS_FIELD,
                groupId)));
    }

    @Override
    public List<UserGroupDto> aggregateRolesByGroup() {
        final Document role = roleDocument();
        final Bson groupBy = group(GROUPS, new BsonField(ROLES, new Document(ADD_TO_SET, role)));
        final Bson lookup = lookup(USER_GROUPS, ID, ID, GROUP_INFO);
        final List<Bson> pipeline = Arrays.asList(unwind(GROUPS), groupBy, lookup,
                project(new Document(GROUP, "$" + ID).append(GROUP_INFO, elementAt(GROUP_INFO, 0))
                        .append(ROLES, "$" + ROLES)),
                project(new Document(GROUP, "$" + ID).append(USERS_FIELD, "$" + GROUP_INFO + "." + USERS_FIELD)
                        .append(ROLES, "$" + ROLES)));

        return stream(aggregate(MongoCollections.ROLES, pipeline))
                .map(d -> convertFromDocument(d, UserGroupDto.class))
                .collect(toList());
    }

    private List<UserRoleDTO> getUserRoleFromFile(CloudProvider cloudProvider) {
        try (InputStream is = getClass().getResourceAsStream(format(ROLES_FILE_FORMAT, cloudProvider.getName()))) {
            return MAPPER.readValue(is, new TypeReference<List<UserRoleDTO>>() {
            });
        } catch (IOException e) {
            log.error("Can not marshall datalab roles due to: {}", e.getMessage(), e);
            throw new IllegalStateException("Can not marshall datalab roles due to: " + e.getMessage());
        }
    }

    private Document roleDocument() {
        return new Document().append(ID, "$" + ID)
                .append(DESCRIPTION, "$" + DESCRIPTION)
                .append(TYPE, "$" + TYPE)
                .append(CLOUD, "$" + CLOUD)
                .append(USERS_FIELD, "$" + USERS_FIELD)
                .append(EXPLORATORY_SHAPES_FIELD, "$" + EXPLORATORY_SHAPES_FIELD)
                .append(PAGES_FIELD, "$" + PAGES_FIELD)
                .append(EXPLORATORIES_FIELD, "$" + EXPLORATORIES_FIELD)
                .append(COMPUTATIONALS_FIELD, "$" + COMPUTATIONALS_FIELD);
    }

    private boolean conditionMatched(UpdateResult updateResult) {
        return updateResult.getMatchedCount() > 0;
    }

}
