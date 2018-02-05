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

import com.epam.dlab.dto.CreateSchedulerJobDTO;
import com.epam.dlab.dto.UserInstanceDTO;
import com.epam.dlab.exceptions.DlabException;
import com.google.inject.Singleton;
import com.mongodb.client.FindIterable;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import java.time.DayOfWeek;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static com.epam.dlab.backendapi.dao.ExploratoryDAO.*;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Projections.*;
import static com.mongodb.client.model.Updates.push;

/**
 * DAO for user's scheduler jobs.
 */
@Slf4j
@Singleton
public class SchedulerJobsDAO extends BaseDAO{

    public static final String SCHEDULER_DATA = "scheduler_data";
    private static final String BEGIN_DATE = "begin_date";
    private static final String FINISH_DATE = "finish_date";
    private static final String START_TIME = "start_time";
    private static final String END_TIME = "end_time";
    private static final String DAYS_REPEAT = "days_repeat";

    public SchedulerJobsDAO() {
        log.info("{} is initialized", getClass().getSimpleName());
    }

    /** Return condition for search scheduler which is equivalent to existing one.
     ** @param beginDate start date during general period of scheduler job duration.
     * @param finishDate finish date during general period of scheduler job duration.
     * @param startTime start time of day for scheduler job executing.
     * @param endTime end time of day for scheduler job executing.
     * @param daysRepeat days of week for scheduler repeating.
     */
    private static Bson schedulerEquivalenceCondition(LocalDate beginDate, LocalDate finishDate, LocalTime startTime, LocalTime endTime, List<DayOfWeek> daysRepeat) {
        return and(eq(BEGIN_DATE, beginDate),
                        eq(FINISH_DATE, finishDate),
                        eq(START_TIME, startTime),
                        eq(END_TIME, endTime),
                        eq(DAYS_REPEAT, daysRepeat));
    }

    /** Return condition for search scheduler which is not null.
     */
    private static Bson schedulerNotNullCondition() {
        return and(ne(BEGIN_DATE, null),
                ne(FINISH_DATE, null),
                ne(START_TIME, null),
                ne(END_TIME, null),
                ne(DAYS_REPEAT, null));
    }

    /** Return condition for search scheduler job to start.
     * @param beginDate start date during general period of scheduler job duration.
     * @param finishDate finish date during general period of scheduler job duration.
     * @param startTime start time of day for scheduler job executing.
     * @param day day for checking if it is included to list of days when scheduler job will start.
     */
    private static Bson schedulerJobStartCondition(Date beginDate, Date finishDate, Date startTime, String day) {
        return and(gte(BEGIN_DATE, beginDate),
                lte(FINISH_DATE, finishDate),
                        eq(START_TIME, startTime),
                regex(DAYS_REPEAT, "(.)*" + day + "(.)*"));
    }

    /** Return condition for search scheduler job to finish.
     * @param beginDate start date during general period of scheduler job duration.
     * @param finishDate finish date during general period of scheduler job duration.
     * @param endTime end time of day for scheduler job executing.
     * @param day day for checking if it is included to list of days when scheduler job will start.
     */
    private static Bson schedulerJobFinishCondition(Date beginDate, Date finishDate, Date endTime, String day) {
        return and(gte(BEGIN_DATE, beginDate),
                lte(FINISH_DATE, finishDate),
                eq(END_TIME, endTime),
                regex(DAYS_REPEAT, "(.)*" + day + "(.)*"));
    }

    /** Get all user's scheduler jobs for all exploratories.
     * @param user user name.
     */
    public Document getAllSchedulerJobs(String user) {
        Optional<Document> opt = findOne(USER_INSTANCES,
                and(eq(USER, user), schedulerNotNullCondition()),
                fields(excludeId(), include(SCHEDULER_DATA)));

        return opt.orElseGet(Document::new);
    }

    /** Get all user's scheduler jobs for all running exploratories.
     * @param user user name.
     */
    public Document getSchedulerJobsForRunningExploratories(String user) {
        Optional<Document> opt = findOne(USER_INSTANCES,
                and(eq(USER, user), schedulerNotNullCondition(), eq(STATUS, "running")),
                fields(excludeId(), include(SCHEDULER_DATA)));

        return opt.orElseGet(Document::new);
    }

    /** Get all user's scheduler jobs for exploratory.
     * @param user user name.
     * @param exploratoryName name of exploratory.
     */
    public Document getSchedulerJobsForExploratory(String user, String exploratoryName) {
        Optional<Document> opt = findOne(USER_INSTANCES,
                and(exploratoryCondition(user, exploratoryName), schedulerNotNullCondition()),
                fields(excludeId(), include(SCHEDULER_DATA)));

        return opt.orElseGet(Document::new);
    }

    /** Get all user's scheduler jobs for running exploratory.
     * @param user user name.
     * @param exploratoryName name of exploratory.
     */
    public Document getSchedulerJobsForRunningExploratory(String user, String exploratoryName) {
        Optional<Document> opt = findOne(USER_INSTANCES,
                and(exploratoryCondition(user, exploratoryName), runningExploratoryCondition(user, exploratoryName),
                        schedulerNotNullCondition()),
                fields(excludeId(), include(SCHEDULER_DATA)));

        return opt.orElseGet(Document::new);
    }

