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

@Singleton
@Slf4j
public class UserInfoDAOMongoImpl implements UserInfoDAO {
	private static final String LAST_ACCESS_FIELD = "lastAccess";
	private final MongoService ms;
	private final long inactiveUserTimeoutMsec;

	@Inject
	public UserInfoDAOMongoImpl(MongoService ms, SecurityServiceConfiguration securityServiceConfiguration) {
		this.ms = ms;
		this.inactiveUserTimeoutMsec = securityServiceConfiguration.getInactiveUserTimeoutMillSec();
	}

	@Override
	public UserInfo getUserInfoByAccessToken(String accessToken) {
		BasicDBObject uiSearchDoc = new BasicDBObject();
		uiSearchDoc.put("_id", accessToken);
		MongoCollection<BasicDBObject> mc = ms.getCollection("security", BasicDBObject.class);
		FindIterable<BasicDBObject> res = mc.find(uiSearchDoc);
		BasicDBObject uiDoc = res.first();
		if (uiDoc == null) {
			log.warn("UI not found {}", accessToken);
			return null;
		}
		Date lastAccess = uiDoc.getDate(LAST_ACCESS_FIELD);
		final Date expireAt = uiDoc.getDate("expireAt");
		if ((inactiveUserTimeoutMsec < Math.abs(new Date().getTime() - lastAccess.getTime())) ||
				(expireAt != null && new Date().after(expireAt))) {
			log.warn("UI for {} expired but were not evicted from DB. Contact MongoDB admin to create expireable " +
					"index on 'lastAccess' key.", accessToken);
			this.deleteUserInfo(accessToken);
			return null;
		}

		String name = uiDoc.get("name").toString();
		String firstName = uiDoc.getString("firstName", "");
		String lastName = uiDoc.getString("lastName", "");
		String remoteIp = uiDoc.getString("remoteIp", "");
		BasicDBList roles = (BasicDBList) uiDoc.get("roles");
		Boolean awsUser = uiDoc.getBoolean("awsUser", false);
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
		log.debug("Found persistent {}", ui);
		return ui;
	}

	@Override
	public void updateUserInfoTTL(String accessToken, UserInfo ui) {
		//Update is caleed often, but does not need to be synchronized with the main thread

		BasicDBObject uiDoc = new BasicDBObject();
		uiDoc.put("_id", accessToken);
		uiDoc.put(LAST_ACCESS_FIELD, new Date(System.currentTimeMillis()));
		MongoCollection<BasicDBObject> security = ms.getCollection("security", BasicDBObject.class);
		security.updateOne(new BasicDBObject("_id", accessToken), new BasicDBObject("$set", uiDoc));
		log.debug("Updated persistent {}", accessToken);

	}

	@Override
	public void deleteUserInfo(String accessToken) {
		//delete used in logout and has to be synchronized
		BasicDBObject uiDoc = new BasicDBObject();
		uiDoc.put("_id", accessToken);
		MongoCollection<BasicDBObject> security = ms.getCollection("security", BasicDBObject.class);
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
		uiDoc.put(LAST_ACCESS_FIELD, new Date(System.currentTimeMillis()));
		uiDoc.put("awsKeys", ui.getKeys());
		uiDoc.put("expireAt", ui.getExpireAt());
		MongoCollection<BasicDBObject> security = ms.getCollection("security", BasicDBObject.class);
		security.insertOne(uiDoc);
		log.debug("Saved persistent {}", ui);

	}

}
