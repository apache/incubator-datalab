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
import com.epam.dlab.backendapi.SelfServiceApplicationConfiguration;
import com.epam.dlab.backendapi.core.UserInstanceDTO;
import com.epam.dlab.backendapi.dao.ComputationalDAO;
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.resources.dto.SparkStandaloneClusterCreateForm;
import com.epam.dlab.backendapi.resources.dto.SparkStandaloneClusterResource;
import com.epam.dlab.backendapi.resources.dto.UserComputationalResource;
import com.epam.dlab.backendapi.util.RequestBuilder;
import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.dto.azure.computational.SparkComputationalCreateAzure;
import com.epam.dlab.dto.base.DataEngineType;
import com.epam.dlab.dto.computational.ComputationalStatusDTO;
import com.epam.dlab.dto.computational.ComputationalTerminateDTO;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.client.RESTService;
import com.epam.dlab.rest.contracts.ComputationalAPI;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

import static com.epam.dlab.UserInstanceStatus.*;

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

    @Inject
    private SelfServiceApplicationConfiguration configuration;


    /**
     * Asynchronously triggers creation of Spark cluster
     *
     * @param userInfo user authentication info
     * @param form     input cluster parameters
     * @return <code>true</code> if action is successfully triggered, <code>false</code>false if cluster with the same
     * name already exists
     * @throws IllegalArgumentException if input parameters exceed limits or docker image name is malformed
     */
    public boolean createSparkCluster(UserInfo userInfo, SparkStandaloneClusterCreateForm form) {

        validateForm(form);

        if (computationalDAO.addComputational(userInfo.getName(), form.getNotebookName(),
                createInitialComputationalResource(form))) {

            try {
                UserInstanceDTO instance = exploratoryDAO.fetchExploratoryFields(userInfo.getName(), form.getNotebookName());

                SparkComputationalCreateAzure dto = RequestBuilder.newComputationalCreate(userInfo, instance, form);

                String uuid = provisioningService.post(ComputationalAPI.COMPUTATIONAL_CREATE_SPARK, userInfo.getAccessToken(), dto, String.class);
                RequestId.put(userInfo.getName(), uuid);
                return true;
            } catch (RuntimeException e) {
                try {
                    updateComputationalStatus(userInfo.getName(), form.getNotebookName(), form.getName(), FAILED);
                } catch (DlabException d) {
                    log.error("Could not update the status of computational resource {} for user {}",
                            form.getName(), userInfo.getName(), d);
                }
                throw e;
            }
        } else {
            log.debug("Computational with name {} is already existing for user {}", form.getName(), userInfo.getName());
            return false;
        }
    }


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
            UserComputationalResource computationalResource = computationalDAO.fetchComputationalFields(userInfo.getName(), exploratoryName, computationalName);

            ComputationalTerminateDTO dto = RequestBuilder.newComputationalTerminate(userInfo, exploratoryName,
                    exploratoryId, computationalName, computationalResource.getComputationalId());

            String uuid = provisioningService.post(getTerminateUrl(computationalResource), userInfo.getAccessToken(), dto, String.class);
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

    private String getTerminateUrl(UserComputationalResource computationalResource) {

        if (DataEngineType.fromDockerImageName(computationalResource.getImageName())
                == DataEngineType.SPARK_STANDALONE) {

            return ComputationalAPI.COMPUTATIONAL_TERMINATE_SPARK;
        } else if (DataEngineType.fromDockerImageName(computationalResource.getImageName())
                == DataEngineType.CLOUD_SERVICE) {

            return ComputationalAPI.COMPUTATIONAL_TERMINATE_CLOUD_SPECIFIC;
        } else {
            throw new IllegalArgumentException("Unknown docker image for " + computationalResource);
        }
    }

    /**
     * Validates if input form is correct
     *
     * @param form user input form
     * @throws IllegalArgumentException if user typed wrong arguments
     */

    private void validateForm(SparkStandaloneClusterCreateForm form) {

        int instanceCount = Integer.parseInt(form.getDataEngineInstanceCount());

        if (instanceCount < configuration.getMinSparkInstanceCount()
                || instanceCount > configuration.getMaxSparkInstanceCount()) {
            throw new IllegalArgumentException(String.format("Instance count should be in range [%d..%d]",
                    configuration.getMinSparkInstanceCount(), configuration.getMaxSparkInstanceCount()));
        }

        if (DataEngineType.fromDockerImageName(form.getImage()) != DataEngineType.SPARK_STANDALONE) {
            throw new IllegalArgumentException(String.format("Unknown data engine %s", form.getImage()));
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

    private SparkStandaloneClusterResource createInitialComputationalResource(SparkStandaloneClusterCreateForm form) {

        return SparkStandaloneClusterResource.builder()
                .computationalName(form.getName())
                .imageName(form.getImage())
                .templateName(form.getTemplateName())
                .status(CREATING.toString())
                .dataEngineInstanceCount(form.getDataEngineInstanceCount())
                .dataEngineMasterSize(form.getDataEngineMasterSize())
                .dataEngineSlaveSize(form.getDataEngineSlaveSize())
                .build();
    }

}