    /** Add the user's scheduler job for exploratory into database.
     * @param user user name.
     * @param exploratoryName name of exploratory.
     * @param job scheduler job.
     * @return <b>true</b> if operation was successful, otherwise <b>false</b>.
     */
    public boolean addSchedulerJob(String user, String exploratoryName, CreateSchedulerJobDTO job) {
        Optional<Document> opt = findOne(USER_INSTANCES,
                and(exploratoryCondition(user, exploratoryName), schedulerNotNullCondition()),
                schedulerEquivalenceCondition(job.getBeginDate(), job.getFinishDate(), job.getStartTime(), job.getEndTime(),
                        job.getDaysRepeat()));
        if (!opt.isPresent()) {
            updateOne(USER_INSTANCES,
                    exploratoryCondition(user, exploratoryName),
                    push(SCHEDULER_DATA, convertToBson(job)));
            return true;
        } else {
            Document values = new Document();
            values.append(BEGIN_DATE, job.getBeginDate());
            values.append(FINISH_DATE, job.getFinishDate());
            values.append(START_TIME, job.getStartTime());
            values.append(END_TIME, job.getEndTime());
            values.append(DAYS_REPEAT, job.getDaysRepeat());

            updateOne(USER_INSTANCES, and(exploratoryCondition(user, exploratoryName),
                    schedulerEquivalenceCondition(job.getBeginDate(), job.getFinishDate(), job.getStartTime(), job.getEndTime(),
                            job.getDaysRepeat())), new Document(SET, values));
            return false;
        }
    }

    /**
     * Finds and returns the info of all user's scheduler jobs.
     * @param user user name.
     * @throws DlabException
     */
    public List<CreateSchedulerJobDTO> fetchSchedulerJobsByUser(String user) throws DlabException {
        FindIterable<Document> docs = getCollection(USER_INSTANCES)
                .find(and(eq(USER, user), schedulerNotNullCondition()));
        if(docs == null){
            throw new DlabException("Scheduler jobs for user " + user + " not found.");
        }
        List<CreateSchedulerJobDTO> jobs = new ArrayList<>();
        for (Document d : docs) {
            if(d.containsKey(SCHEDULER_DATA)){
                jobs.add(d.get(SCHEDULER_DATA, CreateSchedulerJobDTO.class));
            }
        }
        return jobs;
    }

    /**
     * Finds and returns the info of user's scheduler jobs by exploratory name.
     * @param user            user name.
     * @param exploratoryName the name of exploratory.
     * @throws DlabException
     */
    public List<CreateSchedulerJobDTO> fetchSchedulerJobsByExploratory(String user, String exploratoryName) throws DlabException{
        FindIterable<Document> docs = getCollection(USER_INSTANCES)
                .find(and(exploratoryCondition(user, exploratoryName), schedulerNotNullCondition()));
        if(docs == null){
            throw new DlabException("Scheduler jobs for user " + user + " and exploratory resource " + exploratoryName + " not found.");
        }
        List<CreateSchedulerJobDTO> jobs = new ArrayList<>();
        for (Document d : docs) {
            if(d.containsKey(SCHEDULER_DATA)){
                jobs.add(d.get(SCHEDULER_DATA, CreateSchedulerJobDTO.class));
            }
        }
        return jobs;
    }

    /**
     * Finds and returns the info of all user's scheduler jobs for running exploratories.
     * @param user user name.
     * @throws DlabException
     */
    public List<CreateSchedulerJobDTO> fetchSchedulerJobsForRunningExploratories(String user) throws DlabException{
        FindIterable<Document> docs = getCollection(USER_INSTANCES)
                .find(and(eq(USER, user), schedulerNotNullCondition()));
        if(docs == null){
            throw new DlabException("Scheduler jobs for user " + user + " not found.");
        }
        List<CreateSchedulerJobDTO> jobs = new ArrayList<>();
        for (Document d : docs) {
            if(d.getString(STATUS).equals(STATUS_RUNNING) && d.containsKey(SCHEDULER_DATA)){
                jobs.add(d.get(SCHEDULER_DATA, CreateSchedulerJobDTO.class));
            }
        }
        return jobs;
    }

    /**
     * Finds and returns the info of user's single scheduler job by exploratory name.
     * @param user            user name.
     * @param exploratoryName the name of exploratory.
     * @throws DlabException
     */
    public CreateSchedulerJobDTO fetchSingleSchedulerJobByExploratory(String user, String exploratoryName) throws DlabException{
        Optional<UserInstanceDTO> opt = findOne(USER_INSTANCES,
                and(exploratoryCondition(user, exploratoryName), schedulerNotNullCondition()),
                UserInstanceDTO.class);

        if (opt.isPresent()) {
            return opt.get().getSchedulerData();
        }
        throw new DlabException("Scheduler job for user " + user + " and exploratory name" + exploratoryName + " not found.");
    }


    /**
     * Finds and returns the info of user's single scheduler job for running exploratory.
     * @param user              user name.
     * @param exploratoryName   name of exploratory.
     * @throws DlabException
     */
    public CreateSchedulerJobDTO fetchSingleSchedulerJobByRunningExploratory(String user, String exploratoryName) throws DlabException{
        Optional<UserInstanceDTO> opt = findOne(USER_INSTANCES,
                and(runningExploratoryCondition(user, exploratoryName), schedulerNotNullCondition()),
                UserInstanceDTO.class);

        if (opt.isPresent()) {
            return opt.get().getSchedulerData();
        }
        throw new DlabException(String.format("Running notebook %s not found for user %s or there are no schedulers for " +
                        "this notebook.",
                exploratoryName, user));
    }

}

