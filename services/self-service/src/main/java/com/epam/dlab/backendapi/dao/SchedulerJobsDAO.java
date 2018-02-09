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

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
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


    /** Return condition for search scheduler job to start/stop exploratory regarding to parameter passed.
     * @param today today's date.
     * @param now this moment time in format 'hh:MM'.
     * @param actionType 'start' string value for starting and another one for stopping.
     */
    private Bson schedulerJobActionCondition(LocalDate today, LocalTime now, String actionType) {
        return and(
                or(
                        lt(SCHEDULER_DATA + "." + BEGIN_DATE + ".0", today.getYear()),
                        and(lte(SCHEDULER_DATA + "." + BEGIN_DATE + ".0", today.getYear()),
                                lt(SCHEDULER_DATA + "." + BEGIN_DATE + ".1", today.getMonthValue())),
                        and(gte(SCHEDULER_DATA + "." + BEGIN_DATE + ".0", today.getYear()),
                                lte(SCHEDULER_DATA + "." + BEGIN_DATE + ".1", today.getMonthValue()),
                                lte(SCHEDULER_DATA + "." + BEGIN_DATE + ".2", today.getDayOfMonth()))
                ),
                or(
                        gt(SCHEDULER_DATA + "." + FINISH_DATE + ".0", today.getYear()),
                        and(gte(SCHEDULER_DATA + "." + FINISH_DATE + ".0", today.getYear()),
                                gt(SCHEDULER_DATA + "." + FINISH_DATE + ".1", today.getMonthValue())),
                        and(gte(SCHEDULER_DATA + "." + FINISH_DATE + ".0", today.getYear()),
                                gte(SCHEDULER_DATA + "." + FINISH_DATE + ".1", today.getMonthValue()),
                                gte(SCHEDULER_DATA + "." + FINISH_DATE + ".2", today.getDayOfMonth()))
                ),
                eq(SCHEDULER_DATA + "." + (actionType.equalsIgnoreCase(ACTION_START) ? START_TIME : END_TIME),
                                                                         Arrays.asList(now.getHour(), now.getMinute())),
                or(
                        and(exists(SCHEDULER_DATA + "." + DAYS_REPEAT + ".0"),
                                eq(SCHEDULER_DATA + "." + DAYS_REPEAT + ".0", today.getDayOfWeek().toString())),
                        and(exists(SCHEDULER_DATA + "." + DAYS_REPEAT + ".1"),
                                eq(SCHEDULER_DATA + "." + DAYS_REPEAT + ".1", today.getDayOfWeek().toString())),
                        and(exists(SCHEDULER_DATA + "." + DAYS_REPEAT + ".2"),
                                eq(SCHEDULER_DATA + "." + DAYS_REPEAT + ".2", today.getDayOfWeek().toString())),
                        and(exists(SCHEDULER_DATA + "." + DAYS_REPEAT + ".3"),
                                eq(SCHEDULER_DATA + "." + DAYS_REPEAT + ".3", today.getDayOfWeek().toString())),
                        and(exists(SCHEDULER_DATA + "." + DAYS_REPEAT + ".4"),
                                eq(SCHEDULER_DATA + "." + DAYS_REPEAT + ".4", today.getDayOfWeek().toString())),
                        and(exists(SCHEDULER_DATA + "." + DAYS_REPEAT + ".5"),
                                eq(SCHEDULER_DATA + "." + DAYS_REPEAT + ".5", today.getDayOfWeek().toString())),
                        and(exists(SCHEDULER_DATA + "." + DAYS_REPEAT + ".6"),
                                eq(SCHEDULER_DATA + "." + DAYS_REPEAT + ".6", today.getDayOfWeek().toString()))
                )
        );
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
                        schedulerNotNullCondition(),
                        schedulerJobActionCondition(date, time, actionType)
                ),
                fields(excludeId(), include(USER, ExploratoryDAO.EXPLORATORY_NAME, SCHEDULER_DATA)));
        for (Document d : docs) {
            if(d.containsKey(USER) && d.containsKey(EXPLORATORY_NAME) && d.containsKey(SCHEDULER_DATA)){
                jobList.add(new SchedulerJobData(d.getString(USER), d.getString(EXPLORATORY_NAME), convertFromDocument((Document) d.get(SCHEDULER_DATA), SchedulerJobDTO.class)));
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

}

