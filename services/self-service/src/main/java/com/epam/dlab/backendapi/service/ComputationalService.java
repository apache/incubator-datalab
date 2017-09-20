/*
 * Copyright (c) 2017, EPAM SYSTEMS INC
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

import com.epam.dlab.UserInstanceStatus;
import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.ComputationalDAO;
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.util.RequestBuilder;
import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.dto.computational.ComputationalStatusDTO;
import com.epam.dlab.dto.computational.ComputationalTerminateDTO;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.client.RESTService;
import com.epam.dlab.rest.contracts.ComputationalAPI;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

import static com.epam.dlab.UserInstanceStatus.FAILED;
import static com.epam.dlab.UserInstanceStatus.TERMINATING;

@Singleton
@Slf4j
public class ComputationalService {

    @Inject
    private ExploratoryDAO exploratoryDAO;

    @Inject
    private ComputationalDAO computationalDAO;

    @Inject
    @Named(ServiceConsts.PROVISIONING_SERVICE_NAME)
    private RESTService provisioningService;


    /**
     * Asynchronously triggers termination of computational resources
     *
     * @param userInfo          user info of authenticated user
     * @param exploratoryName   name of exploratory where to terminate computational resources with
     *                          <code>computationalName</code>
     * @param computationalName computational name
     */
    public void terminateComputationalEnvironment(UserInfo userInfo, String exploratoryName, String computationalName) {
        try {

            updateComputationalStatus(userInfo.getName(), exploratoryName, computationalName, TERMINATING);

            String exploratoryId = exploratoryDAO.fetchExploratoryId(userInfo.getName(), exploratoryName);
            String computationalId = computationalDAO.fetchComputationalId(userInfo.getName(), exploratoryName, computationalName);

            ComputationalTerminateDTO dto = RequestBuilder.newComputationalTerminate(userInfo, exploratoryName,
                    exploratoryId, computationalName, computationalId);

            String uuid = provisioningService.post(ComputationalAPI.COMPUTATIONAL_TERMINATE, userInfo.getAccessToken(), dto, String.class);
            RequestId.put(userInfo.getName(), uuid);
        } catch (RuntimeException re) {

            try {
                updateComputationalStatus(userInfo.getName(), exploratoryName, computationalName, FAILED);
            } catch (DlabException e) {
                log.error("Could not update the status of computational resource {} for user {}",
                        computationalName, userInfo.getName(), e);
            }

            throw re;
        }

    }

    /**
     * Updates the status of computational resource in database.
     *
     * @param user              user name.
     * @param exploratoryName   name of exploratory.
     * @param computationalName name of computational resource.
     * @param status            status
     */
    private void updateComputationalStatus(String user, String exploratoryName, String computationalName, UserInstanceStatus status) {
        ComputationalStatusDTO computationalStatus = new ComputationalStatusDTO()
                .withUser(user)
                .withExploratoryName(exploratoryName)
                .withComputationalName(computationalName)
                .withStatus(status);

        computationalDAO.updateComputationalStatus(computationalStatus);
    }

}
