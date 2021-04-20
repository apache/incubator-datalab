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

import com.epam.datalab.backendapi.util.DateRemoverUtil;
import com.epam.datalab.dto.ResourceURL;
import com.epam.datalab.dto.SchedulerJobDTO;
import com.epam.datalab.dto.StatusEnvBaseDTO;
import com.epam.datalab.dto.UserInstanceDTO;
import com.epam.datalab.dto.UserInstanceStatus;
import com.epam.datalab.dto.aws.computational.ClusterConfig;
import com.epam.datalab.dto.base.DataEngineType;
import com.epam.datalab.dto.computational.ComputationalStatusDTO;
import com.epam.datalab.dto.computational.UserComputationalResource;
import com.epam.datalab.exceptions.DatalabException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.UpdateResult;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.epam.datalab.backendapi.dao.ExploratoryDAO.COMPUTATIONAL_RESOURCES;
import static com.epam.datalab.backendapi.dao.ExploratoryDAO.UPTIME;
import static com.epam.datalab.backendapi.dao.ExploratoryDAO.exploratoryCondition;
import static com.epam.datalab.backendapi.dao.MongoCollections.USER_INSTANCES;
import static com.epam.datalab.backendapi.dao.SchedulerJobDAO.SCHEDULER_DATA;
import static com.epam.datalab.dto.UserInstanceStatus.TERMINATED;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Filters.ne;
import static com.mongodb.client.model.Filters.not;
import static com.mongodb.client.model.Projections.elemMatch;
import static com.mongodb.client.model.Projections.excludeId;
import static com.mongodb.client.model.Projections.fields;
import static com.mongodb.client.model.Projections.include;
import static com.mongodb.client.model.Updates.push;
import static com.mongodb.client.model.Updates.set;
import static java.util.stream.Collectors.toList;

/**
 * DAO for user computational resources.
 */
@Slf4j
public class ComputationalDAO extends BaseDAO {
    static final String COMPUTATIONAL_NAME = "computational_name";
    static final String COMPUTATIONAL_ID = "computational_id";
    static final String PROJECT = "project";
    static final String ENDPOINT = "endpoint";

    static final String IMAGE = "image";
    private static final String COMPUTATIONAL_URL = "computational_url";
    private static final String EXPLORATORY_NAME = "exploratory_name";
    private static final String COMPUTATIONAL_URL_DESC = "description";
    private static final String COMPUTATIONAL_URL_URL = "url";
    private static final String COMPUTATIONAL_LAST_ACTIVITY = "last_activity";
    private static final String CONFIG = "config";

    private static String computationalFieldFilter(String fieldName) {
        return COMPUTATIONAL_RESOURCES + FIELD_SET_DELIMETER + fieldName;
    }

    private static Bson computationalCondition(String user, String project, String exploratoryName, String compName) {
        return and(eq(USER, user), eq(PROJECT, project), eq(EXPLORATORY_NAME, exploratoryName),
                eq(COMPUTATIONAL_RESOURCES + "." + COMPUTATIONAL_NAME, compName));
    }

    /**
     * Add the user's computational resource for notebook into database.
     *
     * @param user             user name.
     * @param exploratoryName  name of exploratory.
     * @param project          name of project
     * @param computationalDTO object of computational resource.
     * @return <b>true</b> if operation was successful, otherwise <b>false</b>.
     */
    public boolean addComputational(String user, String exploratoryName, String project,
                                    UserComputationalResource computationalDTO) {
        final UpdateResult updateResult = updateOne(USER_INSTANCES,
                and(exploratoryCondition(user, exploratoryName, project),
                        not(elemMatch(COMPUTATIONAL_RESOURCES,
                                eq(COMPUTATIONAL_NAME, computationalDTO.getComputationalName())))),
                push(COMPUTATIONAL_RESOURCES, convertToBson(computationalDTO)));
        return updateResult.getModifiedCount() > 0;
    }

