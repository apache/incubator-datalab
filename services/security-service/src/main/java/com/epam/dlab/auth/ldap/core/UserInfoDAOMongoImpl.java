package com.epam.dlab.auth.ldap.core;

import java.util.Date;

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
		UserInfo ui = new UserInfo(name, accessToken);
		ui.setFirstName(firstName);
		ui.setLastName(lastName);
		ui.setRemoteIp(remoteIp);
		roles.forEach(o->ui.addRole(""+o));
		LOG.debug("Found persistent {}",ui);
		return ui;
	}

	@Override
	public void updateUserInfoTTL(String accessToken, UserInfo ui) {
		BasicDBObject uiDoc = new BasicDBObject();
		uiDoc.put("_id", accessToken);
		uiDoc.put("expireAt", new Date( System.currentTimeMillis() + inactiveUserTimeoutMsec));
		MongoCollection<BasicDBObject> security = ms.getCollection("security",BasicDBObject.class);
		security.updateOne(new BasicDBObject("_id",accessToken),new BasicDBObject("$set",uiDoc));
		LOG.debug("Updated persistent {}",accessToken);
	}

	@Override
	public void deleteUserInfo(String accessToken) {
		BasicDBObject uiDoc = new BasicDBObject();
		uiDoc.put("_id", accessToken);
		MongoCollection<BasicDBObject> security = ms.getCollection("security",BasicDBObject.class);
		security.deleteOne(uiDoc);
		LOG.debug("Deleted persistent {}",accessToken);
		
	}

	@Override
	public void saveUserInfo(UserInfo ui) {
		BasicDBObject uiDoc = new BasicDBObject();
		uiDoc.put("_id", ui.getAccessToken());
		uiDoc.put("name", ui.getName());
		uiDoc.put("firstName", ui.getFirstName());
		uiDoc.put("lastName", ui.getLastName());
		uiDoc.put("roles", ui.getRoles());
		uiDoc.put("remoteIp", ui.getRemoteIp());
		uiDoc.put("expireAt", new Date( System.currentTimeMillis() + inactiveUserTimeoutMsec));
		
		MongoCollection<BasicDBObject> security = ms.getCollection("security",BasicDBObject.class);
		security.insertOne(uiDoc);
		LOG.debug("Saved persistent {}",ui);
		
	}

}
