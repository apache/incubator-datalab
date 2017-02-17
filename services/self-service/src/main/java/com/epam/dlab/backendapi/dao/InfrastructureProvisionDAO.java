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
import com.mongodb.client.result.UpdateResult;

import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

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
    private static final String EXPLORATORY_URL_DESC = "description";
    private static final String EXPLORATORY_URL_URL = "url";
    private static final String EXPLORATORY_USER = "exploratory_user";
    private static final String EXPLORATORY_PASSWORD = "exploratory_pass";
    private static final String EXPLORATORY_PRIVATE_IP = "private_ip";
    private static final String UPTIME = "up_time";
    private static final String COMPUTATIONAL_RESOURCES = "computational_resources";
    private static final String COMPUTATIONAL_NAME = "computational_name";
    private static final String COMPUTATIONAL_ID = "computational_id";

    private static final String SET = "$set";

    public static Bson exploratoryCondition(String user, String exploratoryName) {
        return and(eq(USER, user), eq(EXPLORATORY_NAME, exploratoryName));
    }

    private static String computationalFieldFilter(String fieldName) {
        return COMPUTATIONAL_RESOURCES + FIELD_SET_DELIMETER + fieldName;
    }

    /** Finds and returns the list of user resources. 
     * @param user name
     * @return
     */
    public Iterable<Document> find(String user) {
        return find(USER_INSTANCES, eq(USER, user));
    }

    /** Finds and returns the list of shapes.
     */
    public Iterable<Document> findShapes() {
        return mongoService.getCollection(SHAPES).find();
    }

    /** Finds and returns the unique id for exploratory.
     * @param user user name.
     * @param exploratoryName the name of exploratory.
     * @exception DlabException
     */
    public String fetchExploratoryId(String user, String exploratoryName) throws DlabException {
        return findOne(USER_INSTANCES,
                exploratoryCondition(user, exploratoryName),
                fields(include(EXPLORATORY_ID), excludeId()))
                .orElse(new Document())
                .getOrDefault(EXPLORATORY_ID, EMPTY).toString();
    }

    /** Finds and returns the status of exploratory.
     * @param user user name.
     * @param exploratoryName the name of exploratory.
     * @exception DlabException
     */
    public UserInstanceStatus fetchExploratoryStatus(String user, String exploratoryName) throws DlabException {
        return UserInstanceStatus.of(
                findOne(USER_INSTANCES,
                        exploratoryCondition(user, exploratoryName),
                        fields(include(STATUS), excludeId()))
                        .orElse(new Document())
                        .getOrDefault(STATUS, EMPTY).toString());
    }

    /** Finds and returns the info of exploratory.
     * @param user user name.
     * @param exploratoryName the name of exploratory.
     * @exception DlabException
     */
    public UserInstanceDTO fetchExploratoryFields(String user, String exploratoryName) throws DlabException {

        Optional<UserInstanceDTO> opt = findOne(USER_INSTANCES,
                exploratoryCondition(user, exploratoryName),
                fields(exclude(COMPUTATIONAL_RESOURCES)),
                UserInstanceDTO.class);

        if( opt.isPresent() ) {
            return opt.get();
        }
        throw new DlabException(String.format("Exploratory instance for user {} with name {} not found.", user, exploratoryName));
    }

    /** Inserts the info about notebook into Mongo database.
     * @param dto the info about notebook
     * @exception DlabException
     */
    public void insertExploratory(UserInstanceDTO dto) throws DlabException {
        insertOne(USER_INSTANCES, dto);
    }

    /** Updates the status of exploratory in Mongo database.
     * @param dto object of exploratory status info.
     * @return The result of an update operation.
     * @exception DlabException
     */
    public UpdateResult updateExploratoryStatus(StatusBaseDTO<?> dto) throws DlabException {
        return updateOne(USER_INSTANCES,
                exploratoryCondition(dto.getUser(), dto.getExploratoryName()),
                set(STATUS, dto.getStatus()));
    }

    /** Updates the info of exploratory in Mongo database.
     * @param dto object of exploratory status info.
     * @return The result of an update operation.
     * @exception DlabException
     */
	@SuppressWarnings("serial")
    public UpdateResult updateExploratoryFields(ExploratoryStatusDTO dto) throws DlabException {
        Document values = new Document(STATUS, dto.getStatus()).append(UPTIME, dto.getUptime());
    	if (dto.getInstanceId() != null) {
    		values.append(INSTANCE_ID, dto.getInstanceId());
    	}
        if (dto.getErrorMessage() != null) {
            values.append(ERROR_MESSAGE, dto.getErrorMessage());
        }
        if (dto.getExploratoryId() != null) {
            values.append(EXPLORATORY_ID, dto.getExploratoryId());
        }


        if (dto.getExploratoryUrl() != null) {
            values.append(EXPLORATORY_URL, dto.getExploratoryUrl().stream()
                    .map(url -> new LinkedHashMap<String, String>() {{
                        put(EXPLORATORY_URL_DESC, url.getDescription());
                        put(EXPLORATORY_URL_URL, url.getUrl());
                    }})
                    .collect(Collectors.toList()));
        } else {
            if (dto.getPrivateIp() != null) {
                UserInstanceDTO inst = fetchExploratoryFields(dto.getUser(),dto.getExploratoryName());
                if (!inst.getPrivateIp().equals(dto.getPrivateIp())) { // IP was changed
                    if (inst.getExploratoryUrl() != null) {
                        values.append(EXPLORATORY_URL, inst.getExploratoryUrl().stream()
                                .map(url -> new LinkedHashMap<String, String>() {{
                                    put(EXPLORATORY_URL_DESC, url.getDescription());
                                    put(EXPLORATORY_URL_URL, url.getUrl().replace(inst.getPrivateIp(),dto.getPrivateIp()));
                                }})
                                .collect(Collectors.toList()));
                    }
                }
            }
        }

        if (dto.getPrivateIp() != null) {
            values.append(EXPLORATORY_PRIVATE_IP, dto.getPrivateIp());
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

    /** Add the user's computational resource for notebook into database.
     * @param user user name.
     * @param exploratoryName name of exploratory.
     * @param computationalDTO object of computational resource.
     * @return <b>true</b> if operation was successful, otherwise <b>false</b>.
     * @exception DlabException
     */
    public boolean addComputational(String user, String exploratoryName, UserComputationalResourceDTO computationalDTO) throws DlabException {
        Optional<Document> optional = findOne(USER_INSTANCES,
        		and(exploratoryCondition(user, exploratoryName),
                        elemMatch(COMPUTATIONAL_RESOURCES,
                        		eq(COMPUTATIONAL_NAME, computationalDTO.getComputationalName())))
                );

        if (!optional.isPresent()) {
            updateOne(USER_INSTANCES,
                    exploratoryCondition(user, exploratoryName),
                    push(COMPUTATIONAL_RESOURCES, convertToBson(computationalDTO)));
            return true;
        } else {
            return false;
        }
    }

    /** Finds and returns the unique id for computational resource.
     * @param user user name.
     * @param exploratoryName the name of exploratory.
     * @param computationalName name of computational resource.
     * @exception DlabException
     */
    public String fetchComputationalId(String user, String exploratoryName, String computationalName) throws DlabException {
        Document doc = findOne(USER_INSTANCES,
                exploratoryCondition(user, exploratoryName),
                elemMatch(COMPUTATIONAL_RESOURCES, eq(COMPUTATIONAL_NAME, computationalName)))
                .orElse(new Document());
        return getDottedOrDefault(doc,
                computationalFieldFilter(COMPUTATIONAL_ID), EMPTY).toString();
    }

    /** Finds and returns the of computational resource.
     * @param user user name.
     * @param exploratoryName the name of exploratory.
     * @param computationalName name of computational resource.
     * @exception DlabException
     */
    public UserComputationalResourceDTO fetchComputationalFields(String user, String exploratoryName, String computationalName) throws DlabException {
    	Optional<UserInstanceDTO> opt = findOne(USER_INSTANCES,
                and(exploratoryCondition(user, exploratoryName),
                		elemMatch(COMPUTATIONAL_RESOURCES, eq(COMPUTATIONAL_NAME, computationalName))),
                fields(include(COMPUTATIONAL_RESOURCES), excludeId()),
                UserInstanceDTO.class);
        if( opt.isPresent() ) {
        	List<UserComputationalResourceDTO> list = opt.get().getResources();
        	UserComputationalResourceDTO comp = list.stream()
        			.filter(r -> r.getComputationalName().equals(computationalName))
        			.findFirst()
        			.orElse(null);
        	if (comp != null) {
        		return comp;
        	}
        }
        throw new DlabException("Computational resource " + computationalName + " for user " + user + " with exploratory name " +
        		exploratoryName + " not found.");
    }

    /** Updates the status of computational resource in Mongo database.
     * @param dto object of computational resource status.
     * @return The result of an update operation.
     * @exception DlabException
     */
    public UpdateResult updateComputationalStatus(ComputationalStatusDTO dto) throws DlabException {
        try {
            Document values = new Document(computationalFieldFilter(STATUS), dto.getStatus());
        	return updateOne(USER_INSTANCES,
                    and(exploratoryCondition(dto.getUser(), dto.getExploratoryName()),
                            elemMatch(COMPUTATIONAL_RESOURCES,
                            		and(eq(COMPUTATIONAL_NAME, dto.getComputationalName()),
                            				not(eq(STATUS, TERMINATED.toString()))))),
                    new Document(SET, values));
        } catch (Throwable t) {
            throw new DlabException("Could not update computational resource status", t);
        }
    }

    /** Updates the status of exploratory notebooks in Mongo database.
     * @param dto object of exploratory status info.
     * @return The result of an update operation.
     * @exception DlabException
     */
    public UpdateResult updateComputationalStatusesForExploratory(StatusBaseDTO<?> dto) throws DlabException {
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

    /** Updates the info of computational resource in Mongo database.
     * @param dto object of computational resource status.
     * @return The result of an update operation.
     * @exception DlabException
     */
    public UpdateResult updateComputationalFields(ComputationalStatusDTO dto) throws DlabException {
        try {
            Document values = new Document(computationalFieldFilter(STATUS), dto.getStatus());
        	if (dto.getUptime() != null) {
        		values.append(computationalFieldFilter(UPTIME), dto.getUptime());
        	}
        	if (dto.getInstanceId() != null) {
        		values.append(computationalFieldFilter(INSTANCE_ID), dto.getInstanceId());
        	}
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