    /**
     * Finds and returns the of computational resource.
     *
     * @param user              user name.
     * @param project           project name
     * @param exploratoryName   the name of exploratory.
     * @param computationalName name of computational resource.
     * @throws DatalabException if exception occurs
     */
    public UserComputationalResource fetchComputationalFields(String user, String project, String exploratoryName,
                                                              String computationalName) {
        Optional<UserInstanceDTO> opt = findOne(USER_INSTANCES,
                and(exploratoryCondition(user, exploratoryName, project),
                        Filters.elemMatch(COMPUTATIONAL_RESOURCES, eq(COMPUTATIONAL_NAME, computationalName))),
                fields(include(COMPUTATIONAL_RESOURCES + ".$"), excludeId()),
                UserInstanceDTO.class);
        return opt.map(UserInstanceDTO::getResources)
                .filter(l -> !l.isEmpty())
                .map(l -> l.get(0))
                .orElseThrow(() -> new DatalabException("Computational resource " + computationalName + " for user " + user + " with " +
                        "exploratory name " + exploratoryName + " not found."));
    }

    public List<UserComputationalResource> findComputationalResourcesWithStatus(String user, String project, String exploratoryName,
                                                                                UserInstanceStatus status) {
        final UserInstanceDTO userInstanceDTO = findOne(USER_INSTANCES,
                and(exploratoryCondition(user, exploratoryName, project),
                        elemMatch(COMPUTATIONAL_RESOURCES, eq(STATUS, status.toString()))),
                fields(include(COMPUTATIONAL_RESOURCES), excludeId()),
                UserInstanceDTO.class)
                .orElseThrow(() -> new DatalabException(String.format("Computational resource with status %s for user " +
                        "%s with exploratory name %s not found.", status, user, exploratoryName)));
        return userInstanceDTO.getResources()
                .stream()
                .filter(computationalResource -> computationalResource.getStatus().equals(status.toString()))
                .collect(toList());
    }

    /**
     * Updates the status of computational resource in Mongo database.
     *
     * @param dto object of computational resource status.
     * @return The result of an update operation.
     */
    public UpdateResult updateComputationalStatus(ComputationalStatusDTO dto) {
        try {
            Document values = new Document(computationalFieldFilter(STATUS), dto.getStatus());
            return updateOne(USER_INSTANCES,
                    and(exploratoryCondition(dto.getUser(), dto.getExploratoryName(), dto.getProject()),
                            elemMatch(COMPUTATIONAL_RESOURCES,
                                    and(eq(COMPUTATIONAL_NAME, dto.getComputationalName()),
                                            not(eq(STATUS, TERMINATED.toString()))))),
                    new Document(SET, values));
        } catch (Exception t) {
            throw new DatalabException("Could not update computational resource status", t);
        }
    }

    /**
     * Updates the status of exploratory notebooks in Mongo database.
     *
     * @param dto object of exploratory status info.
     * @return The result of an update operation.
     */
    public int updateComputationalStatusesForExploratory(StatusEnvBaseDTO<?> dto) {
        Document values = new Document(computationalFieldFilter(STATUS), dto.getStatus());
        values.append(computationalFieldFilter(UPTIME), null);
        int count = 0;
        UpdateResult result;
        do {
            result = updateOne(USER_INSTANCES,
                    and(exploratoryCondition(dto.getUser(), dto.getExploratoryName(), dto.getProject()),
                            elemMatch(COMPUTATIONAL_RESOURCES,
                                    and(not(eq(STATUS, TERMINATED.toString())),
                                            not(eq(STATUS, dto.getStatus()))))),
                    new Document(SET, values));
            count += result.getModifiedCount();
        }
        while (result.getModifiedCount() > 0);

        return count;
    }

    public void updateComputationalStatusesForExploratory(String user, String project, String exploratoryName,
                                                          UserInstanceStatus dataengineStatus,
                                                          UserInstanceStatus dataengineServiceStatus,
                                                          UserInstanceStatus... excludedStatuses) {
        updateComputationalResource(user, project, exploratoryName, dataengineStatus,
                DataEngineType.SPARK_STANDALONE, excludedStatuses);
        updateComputationalResource(user, project, exploratoryName, dataengineServiceStatus,
                DataEngineType.CLOUD_SERVICE, excludedStatuses);
    }

    /**
     * Updates the status for single computational resource in Mongo database.
     *
     * @param user              user name.
     * @param project           project name
     * @param exploratoryName   exploratory's name.
     * @param computationalName name of computational resource.
     * @param newStatus         new status of computational resource.
     */

    public void updateStatusForComputationalResource(String user, String project, String exploratoryName,
                                                     String computationalName, UserInstanceStatus newStatus) {
        updateComputationalField(user, project, exploratoryName, computationalName, STATUS, newStatus.toString());
    }


