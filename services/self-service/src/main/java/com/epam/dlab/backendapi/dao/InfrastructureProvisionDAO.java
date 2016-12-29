/***************************************************************************

 Copyright (c) 2016, EPAM SYSTEMS INC

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.

 ****************************************************************************/

package com.epam.dlab.backendapi.dao;

import com.epam.dlab.UserInstanceStatus;
import com.epam.dlab.backendapi.core.UserComputationalResourceDTO;
import com.epam.dlab.backendapi.core.UserInstanceDTO;
import com.epam.dlab.dto.StatusBaseDTO;
import com.epam.dlab.dto.computational.ComputationalStatusDTO;
import com.epam.dlab.dto.exploratory.ExploratoryStatusDTO;
import com.epam.dlab.exceptions.DlabException;
import com.mongodb.MongoWriteException;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.Optional;

import static com.epam.dlab.UserInstanceStatus.TERMINATED;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Projections.elemMatch;
import static com.mongodb.client.model.Projections.fields;
import static com.mongodb.client.model.Projections.include;
import static com.mongodb.client.model.Projections.excludeId;
import static com.mongodb.client.model.Projections.exclude;
import static com.mongodb.client.model.Updates.push;
import static com.mongodb.client.model.Updates.set;
import static org.apache.commons.lang3.StringUtils.EMPTY;

public class InfrastructureProvisionDAO extends BaseDAO {
    public static final String EXPLORATORY_NAME = "exploratory_name";
    private static final String EXPLORATORY_ID = "exploratory_id";
    private static final String EXPLORATORY_URL = "exploratory_url";
    private static final String EXPLORATORY_USER = "exploratory_user";
    private static final String EXPLORATORY_PASSWORD = "exploratory_pass";
    private static final String UPTIME = "up_time";
    private static final String COMPUTATIONAL_RESOURCES = "computational_resources";
    private static final String COMPUTATIONAL_NAME = "computational_name";
    private static final String COMPUTATIONAL_ID = "computational_id";

    private static final String SET = "$set";

    public static Bson exploratoryCondition(String user, String exploratoryName) {
        return and(eq(USER, user), eq(EXPLORATORY_NAME, exploratoryName));
    }

    public static Bson computationalCondition(String user,
                                              String exploratoryName,
                                              String computationalName) {
        return and(exploratoryCondition(user, exploratoryName),
                elemMatch(COMPUTATIONAL_RESOURCES, eq(COMPUTATIONAL_NAME, computationalName)));
    }

    public static Bson computationalConditionUnwind(String user,
                                                    String exploratoryName,
                                                    String computationalName) {
        return and(exploratoryCondition(user, exploratoryName),
                eq(computationalFieldDotted(COMPUTATIONAL_NAME), computationalName));
    }

    public static String computationalFieldFilter(String fieldName) {
        return COMPUTATIONAL_RESOURCES + FIELD_SET_DELIMETER + fieldName;
    }

    public static String computationalFieldDotted(String fieldName) {
        return COMPUTATIONAL_RESOURCES + FIELD_DELIMETER + fieldName;
    }

    public Iterable<Document> find(String user) {
        return find(USER_INSTANCES, eq(USER, user));
    }

    public Iterable<Document> findShapes() {
        return mongoService.getCollection(SHAPES).find();
    }

    public String fetchExploratoryId(String user, String exploratoryName) {
        return findOne(USER_INSTANCES,
                exploratoryCondition(user, exploratoryName),
                fields(include(EXPLORATORY_ID), excludeId()))
                .orElse(new Document())
                .getOrDefault(EXPLORATORY_ID, EMPTY).toString();
    }

    public UserInstanceStatus fetchExploratoryStatus(String user, String exploratoryName) {
        return UserInstanceStatus.of(
                findOne(USER_INSTANCES,
                        exploratoryCondition(user, exploratoryName),
                        fields(include(STATUS), excludeId()))
                        .orElse(new Document())
                        .getOrDefault(STATUS, EMPTY).toString());
    }

    public Optional<UserInstanceDTO> fetchExploratoryFields(String user, String exploratoryName) {
        return findOne(USER_INSTANCES,
                exploratoryCondition(user, exploratoryName),
                fields(exclude(COMPUTATIONAL_RESOURCES)),
                UserInstanceDTO.class);
    }

    public boolean insertExploratory(UserInstanceDTO dto) {
        try {
            insertOne(USER_INSTANCES, dto);
            return true;
        } catch (MongoWriteException e) {
            return false;
        }
    }

    public UpdateResult updateExploratoryStatus(StatusBaseDTO dto) {
        return updateOne(USER_INSTANCES,
                exploratoryCondition(dto.getUser(), dto.getExploratoryName()),
                set(STATUS, dto.getStatus()));
    }

