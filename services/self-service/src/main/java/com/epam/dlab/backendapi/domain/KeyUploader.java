/***************************************************************************

 Copyright (c) 2016, EPAM SYSTEMS INC

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

package com.epam.dlab.backendapi.domain;

import static com.epam.dlab.constants.ServiceConsts.PROVISIONING_SERVICE_NAME;

import javax.ws.rs.core.Response;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.KeyDAO;
import com.epam.dlab.backendapi.dao.SettingsDAO;
import com.epam.dlab.backendapi.domain.contracts.IKeyUploader;
import com.epam.dlab.dto.edge.EdgeCreateDTO;
import com.epam.dlab.dto.keyload.KeyLoadStatus;
import com.epam.dlab.dto.keyload.UploadFileDTO;
import com.epam.dlab.dto.keyload.UploadFileResultDTO;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.client.RESTService;
import com.epam.dlab.rest.contracts.KeyLoaderAPI;
import com.epam.dlab.utils.UsernameUtils;
import com.google.inject.Inject;
import com.google.inject.name.Named;

/** Uploads the user key to EDGE notebook.
 */
public class KeyUploader implements KeyLoaderAPI, IKeyUploader {
	private static final Logger LOGGER = LoggerFactory.getLogger(KeyUploader.class);
	
    @Inject
    private KeyDAO keyDAO;
    @Inject
    private SettingsDAO settingsDAO;

    @Inject
    @Named(PROVISIONING_SERVICE_NAME)
    private RESTService provisioningService;

    @Override
    public KeyLoadStatus checkKey(UserInfo userInfo) throws DlabException {
    	LOGGER.trace("Find the status of the user key for {}", userInfo.getName());
        return keyDAO.findKeyStatus(userInfo.getName());
    }

    @Override
    public void startKeyUpload(UserInfo userInfo, String content) throws DlabException {
    	LOGGER.debug("The upload of the user key will be started for user {}", userInfo.getName());
        keyDAO.uploadKey(userInfo.getName(), content);
        try {
            EdgeCreateDTO edge = new EdgeCreateDTO()
                    .withIamUser(userInfo.getName())
                    .withEdgeUserName(UsernameUtils.removeDomain(userInfo.getName()))
                    .withServiceBaseName(settingsDAO.getServiceBaseName())
                    .withAwsSecurityGroupIds(settingsDAO.getAwsSecurityGroups())
                    .withAwsRegion(settingsDAO.getAwsRegion())
                    .withAwsVpcId(settingsDAO.getAwsVpcId())
                    .withAwsSubnetId(settingsDAO.getAwsSubnetId())
                    .withConfOsUser(settingsDAO.getConfOsUser())
                    .withConfOsFamily(settingsDAO.getConfOsFamily());
            UploadFileDTO dto = new UploadFileDTO()
                    .withEdge(edge)
                    .withContent(content);
            Response response = provisioningService.post(KEY_LOADER, userInfo.getAccessToken(), dto, Response.class);
        	LOGGER.debug("The upload of the user key for user {} response status {}", userInfo.getName(), response.getStatus());
            
            if (Response.Status.ACCEPTED.getStatusCode() != response.getStatus()) {
                keyDAO.deleteKey(userInfo.getName());
            }
        } catch (Exception e) {
        	LOGGER.error("The upload of the user key for user {} fails", userInfo.getName(), e);
            keyDAO.deleteKey(userInfo.getName());
            throw new DlabException("Could not upload the key", e);
        }
    }

    @Override
    public void onKeyUploadComplete(UploadFileResultDTO uploadKeyResult) throws DlabException {
    	LOGGER.debug("The upload of the user key for user {} has been completed, status is {}", uploadKeyResult.getUser(), uploadKeyResult.isSuccess());
        keyDAO.updateKey(uploadKeyResult.getUser(), KeyLoadStatus.getStatus(uploadKeyResult.isSuccess()));
        if (uploadKeyResult.isSuccess()) {
            keyDAO.saveCredential(uploadKeyResult.getUser(), uploadKeyResult.getCredential());
        } else {
            keyDAO.deleteKey(uploadKeyResult.getUser());
        }
    }
}