    private void updateComputationalResource(String user, String project, String exploratoryName,
                                             UserInstanceStatus dataengineServiceStatus, DataEngineType cloudService,
                                             UserInstanceStatus... excludedStatuses) {
        UpdateResult result;
        do {
            result = updateMany(USER_INSTANCES,
                    computationalFilter(user, project, exploratoryName,
                            dataengineServiceStatus.toString(), DataEngineType.getDockerImageName(cloudService), excludedStatuses),
                    new Document(SET,
                            new Document(computationalFieldFilter(STATUS), dataengineServiceStatus.toString())));
        } while (result.getModifiedCount() > 0);
    }

    private Bson computationalFilter(String user, String project, String exploratoryName, String computationalStatus,
                                     String computationalImage, UserInstanceStatus[] excludedStatuses) {
        final String[] statuses = Arrays.stream(excludedStatuses)
                .map(UserInstanceStatus::toString)
                .toArray(String[]::new);
        return and(exploratoryCondition(user, exploratoryName, project),
                elemMatch(COMPUTATIONAL_RESOURCES, and(eq(IMAGE, computationalImage),
                        not(in(STATUS, statuses)),
                        not(eq(STATUS, computationalStatus)))));
    }

    /**
     * Updates the info of computational resource in Mongo database.
     *
     * @param dto object of computational resource status.
     * @return The result of an update operation.
     * @throws DatalabException if exception occurs
     */
    public UpdateResult updateComputationalFields(ComputationalStatusDTO dto) {
        try {
            Document values = new Document(computationalFieldFilter(STATUS), dto.getStatus());
            if (dto.getUptime() != null) {
                values.append(computationalFieldFilter(UPTIME), dto.getUptime());
            }
            if (dto.getInstanceId() != null) {
                values.append(computationalFieldFilter(INSTANCE_ID), dto.getInstanceId());
            }
            if (null != dto.getErrorMessage()) {
                values.append(computationalFieldFilter(ERROR_MESSAGE),
                        DateRemoverUtil.removeDateFormErrorMessage(dto.getErrorMessage()));
            }
            if (dto.getComputationalId() != null) {
                values.append(computationalFieldFilter(COMPUTATIONAL_ID), dto.getComputationalId());
            }
            if (dto.getResourceUrl() != null && !dto.getResourceUrl().isEmpty()) {
                values.append(computationalFieldFilter(COMPUTATIONAL_URL), getResourceUrlData(dto));
            }
            if (dto.getLastActivity() != null) {
                values.append(computationalFieldFilter(COMPUTATIONAL_LAST_ACTIVITY), dto.getLastActivity());
            }
            if (dto.getConfig() != null) {
                values.append(computationalFieldFilter(CONFIG),
                        dto.getConfig().stream().map(this::convertToBson).collect(toList()));
            }
            return updateOne(USER_INSTANCES, and(exploratoryCondition(dto.getUser(), dto.getExploratoryName(), dto.getProject()),
                    elemMatch(COMPUTATIONAL_RESOURCES,
                            and(eq(COMPUTATIONAL_NAME, dto.getComputationalName()),
                                    not(eq(STATUS, TERMINATED.toString()))))),
                    new Document(SET, values));
        } catch (Exception t) {
            throw new DatalabException("Could not update computational resource status", t);
        }
    }

    private List<Map<String, String>> getResourceUrlData(ComputationalStatusDTO dto) {
        return dto.getResourceUrl().stream()
                .map(this::toUrlDocument)
                .collect(toList());
    }

    private LinkedHashMap<String, String> toUrlDocument(ResourceURL url) {
        LinkedHashMap<String, String> map = new LinkedHashMap<>();
        map.put(COMPUTATIONAL_URL_DESC, url.getDescription());
        map.put(COMPUTATIONAL_URL_URL, url.getUrl());
        return map;
    }

    /**
     * Updates the requirement for reuploading key for single computational resource in Mongo database.
     *
     * @param user                user name.
     * @param project             project name
     * @param exploratoryName     exploratory's name.
     * @param computationalName   name of computational resource.
     * @param reuploadKeyRequired true/false.
     */

    public void updateReuploadKeyFlagForComputationalResource(String user, String project, String exploratoryName,
                                                              String computationalName, boolean reuploadKeyRequired) {
        updateComputationalField(user, project, exploratoryName, computationalName, REUPLOAD_KEY_REQUIRED, reuploadKeyRequired);
    }

