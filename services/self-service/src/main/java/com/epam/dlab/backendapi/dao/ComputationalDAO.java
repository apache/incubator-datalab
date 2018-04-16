/*
 * Copyright (c) 2017, EPAM SYSTEMS INC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.dlab.backendapi.dao;

import com.epam.dlab.UserInstanceStatus;
import com.epam.dlab.backendapi.util.DateRemoverUtil;
import com.epam.dlab.dto.SchedulerJobDTO;
import com.epam.dlab.dto.StatusEnvBaseDTO;
import com.epam.dlab.dto.UserInstanceDTO;
import com.epam.dlab.dto.base.DataEngineType;
import com.epam.dlab.dto.computational.ComputationalStatusDTO;
import com.epam.dlab.dto.computational.UserComputationalResource;
import com.epam.dlab.exceptions.DlabException;
import com.mongodb.client.result.UpdateResult;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.epam.dlab.UserInstanceStatus.FAILED;
import static com.epam.dlab.UserInstanceStatus.TERMINATED;
import static com.epam.dlab.backendapi.dao.ExploratoryDAO.*;
import static com.epam.dlab.backendapi.dao.SchedulerJobDAO.SCHEDULER_DATA;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Projections.elemMatch;
import static com.mongodb.client.model.Projections.*;
import static com.mongodb.client.model.Updates.push;
import static com.mongodb.client.model.Updates.set;
import static org.apache.commons.lang3.StringUtils.EMPTY;

/**
 * DAO for user computational resources.
 */
@Slf4j
public class ComputationalDAO extends BaseDAO {
	static final String COMPUTATIONAL_NAME = "computational_name";
	static final String COMPUTATIONAL_ID = "computational_id";

	static final String IMAGE = "image";
	public static final String COMPUTATIONAL_NOT_FOUND_MSG = "Computational resource %s affiliated with exploratory " +
			"%s for user %s not found";

	private static String computationalFieldFilter(String fieldName) {
		return COMPUTATIONAL_RESOURCES + FIELD_SET_DELIMETER + fieldName;
	}

	private static Bson computationalCondition(String user, String exploratoryField, String exploratoryFieldValue,
											   String compResourceField, String compResourceFieldValue) {
		return and(eq(USER, user), eq(exploratoryField, exploratoryFieldValue),
				eq(COMPUTATIONAL_RESOURCES + "." + compResourceField, compResourceFieldValue));
	}

