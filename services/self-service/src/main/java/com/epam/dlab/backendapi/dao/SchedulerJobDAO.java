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

import com.epam.dlab.UserInstanceStatus;
import com.epam.dlab.dto.SchedulerJobDTO;
import com.epam.dlab.model.scheduler.SchedulerJobData;
import com.google.inject.Singleton;
import com.mongodb.client.FindIterable;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.time.*;
import java.util.List;
import java.util.Optional;
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
	 *
	 * @return Bson condition.
     */
    private Bson schedulerNotNullCondition() {
        return ne(SCHEDULER_DATA, null);
    }

	/**
	 * Return condition for search exploratory which has stopped state for starting it, running state for stopping it
	 * and stopped/running state for termination.
	 *
	 * @param desiredStatus 'running' value for searching stopped exploratories, 'stopped' - for searching
	 *                       exploratories with 'running' state, 'terminated' for searching exploratories with
	 *                       'stopped'
	 *                       or 'running' state.
	 * @return Bson condition.
	 */
	private Bson statusCondition(UserInstanceStatus desiredStatus) {
		switch (desiredStatus) {
			case RUNNING:
				return eq(STATUS, UserInstanceStatus.STOPPED.toString());
			case STOPPED:
				return eq(STATUS, UserInstanceStatus.RUNNING.toString());
			case TERMINATED:
				return or(eq(STATUS, UserInstanceStatus.STOPPED.toString()),
						eq(STATUS, UserInstanceStatus.RUNNING.toString()));
			default:
				return null;
		}
    }


    /**
	 * Finds and returns the list of all scheduler jobs for starting/stopping/terminating regarding to parameter
	 * passed.
	 *
	 * @param dateTime for seeking appropriate scheduler jobs for starting/stopping/terminating.
	 * @param desiredStatus 'running' value for starting exploratory, 'stopped' - for stopping and 'terminated' - for
	 *                        terminating.
	 * @return list of scheduler jobs.
	 */
	public List<SchedulerJobData> getSchedulerJobsToAchieveStatus(UserInstanceStatus desiredStatus, OffsetDateTime dateTime) {
        FindIterable<Document> docs = find(USER_INSTANCES,
				and(
						statusCondition(desiredStatus),
                        schedulerNotNullCondition()
                ),
                fields(excludeId(), include(USER, EXPLORATORY_NAME, SCHEDULER_DATA)));

		return StreamSupport.stream(docs.spliterator(), false)
				.map(d -> convertFromDocument(d, SchedulerJobData.class))
				.filter(jobData -> isSchedulerJobDtoSatisfyCondition(jobData.getJobDTO(), dateTime, desiredStatus))
				.collect(Collectors.toList());
    }

    /**
     * Finds and returns the info of user's single scheduler job by exploratory name.
     * @param user            user name.
     * @param exploratoryName the name of exploratory.
	 * @return scheduler job data.
     */
	public Optional<SchedulerJobDTO> fetchSingleSchedulerJobByUserAndExploratory(String user, String exploratoryName) {
		return findOne(USER_INSTANCES, and(exploratoryCondition(user, exploratoryName), schedulerNotNullCondition()),
				fields(include(SCHEDULER_DATA), excludeId()))
				.map(d -> convertFromDocument((Document) d.get(SCHEDULER_DATA), SchedulerJobDTO.class));
	}

	/**
	 * Checks if scheduler's time data satisfies existing time parameters.
	 *
	 * @param dto            scheduler job data.
	 * @param dateTime       existing time data.
	 * @param desiredStatus  target exploratory status which has influence for time/date checking ('running' status
	 *                       requires for checking start time, 'stopped' - for end time, 'terminated' - for
	 *                       'terminatedDateTime').
	 * @return true/false.
	 */
	private boolean isSchedulerJobDtoSatisfyCondition(SchedulerJobDTO dto, OffsetDateTime dateTime,
													  UserInstanceStatus desiredStatus) {
		ZoneOffset zOffset = dto.getTimeZoneOffset();
		OffsetDateTime roundedDateTime = OffsetDateTime.of(
				dateTime.toLocalDate(),
				LocalTime.of(dateTime.toLocalTime().getHour(), dateTime.toLocalTime().getMinute()),
				dateTime.getOffset());

		LocalDateTime convertedDateTime = ZonedDateTime.ofInstant(roundedDateTime.toInstant(),
				ZoneId.ofOffset(TIMEZONE_PREFIX, zOffset)).toLocalDateTime();

		return desiredStatus.equals(UserInstanceStatus.TERMINATED) ?
				convertedDateTime.toLocalDate().equals(dto.getTerminateDateTime().toLocalDate())
						&& convertedDateTime.toLocalTime().equals(getDesiredTime(dto, desiredStatus)) :
				!convertedDateTime.toLocalDate().isBefore(dto.getBeginDate())
				&& !convertedDateTime.toLocalDate().isAfter(dto.getFinishDate())
				&& dto.getDaysRepeat().contains(convertedDateTime.toLocalDate().getDayOfWeek())
				&& convertedDateTime.toLocalTime()
						.equals(getDesiredTime(dto, desiredStatus));
	}

	private LocalTime getDesiredTime(SchedulerJobDTO dto, UserInstanceStatus desiredStatus) {
		switch (desiredStatus) {
			case RUNNING:
				return dto.getStartTime();
			case STOPPED:
				return dto.getEndTime();
			case TERMINATED:
				return dto.getTerminateDateTime().toLocalTime();
			default:
				return null;
		}
	}

}

