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

import com.epam.dlab.UserInstanceStatus;
import com.epam.dlab.dto.base.edge.EdgeInfo;
import com.epam.dlab.dto.keyload.KeyLoadStatus;
import com.epam.dlab.dto.keyload.UserKeyDTO;
import com.epam.dlab.exceptions.DlabException;
import com.mongodb.client.model.Updates;
import org.bson.Document;

import java.util.Date;
import java.util.Optional;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Projections.*;
import static com.mongodb.client.model.Updates.set;

/**
 * DAO for manage the user key.
 */
public abstract class KeyDAO extends BaseDAO {
	static final String EDGE_STATUS = "edge_status";
	private static final String KEY_CONTENT = "content";

	/**
	 * Store the user key to Mongo database.
	 *
	 * @param user    user name
	 * @param content key
	 */
	public void insertKey(final String user, String content) {
		UserKeyDTO key = new UserKeyDTO()
				.withContent(content)
				.withStatus(KeyLoadStatus.NEW.getStatus());
		insertOne(USER_KEYS, key, user);
	}

	/**
	 * Write the status of user key to Mongo database.
	 *
	 * @param user   user name
	 * @param status the status of user key.
	 */
	public void updateKey(String user, String status) {
		updateOne(USER_KEYS, eq(ID, user), set(STATUS, status));
	}

	/**
	 * Delete the user key from Mongo database.
	 *
	 * @param user user name
	 */
	public void deleteKey(String user) {
		mongoService.getCollection(USER_KEYS).deleteOne(eq(ID, user));
	}

	/**
	 * Inserts ('insertRequired' equals 'true') or updates ('insertRequired' equals 'false') the user key to/in Mongo
	 * database.
	 *
	 * @param user           user name
	 * @param content        key content
	 * @param insertRequired true/false
	 */
	public void upsertKey(final String user, String content, boolean insertRequired) {
		Document doc = new Document(SET,
				new Document()
				.append(ID, user)
				.append(KEY_CONTENT, content)
				.append(STATUS, insertRequired ? KeyLoadStatus.NEW.getStatus() : KeyLoadStatus.SUCCESS.getStatus())
						.append(TIMESTAMP, new Date()));
		updateOne(USER_KEYS, eq(ID, user), doc, insertRequired);
	}

	/**
	 * Finds and returns the user key.
	 *
	 * @param user user name.
	 */
	public UserKeyDTO fetchKey(String user) {
		Optional<UserKeyDTO> opt = findOne(USER_KEYS,
				eq(ID, user),
				UserKeyDTO.class);

		if (opt.isPresent()) {
			return opt.get();
		}
		throw new DlabException("Key of user " + user + " not found.");
	}

	/**
	 * Finds and returns the user key with the specified status
	 *
	 * @param user   user name.
	 * @param status key status
	 */
	public UserKeyDTO fetchKey(String user, KeyLoadStatus status) {
		return findOne(USER_KEYS,
				and(eq(ID, user), eq(STATUS, status.getStatus())),
				UserKeyDTO.class)
				.orElseThrow(() -> new DlabException(String.format("Key of user %s with status %s not found", user,
						status.getStatus())));
	}

	/**
	 * Store the EDGE of user to Mongo database.
	 *
	 * @param user     user name
	 * @param edgeInfo the EDGE of user
	 */
	public void updateEdgeInfo(String user, EdgeInfo edgeInfo) {
		Document d = new Document(SET,
				convertToBson(edgeInfo)
						.append(ID, user));
		updateOne(USER_EDGE,
				eq(ID, user),
				d,
				true);
	}

	public abstract EdgeInfo getEdgeInfo(String user);

	public <T extends EdgeInfo> T getEdgeInfo(String user, Class<T> target, T defaultValue) {
		return findOne(USER_EDGE,
				eq(ID, user), target)
				.orElse(defaultValue);
	}

	/**
	 * Finds and returns the status of user key.
	 *
	 * @param user user name
	 */
	public KeyLoadStatus findKeyStatus(String user) {
		return findOne(USER_KEYS, eq(ID, user), UserKeyDTO.class)
				.map(UserKeyDTO::getStatus)
				.map(KeyLoadStatus::findByStatus)
				.orElse(KeyLoadStatus.NONE);
	}

	/**
	 * Updates the status of EDGE node.
	 *
	 * @param user   user name
	 * @param status status of EDGE node
	 */
	public void updateEdgeStatus(String user, String status) {
		updateOne(USER_EDGE,
				eq(ID, user),
				Updates.set(EDGE_STATUS, status));
	}

	public void deleteEdge(String user) {
		mongoService.getCollection(USER_EDGE).deleteOne(eq(ID, user));
	}

	/**
	 * Return the status of EDGE node.
	 *
	 * @param user user name
	 */
	public String getEdgeStatus(String user) {
		Document d = findOne(USER_EDGE,
				eq(ID, user),
				fields(include(EDGE_STATUS), excludeId())).orElse(null);
		return (d == null ? "" : d.getString(EDGE_STATUS));
	}

	public boolean edgeNodeExist(String user) {
		return findOne(USER_EDGE, and(eq(ID, user), not(in(EDGE_STATUS, UserInstanceStatus.TERMINATING.toString(),
				UserInstanceStatus.TERMINATED.toString()))))
				.isPresent();
	}
}
