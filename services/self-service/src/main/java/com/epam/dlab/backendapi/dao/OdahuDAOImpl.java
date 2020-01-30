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

import com.epam.dlab.backendapi.domain.OdahuDTO;
import com.epam.dlab.backendapi.domain.ProjectDTO;
import com.epam.dlab.dto.ResourceURL;
import com.epam.dlab.dto.UserInstanceStatus;
import com.mongodb.BasicDBObject;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Projections.elemMatch;
import static com.mongodb.client.model.Projections.excludeId;
import static com.mongodb.client.model.Projections.fields;
import static com.mongodb.client.model.Projections.include;
import static com.mongodb.client.model.Updates.push;
import static java.util.stream.Collectors.toList;

public class OdahuDAOImpl extends BaseDAO implements OdahuDAO {

    private static final String PROJECTS_COLLECTION = "Projects";
    private static final String ENDPOINTS = "endpoints";
    private static final String ODAHU_FIELD = "odahu";
    private static final String NAME_FIELD = "name";
    private static final String ENDPOINT_FIELD = "endpoint";
    private static final String PROJECT_FIELD = "project";
    private static final String STATUS_FIELD = "status";
    private static final String URLS_FIELD = "urls";
    private static final String COMPUTATIONAL_URL_DESC = "description";
    private static final String COMPUTATIONAL_URL_URL = "url";

    @Override
    public Optional<OdahuDTO> getByProjectEndpoint(String project, String endpoint) {
        Optional<ProjectDTO> one = findOne(PROJECTS_COLLECTION, odahuProjectEndpointCondition(project, endpoint),
                fields(include(ODAHU_FIELD), excludeId()),
                ProjectDTO.class);

        return one.flatMap(projectDTO -> projectDTO.getOdahu().stream()
                .filter(odahu -> project.equals(odahu.getProject()) && endpoint.equals(odahu.getEndpoint()))
                .findAny());
    }

    @Override
    public List<OdahuDTO> findOdahuClusters() {
        List<ProjectDTO> projectDTOS = find(PROJECTS_COLLECTION, ProjectDTO.class);
        return projectDTOS.stream()
                .map(ProjectDTO::getOdahu)
                .flatMap(List::stream)
                .collect(toList());
    }

    @Override
    public boolean create(OdahuDTO odahuDTO) {
        UpdateResult updateResult = updateOne(PROJECTS_COLLECTION, projectEndpointCondition(odahuDTO.getProject(),
                odahuDTO.getEndpoint()),
                push(ODAHU_FIELD, convertToBson(odahuDTO)));
        return updateResult.getModifiedCount() > 0;
    }

    @Override
    public void updateStatus(String name, String project, String endpoint, UserInstanceStatus status) {
        BasicDBObject dbObject = new BasicDBObject();
        dbObject.put(ODAHU_FIELD + ".$." + STATUS_FIELD, status.name());
        updateOne(PROJECTS_COLLECTION, and(elemMatch(ODAHU_FIELD, eq(NAME_FIELD, name)),
                odahuProjectEndpointCondition(project, endpoint)), new Document(SET, dbObject));
    }

    @Override
    public void updateStatusAndUrls(String name, String project, String endpoint, List<ResourceURL> urls,
                                    UserInstanceStatus status) {
        BasicDBObject dbObject = new BasicDBObject();
        dbObject.put(ODAHU_FIELD + ".$." + STATUS_FIELD, status.name());
        dbObject.put(ODAHU_FIELD + ".$." + URLS_FIELD, getResourceUrlData(urls));
        updateOne(PROJECTS_COLLECTION, and(elemMatch(ODAHU_FIELD, eq(NAME_FIELD, name)),
                odahuProjectEndpointCondition(project, endpoint)), new Document(SET, dbObject));
    }

    private Bson odahuProjectEndpointCondition(String projectName, String endpointName) {
        return elemMatch(ODAHU_FIELD, and(eq(ENDPOINT_FIELD, endpointName), eq(PROJECT_FIELD, projectName)));
    }

    private Bson projectEndpointCondition(String projectName, String endpointName) {
        return and(eq(NAME_FIELD, projectName), and(elemMatch(ENDPOINTS, eq(NAME_FIELD, endpointName))));
    }

    private List<Map<String, String>> getResourceUrlData(List<ResourceURL> urls) {
        return urls.stream()
                .map(this::toUrlDocument)
                .collect(toList());
    }

    private LinkedHashMap<String, String> toUrlDocument(ResourceURL url) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put(COMPUTATIONAL_URL_URL, url.getUrl());
        map.put(COMPUTATIONAL_URL_DESC, url.getDescription());
        return map;
    }
}
