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

package com.epam.datalab.backendapi.schedulers.endpoint;

import com.epam.datalab.auth.UserInfo;
import com.epam.datalab.backendapi.domain.EndpointDTO;
import com.epam.datalab.backendapi.schedulers.internal.Scheduled;
import com.epam.datalab.backendapi.service.EndpointService;
import com.epam.datalab.backendapi.service.SecurityService;
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
        endpointService.getEndpoints().stream()
                .filter(endpoint -> checkUrlWithStatus(serviceUser, endpoint))
                .forEach(this::changeStatusToOpposite);
    }

    private boolean checkUrlWithStatus(UserInfo serviceUser, EndpointDTO endpoint) {
        try {
            endpointService.checkUrl(serviceUser, endpoint.getUrl());
        } catch (Exception e) {
            log.warn("Failed connecting to endpoint {}, url: '{}'. {}", endpoint.getName(), endpoint.getUrl(), e.getMessage(), e);
            return endpoint.getStatus() == EndpointDTO.EndpointStatus.ACTIVE;
        }
        return endpoint.getStatus() == EndpointDTO.EndpointStatus.INACTIVE;
    }

    private void changeStatusToOpposite(EndpointDTO endpoint) {
        if (endpoint.getStatus() == EndpointDTO.EndpointStatus.ACTIVE) {
            endpointService.updateEndpointStatus(endpoint.getName(), EndpointDTO.EndpointStatus.INACTIVE);
        } else {
            endpointService.updateEndpointStatus(endpoint.getName(), EndpointDTO.EndpointStatus.ACTIVE);
        }
    }
}
