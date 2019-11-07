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

package com.epam.dlab.backendapi.service.impl;

import com.epam.dlab.backendapi.dao.EndpointDAO;
import com.epam.dlab.backendapi.domain.EndpointDTO;
import com.epam.dlab.backendapi.service.EndpointService;
import com.epam.dlab.exceptions.ResourceConflictException;
import com.epam.dlab.exceptions.ResourceNotFoundException;
import com.google.inject.Inject;

import java.util.List;

public class EndpointServiceImpl implements EndpointService {
	private final EndpointDAO endpointDAO;

	@Inject
	public EndpointServiceImpl(EndpointDAO endpointDAO) {
		this.endpointDAO = endpointDAO;
	}

	@Override
	public List<EndpointDTO> getEndpoints() {
		return endpointDAO.getEndpoints();
	}

	@Override
	public EndpointDTO get(String name) {
		return endpointDAO.get(name)
				.orElseThrow(() -> new ResourceNotFoundException("Endpoint with name " + name + " not found"));
	}

	@Override
	public void create(EndpointDTO endpointDTO) {
		if (!endpointDAO.get(endpointDTO.getName()).isPresent()) {
			endpointDAO.create(endpointDTO);
		} else {
			throw new ResourceConflictException("Endpoint with passed name already exist in system");
		}
	}

	@Override
	public void remove(String name) {
		endpointDAO.remove(name);
	}
}
