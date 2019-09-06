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

import com.epam.dlab.dto.SchedulerJobDTO;
import com.epam.dlab.dto.UserInstanceStatus;
import com.epam.dlab.dto.base.DataEngineType;
import com.epam.dlab.model.scheduler.SchedulerJobData;
import com.google.inject.Singleton;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.epam.dlab.backendapi.dao.ComputationalDAO.COMPUTATIONAL_NAME;
import static com.epam.dlab.backendapi.dao.ComputationalDAO.IMAGE;
import static com.epam.dlab.backendapi.dao.ExploratoryDAO.*;
import static com.epam.dlab.backendapi.dao.MongoCollections.USER_INSTANCES;
import static com.epam.dlab.dto.base.DataEngineType.fromDockerImageName;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Projections.*;
import static java.util.stream.Collectors.toList;

/**
 * DAO for user's scheduler jobs.
 */
@Slf4j
@Singleton
public class SchedulerJobDAO extends BaseDAO {

	static final String SCHEDULER_DATA = "scheduler_data";
	private static final String CONSIDER_INACTIVITY_FLAG = SCHEDULER_DATA + ".consider_inactivity";
	public static final String TIMEZONE_PREFIX = "UTC";
	private static final String LAST_ACTIVITY = "last_activity";
	private static final String CHECK_INACTIVITY_REQUIRED = "check_inactivity_required";
	private static final String CHECK_INACTIVITY_FLAG = SCHEDULER_DATA + "." + CHECK_INACTIVITY_REQUIRED;


	public SchedulerJobDAO() {
		log.info("{} is initialized", getClass().getSimpleName());
	}

	/**
	 * Returns condition for search scheduler for exploratory which is not null.
	 *
	 * @return Bson condition.
	 */
	private Bson schedulerNotNullCondition() {
		return and(exists(SCHEDULER_DATA), ne(SCHEDULER_DATA, null));
	}

	/**
	 * Finds and returns the info of user's single scheduler job by exploratory name.
	 *
	 * @param user            user name.
	 * @param exploratoryName the name of exploratory.
	 * @return scheduler job data.
	 */
	public Optional<SchedulerJobDTO> fetchSingleSchedulerJobByUserAndExploratory(String user, String exploratoryName) {
		return findOne(USER_INSTANCES,
				and(exploratoryCondition(user, exploratoryName), schedulerNotNullCondition()),
				fields(include(SCHEDULER_DATA), excludeId()))
				.map(d -> convertFromDocument((Document) d.get(SCHEDULER_DATA), SchedulerJobDTO.class));
	}

	/**
	 * Finds and returns the info of user's single scheduler job for computational resource.
	 *
	 * @param user              user name.
	 * @param exploratoryName   the name of exploratory.
	 * @param computationalName the name of computational resource.
	 * @return scheduler job data.
	 */

	@SuppressWarnings("unchecked")
	public Optional<SchedulerJobDTO> fetchSingleSchedulerJobForCluster(String user, String exploratoryName,
																	   String computationalName) {
		return findOne(USER_INSTANCES,
				exploratoryCondition(user, exploratoryName),
				fields(include(COMPUTATIONAL_RESOURCES), excludeId()))
				.map(d -> (List<Document>) d.get(COMPUTATIONAL_RESOURCES))
				.map(list -> list.stream().filter(d -> d.getString(COMPUTATIONAL_NAME).equals(computationalName))
						.findAny().orElse(new Document()))
				.map(d -> (Document) d.get(SCHEDULER_DATA))
				.map(d -> convertFromDocument(d, SchedulerJobDTO.class));
	}

	/**
	 * Finds and returns the list of all scheduler jobs for starting/stopping/terminating exploratories regarding to
	 * parameter passed.
	 *
	 * @param status 'running' value for starting exploratory, 'stopped' - for stopping and 'terminated' -
	 *               for
	 *               terminating.
	 * @return list of scheduler jobs.
	 */
	public List<SchedulerJobData> getExploratorySchedulerDataWithStatus(UserInstanceStatus status) {
		FindIterable<Document> userInstances = userInstancesWithScheduler(eq(STATUS, status.toString()));

		return stream(userInstances).map(d -> convertFromDocument(d, SchedulerJobData.class))
				.collect(toList());
	}

	public List<SchedulerJobData> getExploratorySchedulerWithStatusAndClusterLastActivityLessThan(UserInstanceStatus status,
																								  Date lastActivity) {
		return stream(find(USER_INSTANCES,
				and(
						eq(STATUS, status.toString()),
						schedulerNotNullCondition(),
						or(and(eq(CONSIDER_INACTIVITY_FLAG, true),
								or(eq(COMPUTATIONAL_RESOURCES, Collections.emptyList()),
										and(ne(COMPUTATIONAL_RESOURCES, Collections.emptyList()),
												Filters.elemMatch(COMPUTATIONAL_RESOURCES,
														lte(LAST_ACTIVITY, lastActivity))))),
								eq(CONSIDER_INACTIVITY_FLAG, false)
						)
				),
				fields(excludeId(), include(USER, EXPLORATORY_NAME, SCHEDULER_DATA))))
				.map(d -> convertFromDocument(d, SchedulerJobData.class))
				.collect(toList());
	}

