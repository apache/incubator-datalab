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
import com.epam.dlab.dto.SchedulerJobDTO;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class SchedulerJobsService {

    @Inject
    private SchedulerJobsDAO schedulerJobsDAO;

    @SuppressWarnings("unchecked")
    public SchedulerJobDTO fetchSchedulerJobForExploratory(String user, String exploratoryName) {
        return schedulerJobsDAO.fetchSingleSchedulerJobByExploratory(user, exploratoryName);
    }

}

