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

import com.epam.datalab.backendapi.domain.OdahuDTO;
import com.epam.datalab.backendapi.domain.OdahuFieldsDTO;
import com.epam.datalab.dto.UserInstanceStatus;
import com.epam.datalab.dto.base.odahu.OdahuResult;

import java.util.List;
import java.util.Optional;

public interface OdahuDAO {
    Optional<OdahuDTO> getByProjectEndpoint(String project, String endpoint);

    OdahuFieldsDTO getFields(String name, String project, String endpoint);

    List<OdahuDTO> findOdahuClusters();

    List<OdahuDTO> findOdahuClusters(String project, String endpoint);

    boolean create(OdahuDTO odahuDTO);

    void updateStatus(String name, String project, String endpoint, UserInstanceStatus status);

    void updateStatusAndUrls(OdahuResult result, UserInstanceStatus status);
}
