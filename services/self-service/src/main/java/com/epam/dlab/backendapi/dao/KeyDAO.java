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

package com.epam.dlab.backendapi.dao;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.dto.keyload.KeyLoadStatus;
import com.epam.dlab.dto.keyload.UserAWSCredentialDTO;
import com.epam.dlab.dto.keyload.UserKeyDTO;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.set;

public class KeyDAO extends BaseDAO {
    public void uploadKey(final String user, String content) {
        UserKeyDTO key = new UserKeyDTO().withContent(content).withStatus(KeyLoadStatus.NEW.getStatus());
        insertOne(USER_KEYS, key, user);
    }

    public void updateKey(String user, String status) {
        updateOne(USER_KEYS, eq(ID, user), set(STATUS, status));
    }

    public void deleteKey(String user) {
        mongoService.getCollection(USER_KEYS).deleteOne(eq(ID, user));
    }

    public void saveCredential(String user, UserAWSCredentialDTO credential) {
        insertOne(USER_AWS_CREDENTIALS, credential, user);
    }

    public String getUserEdgeIP(String user) {
        return findOne(USER_AWS_CREDENTIALS, eq(ID, user), UserAWSCredentialDTO.class)
                .orElse(new UserAWSCredentialDTO())
                .getPublicIp();
    }

    public KeyLoadStatus findKeyStatus(UserInfo userInfo) {
        return findOne(USER_KEYS, eq(ID, userInfo.getName()), UserKeyDTO.class)
                .map(UserKeyDTO::getStatus)
                .map(KeyLoadStatus::findByStatus)
                .orElse(KeyLoadStatus.NONE);
    }
}