    /**
     * Returns names of computational resources which status is among existing ones. Also these resources will
     * have predefined type.
     *
     * @param user                  user name.
     * @param project               project name
     * @param computationalTypes    type list of computational resource which may contain 'dataengine' and/or
     *                              'dataengine-service'.
     * @param exploratoryName       name of exploratory.
     * @param computationalStatuses statuses of computational resource.
     * @return list of computational resources' names
     */

    @SuppressWarnings("unchecked")
    public List<String> getComputationalResourcesWhereStatusIn(String user, String project,
                                                               List<DataEngineType> computationalTypes,
                                                               String exploratoryName,
                                                               UserInstanceStatus... computationalStatuses) {
        return stream((List<Document>) find(USER_INSTANCES, exploratoryCondition(user, exploratoryName, project),
                fields(include(COMPUTATIONAL_RESOURCES))).first().get(COMPUTATIONAL_RESOURCES))
                .filter(doc ->
                        statusList(computationalStatuses).contains(doc.getString(STATUS)) &&
                                computationalTypes.contains(DataEngineType.fromDockerImageName(doc.getString(IMAGE))))
                .map(doc -> doc.getString(COMPUTATIONAL_NAME)).collect(toList());
    }

    @SuppressWarnings("unchecked")
    public List<ClusterConfig> getClusterConfig(String user, String project, String exploratoryName, String computationalName) {
        return findOne(USER_INSTANCES,
                and(exploratoryCondition(user, exploratoryName, project),
                        Filters.elemMatch(COMPUTATIONAL_RESOURCES, and(eq(COMPUTATIONAL_NAME, computationalName),
                                notNull(CONFIG)))),
                fields(include(COMPUTATIONAL_RESOURCES + ".$"), excludeId())
        ).map(d -> ((List<Document>) d.get(COMPUTATIONAL_RESOURCES)).get(0))
                .map(d -> convertFromDocument((List<Document>) d.get(CONFIG),
                        new TypeReference<List<ClusterConfig>>() {
                        }))
                .orElse(Collections.emptyList());
    }

    /**
     * Updates computational resource's field.
     *
     * @param user              user name.
     * @param project           project name
     * @param exploratoryName   name of exploratory.
     * @param computationalName name of computational resource.
     * @param fieldName         computational field's name for updating.
     * @param fieldValue        computational field's value for updating.
     */

    private <T> UpdateResult updateComputationalField(String user, String project, String exploratoryName, String computationalName,
                                                      String fieldName, T fieldValue) {
        return updateOne(USER_INSTANCES,
                computationalCondition(user, project, exploratoryName, computationalName),
                set(computationalFieldFilter(fieldName), fieldValue));
    }

    public void updateSchedulerSyncFlag(String user, String project, String exploratoryName, boolean syncFlag) {
        final String syncStartField = SCHEDULER_DATA + ".sync_start_required";
        UpdateResult result;
        do {

            result = updateOne(USER_INSTANCES, and(exploratoryCondition(user, exploratoryName, project),
                    elemMatch(COMPUTATIONAL_RESOURCES, and(ne(SCHEDULER_DATA, null), ne(syncStartField, syncFlag)))),
                    set(computationalFieldFilter(syncStartField), syncFlag));

        } while (result.getModifiedCount() != 0);
    }

    public UpdateResult updateSchedulerDataForComputationalResource(String user, String project, String exploratoryName,
                                                                    String computationalName, SchedulerJobDTO dto) {
        return updateComputationalField(user, project, exploratoryName, computationalName,
                SCHEDULER_DATA, Objects.isNull(dto) ? null : convertToBson(dto));
    }

    public void updateLastActivity(String user, String project, String exploratoryName,
                                   String computationalName, LocalDateTime lastActivity) {
        updateOne(USER_INSTANCES,
                computationalCondition(user, project, exploratoryName, computationalName),
                set(computationalFieldFilter(COMPUTATIONAL_LAST_ACTIVITY),
                        Date.from(lastActivity.atZone(ZoneId.systemDefault()).toInstant())));
    }

    public void updateComputeStatus(String project, String endpoint, String computeName, String instanceId, UserInstanceStatus status) {
        updateOne(USER_INSTANCES,
                and(eq(PROJECT, project), eq(ENDPOINT, endpoint), eq(COMPUTATIONAL_RESOURCES + "." + INSTANCE_ID, instanceId),
                        eq(COMPUTATIONAL_RESOURCES + "." + COMPUTATIONAL_NAME, computeName)),
                set(computationalFieldFilter(STATUS), status.toString()));
    }
}