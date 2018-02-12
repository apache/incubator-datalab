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
import com.epam.dlab.dto.UserInstanceDTO;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.model.scheduler.SchedulerJobData;
import com.google.inject.Singleton;
import com.mongodb.client.FindIterable;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.time.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.epam.dlab.backendapi.dao.ExploratoryDAO.EXPLORATORY_NAME;
import static com.epam.dlab.backendapi.dao.ExploratoryDAO.exploratoryCondition;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Projections.*;

/**
 * DAO for user's scheduler jobs.
 */
@Slf4j
@Singleton
public class SchedulerJobsDAO extends BaseDAO{

    static final String SCHEDULER_DATA = "scheduler_data";
    public static final String BEGIN_DATE = "begin_date";
    public static final String FINISH_DATE = "finish_date";
    public static final String START_TIME = "start_time";
    public static final String END_TIME = "end_time";
    public static final String DAYS_REPEAT = "days_repeat";
    public static final String TIMEZONE_PREFIX = "timezone_prefix";
    public static final String TIMEZONE_OFFSET = "timezone_offset";

    public SchedulerJobsDAO() {
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
     * @param date and time for seeking appropriate scheduler jobs for starting/stopping.
     * @param actionType 'start' string value for starting exploratory and another one for stopping.
     */
    public List<SchedulerJobData> getSchedulerJobsForAction(LocalDate date, LocalTime time, String actionType){
        List<SchedulerJobData> jobList = new ArrayList<>();
        FindIterable<Document> docs = find(USER_INSTANCES,
                and(
                        statusConditionByAction(actionType),
                        schedulerNotNullCondition()
                ),
                fields(excludeId(), include(USER, EXPLORATORY_NAME, SCHEDULER_DATA)));

        for (Document doc : docs) {
            String user = doc.getString(USER);
            String exploratoryName = doc.getString(EXPLORATORY_NAME);
            SchedulerJobDTO dto = convertFromDocument((Document) doc.get(SCHEDULER_DATA), SchedulerJobDTO.class);
            if (isSchedulerJobDtoSatisfyCondition(dto, date, time, actionType)) {
                jobList.add(new SchedulerJobData(user, exploratoryName, dto));
            }
        }
        return jobList;
    }

    /**
     * Finds and returns the info of user's single scheduler job by exploratory name.
     * @param user            user name.
     * @param exploratoryName the name of exploratory.
     */
	public SchedulerJobDTO fetchSingleSchedulerJobByUserAndExploratory(String user, String exploratoryName) {
        Optional<UserInstanceDTO> opt = findOne(USER_INSTANCES,
                and(exploratoryCondition(user, exploratoryName), schedulerNotNullCondition()),
                UserInstanceDTO.class);

        if (opt.isPresent()) {
            return opt.get().getSchedulerData();
        }
        throw new DlabException("Scheduler job for user " + user + " and exploratory name" + exploratoryName + " not found.");
    }

    private boolean isSchedulerJobDtoSatisfyCondition(SchedulerJobDTO dto, LocalDate date, LocalTime time,
                                                      String actionType) {

        String tzPrefix = dto.getTimeZonePrefix();
        ZoneOffset zOffset = dto.getTimeZoneOffset();
        LocalDateTime ldt = LocalDateTime.of(date, time);
        LocalDateTime zonedDateTime = ldt.atZone(ZoneId.ofOffset(tzPrefix, zOffset)).toLocalDateTime();
        LocalDate zonedDate = zonedDateTime.toLocalDate();
        LocalTime zonedTime = zonedDateTime.toLocalTime();

        return !zonedDate.isBefore(dto.getBeginDate()) && !zonedDate.isAfter(dto.getFinishDate())
                && dto.getDaysRepeat().contains(zonedDate.getDayOfWeek())
                && zonedTime.equals(actionType.equalsIgnoreCase(ACTION_START) ? dto.getStartTime() : dto.getEndTime());
    }

}

