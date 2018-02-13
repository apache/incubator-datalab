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
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.model.scheduler.SchedulerJobData;
import com.google.inject.Singleton;
import com.mongodb.client.FindIterable;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.time.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.epam.dlab.backendapi.dao.ExploratoryDAO.EXPLORATORY_NAME;
import static com.epam.dlab.backendapi.dao.ExploratoryDAO.exploratoryCondition;
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

    /** Return condition for search scheduler which is not null.
     */
    private Bson schedulerNotNullCondition() {
        return ne(SCHEDULER_DATA, null);
    }

    private Bson statusConditionByAction(String actionType) {
        return actionType.equalsIgnoreCase(ACTION_START) ? eq(STATUS, STATUS_STOPPED) : eq(STATUS, STATUS_RUNNING);
    }


    /**
     * Finds and returns the list of all scheduler jobs for starting/stopping regarding to parameter passed.
	 * @param dateTime for seeking appropriate scheduler jobs for starting/stopping.
     * @param actionType 'start' string value for starting exploratory and another one for stopping.
     */
	public List<SchedulerJobData> getSchedulerJobsForAction(OffsetDateTime dateTime, String actionType) {
        FindIterable<Document> docs = find(USER_INSTANCES,
                and(
                        statusConditionByAction(actionType),
                        schedulerNotNullCondition()
                ),
                fields(excludeId(), include(USER, EXPLORATORY_NAME, SCHEDULER_DATA)));

		return StreamSupport.stream(docs.spliterator(), false)
				.map(d -> convertFromDocument(d, SchedulerJobData.class))
				.filter(jobData -> isSchedulerJobDtoSatisfyCondition(jobData.getJobDTO(), dateTime, actionType))
				.collect(Collectors.toList());
    }

    /**
     * Finds and returns the info of user's single scheduler job by exploratory name.
     * @param user            user name.
     * @param exploratoryName the name of exploratory.
     */
	public SchedulerJobDTO fetchSingleSchedulerJobByUserAndExploratory(String user, String exploratoryName) {
		return convertFromDocument((Document) findOne(USER_INSTANCES, and(exploratoryCondition(user, exploratoryName),
				schedulerNotNullCondition()))
				.orElseThrow(() -> new DlabException(
						String.format("Scheduler job for user %s with exploratory instance name %s not found.",
								user, exploratoryName))).get(SCHEDULER_DATA), SchedulerJobDTO.class);
	}

	private boolean isSchedulerJobDtoSatisfyCondition(SchedulerJobDTO dto, OffsetDateTime dateTime,
													  String actionType) {
        ZoneOffset zOffset = dto.getTimeZoneOffset();
		OffsetDateTime roundedDateTime = OffsetDateTime.of(
				dateTime.toLocalDate(),
				LocalTime.of(dateTime.toLocalTime().getHour(), dateTime.toLocalTime().getMinute()),
				dateTime.getOffset());

		LocalDateTime convertedDateTime = ZonedDateTime.ofInstant(roundedDateTime.toInstant(),
				ZoneId.ofOffset(TIMEZONE_PREFIX, zOffset)).toLocalDateTime();

		return !convertedDateTime.toLocalDate().isBefore(dto.getBeginDate())
				&& !convertedDateTime.toLocalDate().isAfter(dto.getFinishDate())
				&& dto.getDaysRepeat().contains(convertedDateTime.toLocalDate().getDayOfWeek())
				&& convertedDateTime.toLocalTime()
				.equals(actionType.equalsIgnoreCase(ACTION_START) ? dto.getStartTime() : dto.getEndTime());
    }

}

