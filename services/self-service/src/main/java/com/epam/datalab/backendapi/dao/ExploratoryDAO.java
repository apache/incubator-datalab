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
import com.epam.datalab.dto.exploratory.ExploratoryStatusDTO;
import com.epam.datalab.exceptions.DatalabException;
import com.epam.datalab.exceptions.ResourceNotFoundException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.inject.Singleton;
import com.mongodb.client.result.UpdateResult;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.epam.datalab.backendapi.dao.MongoCollections.USER_INSTANCES;
import static com.epam.datalab.backendapi.dao.SchedulerJobDAO.SCHEDULER_DATA;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Filters.not;
import static com.mongodb.client.model.Filters.or;
import static com.mongodb.client.model.Projections.exclude;
import static com.mongodb.client.model.Projections.excludeId;
import static com.mongodb.client.model.Projections.fields;
import static com.mongodb.client.model.Projections.include;
import static com.mongodb.client.model.Updates.set;
import static java.util.stream.Collectors.toList;

/**
 * DAO for user exploratory.
 */
@Slf4j
@Singleton
public class ExploratoryDAO extends BaseDAO {
    public static final String COMPUTATIONAL_RESOURCES = "computational_resources";
    static final String EXPLORATORY_ID = "exploratory_id";
    static final String EXPLORATORY_NAME = "exploratory_name";
    static final String UPTIME = "up_time";

    private static final String COMPUTATIONAL_NAME = "computational_name";
    private static final String EXPLORATORY_URL = "exploratory_url";
    private static final String EXPLORATORY_URL_DESC = "description";
    private static final String EXPLORATORY_URL_URL = "url";
    private static final String EXPLORATORY_USER = "exploratory_user";
    private static final String EXPLORATORY_PASS = "exploratory_pass";
    private static final String CLUSTER_CONFIG = "cluster_config";
    private static final String EXPLORATORY_PRIVATE_IP = "private_ip";
    public static final String EXPLORATORY_NOT_FOUND_MSG = "Exploratory for user %s with name %s not found";
    private static final String EXPLORATORY_LAST_ACTIVITY = "last_activity";
    private static final String PROJECT = "project";
    private static final String ENDPOINT = "endpoint";

    public ExploratoryDAO() {
        log.info("{} is initialized", getClass().getSimpleName());
    }

    static Bson exploratoryCondition(String user, String exploratoryName, String project) {
        return and(eq(USER, user), eq(EXPLORATORY_NAME, exploratoryName), eq(PROJECT, project));
    }

    private static Bson runningExploratoryCondition(String user, String exploratoryName, String project) {
        return and(eq(USER, user), eq(PROJECT, project),
                and(eq(EXPLORATORY_NAME, exploratoryName), eq(STATUS, UserInstanceStatus.RUNNING.toString())));
    }

    static Bson runningExploratoryAndComputationalCondition(String user, String project, String exploratoryName,
                                                            String computationalName) {
        return and(eq(USER, user), eq(PROJECT, project),
                and(eq(EXPLORATORY_NAME, exploratoryName), eq(STATUS, UserInstanceStatus.RUNNING.toString()),
                        eq(COMPUTATIONAL_RESOURCES + "." + COMPUTATIONAL_NAME, computationalName),
                        eq(COMPUTATIONAL_RESOURCES + "." + STATUS, UserInstanceStatus.RUNNING.toString())));
    }

    public List<UserInstanceDTO> findExploratories(String user, String project) {
        return getUserInstances(and(eq(USER, user), eq(PROJECT, project)), true);
    }

    /**
     * Finds and returns the info of all user's running notebooks.
     *
     * @param user user name.
     */
    public List<UserInstanceDTO> fetchRunningExploratoryFields(String user) {
        return getUserInstances(and(eq(USER, user), eq(STATUS, UserInstanceStatus.RUNNING.toString())), false);
    }

