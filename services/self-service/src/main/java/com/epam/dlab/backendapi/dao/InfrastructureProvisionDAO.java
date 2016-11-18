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

import com.epam.dlab.backendapi.api.instance.UserComputationalResourceDTO;
import com.epam.dlab.backendapi.api.instance.UserInstanceDTO;
import com.epam.dlab.constants.UserInstanceStatus;
import com.epam.dlab.dto.StatusBaseDTO;
import com.epam.dlab.dto.computational.ComputationalStatusDTO;
import com.epam.dlab.dto.exploratory.ExploratoryStatusDTO;
import com.epam.dlab.exceptions.DlabException;
import com.mongodb.MongoWriteException;
import org.bson.Document;

import java.util.Optional;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.push;
import static com.mongodb.client.model.Updates.set;
import static org.apache.commons.lang3.StringUtils.EMPTY;

public class InfrastructureProvisionDAO extends BaseDAO {
    public static final String EXPLORATORY_NAME = "exploratory_name";
    private static final String EXPLORATORY_ID = "exploratory_id";
    private static final String EXPLORATORY_URL = "exploratory_url";
    private static final String UPTIME = "up_time";
    private static final String COMPUTATIONAL_RESOURCES = "computational_resources";
    private static final String COMPUTATIONAL_NAME = "computational_name";
    private static final String COMPUTATIONAL_ID = "computational_id";

    private static final String SET = "$set";

    public Iterable<Document> find(String user) {
        return mongoService.getCollection(USER_INSTANCES).find(eq(USER, user));
    }

    public Iterable<Document> findShapes() {
        return mongoService.getCollection(SHAPES).find();
    }

    public String fetchExploratoryId(String user, String exploratoryName) {
        return Optional.ofNullable(mongoService.getCollection(USER_INSTANCES)
                .find(and(eq(USER, user), eq(EXPLORATORY_NAME, exploratoryName))).first())
                .orElse(new Document())
                .getOrDefault(EXPLORATORY_ID, EMPTY).toString();
    }

    public UserInstanceStatus fetchExploratoryStatus(String user, String exploratoryName) {
        return UserInstanceStatus.of(
                Optional.ofNullable(mongoService.getCollection(USER_INSTANCES)
                        .find(and(eq(USER, user), eq(EXPLORATORY_NAME, exploratoryName))).first())
                        .orElse(new Document())
                        .getOrDefault(STATUS, EMPTY).toString());
    }

    public boolean insertExploratory(UserInstanceDTO dto) {
        try {
            insertOne(USER_INSTANCES, dto);
            return true;
        } catch (MongoWriteException e) {
            return false;
        }
    }

    public void updateExploratoryStatus(StatusBaseDTO dto) {
        update(USER_INSTANCES, and(eq(USER, dto.getUser()), eq(EXPLORATORY_NAME, dto.getExploratoryName())), set(STATUS, dto.getStatus()));
    }

    public void updateExploratoryFields(ExploratoryStatusDTO dto) {
        Document values = new Document(STATUS, dto.getStatus()).append(UPTIME, dto.getUptime());
        if (dto.getExploratoryId() != null) {
            values.append(EXPLORATORY_ID, dto.getExploratoryId());
        }
        if (dto.getExploratoryUrl() != null) {
            values.append(EXPLORATORY_URL, dto.getExploratoryUrl());
        }
        update(USER_INSTANCES, and(eq(USER, dto.getUser()), eq(EXPLORATORY_NAME, dto.getExploratoryName())), new Document(SET, values));
    }

    public void updateComputationalStatusesForExploratory(StatusBaseDTO dto) {
        find(USER_INSTANCES, and(eq(USER, dto.getUser()), eq(EXPLORATORY_NAME, dto.getExploratoryName())), UserInstanceDTO.class)
                .ifPresent(instance -> instance.getResources().forEach(resource -> {
                    updateComputationalStatus(dto, resource.getComputationalName());
                }));
    }

    public boolean addComputational(String user, String name, UserComputationalResourceDTO computationalDTO) {
        Optional<UserInstanceDTO> optional = find(USER_INSTANCES, and(eq(USER, user), eq(EXPLORATORY_NAME, name)), UserInstanceDTO.class);
        if (optional.isPresent()) {
            UserInstanceDTO dto = optional.get();
            long count = dto.getResources().stream()
                    .filter(i -> computationalDTO.getComputationalName().equals(i.getComputationalName()))
                    .count();
            if (count == 0) {
                update(USER_INSTANCES, eq(ID, dto.getId()), push(COMPUTATIONAL_RESOURCES, convertToBson(computationalDTO)));
                return true;
            } else {
                return false;
            }
        } else {
            throw new DlabException("User '" + user + "' has no records for environment '" + name + "'");
        }
    }

    @SuppressWarnings("unchecked")
    public String fetchComputationalId(String user, String exploratoryName, String computationalName) {
        return find(USER_INSTANCES, and(eq(USER, user), eq(EXPLORATORY_NAME, exploratoryName)), UserInstanceDTO.class)
                .flatMap(exploratory -> exploratory.getResources()
                        .stream()
                        .filter(computational -> computationalName.equals(computational.getComputationalName()))
                        .findFirst())
                .flatMap(computational -> Optional.ofNullable(computational.getComputationalId()))
                .orElse(EMPTY);
    }

    public void updateComputationalStatus(ComputationalStatusDTO dto) {
        updateComputationalStatus(dto.getUser(), dto.getExploratoryName(), dto.getComputationalName(), dto.getStatus(), false);
    }

    private void updateComputationalStatus(StatusBaseDTO dto, String computationalName) {
        updateComputationalStatus(dto.getUser(), dto.getExploratoryName(), computationalName, dto.getStatus(), true);
    }

    private void updateComputationalStatus(String user, String exploratoryName, String computationalName, String status, boolean clearUptime) {
        try {
            Document values = new Document(getComputationalSetPrefix() + STATUS, status);
            if (clearUptime) {
                values.append(getComputationalSetPrefix() + UPTIME, null);
            }
            update(USER_INSTANCES, and(eq(USER, user), eq(EXPLORATORY_NAME, exploratoryName)
                    , eq(COMPUTATIONAL_RESOURCES + FIELD_DELIMETER + COMPUTATIONAL_NAME, computationalName)),
                    new Document(SET, values));
        } catch (Throwable t) {
            throw new DlabException("Could not update computational resource status", t);
        }
    }

    public void updateComputationalFields(ComputationalStatusDTO dto) {
        try {
            Document values = new Document(getComputationalSetPrefix() + STATUS, dto.getStatus())
                    .append(getComputationalSetPrefix() + UPTIME, dto.getUptime());
            if (dto.getComputationalId() != null) {
                values.append(getComputationalSetPrefix() + COMPUTATIONAL_ID, dto.getComputationalId());
            }
            update(USER_INSTANCES, and(eq(USER, dto.getUser()), eq(EXPLORATORY_NAME, dto.getExploratoryName())
                    , eq(COMPUTATIONAL_RESOURCES + FIELD_DELIMETER + COMPUTATIONAL_NAME, dto.getComputationalName())),
                    new Document(SET, values));
        } catch (Throwable t) {
            throw new DlabException("Could not update computational resource status", t);
        }
    }

    private String getComputationalSetPrefix() {
        return COMPUTATIONAL_RESOURCES + FIELD_SET_DELIMETER;
    }
}