	public List<SchedulerJobData> getExploratorySchedulerDataWithOneOfStatus(UserInstanceStatus... statuses) {
		FindIterable<Document> userInstances = userInstancesWithScheduler(in(STATUS,
				Arrays.stream(statuses).map(UserInstanceStatus::toString).collect(toList())));

		return stream(userInstances).map(d -> convertFromDocument(d, SchedulerJobData.class))
				.collect(toList());
	}

	public List<SchedulerJobData> getComputationalSchedulerDataWithOneOfStatus(UserInstanceStatus exploratoryStatus,
																			   DataEngineType dataEngineType,
																			   UserInstanceStatus... statuses) {
		return stream(computationalResourcesWithScheduler(exploratoryStatus))
				.map(doc -> computationalSchedulerDataStream(doc, dataEngineType, statuses))
				.flatMap(Function.identity())
				.collect(toList());
	}

	public List<SchedulerJobData> getComputationalSchedulerDataWithOneOfStatus(UserInstanceStatus exploratoryStatus,
																			   UserInstanceStatus... statuses) {
		return stream(computationalResourcesWithScheduler(exploratoryStatus))
				.map(doc -> computationalSchedulerData(doc, statuses).map(compResource -> toSchedulerData(doc,
						compResource)))
				.flatMap(Function.identity())
				.collect(toList());
	}

	private FindIterable<Document> computationalResourcesWithScheduler(UserInstanceStatus exploratoryStatus) {
		final Bson computationalSchedulerCondition = Filters.elemMatch(COMPUTATIONAL_RESOURCES,
				and(schedulerNotNullCondition(), eq(CHECK_INACTIVITY_FLAG, false)));
		return find(USER_INSTANCES,
				and(eq(STATUS, exploratoryStatus.toString()), computationalSchedulerCondition),
				fields(excludeId(), include(USER, EXPLORATORY_NAME, COMPUTATIONAL_RESOURCES)));
	}

	public void removeScheduler(String user, String exploratory) {
		updateOne(USER_INSTANCES, and(eq(USER, user), eq(EXPLORATORY_NAME, exploratory)),
				unset(SCHEDULER_DATA, StringUtils.EMPTY));
	}

	public void removeScheduler(String user, String exploratory, String computational) {
		updateOne(USER_INSTANCES, and(eq(USER, user), eq(EXPLORATORY_NAME, exploratory),
				Filters.elemMatch(COMPUTATIONAL_RESOURCES, eq(COMPUTATIONAL_NAME, computational))),
				unset(COMPUTATIONAL_RESOURCES + ".$." + SCHEDULER_DATA, StringUtils.EMPTY));
	}

	private FindIterable<Document> userInstancesWithScheduler(Bson statusCondition) {
		return find(USER_INSTANCES,
				and(
						statusCondition,
						schedulerNotNullCondition(), eq(CHECK_INACTIVITY_FLAG, false)
				),
				fields(excludeId(), include(USER, EXPLORATORY_NAME, SCHEDULER_DATA)));
	}

	private Stream<SchedulerJobData> computationalSchedulerDataStream(Document doc, DataEngineType computationalType,
																	  UserInstanceStatus... computationalStatuses) {
		return computationalSchedulerData(doc, computationalStatuses)
				.filter(compResource -> fromDockerImageName(compResource.getString(IMAGE)) == computationalType)
				.map(compResource -> toSchedulerData(doc, compResource));
	}

	private SchedulerJobData toSchedulerData(Document userInstanceDocument, Document compResource) {
		final String user = userInstanceDocument.getString(USER);
		final String exploratoryName = userInstanceDocument.getString(EXPLORATORY_NAME);
		final String computationalName = compResource.getString(COMPUTATIONAL_NAME);
		final SchedulerJobDTO schedulerData = convertFromDocument((Document) compResource.get(SCHEDULER_DATA),
				SchedulerJobDTO.class);
		return new SchedulerJobData(user, exploratoryName, computationalName, schedulerData);
	}

	@SuppressWarnings("unchecked")
	private Stream<Document> computationalSchedulerData(Document doc, UserInstanceStatus... computationalStatuses) {
		final Set<String> statusSet = Arrays.stream(computationalStatuses)
				.map(UserInstanceStatus::toString)
				.collect(Collectors.toSet());
		return ((List<Document>) doc.get(COMPUTATIONAL_RESOURCES))
				.stream()
				.filter(compResource -> Objects.nonNull(compResource.get(SCHEDULER_DATA)) &&
						statusSet.contains(compResource.getString(STATUS)));
	}
}