    public List<UserInstanceDTO> fetchRunningExploratoryFieldsForProject(String project) {
        return getUserInstances(and(eq(PROJECT, project), eq(STATUS, UserInstanceStatus.RUNNING.toString())), false);
    }

    public List<UserInstanceDTO> fetchRunningExploratoryFieldsForProject(String project, List<String> endpoints) {
        return getUserInstances(and(eq(PROJECT, project), eq(STATUS, UserInstanceStatus.RUNNING.toString()), in(ENDPOINT, endpoints)), false);
    }

    public List<UserInstanceDTO> fetchExploratoryFieldsForProject(String project) {
        return getUserInstances(and(eq(PROJECT, project)), false);
    }

    public List<UserInstanceDTO> fetchExploratoryFieldsForProjectWithComp(String project) {
        return getUserInstances(and(eq(PROJECT, project)), true);
    }

    public List<UserInstanceDTO> fetchExploratoryFieldsForProjectWithComp(List<String> projects) {
        return getUserInstances(and(in(PROJECT, projects)), true);
    }

    public List<UserInstanceDTO> findExploratories(String project, String endpoint, String user) {
        return getUserInstances(and(eq(PROJECT, project), eq(ENDPOINT, endpoint), eq(USER, user)), true);
    }

    public List<UserInstanceDTO> fetchUserExploratoriesWhereStatusIn(String user, boolean computationalFieldsRequired,
                                                                     UserInstanceStatus... statuses) {
        final List<String> statusList = statusList(statuses);
        return getUserInstances(
                and(
                        eq(USER, user),
                        in(STATUS, statusList)
                ),
                computationalFieldsRequired);
    }

    /**
     * Finds and returns the info of all user's notebooks whose status or status of affiliated computational resource
     * is present among predefined ones.
     *
     * @param user                  user name.
     * @param exploratoryStatuses   array of exploratory statuses.
     * @param computationalStatuses array of computational statuses.
     */
    public List<UserInstanceDTO> fetchUserExploratoriesWhereStatusIn(String user,
                                                                     List<UserInstanceStatus> exploratoryStatuses,
                                                                     UserInstanceStatus... computationalStatuses) {
        final List<String> exploratoryStatusList = statusList(exploratoryStatuses);
        final List<String> computationalStatusList = statusList(computationalStatuses);
        return getUserInstances(
                and(
                        eq(USER, user),
                        or(in(STATUS, exploratoryStatusList),
                                in(COMPUTATIONAL_RESOURCES + "." + STATUS, computationalStatusList))
                ),
                false);
    }

    public List<UserInstanceDTO> fetchProjectExploratoriesWhereStatusIn(String project,
                                                                        List<UserInstanceStatus> exploratoryStatuses,
                                                                        UserInstanceStatus... computationalStatuses) {
        final List<String> exploratoryStatusList = statusList(exploratoryStatuses);
        final List<String> computationalStatusList = statusList(computationalStatuses);
        return getUserInstances(
                and(
                        eq(PROJECT, project),
                        or(in(STATUS, exploratoryStatusList),
                                in(COMPUTATIONAL_RESOURCES + "." + STATUS, computationalStatusList))
                ),
                false);
    }

    public List<UserInstanceDTO> fetchProjectEndpointExploratoriesWhereStatusIn(String project, List<String> endpoints,
                                                                                List<UserInstanceStatus> exploratoryStatuses,
                                                                                UserInstanceStatus... computationalStatuses) {
        final List<String> exploratoryStatusList = statusList(exploratoryStatuses);
        final List<String> computationalStatusList = statusList(computationalStatuses);
        return getUserInstances(
                and(
                        eq(PROJECT, project),
                        in(ENDPOINT, endpoints),
                        or(in(STATUS, exploratoryStatusList),
                                in(COMPUTATIONAL_RESOURCES + "." + STATUS, computationalStatusList))
                ),
                false);
    }

