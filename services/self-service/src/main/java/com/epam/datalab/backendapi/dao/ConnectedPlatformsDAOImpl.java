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

import com.epam.datalab.backendapi.resources.dto.ConnectedPlatformDTO;

import java.util.List;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

public class ConnectedPlatformsDAOImpl extends BaseDAO implements ConnectedPlatformsDAO {

    private static final String CONNECTED_PLATFORMS = "connectedPlatforms";
    private static final String NAME = "name";
    private static final String USER = "user";

    @Override
    public boolean exist(String name) {
        return findOne(CONNECTED_PLATFORMS, eq(NAME, name)).isPresent();
    }

    @Override
    public void addPlatform(ConnectedPlatformDTO connectedPlatformDTO) {
        insertOne(CONNECTED_PLATFORMS, connectedPlatformDTO);
    }

    @Override
    public List<ConnectedPlatformDTO> getAll() {
        return find(CONNECTED_PLATFORMS, ConnectedPlatformDTO.class);
    }

    @Override
    public List<ConnectedPlatformDTO> getUserPlatforms(String userName) {
        return find(CONNECTED_PLATFORMS, eq(USER, userName), ConnectedPlatformDTO.class);
    }

    @Override
    public void delete(String user, String name) {
        deleteOne(CONNECTED_PLATFORMS, and(eq(USER,user),eq(NAME,name)));
    }
}
