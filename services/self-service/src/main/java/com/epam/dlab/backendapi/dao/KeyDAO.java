/******************************************************************************************************

 Copyright (c) 2016 EPAM Systems Inc.

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 *****************************************************************************************************/

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
        update(USER_KEYS, eq(ID, user), set(STATUS, status));
    }

    public void deleteKey(String user) {
        mongoService.getCollection(USER_KEYS).deleteOne(eq(ID, user));
    }

    public void saveCredential(String user, UserAWSCredentialDTO credential) {
        insertOne(USER_AWS_CREDENTIALS, credential, user);
    }

    public KeyLoadStatus findKeyStatus(UserInfo userInfo) {
        return find(USER_KEYS, eq(ID, userInfo.getName()), UserKeyDTO.class)
                .map(UserKeyDTO::getStatus)
                .map(KeyLoadStatus::findByStatus)
                .orElse(KeyLoadStatus.NONE);
    }
}
