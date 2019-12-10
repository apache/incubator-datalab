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

package com.epam.dlab.backendapi.dao;

import com.epam.dlab.backendapi.domain.EndpointDTO;
import org.bson.conversions.Bson;

import java.util.List;
import java.util.Optional;

import static com.mongodb.client.model.Filters.eq;

public class EndpointDAOImpl extends BaseDAO implements EndpointDAO {

	private static final String ENDPOINTS_COLLECTION = "endpoints";

	@Override
	public List<EndpointDTO> getEndpoints() {
		return find(ENDPOINTS_COLLECTION, EndpointDTO.class);
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
	public void remove(String name) {
		deleteOne(ENDPOINTS_COLLECTION, endpointCondition(name));
	}

	private Bson endpointCondition(String name) {
		return eq("name", name);
	}
}
