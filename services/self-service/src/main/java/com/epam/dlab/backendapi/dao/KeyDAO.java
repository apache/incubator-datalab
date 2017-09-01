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
import static com.mongodb.client.model.Projections.excludeId;
import static com.mongodb.client.model.Projections.fields;
import static com.mongodb.client.model.Projections.include;
import static com.mongodb.client.model.Updates.set;

import java.util.Optional;

import com.epam.dlab.dto.base.EdgeInfo;
import org.bson.Document;

import com.epam.dlab.dto.keyload.KeyLoadStatus;
import com.epam.dlab.dto.keyload.UserKeyDTO;
import com.epam.dlab.exceptions.DlabException;
import com.mongodb.client.model.Updates;

/** DAO for manage the user key.
 */
public class KeyDAO extends BaseDAO {
	protected static final String EDGE_STATUS = "edge_status";
	
	/** Store the user key to Mongo database.
	 * @param user user name
	 * @param content key
	 * @exception DlabException
	 */
    public void insertKey(final String user, String content) throws DlabException {
        UserKeyDTO key = new UserKeyDTO()
        		.withContent(content)
        		.withStatus(KeyLoadStatus.NEW.getStatus());
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
    
    /** Finds and returns the user key.
     * @param user user name.
     * @exception DlabException
     */
    public UserKeyDTO fetchKey(String user) throws DlabException {
        Optional<UserKeyDTO> opt = findOne(USER_KEYS,
        		eq(ID, user),
        		UserKeyDTO.class);

        if( opt.isPresent() ) {
            return opt.get();
        }
        throw new DlabException("Key of user " + user + " not found.");
    }

	/** Store the EDGE of user to Mongo database.
	 * @param user user name
	 * @param edgeInfo the EDGE of user
	 * @exception DlabException
	 */
    public void updateEdgeInfo(String user, EdgeInfo edgeInfo) throws DlabException {
    	Document d = new Document(SET,
    					convertToBson(edgeInfo)
    						.append(ID, user));
        updateOne(USER_EDGE,
        		eq(ID, user),
        		d,
        		true);
    }

    public <T extends EdgeInfo> T getEdgeInfo(String user, Class<T> target, T defaultValue) {
    	return findOne(USER_EDGE,
    			eq(ID, user), target)
    			.orElse(defaultValue);
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
    
    /** Updates the status of EDGE node.
     * @param user user name
	 * @param status status of EDGE node
     * @throws DlabException
     */
    public void updateEdgeStatus(String user, String status) throws DlabException {
    	updateOne(USER_EDGE,
        		eq(ID, user),
        		Updates.set(EDGE_STATUS, status));
    }

    /** Return the status of EDGE node.
     * @param user user name
     * @throws DlabException
     */
    public String getEdgeStatus(String user) throws DlabException {
    	Document d = findOne(USER_EDGE,
    			eq(ID, user),
    			fields(include(EDGE_STATUS), excludeId())).orElse(null);
    	return (d == null ? "" : d.getString(EDGE_STATUS));
    }
}
