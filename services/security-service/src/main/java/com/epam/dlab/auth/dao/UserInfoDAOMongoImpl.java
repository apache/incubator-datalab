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

package com.epam.dlab.auth.dao;

import com.epam.dlab.auth.SecurityServiceConfiguration;
import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.auth.UserInfoDAO;
import com.epam.dlab.mongo.MongoService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import lombok.extern.slf4j.Slf4j;

import java.util.Date;
import java.util.Optional;

@Singleton
@Slf4j
public class UserInfoDAOMongoImpl implements UserInfoDAO {
	private static final String EXPIRE_AT_COLUMN = "expireAt";
	private static final String SECURITY_COLLECTION = "security";
	private final MongoService ms;
	private final long inactiveUserTimeoutMsec;

	@Inject
	public UserInfoDAOMongoImpl(MongoService ms, SecurityServiceConfiguration securityServiceConfiguration) {
		this.ms = ms;
		this.inactiveUserTimeoutMsec = securityServiceConfiguration.getInactiveUserTimeoutMillSec();
	}

	@Override
	public Optional<UserInfo> getUserInfoByAccessToken(String accessToken) {
		BasicDBObject uiSearchDoc = new BasicDBObject();
		uiSearchDoc.put("_id", accessToken);
		MongoCollection<BasicDBObject> mc = ms.getCollection(SECURITY_COLLECTION, BasicDBObject.class);
		FindIterable<BasicDBObject> res = mc.find(uiSearchDoc);
		BasicDBObject uiDoc = res.first();
		return Optional.ofNullable(uiDoc)
				.filter(doc -> !isExpired(accessToken, doc.getDate(EXPIRE_AT_COLUMN)))
				.map(doc -> toUserInfo(accessToken, doc));
	}

	@Override
	public void updateUserInfoTTL(String accessToken, UserInfo ui) {

		BasicDBObject uiDoc = new BasicDBObject();
		uiDoc.put("_id", accessToken);
		uiDoc.put(EXPIRE_AT_COLUMN, new Date(System.currentTimeMillis()));
		MongoCollection<BasicDBObject> security = ms.getCollection(SECURITY_COLLECTION, BasicDBObject.class);
		security.updateOne(new BasicDBObject("_id", accessToken), new BasicDBObject("$set", uiDoc));
		log.debug("Updated persistent {}", accessToken);

	}

	@Override
	public void deleteUserInfo(String accessToken) {
		//delete used in logout and has to be synchronized
		BasicDBObject uiDoc = new BasicDBObject();
		uiDoc.put("_id", accessToken);
		MongoCollection<BasicDBObject> security = ms.getCollection(SECURITY_COLLECTION, BasicDBObject.class);
		security.deleteOne(uiDoc);
		log.debug("Deleted persistent {}", accessToken);
	}

	@Override
	public void saveUserInfo(UserInfo ui) {
		//UserInfo first cached and immediately becomes available
		//Saving can be asynch

		BasicDBObject uiDoc = new BasicDBObject();
		uiDoc.put("_id", ui.getAccessToken());
		uiDoc.put("name", ui.getName());
		uiDoc.put("firstName", ui.getFirstName());
		uiDoc.put("lastName", ui.getLastName());
		uiDoc.put("roles", ui.getRoles());
		uiDoc.put("remoteIp", ui.getRemoteIp());
		uiDoc.put("awsUser", ui.isAwsUser());
		uiDoc.put(EXPIRE_AT_COLUMN, new Date(System.currentTimeMillis()));
		uiDoc.put("awsKeys", ui.getKeys());
		MongoCollection<BasicDBObject> security = ms.getCollection(SECURITY_COLLECTION, BasicDBObject.class);
		security.insertOne(uiDoc);
		log.debug("Saved persistent {}", ui);

	}

	private UserInfo toUserInfo(String accessToken, BasicDBObject uiDoc) {
		String name = uiDoc.get("name").toString();
		String firstName = uiDoc.getString("firstName", "");
		String lastName = uiDoc.getString("lastName", "");
		String remoteIp = uiDoc.getString("remoteIp", "");
		BasicDBList roles = (BasicDBList) uiDoc.get("roles");
		boolean awsUser = uiDoc.getBoolean("awsUser", false);
		UserInfo ui = new UserInfo(name, accessToken);
		ui.setFirstName(firstName);
		ui.setLastName(lastName);
		ui.setRemoteIp(remoteIp);
		ui.setAwsUser(awsUser);
		Object awsKeys = uiDoc.get("awsKeys");
		if (awsKeys != null) {
			((BasicDBObject) awsKeys).forEach((key, val) -> ui.addKey(key, val.toString()));
		}
		roles.forEach(o -> ui.addRole("" + o));
		return ui;
	}

	private boolean isExpired(String accessToken, Date lastAccess) {
		if (inactiveUserTimeoutMsec < Math.abs(new Date().getTime() - lastAccess.getTime())) {
			log.warn("UI for {} expired but were not evicted from DB. Contact MongoDB admin to create expireable " +
					"index on 'expireAt' key.", accessToken);
			this.deleteUserInfo(accessToken);
			return true;
		}
		return false;
	}

}
