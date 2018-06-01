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

package com.epam.dlab.backendapi.resources.callback.base;

import com.epam.dlab.backendapi.dao.KeyDAO;
import com.epam.dlab.dto.UserInstanceStatus;
import com.epam.dlab.dto.base.edge.EdgeInfo;
import com.epam.dlab.dto.keyload.KeyLoadStatus;
import com.epam.dlab.exceptions.DlabException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class KeyUploaderCallback {
    @Inject
    private KeyDAO keyDAO;

    public void handleCallback(String status, String user, EdgeInfo edgeInfo) {

        boolean isSuccess = UserInstanceStatus.of(status) == UserInstanceStatus.RUNNING;
        try {
            keyDAO.updateKey(user, KeyLoadStatus.getStatus(isSuccess));
            if (isSuccess) {
                keyDAO.updateEdgeInfo(user, edgeInfo);
            } else {
                UserInstanceStatus instanceStatus = UserInstanceStatus.of(keyDAO.getEdgeStatus(user));
                if (instanceStatus == null) {
                    // Upload the key first time
                    log.debug("Delete the key for user {}", user);
                    keyDAO.deleteKey(user);
                } else {
                    keyDAO.updateEdgeStatus(user, status);
                }
            }
        } catch (DlabException e) {
            log.error("Could not upload the key result and create EDGE node for user {}", user, e);
            throw new DlabException("Could not upload the key result and create EDGE node for user " + user + ": " + e.getLocalizedMessage(), e);
        }
    }
}
