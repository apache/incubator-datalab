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

import java.util.List;
import java.util.Optional;

/**
 * The interface specifies behaviour for objects, which retrieve, update, remove
 * the endpoints entities from the DataBase, according passed fields, i.e name, url, status.
 */
public interface EndpointDAO {
    List<EndpointDTO> getEndpoints();

    List<EndpointDTO> getEndpointsWithStatus(String status);

    /*** Retrieve the Endpoint entity according required name
     * @param name - the Endpoint regular title
     * @return the Optional object
     */
    Optional<EndpointDTO> get(String name);

    /*** Retrieve the Endpoint entity according required Endpoint URL
     * @param url - the Endpoint web address
     * @return the Optional object
     */
    Optional<EndpointDTO> getEndpointWithUrl(String url);

    void create(EndpointDTO endpointDTO);

    void updateEndpointStatus(String name, String status);

    void remove(String name);
}
