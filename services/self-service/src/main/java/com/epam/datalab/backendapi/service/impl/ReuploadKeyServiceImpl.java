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

package com.epam.datalab.backendapi.service.impl;

import com.epam.datalab.backendapi.dao.ComputationalDAO;
import com.epam.datalab.backendapi.dao.ExploratoryDAO;
import com.epam.datalab.backendapi.domain.RequestId;
import com.epam.datalab.backendapi.service.ExploratoryService;
import com.epam.datalab.backendapi.service.ReuploadKeyService;
import com.epam.datalab.backendapi.util.RequestBuilder;
import com.epam.datalab.dto.UserInstanceStatus;
import com.epam.datalab.dto.reuploadkey.ReuploadKeyStatus;
import com.epam.datalab.dto.reuploadkey.ReuploadKeyStatusDTO;
import com.epam.datalab.model.ResourceData;
import com.epam.datalab.model.ResourceType;
import com.epam.datalab.rest.client.RESTService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

import static com.epam.datalab.constants.ServiceConsts.PROVISIONING_SERVICE_NAME;
import static com.epam.datalab.dto.UserInstanceStatus.RUNNING;

@Singleton
@Slf4j
public class ReuploadKeyServiceImpl implements ReuploadKeyService {

    @Inject
    @Named(PROVISIONING_SERVICE_NAME)
    private RESTService provisioningService;
    @Inject
    private RequestBuilder requestBuilder;
    @Inject
    private RequestId requestId;
    @Inject
    private ExploratoryService exploratoryService;
    @Inject
    private ComputationalDAO computationalDAO;
    @Inject
    private ExploratoryDAO exploratoryDAO;

    private static final String REUPLOAD_KEY_UPDATE_MSG = "Reuploading key process is successfully finished. " +
            "Updating 'reupload_key_required' flag to 'false' for {}.";
    private static final String REUPLOAD_KEY_ERROR_MSG = "Reuploading key process is failed for {}. The next attempt" +
            "starts after resource restarting.";

    @Override
    public void updateResourceData(ReuploadKeyStatusDTO dto) {
        String user = dto.getUser();
        ResourceData resource = dto.getReuploadKeyCallbackDTO().getResource();
        log.debug("Updating resource {} to status RUNNING...", resource.toString());
        updateResourceStatus(user, null, resource, RUNNING);
        if (dto.getReuploadKeyStatus() == ReuploadKeyStatus.COMPLETED) {
            log.debug(REUPLOAD_KEY_UPDATE_MSG, resource.toString());
            updateResourceReuploadKeyFlag(user, null, resource, false);
        } else {
            log.error(REUPLOAD_KEY_ERROR_MSG, resource.toString());
        }
    }

    private void updateResourceStatus(String user, String project, ResourceData resourceData, UserInstanceStatus newStatus) {
        if (resourceData.getResourceType() == ResourceType.EXPLORATORY) {
            exploratoryDAO.updateStatusForExploratory(user, project, resourceData.getExploratoryName(), newStatus);
        } else if (resourceData.getResourceType() == ResourceType.COMPUTATIONAL) {
            computationalDAO.updateStatusForComputationalResource(user, project,
                    resourceData.getExploratoryName(), resourceData.getComputationalName(), newStatus);
        }
    }

    private void updateResourceReuploadKeyFlag(String user, String project, ResourceData resourceData, boolean reuploadKeyRequired) {
        if (resourceData.getResourceType() == ResourceType.EXPLORATORY) {
            exploratoryDAO.updateReuploadKeyForExploratory(user, project, resourceData.getExploratoryName(), reuploadKeyRequired);
        } else if (resourceData.getResourceType() == ResourceType.COMPUTATIONAL) {
            computationalDAO.updateReuploadKeyFlagForComputationalResource(user, project,
                    resourceData.getExploratoryName(), resourceData.getComputationalName(), reuploadKeyRequired);
        }
    }
}