	/**
	 * Add the user's computational resource for notebook into database.
	 *
	 * @param user             user name.
	 * @param exploratoryName  name of exploratory.
	 * @param computationalDTO object of computational resource.
	 * @return <b>true</b> if operation was successful, otherwise <b>false</b>.
	 */
	public boolean addComputational(String user, String exploratoryName, UserComputationalResource computationalDTO) {
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

	/**
	 * Finds and returns the unique id for computational resource.
	 *
	 * @param user              user name.
	 * @param exploratoryName   the name of exploratory.
	 * @param computationalName name of computational resource.
	 */
	public String fetchComputationalId(String user, String exploratoryName, String computationalName) {
		Document doc = findOne(USER_INSTANCES,
				exploratoryCondition(user, exploratoryName),
				elemMatch(COMPUTATIONAL_RESOURCES, eq(COMPUTATIONAL_NAME, computationalName)))
				.orElse(new Document());
		return getDottedOrDefault(doc,
				computationalFieldFilter(COMPUTATIONAL_ID), EMPTY).toString();
	}

	/**
	 * Finds and returns the of computational resource.
	 *
	 * @param user              user name.
	 * @param exploratoryName   the name of exploratory.
	 * @param computationalName name of computational resource.
	 * @throws DlabException
	 */
	public UserComputationalResource fetchComputationalFields(String user, String exploratoryName,
															  String computationalName) {
		Optional<UserInstanceDTO> opt = findOne(USER_INSTANCES,
				and(exploratoryCondition(user, exploratoryName),
						elemMatch(COMPUTATIONAL_RESOURCES, eq(COMPUTATIONAL_NAME, computationalName))),
				fields(include(COMPUTATIONAL_RESOURCES), excludeId()),
				UserInstanceDTO.class);
		if (opt.isPresent()) {
			List<UserComputationalResource> list = opt.get().getResources();
			UserComputationalResource comp = list.stream()
					.filter(r -> r.getComputationalName().equals(computationalName))
					.findFirst()
					.orElse(null);
			if (comp != null) {
				return comp;
			}
		}
		throw new DlabException("Computational resource " + computationalName + " for user " + user + " with " +
				"exploratory name " + exploratoryName + " not found.");
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
					and(exploratoryCondition(dto.getUser(), dto.getExploratoryName()),
							elemMatch(COMPUTATIONAL_RESOURCES,
									and(eq(COMPUTATIONAL_NAME, dto.getComputationalName()),
											not(eq(STATUS, TERMINATED.toString()))))),
					new Document(SET, values));
		} catch (Exception t) {
			throw new DlabException("Could not update computational resource status", t);
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
					and(exploratoryCondition(dto.getUser(), dto.getExploratoryName()),
							elemMatch(COMPUTATIONAL_RESOURCES,
									and(not(eq(STATUS, TERMINATED.toString())),
											not(eq(STATUS, dto.getStatus()))))),
					new Document(SET, values));
			count += result.getModifiedCount();
		}
		while (result.getModifiedCount() > 0);

		return count;
	}

	public void updateComputationalStatusesForExploratory(String user, String exploratoryName,
														  UserInstanceStatus dataengineStatus,
														  UserInstanceStatus dataengineServiceStatus) {
		updateComputationalResource(user, exploratoryName, dataengineStatus, DataEngineType.SPARK_STANDALONE);
		updateComputationalResource(user, exploratoryName, dataengineServiceStatus, DataEngineType.CLOUD_SERVICE);

	}

	private void updateComputationalResource(String user, String exploratoryName,
											 UserInstanceStatus dataengineServiceStatus, DataEngineType cloudService) {
		UpdateResult result;
		do {
			result = updateMany(USER_INSTANCES,
					computationalFilter(user, exploratoryName, dataengineServiceStatus.toString(),
							DataEngineType.getDockerImageName(cloudService)),
					new Document(SET,
							new Document(computationalFieldFilter(STATUS), dataengineServiceStatus.toString())));
		} while (result.getModifiedCount() > 0);
	}

	private Bson computationalFilter(String user, String exploratoryName, String computationalStatus, String
			computationalImage) {
		return and(exploratoryCondition(user, exploratoryName),
				elemMatch(COMPUTATIONAL_RESOURCES, and(eq(IMAGE, computationalImage),
						not(in(STATUS, TERMINATED.toString(), FAILED.toString())),
						not(eq(STATUS, computationalStatus)))));
	}

	/**
	 * Updates the info of computational resource in Mongo database.
	 *
	 * @param dto object of computational resource status.
	 * @return The result of an update operation.
	 * @throws DlabException
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
			return updateOne(USER_INSTANCES, and(exploratoryCondition(dto.getUser(), dto.getExploratoryName()),
					elemMatch(COMPUTATIONAL_RESOURCES,
							and(eq(COMPUTATIONAL_NAME, dto.getComputationalName()),
									not(eq(STATUS, TERMINATED.toString()))))),
					new Document(SET, values));
		} catch (Exception t) {
			throw new DlabException("Could not update computational resource status", t);
		}
	}


	/**
	 * Updates the requirement for reuploading key for all corresponding computational resources in Mongo database.
	 *
	 * @param user                 user name.
	 * @param exploratoryStatus    status of exploratory.
	 * @param computationalType    type of computational resource ('dataengine', 'dataengine-service').
	 * @param computationalStatus  status of computational resource.
	 * @param reuploadKeyRequired  true/false.	 */

	public void updateReuploadKeyFlagForComputationalResources(String user, UserInstanceStatus exploratoryStatus,
															   DataEngineType computationalType,
															   UserInstanceStatus computationalStatus,
															   boolean reuploadKeyRequired) {

		List<String> exploratoryNames = stream(find(USER_INSTANCES,
				and(eq(USER, user), eq(STATUS, exploratoryStatus.toString())),
				fields(include(EXPLORATORY_NAME)))).map(d -> d.getString(EXPLORATORY_NAME)).collect(Collectors
				.toList());

		exploratoryNames.forEach(explName ->
				getComputationalResourcesWithStatus(computationalStatus, user, computationalType, explName)
						.forEach(compName -> updateComputationalField(user, explName, compName,
								REUPLOAD_KEY_REQUIRED, reuploadKeyRequired))
		);
	}

	/**
	 * Returns names of computational resources with predefined status and type for user's exploratory.
	 *
	 * @param computationalStatus status of computational resource.
	 * @param user                user name.
	 * @param computationalType   type of computational resource ('dataengine', 'dataengine-service').
	 * @param exploratoryName     name of exploratory.
	 * @return list of computational resources' names
	 */

	@SuppressWarnings("unchecked")
	public List<String> getComputationalResourcesWithStatus(UserInstanceStatus computationalStatus, String user,
															DataEngineType computationalType, String exploratoryName) {
		return stream((List<Document>) find(USER_INSTANCES, and(eq(USER, user), eq(EXPLORATORY_NAME, exploratoryName)),
				fields(include(COMPUTATIONAL_RESOURCES))).first().get(COMPUTATIONAL_RESOURCES))
				.filter(doc ->
						doc.getString(STATUS).equals(computationalStatus.toString())
								&& DataEngineType.fromDockerImageName(doc.getString(IMAGE)) == computationalType)
				.map(doc -> doc.getString(COMPUTATIONAL_NAME)).collect(Collectors.toList());
	}

	/**
	 * Updates computational resource's field.
	 *
	 * @param user              user name.
	 * @param exploratoryName   name of exploratory.
	 * @param computationalName name of computational resource.
	 * @param fieldName         computational field's name for updating.
	 * @param fieldValue        computational field's value for updating.
	 */

	private <T> UpdateResult updateComputationalField(String user, String exploratoryName, String computationalName,
													  String fieldName, T fieldValue) {
		return updateOne(USER_INSTANCES,
				computationalCondition(user, EXPLORATORY_NAME, exploratoryName, COMPUTATIONAL_NAME, computationalName),
				set(computationalFieldFilter(fieldName), fieldValue));
	}

	public UpdateResult updateSchedulerDataForComputationalResource(String user, String exploratoryName,
																	String computationalName, SchedulerJobDTO dto) {
		return updateComputationalField(user, exploratoryName, computationalName, SCHEDULER_DATA,
				Objects.isNull(dto) ? null : convertToBson(dto));
	}

	/**
	 * Checks if computational resource exists.
	 *
	 * @param user              user name.
	 * @param exploratoryName   the name of exploratory.
	 * @param computationalName the name of computational resource.
	 */
	public boolean isComputationalExist(String user, String exploratoryName, String computationalName) {
		return !Objects.isNull(fetchComputationalFields(user, exploratoryName, computationalName));
	}


}