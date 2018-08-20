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


import com.epam.dlab.backendapi.util.DateRemoverUtil;
import com.epam.dlab.dto.*;
import com.epam.dlab.dto.base.DataEngineType;
import com.epam.dlab.dto.computational.ComputationalStatusDTO;
import com.epam.dlab.dto.computational.UserComputationalResource;
import com.epam.dlab.exceptions.DlabException;
import com.mongodb.client.result.UpdateResult;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.*;
import java.util.stream.Collectors;

import static com.epam.dlab.backendapi.dao.ExploratoryDAO.*;
import static com.epam.dlab.backendapi.dao.MongoCollections.USER_INSTANCES;
import static com.epam.dlab.backendapi.dao.SchedulerJobDAO.SCHEDULER_DATA;
import static com.epam.dlab.dto.UserInstanceStatus.TERMINATED;
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
	private static final String COMPUTATIONAL_URL = "computational_url";
	private static final String COMPUTATIONAL_URL_DESC = "description";
	private static final String COMPUTATIONAL_URL_URL = "url";
	private static final String COMPUTATIONAL_LAST_ACTIVITY = "last_activity";

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
	 * @throws DlabException    if exception occurs
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
														  UserInstanceStatus dataengineServiceStatus,
														  UserInstanceStatus... excludedStatuses) {
		updateComputationalResource(user, exploratoryName, dataengineStatus, DataEngineType.SPARK_STANDALONE,
				excludedStatuses);
		updateComputationalResource(user, exploratoryName, dataengineServiceStatus, DataEngineType.CLOUD_SERVICE,
				excludedStatuses);

	}

	/**
	 * Updates status for all corresponding computational resources in Mongo database.
	 *
	 * @param newStatus                new status for computational resources.
	 * @param user                     user name.
	 * @param exploratoryStatuses      exploratory's status list.
	 * @param computationalTypes       type list of computational resource (may contain 'dataengine' and/or
	 *                                 'dataengine-service').
	 * @param oldComputationalStatuses old statuses of computational resources.
	 */

	public void updateStatusForComputationalResources(UserInstanceStatus newStatus, String user,
													  List<UserInstanceStatus> exploratoryStatuses,
													  List<DataEngineType> computationalTypes,
													  UserInstanceStatus... oldComputationalStatuses) {

		List<String> exploratoryNames = stream(find(USER_INSTANCES,
				and(eq(USER, user), in(STATUS, statusList(exploratoryStatuses))),
				fields(include(EXPLORATORY_NAME)))).map(d -> d.getString(EXPLORATORY_NAME))
				.collect(Collectors.toList());

		exploratoryNames.forEach(explName ->
				getComputationalResourcesWhereStatusIn(user, computationalTypes, explName, oldComputationalStatuses)
						.forEach(compName -> updateComputationalField(user, explName, compName,
								STATUS, newStatus.toString()))
		);
	}

	/**
	 * Updates the status for single computational resource in Mongo database.
	 *
	 * @param user              user name.
	 * @param exploratoryName   exploratory's name.
	 * @param computationalName name of computational resource.
	 * @param newStatus         new status of computational resource.
	 */

	public void updateStatusForComputationalResource(String user, String exploratoryName,
													 String computationalName,
													 UserInstanceStatus newStatus) {
		updateComputationalField(user, exploratoryName, computationalName, STATUS, newStatus.toString());
	}


	private void updateComputationalResource(String user, String exploratoryName,
											 UserInstanceStatus dataengineServiceStatus, DataEngineType cloudService,
											 UserInstanceStatus... excludedStatuses) {
		UpdateResult result;
		do {
			result = updateMany(USER_INSTANCES,
					computationalFilter(user, exploratoryName, dataengineServiceStatus.toString(),
							DataEngineType.getDockerImageName(cloudService), excludedStatuses),
					new Document(SET,
							new Document(computationalFieldFilter(STATUS), dataengineServiceStatus.toString())));
		} while (result.getModifiedCount() > 0);
	}

	public void updateLastActivityForCluster(String user, String exploratoryName, String computationalName,
											 Date lastActivity) {
		updateComputationalField(user, exploratoryName, computationalName, COMPUTATIONAL_LAST_ACTIVITY, lastActivity);
	}

	private Bson computationalFilter(String user, String exploratoryName, String computationalStatus, String
			computationalImage, UserInstanceStatus[] excludedStatuses) {
		final String[] statuses = Arrays.stream(excludedStatuses)
				.map(UserInstanceStatus::toString)
				.toArray(String[]::new);
		return and(exploratoryCondition(user, exploratoryName),
				elemMatch(COMPUTATIONAL_RESOURCES, and(eq(IMAGE, computationalImage),
						not(in(STATUS, statuses)),
						not(eq(STATUS, computationalStatus)))));
	}

	/**
	 * Updates the info of computational resource in Mongo database.
	 *
	 * @param dto object of computational resource status.
	 * @return The result of an update operation.
	 * @throws DlabException if exception occurs
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
			return updateOne(USER_INSTANCES, and(exploratoryCondition(dto.getUser(), dto.getExploratoryName()),
					elemMatch(COMPUTATIONAL_RESOURCES,
							and(eq(COMPUTATIONAL_NAME, dto.getComputationalName()),
									not(eq(STATUS, TERMINATED.toString()))))),
					new Document(SET, values));
		} catch (Exception t) {
			throw new DlabException("Could not update computational resource status", t);
		}
	}

	private List<Map<String, String>> getResourceUrlData(ComputationalStatusDTO dto) {
		return dto.getResourceUrl().stream()
				.map(this::toUrlDocument).collect(Collectors.toList());
	}

	private LinkedHashMap<String, String> toUrlDocument(ResourceURL url) {
		LinkedHashMap<String, String> map = new LinkedHashMap<>();
		map.put(COMPUTATIONAL_URL_DESC, url.getDescription());
		map.put(COMPUTATIONAL_URL_URL, url.getUrl());
		return map;
	}


	/**
	 * Updates the requirement for reuploading key for all corresponding computational resources in Mongo database.
	 *
	 * @param user                  user name.
	 * @param exploratoryStatuses   exploratory's status list.
	 * @param computationalTypes    type list of computational resource (may contain 'dataengine' and/or
	 *                                 'dataengine-service').
	 * @param reuploadKeyRequired   true/false.
	 * @param computationalStatuses statuses of computational resource.
	 */

	public void updateReuploadKeyFlagForComputationalResources(String user,
															   List<UserInstanceStatus> exploratoryStatuses,
															   List<DataEngineType> computationalTypes,
															   boolean reuploadKeyRequired,
															   UserInstanceStatus... computationalStatuses) {

		List<String> exploratoryNames = stream(find(USER_INSTANCES,
				and(eq(USER, user), in(STATUS, statusList(exploratoryStatuses))),
				fields(include(EXPLORATORY_NAME)))).map(d -> d.getString(EXPLORATORY_NAME))
				.collect(Collectors.toList());

		exploratoryNames.forEach(explName ->
				getComputationalResourcesWhereStatusIn(user, computationalTypes, explName, computationalStatuses)
						.forEach(compName -> updateComputationalField(user, explName, compName,
								REUPLOAD_KEY_REQUIRED, reuploadKeyRequired))
		);
	}

	/**
	 * Updates the requirement for reuploading key for single computational resource in Mongo database.
	 *
	 * @param user                user name.
	 * @param exploratoryName     exploratory's name.
	 * @param computationalName   name of computational resource.
	 * @param reuploadKeyRequired true/false.
	 */

	public void updateReuploadKeyFlagForComputationalResource(String user, String exploratoryName,
															  String computationalName, boolean
																			reuploadKeyRequired) {
		updateComputationalField(user, exploratoryName, computationalName, REUPLOAD_KEY_REQUIRED, reuploadKeyRequired);
	}

	/**
	 * Updates the requirement for checking inactivity for single computational resource in Mongo database.
	 *
	 * @param user                    user name.
	 * @param exploratoryName         exploratory's name.
	 * @param computationalName       name of computational resource.
	 * @param checkInactivityRequired true/false.
	 */

	public void updateCheckInactivityFlagForComputationalResource(String user, String exploratoryName,
																  String computationalName,
																  boolean checkInactivityRequired) {
		updateComputationalField(user, exploratoryName, computationalName, CHECK_INACTIVITY_REQUIRED,
				checkInactivityRequired);
	}

	/**
	 * Returns names of computational resources which status is among existing ones. Also these resources will
	 * have predefined type.
	 *
	 * @param user                  user name.
	 * @param computationalTypes    type list of computational resource which may contain 'dataengine' and/or
	 *                                 'dataengine-service'.
	 * @param exploratoryName       name of exploratory.
	 * @param computationalStatuses statuses of computational resource.
	 * @return list of computational resources' names
	 */

	@SuppressWarnings("unchecked")
	public List<String> getComputationalResourcesWhereStatusIn(String user, List<DataEngineType> computationalTypes,
															   String exploratoryName,
															   UserInstanceStatus... computationalStatuses) {
		return stream((List<Document>) find(USER_INSTANCES, and(eq(USER, user), eq(EXPLORATORY_NAME, exploratoryName)),
				fields(include(COMPUTATIONAL_RESOURCES))).first().get(COMPUTATIONAL_RESOURCES))
				.filter(doc ->
						statusList(computationalStatuses).contains(doc.getString(STATUS)) &&
								computationalTypes.contains(DataEngineType.fromDockerImageName(doc.getString(IMAGE))))
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

	public void updateSchedulerSyncFlag(String user, String exploratoryName, boolean syncFlag) {
		final String syncStartField = SCHEDULER_DATA + ".sync_start_required";
		UpdateResult result;
		do {

			result = updateOne(USER_INSTANCES, and(exploratoryCondition(user, exploratoryName),
					elemMatch(COMPUTATIONAL_RESOURCES, and(ne(SCHEDULER_DATA, null), ne(syncStartField, syncFlag)))),
					set(computationalFieldFilter(syncStartField), syncFlag));

		} while (result.getModifiedCount() != 0);
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