    public List<UserInstanceDTO> fetchProjectExploratoriesWhereStatusNotIn(String project, String endpoint,
                                                                           UserInstanceStatus... statuses) {
        final List<String> statusList = statusList(statuses);
        return getUserInstances(
                and(
                        eq(PROJECT, project),
                        eq(ENDPOINT, endpoint),
                        not(in(STATUS, statusList))
                ),
                false);
    }

    public List<UserInstanceDTO> fetchExploratoriesByEndpointWhereStatusIn(List<String> endpoints,
                                                                           List<UserInstanceStatus> statuses,
                                                                           boolean computationalFieldsRequired) {
        final List<String> exploratoryStatusList = statusList(statuses);

        return getUserInstances(
                and(
                        in(ENDPOINT, endpoints),
                        in(STATUS, exploratoryStatusList)
                ),
                computationalFieldsRequired);
    }

	public List<UserInstanceDTO> fetchExploratoriesByEndpointWhereStatusNotIn(String endpoint, List<UserInstanceStatus> statuses,
	                                                                          boolean computationalFieldsRequired) {
		final List<String> exploratoryStatusList = statusList(statuses);

		return getUserInstances(
				and(
						eq(ENDPOINT, endpoint),
						not(in(STATUS, exploratoryStatusList))
				),
				computationalFieldsRequired);
	}

    private List<UserInstanceDTO> getUserInstances(Bson condition, boolean computationalFieldsRequired) {
        return stream(getCollection(USER_INSTANCES)
                .find(condition)
                .projection(computationalFieldsRequired ? null : fields(exclude(COMPUTATIONAL_RESOURCES))))
                .map(d -> convertFromDocument(d, UserInstanceDTO.class))
                .collect(Collectors.toList());
    }

    /**
     * Finds and returns the info about all exploratories in database.
     **/
    public List<UserInstanceDTO> getInstances() {
        return stream(getCollection(USER_INSTANCES)
                .find())
                .map(d -> convertFromDocument(d, UserInstanceDTO.class))
                .collect(Collectors.toList());
    }

    public void updateLastActivity(String user, String exploratoryName, LocalDateTime lastActivity) {
        updateOne(USER_INSTANCES, and(eq(USER, user), eq(EXPLORATORY_NAME, exploratoryName)),
                set(EXPLORATORY_LAST_ACTIVITY, toDate(lastActivity)));
    }

    private Date toDate(LocalDateTime lastActivity) {
        return Date.from(lastActivity.atZone(ZoneId.systemDefault()).toInstant());
    }

    /**
     * Finds and returns the info of exploratory (without info about computational resources).
     *
     * @param user            user name.
     * @param project         project name
     * @param exploratoryName the name of exploratory.
     */
    public UserInstanceDTO fetchExploratoryFields(String user, String project, String exploratoryName) {
        return getExploratory(user, project, exploratoryName, false).orElseThrow(() ->
                new ResourceNotFoundException(String.format(EXPLORATORY_NOT_FOUND_MSG, user, exploratoryName)));

    }

    public UserInstanceDTO fetchExploratoryFields(String user, String project, String exploratoryName, boolean includeCompResources) {
        return getExploratory(user, project, exploratoryName, includeCompResources).orElseThrow(() ->
                new ResourceNotFoundException(String.format(EXPLORATORY_NOT_FOUND_MSG, user, exploratoryName)));

    }

    private Optional<UserInstanceDTO> getExploratory(String user, String project, String exploratoryName,
                                                     boolean includeCompResources) {
        return findOne(USER_INSTANCES,
                exploratoryCondition(user, exploratoryName, project),
                includeCompResources ? null : fields(exclude(COMPUTATIONAL_RESOURCES)),
                UserInstanceDTO.class);
    }

    /**
     * Finds and returns the info of running exploratory with running cluster.
     *
     * @param user              user name.
     * @param project           name of project
     * @param exploratoryName   name of exploratory.
     * @param computationalName name of cluster
     */
    public UserInstanceDTO fetchExploratoryFields(String user, String project, String exploratoryName, String computationalName) {
        return findOne(USER_INSTANCES,
                runningExploratoryAndComputationalCondition(user, project, exploratoryName, computationalName),
                UserInstanceDTO.class)
                .orElseThrow(() -> new DatalabException(String.format("Running notebook %s with running cluster %s not " +
                                "found for user %s",
                        exploratoryName, computationalName, user)));
    }

