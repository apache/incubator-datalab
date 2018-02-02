/*
 * Copyright (c) 2018, EPAM SYSTEMS INC
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


package com.epam.dlab.backendapi.service;

import com.epam.dlab.backendapi.dao.SchedulerJobsDAO;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Singleton
public class SchedulerJobsService {

    @Inject
    private SchedulerJobsDAO schedulerJobsDAO;

    @SuppressWarnings("unchecked")
    public List<Document> getAllSchedulerJobs(String user) {
        return (List<Document>) schedulerJobsDAO.getAllSchedulerJobs(user)
                .getOrDefault(SchedulerJobsDAO.SCHEDULER_DATA, new ArrayList<>());
    }

    @SuppressWarnings("unchecked")
    private List<Document> getSchedulerJobsForRunningExploratories(String user) {
        return (List<Document>) schedulerJobsDAO.getSchedulerJobsForRunningExploratories(user)
                .getOrDefault(SchedulerJobsDAO.SCHEDULER_DATA, new ArrayList<>());
    }

    @SuppressWarnings("unchecked")
    public List<Document> getSchedulerJobsForExploratory(String user, String exploratoryName) {
        return (List<Document>) schedulerJobsDAO.getSchedulerJobsForExploratory(user, exploratoryName)
                .getOrDefault(SchedulerJobsDAO.SCHEDULER_DATA, new ArrayList<>());
    }

}

