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

import com.epam.datalab.backendapi.domain.EndpointDTO;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.regex;

public class EndpointDAOImpl extends BaseDAO implements EndpointDAO {

    private static final String ENDPOINTS_COLLECTION = "endpoints";
    private static final String ENDPOINT_NAME_FIELD = "name";
    private static final String ENDPOINT_STATUS_FIELD = "status";
    private static final String ENDPOINT_URL_FIELD = "url";

    @Override
    public List<EndpointDTO> getEndpoints() {
        return find(ENDPOINTS_COLLECTION, EndpointDTO.class);
    }

    @Override
    public List<EndpointDTO> getEndpointsWithStatus(String status) {
        return find(ENDPOINTS_COLLECTION, endpointStatusCondition(status), EndpointDTO.class);
    }

    @Override
    public Optional<EndpointDTO> getEndpointWithUrl(String url) {
        return findOne(ENDPOINTS_COLLECTION, endpointUrlCondition(url), EndpointDTO.class);
    }

    @Override
    public Optional<EndpointDTO> get(String name) {
        return findOne(ENDPOINTS_COLLECTION, endpointCondition(name), EndpointDTO.class);
    }

    @Override
    public void create(EndpointDTO endpointDTO) {
        insertOne(ENDPOINTS_COLLECTION, endpointDTO);
    }

    @Override
    public void updateEndpointStatus(String name, String status) {
        final Document updatedFiled = new Document(ENDPOINT_STATUS_FIELD, status);
        updateOne(ENDPOINTS_COLLECTION, endpointCondition(name), new Document(SET, updatedFiled));
    }

    @Override
    public void remove(String name) {
        deleteOne(ENDPOINTS_COLLECTION, endpointCondition(name));
    }

    private Bson endpointCondition(String name) {
        Pattern endPointName = Pattern.compile("^" + name + "$", Pattern.CASE_INSENSITIVE);
        return regex(ENDPOINT_NAME_FIELD, endPointName);
    }

    private Bson endpointUrlCondition(String url) {
        Pattern endPointUrl = Pattern.compile("^" + url + "$", Pattern.CASE_INSENSITIVE);
        return regex(ENDPOINT_URL_FIELD, endPointUrl);
    }

    private Bson endpointStatusCondition(String status) {
        return eq(ENDPOINT_STATUS_FIELD, status);
    }
}