    /**
     * Finds and returns the info of running exploratory.
     *
     * @param user            user name.
     * @param project         project
     * @param exploratoryName name of exploratory.
     */
    public UserInstanceDTO fetchRunningExploratoryFields(String user, String project, String exploratoryName) {
        return findOne(USER_INSTANCES, runningExploratoryCondition(user, exploratoryName, project),
                fields(exclude(COMPUTATIONAL_RESOURCES)), UserInstanceDTO.class)
                .orElseThrow(() -> new DatalabException(
                        String.format("Running exploratory instance for user %s with name %s not found.",
                                user, exploratoryName)));
    }

    /**
     * Inserts the info about notebook into Mongo database.
     *
     * @param dto the info about notebook
     */
    public void insertExploratory(UserInstanceDTO dto) {
        insertOne(USER_INSTANCES, dto);
    }

    /**
     * Updates the status of exploratory in Mongo database.
     *
     * @param dto object of exploratory status info.
     * @return The result of an update operation.
     */
    public UpdateResult updateExploratoryStatus(StatusEnvBaseDTO<?> dto) {
        return updateOne(USER_INSTANCES,
                exploratoryCondition(dto.getUser(), dto.getExploratoryName(), dto.getProject()),
                set(STATUS, dto.getStatus()));
    }

    /**
     * Updates status for single exploratory in Mongo database.
     *
     * @param user            user.
     * @param project         project name
     * @param exploratoryName name of exploratory.
     * @param newStatus       new status of exploratory.
     * @return The result of an update operation.
     */
    public UpdateResult updateStatusForExploratory(String user, String project, String exploratoryName, UserInstanceStatus newStatus) {
        return updateOne(USER_INSTANCES,
                exploratoryCondition(user, exploratoryName, project),
                set(STATUS, newStatus.toString()));
    }

    public UpdateResult updateExploratoryStatus(String project, String endpoint, String name, String instanceId, UserInstanceStatus status) {
        return updateOne(USER_INSTANCES,
                and(eq(ENDPOINT, endpoint), eq(PROJECT, project), eq(EXPLORATORY_NAME, name), eq(INSTANCE_ID, instanceId)),
                set(STATUS, status.toString()));
    }

    /**
     * Updates the scheduler's data for exploratory in Mongo database.
     *
     * @param user            user.
     * @param project         name of project
     * @param exploratoryName name of exploratory.
     * @param dto             object of scheduler data.
     * @return The result of an update operation.
     */
    public UpdateResult updateSchedulerDataForUserAndExploratory(String user, String project, String exploratoryName,
                                                                 SchedulerJobDTO dto) {
        return updateOne(USER_INSTANCES,
                exploratoryCondition(user, exploratoryName, project),
                set(SCHEDULER_DATA, Objects.isNull(dto) ? null : convertToBson(dto)));
    }

    /**
     * Updates the requirement for reuploading key for single exploratory in Mongo database.
     *
     * @param user                user name.
     * @param project             project name
     * @param exploratoryName     exploratory's name
     * @param reuploadKeyRequired true/false.
     */
    public void updateReuploadKeyForExploratory(String user, String project, String exploratoryName, boolean reuploadKeyRequired) {
        updateOne(USER_INSTANCES,
                exploratoryCondition(user, exploratoryName, project),
                set(REUPLOAD_KEY_REQUIRED, reuploadKeyRequired));
    }