    public UpdateResult updateExploratoryFields(ExploratoryStatusDTO dto) {
        Document values = new Document(STATUS, dto.getStatus()).append(UPTIME, dto.getUptime());
        if (dto.getExploratoryId() != null) {
            values.append(EXPLORATORY_ID, dto.getExploratoryId());
        }
        if (dto.getExploratoryUrl() != null) {
            values.append(EXPLORATORY_URL, dto.getExploratoryUrl());
        }
        if (dto.getExploratoryUser() != null) {
            values.append(EXPLORATORY_USER, dto.getExploratoryUser());
        }
        if (dto.getExploratoryPassword() != null) {
            values.append(EXPLORATORY_PASSWORD, dto.getExploratoryPassword());
        }
        return updateOne(USER_INSTANCES,
                exploratoryCondition(dto.getUser(), dto.getExploratoryName()),
                new Document(SET, values));
    }

    public boolean addComputational(String user, String exploratoryName, UserComputationalResourceDTO computationalDTO) {
        Optional<Document> optional = findOne(USER_INSTANCES,
                computationalCondition(user, exploratoryName, computationalDTO.getComputationalName()));

        if (!optional.isPresent()) {
            updateOne(USER_INSTANCES,
                    exploratoryCondition(user, exploratoryName),
                    push(COMPUTATIONAL_RESOURCES, convertToBson(computationalDTO)));
            return true;
        } else {
            return false;
        }
    }

    public String fetchComputationalId(String user, String exploratoryName, String computationalName) {
        Document doc = findOne(USER_INSTANCES,
                exploratoryCondition(user, exploratoryName),
                elemMatch(COMPUTATIONAL_RESOURCES, eq(COMPUTATIONAL_NAME, computationalName)))
                .orElse(new Document());
        return getDottedOrDefault(doc,
                computationalFieldFilter(COMPUTATIONAL_ID), EMPTY).toString();
    }

    public UpdateResult updateComputationalStatus(ComputationalStatusDTO dto) {
        return updateComputationalStatus(dto.getUser(), dto.getExploratoryName(), dto.getComputationalName(), dto.getStatus(), false);
    }

    private UpdateResult updateComputationalStatus(String user, String exploratoryName, String computationalName, String status, boolean clearUptime) {
        try {
            Document values = new Document(computationalFieldFilter(STATUS), status);
            if (clearUptime) {
                values.append(computationalFieldFilter(UPTIME), null);
            }
            return updateOne(USER_INSTANCES,
                    and(exploratoryCondition(user, exploratoryName),
                            elemMatch(COMPUTATIONAL_RESOURCES, and(eq(COMPUTATIONAL_NAME, computationalName), not(eq(STATUS, TERMINATED.toString()))))),
                    new Document(SET, values));
        } catch (Throwable t) {
            throw new DlabException("Could not update computational resource status", t);
        }
    }

    public UpdateResult updateComputationalStatusesForExploratory(StatusBaseDTO dto) {
        Document values = new Document(computationalFieldFilter(STATUS), dto.getStatus());
        values.append(computationalFieldFilter(UPTIME), null);
        long modifiedCount;
        UpdateResult result;
        UpdateResult lastUpdate = null;
        do {
            result = lastUpdate;
            lastUpdate = updateOne(USER_INSTANCES,
                    and(exploratoryCondition(dto.getUser(), dto.getExploratoryName()),
                            elemMatch(COMPUTATIONAL_RESOURCES,
                                    and(not(eq(STATUS, TERMINATED.toString())),
                                            not(eq(STATUS, dto.getStatus()))))),
                    new Document(SET, values));
            modifiedCount = lastUpdate.getModifiedCount();
        }
        while (modifiedCount > 0);
        return result;
    }

    public UpdateResult updateComputationalFields(ComputationalStatusDTO dto) {
        try {
            Document values = new Document(computationalFieldFilter(STATUS), dto.getStatus())
                    .append(computationalFieldFilter(UPTIME), dto.getUptime());
            if (dto.getComputationalId() != null) {
                values.append(computationalFieldFilter(COMPUTATIONAL_ID), dto.getComputationalId());
            }
            return updateOne(USER_INSTANCES, and(exploratoryCondition(dto.getUser(), dto.getExploratoryName()),
                    elemMatch(COMPUTATIONAL_RESOURCES,
                            and(eq(COMPUTATIONAL_NAME, dto.getComputationalName()),
                                    not(eq(STATUS, TERMINATED.toString()))))),
                    new Document(SET, values));
        } catch (Throwable t) {
            throw new DlabException("Could not update computational resource status", t);
        }
    }
}
