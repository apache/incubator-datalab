/***************************************************************************

 Copyright (c) 2018, EPAM SYSTEMS INC

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

import com.epam.dlab.dto.SchedulerJobDTO;
import com.epam.dlab.dto.UserInstanceStatus;
import com.epam.dlab.dto.base.DataEngineType;
import com.epam.dlab.model.scheduler.SchedulerJobData;
import com.google.inject.Singleton;
import com.mongodb.client.FindIterable;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.epam.dlab.dto.UserInstanceStatus.*;
import static com.epam.dlab.backendapi.dao.ComputationalDAO.COMPUTATIONAL_NAME;
import static com.epam.dlab.backendapi.dao.ComputationalDAO.IMAGE;
import static com.epam.dlab.backendapi.dao.ExploratoryDAO.*;
import static com.epam.dlab.backendapi.dao.MongoCollections.USER_INSTANCES;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Projections.*;

/**
 * DAO for user's scheduler jobs.
 */
@Slf4j
@Singleton
public class SchedulerJobDAO extends BaseDAO {

    static final String SCHEDULER_DATA = "scheduler_data";
	public static final String TIMEZONE_PREFIX = "UTC";

	public SchedulerJobDAO() {
        log.info("{} is initialized", getClass().getSimpleName());
    }

	/** Returns condition for search scheduler for exploratory which is not null.
	 *
	 * @return Bson condition.
     */
    private Bson schedulerNotNullCondition() {
        return ne(SCHEDULER_DATA, null);
    }

	/**
	 * Returns condition for search exploratory which has stopped state for starting it, running state for stopping it
	 * and stopped/running state for termination.
	 *
	 * @param desiredStatus 'running' value for searching stopped exploratories, 'stopped' - for searching
	 *                       exploratories with 'running' state, 'terminated' for searching exploratories with
	 *                       'stopped'
	 *                       or 'running' state.
	 * @return Bson condition.
	 */
	private Bson statusCondition(UserInstanceStatus desiredStatus) {
		if (desiredStatus == RUNNING) {
			return eq(STATUS, UserInstanceStatus.STOPPED.toString());
		} else if (desiredStatus == STOPPED) {
			return eq(STATUS, UserInstanceStatus.RUNNING.toString());
		} else if (desiredStatus == TERMINATED) {
			return or(eq(STATUS, UserInstanceStatus.STOPPED.toString()),
					eq(STATUS, UserInstanceStatus.RUNNING.toString()));
		} else return null;
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

	public List<SchedulerJobData> getSchedulerJobsToAchieveStatus(UserInstanceStatus desiredStatus,
																  boolean isAppliedForClusters) {
		return isAppliedForClusters ? getSchedulerComputationalJobsToAchieveStatus(desiredStatus) :
				getSchedulerExploratoryJobsToAchieveStatus(desiredStatus);
	}

	/**
	 * Finds and returns the list of all scheduler jobs for starting/stopping/terminating exploratories regarding to
	 * parameter passed.
	 *
	 * @param desiredStatus 'running' value for starting exploratory, 'stopped' - for stopping and 'terminated' - for
	 *                        terminating.
	 * @return list of scheduler jobs.
	 */
	private List<SchedulerJobData> getSchedulerExploratoryJobsToAchieveStatus(UserInstanceStatus desiredStatus) {
		FindIterable<Document> userInstances = find(USER_INSTANCES,
				and(
						statusCondition(desiredStatus),
                        schedulerNotNullCondition()
                ),
                fields(excludeId(), include(USER, EXPLORATORY_NAME, SCHEDULER_DATA)));

		return stream(userInstances).map(d -> convertFromDocument(d, SchedulerJobData.class))
				.collect(Collectors.toList());
    }


	/**
	 * Finds and returns the list of all scheduler jobs for starting/stopping/terminating Spark clusters regarding to
	 * parameter passed.
	 *
	 * @param desiredStatus 'running' value for starting computational resource, 'stopped' - for stopping and
	 *                      'terminated' - for terminating.
	 * @return list of scheduler jobs.
	 */

	@SuppressWarnings("unchecked")
	private List<SchedulerJobData> getSchedulerComputationalJobsToAchieveStatus(UserInstanceStatus desiredStatus) {
		FindIterable<Document> userInstances = find(USER_INSTANCES,
				and(
						eq(STATUS, RUNNING.toString()),
						ne(COMPUTATIONAL_RESOURCES, null)
				),
				fields(excludeId(), include(USER, EXPLORATORY_NAME, COMPUTATIONAL_RESOURCES)));

		return stream(userInstances).map(doc ->
				schedulerComputationalDataFromDocument(doc, DataEngineType.SPARK_STANDALONE, desiredStatus)
		).flatMap(Function.identity()).collect(Collectors.toList());
	}

	private Stream<SchedulerJobData> schedulerComputationalDataFromDocument(Document doc,
																			DataEngineType computationalType,
																			UserInstanceStatus
																					targetComputationalStatus) {

		return computationalResourcesWithSchedulersFromDocument(doc, computationalType, targetComputationalStatus)
				.stream().map(compResource ->
						new SchedulerJobData(doc.getString(USER), doc.getString(EXPLORATORY_NAME),
								compResource.getString(COMPUTATIONAL_NAME),
								convertFromDocument((Document) compResource.get(SCHEDULER_DATA),
										SchedulerJobDTO.class)));
	}

	@SuppressWarnings("unchecked")
	private List<Document> computationalResourcesWithSchedulersFromDocument(Document doc,
																			DataEngineType computationalType,
																			UserInstanceStatus
																						targetComputationalStatus) {
		return ((List<Document>) doc.get(COMPUTATIONAL_RESOURCES)).stream()
				.filter(compResource ->
						DataEngineType.fromDockerImageName(compResource.getString(IMAGE)) ==
								computationalType &&
								computationalStatusCondition(compResource, targetComputationalStatus) &&
								compResource.get(SCHEDULER_DATA) != null)
				.collect(Collectors.toList());
	}

	private boolean computationalStatusCondition(Document computationalResource, UserInstanceStatus desiredStatus) {
		if (desiredStatus == RUNNING) {
			return computationalResource.get(STATUS).equals(STOPPED.toString());
		} else if (desiredStatus == STOPPED) {
			return computationalResource.get(STATUS).equals(RUNNING.toString());
		} else
			return desiredStatus == TERMINATED && (computationalResource.get(STATUS).equals(STOPPED.toString()) ||
					computationalResource.get(STATUS).equals(RUNNING.toString()));
	}

}