    /**
     * Updates the info of exploratory in Mongo database.
     *
     * @param dto object of exploratory status info.
     * @return The result of an update operation.
     */
    @SuppressWarnings("serial")
    public UpdateResult updateExploratoryFields(ExploratoryStatusDTO dto) {
        Document values = new Document(STATUS, dto.getStatus()).append(UPTIME, dto.getUptime());
        if (dto.getInstanceId() != null) {
            values.append(INSTANCE_ID, dto.getInstanceId());
        }
        if (dto.getErrorMessage() != null) {
            values.append(ERROR_MESSAGE, DateRemoverUtil.removeDateFormErrorMessage(dto.getErrorMessage()));
        }
        if (dto.getExploratoryId() != null) {
            values.append(EXPLORATORY_ID, dto.getExploratoryId());
        }

        if (dto.getLastActivity() != null) {
            values.append(EXPLORATORY_LAST_ACTIVITY, dto.getLastActivity());
        }

        if (dto.getResourceUrl() != null) {
            values.append(EXPLORATORY_URL, dto.getResourceUrl().stream()
                    .map(url -> {
                                LinkedHashMap<String, String> map = new LinkedHashMap<>();
                                map.put(EXPLORATORY_URL_DESC, url.getDescription());
                                map.put(EXPLORATORY_URL_URL, url.getUrl());
                                return map;
                            }
                    ).collect(Collectors.toList()));
        } else if (dto.getPrivateIp() != null) {
            UserInstanceDTO inst = fetchExploratoryFields(dto.getUser(), dto.getProject(), dto.getExploratoryName());
            if (!inst.getPrivateIp().equals(dto.getPrivateIp()) && inst.getResourceUrl() != null) {
                values.append(EXPLORATORY_URL, inst.getResourceUrl().stream()
                        .map(url -> replaceIp(dto.getPrivateIp(), inst, url))
                        .collect(Collectors.toList()));
            }
        }

        if (dto.getPrivateIp() != null) {
            values.append(EXPLORATORY_PRIVATE_IP, dto.getPrivateIp());
        }
        if (dto.getExploratoryUser() != null) {
            values.append(EXPLORATORY_USER, dto.getExploratoryUser());
        }
        if (dto.getExploratoryPassword() != null) {
            values.append(EXPLORATORY_PASS, dto.getExploratoryPassword());
        }
        if (dto.getConfig() != null) {
            values.append(CLUSTER_CONFIG, dto.getConfig().stream().map(this::convertToBson).collect(toList()));
        }
        return updateOne(USER_INSTANCES,
                exploratoryCondition(dto.getUser(), dto.getExploratoryName(), dto.getProject()),
                new Document(SET, values));
    }

    public void updateExploratoryIp(String user, String project, String ip, String exploratoryName) {

        UserInstanceDTO inst = fetchExploratoryFields(user, project, exploratoryName);
        if (!inst.getPrivateIp().equals(ip)) {
            Document values = new Document();
            values.append(EXPLORATORY_PRIVATE_IP, ip);
            if (inst.getResourceUrl() != null) {
                values.append(EXPLORATORY_URL, inst.getResourceUrl().stream()
                        .map(url -> replaceIp(ip, inst, url)
                        ).collect(Collectors.toList()));
            }

            updateOne(USER_INSTANCES,
                    exploratoryCondition(user, exploratoryName, project),
                    new Document(SET, values));
        }

    }

    @SuppressWarnings("unchecked")
    public List<ClusterConfig> getClusterConfig(String user, String project, String exploratoryName) {
        return findOne(USER_INSTANCES, and(exploratoryCondition(user, exploratoryName, project), notNull(CLUSTER_CONFIG)),
                fields(include(CLUSTER_CONFIG), excludeId()))
                .map(d -> convertFromDocument((List<Document>) d.get(CLUSTER_CONFIG),
                        new TypeReference<List<ClusterConfig>>() {
                        }))
                .orElse(Collections.emptyList());
    }

    private Map<String, String> replaceIp(String ip, UserInstanceDTO inst, ResourceURL url) {
        Map<String, String> map = new LinkedHashMap<>();
        map.put(EXPLORATORY_URL_DESC, url.getDescription());
        map.put(EXPLORATORY_URL_URL, url.getUrl().replace(inst.getPrivateIp(), ip));
        return map;
    }
}