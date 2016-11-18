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

package com.epam.dlab.auth.ldap.core;

import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.auth.UserInfoDAO;
import com.epam.dlab.client.mongo.MongoService;
import com.mongodb.BasicDBList;
import com.mongodb.BasicDBObject;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;

public class UserInfoDAOMongoImpl implements UserInfoDAO {
	
	private static final Logger LOG = LoggerFactory.getLogger(UserInfoDAOMongoImpl.class);

	private final MongoService ms;
	private final long inactiveUserTimeoutMsec;
	private final ExecutorService executor = Executors.newCachedThreadPool();
	
	public UserInfoDAOMongoImpl(MongoService ms, long inactiveUserTimeoutMsec) {
		this.ms = ms;
		this.inactiveUserTimeoutMsec = inactiveUserTimeoutMsec;
	}
	
	@Override
	public UserInfo getUserInfoByAccessToken(String accessToken) {
		BasicDBObject uiSearchDoc = new BasicDBObject();
		uiSearchDoc.put("_id", accessToken);
		MongoCollection<BasicDBObject> mc = ms.getCollection("security",BasicDBObject.class);
		FindIterable<BasicDBObject>  res = mc.find(uiSearchDoc);
		BasicDBObject uiDoc = res.first();
		if(uiDoc == null) {
			LOG.warn("UI not found {}",accessToken);
			return null;
		}
		String name = uiDoc.get("name").toString();
		String firstName = uiDoc.get("firstName").toString();
		String lastName  = uiDoc.get("lastName").toString();
		String remoteIp  = uiDoc.get("remoteIp").toString();
		BasicDBList roles = (BasicDBList) uiDoc.get("roles");
		Boolean awsUser   = (Boolean)uiDoc.get("awsUser");
		UserInfo ui = new UserInfo(name, accessToken);
		ui.setFirstName(firstName);
		ui.setLastName(lastName);
		ui.setRemoteIp(remoteIp);
		if(awsUser != null) {
			ui.setAwsUser(awsUser);
		}
		roles.forEach(o->ui.addRole(""+o));
		LOG.debug("Found persistent {}",ui);
		return ui;
	}

	@Override
	public void updateUserInfoTTL(String accessToken, UserInfo ui) {
		//Update is caleed often, but does not need to be synchronized with the main thread
		executor.submit(()->{
			BasicDBObject uiDoc = new BasicDBObject();
			uiDoc.put("_id", accessToken);
			uiDoc.put("expireAt", new Date( System.currentTimeMillis() + inactiveUserTimeoutMsec));
			MongoCollection<BasicDBObject> security = ms.getCollection("security",BasicDBObject.class);
			security.updateOne(new BasicDBObject("_id",accessToken),new BasicDBObject("$set",uiDoc));
			LOG.debug("Updated persistent {}",accessToken);
		});
	}

	@Override
	public void deleteUserInfo(String accessToken) {
		//delete used in logout and has to be synchronized
		BasicDBObject uiDoc = new BasicDBObject();
		uiDoc.put("_id", accessToken);
		MongoCollection<BasicDBObject> security = ms.getCollection("security", BasicDBObject.class);
		security.deleteOne(uiDoc);
		LOG.debug("Deleted persistent {}", accessToken);
	}

	@Override
	public void saveUserInfo(UserInfo ui) {
		//UserInfo first cached and immediately becomes available
		//Saving can be asynch
		executor.submit(()-> {
			BasicDBObject uiDoc = new BasicDBObject();
			uiDoc.put("_id", ui.getAccessToken());
			uiDoc.put("name", ui.getName());
			uiDoc.put("firstName", ui.getFirstName());
			uiDoc.put("lastName", ui.getLastName());
			uiDoc.put("roles", ui.getRoles());
			uiDoc.put("remoteIp", ui.getRemoteIp());
			uiDoc.put("awsUser", ui.isAwsUser());
			uiDoc.put("expireAt", new Date(System.currentTimeMillis() + inactiveUserTimeoutMsec));
			MongoCollection<BasicDBObject> security = ms.getCollection("security", BasicDBObject.class);
			security.insertOne(uiDoc);
			LOG.debug("Saved persistent {}", ui);
		});
	}

}
