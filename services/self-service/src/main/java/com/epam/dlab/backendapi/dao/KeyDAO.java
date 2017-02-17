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

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.set;

import com.epam.dlab.dto.keyload.KeyLoadStatus;
import com.epam.dlab.dto.keyload.UserAWSCredentialDTO;
import com.epam.dlab.dto.keyload.UserKeyDTO;
import com.epam.dlab.exceptions.DlabException;

/** DAO for manage the user key.
 */
public class KeyDAO extends BaseDAO {
	
	/** Write the user key to Mongo database.
	 * @param user user name
	 * @param content key
	 * @exception DlabException
	 */
    public void uploadKey(final String user, String content) throws DlabException {
        UserKeyDTO key = new UserKeyDTO().withContent(content).withStatus(KeyLoadStatus.NEW.getStatus());
        insertOne(USER_KEYS, key, user);
    }

	/** Write the status of user key to Mongo database.
	 * @param user user name
	 * @param status the status of user key.
	 * @exception DlabException
	 */
    public void updateKey(String user, String status) throws DlabException {
        updateOne(USER_KEYS, eq(ID, user), set(STATUS, status));
    }

	/** Delete the user key from Mongo database.
	 * @param user user name
	 */
    public void deleteKey(String user) {
        mongoService.getCollection(USER_KEYS).deleteOne(eq(ID, user));
    }

	/** Write the credential of user to Mongo database.
	 * @param user user name
	 * @param credential the credential of user
	 * @exception DlabException
	 */
    public void saveCredential(String user, UserAWSCredentialDTO credential) throws DlabException {
        insertOne(USER_AWS_CREDENTIALS, credential, user);
    }

	/** Finds and returns the IP address of EDGE notebook for user.
	 * @param user user name
	 * @exception DlabException
	 */
    public String getUserEdgeIP(String user) throws DlabException {
        return findOne(USER_AWS_CREDENTIALS, eq(ID, user), UserAWSCredentialDTO.class)
                .orElse(new UserAWSCredentialDTO())
                .getPublicIp();
	}
    
    public UserAWSCredentialDTO getUserAWSCredential(String user) {
    	return findOne(USER_AWS_CREDENTIALS,
    			eq(ID, user),
    			UserAWSCredentialDTO.class)
    			.orElse(new UserAWSCredentialDTO());
    }

	/** Finds and returns the status of user key.
	 * @param user user name
	 * @exception DlabException
	 */
    public KeyLoadStatus findKeyStatus(String user) throws DlabException {
        return findOne(USER_KEYS, eq(ID, user), UserKeyDTO.class)
                .map(UserKeyDTO::getStatus)
                .map(KeyLoadStatus::findByStatus)
                .orElse(KeyLoadStatus.NONE);
    }
}
