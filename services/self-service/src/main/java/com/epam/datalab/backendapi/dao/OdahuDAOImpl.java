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
import com.epam.datalab.backendapi.domain.ProjectDTO;
import com.epam.datalab.dto.ResourceURL;
import com.epam.datalab.dto.UserInstanceStatus;
import com.epam.datalab.dto.base.odahu.OdahuResult;
import com.epam.datalab.exceptions.DatalabException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.mongodb.BasicDBObject;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

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
    private static final String GRAFANA_ADMIN_FIELD = "grafana_admin";
    private static final String GRAFANA_PASSWORD_FIELD = "grafana_pass";
    private static final String OAUTH_COOKIE_SECRET_FIELD = "oauth_cookie_secret";
    private static final String DECRYPT_TOKEN_FIELD = "odahuflow_connection_decrypt_token";
    private static final String URLS_FIELD = "urls";
    private static final String COMPUTATIONAL_URL_DESC = "description";
    private static final String COMPUTATIONAL_URL_URL = "url";

    @Override
    public Optional<OdahuDTO> getByProjectEndpoint(String project, String endpoint) {
        Optional<ProjectDTO> projectDTO = findOne(PROJECTS_COLLECTION, odahuProjectEndpointCondition(project, endpoint),
                fields(include(ODAHU_FIELD), excludeId()),
                ProjectDTO.class);

        return projectDTO.flatMap(p -> p.getOdahu().stream()
                .filter(odahu -> project.equals(odahu.getProject()) && endpoint.equals(odahu.getEndpoint()))
                .findAny());
    }

    @Override
    public List<OdahuDTO> findOdahuClusters(String project, String endpoint) {
        Optional<ProjectDTO> projectDTO = findOne(PROJECTS_COLLECTION, odahuProjectEndpointCondition(project, endpoint),
                fields(include(ODAHU_FIELD), excludeId()),
                ProjectDTO.class);

        return projectDTO.map(p -> p.getOdahu().stream()
                .filter(odahu -> project.equals(odahu.getProject()) && endpoint.equals(odahu.getEndpoint()))
                .collect(Collectors.toList()))
                .orElse(new ArrayList<>());
    }

    @Override
    public OdahuFieldsDTO getFields(String name, String project, String endpoint) {
        Document odahuDocument = findOne(PROJECTS_COLLECTION, odahuProjectEndpointCondition(name, project, endpoint),
                fields(include(ODAHU_FIELD), excludeId()))
                .orElseThrow(() -> new DatalabException(project.toString() + " does not contain odahu " + name.toString() + " cluster"));

        List<OdahuFieldsDTO> list = convertFromDocument(odahuDocument.get(ODAHU_FIELD, ArrayList.class), new TypeReference<List<OdahuFieldsDTO>>() {
        });
        return list.stream()
                .filter(odahuFieldsDTO -> name.equals(odahuFieldsDTO.getName()))
                .findAny()
                .orElseThrow(() -> new DatalabException("Unable to find the " + name + " cluster fields"));
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
    public void updateStatusAndUrls(OdahuResult result, UserInstanceStatus status) {
        BasicDBObject dbObject = new BasicDBObject();
        dbObject.put(ODAHU_FIELD + ".$." + STATUS_FIELD, status.name());
        dbObject.put(ODAHU_FIELD + ".$." + URLS_FIELD, getResourceUrlData(result.getResourceUrls()));
        dbObject.put(ODAHU_FIELD + ".$." + GRAFANA_ADMIN_FIELD, result.getGrafanaAdmin());
        dbObject.put(ODAHU_FIELD + ".$." + GRAFANA_PASSWORD_FIELD, result.getGrafanaPassword());
        dbObject.put(ODAHU_FIELD + ".$." + OAUTH_COOKIE_SECRET_FIELD, result.getOauthCookieSecret());
        dbObject.put(ODAHU_FIELD + ".$." + DECRYPT_TOKEN_FIELD, result.getDecryptToken());
        updateOne(PROJECTS_COLLECTION, odahuProjectEndpointCondition(result.getName(), result.getProjectName(), result.getEndpointName()),
                new Document(SET, dbObject));
    }

    private Bson odahuProjectEndpointCondition(String name, String projectName, String endpointName) {
        return and(elemMatch(ODAHU_FIELD, eq(NAME_FIELD, name)), odahuProjectEndpointCondition(projectName, endpointName));
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
