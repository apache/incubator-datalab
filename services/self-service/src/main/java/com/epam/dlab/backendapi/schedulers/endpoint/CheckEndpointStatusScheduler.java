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

package com.epam.dlab.backendapi.schedulers.endpoint;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.domain.EndpointDTO;
import com.epam.dlab.backendapi.schedulers.internal.Scheduled;
import com.epam.dlab.backendapi.service.EndpointService;
import com.epam.dlab.backendapi.service.SecurityService;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

@Scheduled("checkEndpointStatusScheduler")
@Slf4j
public class CheckEndpointStatusScheduler implements Job {

    @Inject
    private EndpointService endpointService;
    @Inject
    private SecurityService securityService;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        UserInfo serviceUser = securityService.getServiceAccountInfo("admin");
        endpointService.getEndpointsWithStatus(EndpointDTO.EndpointStatus.ACTIVE).stream()
                .filter(endpoint -> checkUrl(serviceUser, endpoint))
                .peek(e -> log.warn("Failed connecting to endpoint {}, url: \'{}\'", e.getName(), e.getUrl()))
                .forEach(e -> endpointService.updateEndpointStatus(e.getName(), EndpointDTO.EndpointStatus.INACTIVE));
    }

    private boolean checkUrl(UserInfo serviceUser, EndpointDTO endpoint) {
        try {
            endpointService.checkEndpointUrl(serviceUser, endpoint.getUrl());
        } catch (Exception e) {
            return true;
        }
        return false;
    }


}
