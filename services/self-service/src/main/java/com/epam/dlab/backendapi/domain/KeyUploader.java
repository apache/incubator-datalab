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

import javax.ws.rs.core.Response;

import static com.epam.dlab.constants.ServiceConsts.PROVISIONING_SERVICE_NAME;

public class KeyUploader implements KeyLoaderAPI, IKeyUploader {
    @Inject
    private KeyDAO keyDAO;
    @Inject
    private SettingsDAO settingsDAO;

    @Inject
    @Named(PROVISIONING_SERVICE_NAME)
    private RESTService provisioningService;

    @Override
    public KeyLoadStatus checkKey(UserInfo userInfo) {
        return keyDAO.findKeyStatus(userInfo);
    }

    @Override
    public void startKeyUpload(UserInfo userInfo, String content) {
        keyDAO.uploadKey(userInfo.getName(), content);
        try {
            EdgeCreateDTO edge = new EdgeCreateDTO()
                    .withIamUser(userInfo.getName())
                    .withEdgeUserName(UsernameUtils.removeDomain(userInfo.getName()))
                    .withServiceBaseName(settingsDAO.getServiceBaseName())
                    .withSecurityGroupIds(settingsDAO.getSecurityGroups())
                    .withRegion(settingsDAO.getCredsRegion())
                    .withVpcId(settingsDAO.getCredsVpcId())
                    .withSubnetId(settingsDAO.getCredsSubnetId());
            UploadFileDTO dto = new UploadFileDTO()
                    .withEdge(edge)
                    .withContent(content);
            Response response = provisioningService.post(KEY_LOADER, dto, Response.class);
            if (Response.Status.ACCEPTED.getStatusCode() != response.getStatus()) {
                keyDAO.deleteKey(userInfo.getName());
            }
        } catch (Exception e) {
            keyDAO.deleteKey(userInfo.getName());
            throw new DlabException("Could not upload the key", e);
        }
    }

    @Override
    public void onKeyUploadComplete(UploadFileResultDTO result) {
        keyDAO.updateKey(result.getUser(), KeyLoadStatus.getStatus(result.isSuccess()));
        if (result.isSuccess()) {
            keyDAO.saveCredential(result.getUser(), result.getCredential());
        } else {
            keyDAO.deleteKey(result.getUser());
        }
    }
}
