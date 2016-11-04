/******************************************************************************************************

 Copyright (c) 2016 EPAM Systems Inc.

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 *****************************************************************************************************/

package com.epam.dlab.auth.rest;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import jersey.repackaged.com.google.common.collect.Lists;
import org.slf4j.LoggerFactory;

import com.epam.dlab.auth.UserInfo;

import org.slf4j.Logger;

public class AuthorizedUsers {
	
	private final static Logger LOG = LoggerFactory.getLogger(AuthorizedUsers.class);
	
	private static class UserInfoHolder {
		private final UserInfo userInfo;
		private long expires;
		public UserInfoHolder(UserInfo ui, long timeout) {
			this.userInfo = ui;
			this.expires  = System.currentTimeMillis() + timeout;
		}
		public boolean expired() {
			return expires < System.currentTimeMillis();
		}
		@Override
		public String toString() {
			return "UserInfoHolder [userInfo=" + userInfo + ", expires=" + new Date(expires) + "]";
		}
		
	}
	
	private final static ScheduledExecutorService cleaner = Executors.newScheduledThreadPool(1);
	
	private final static AuthorizedUsers users = new AuthorizedUsers();
	
	static {
		cleaner.scheduleAtFixedRate(new Runnable() {
			@Override
			public void run() {
				users.cleanup();
			}
		}, 1, 1, TimeUnit.MINUTES);
	}
	private volatile long inactiveTimeout = 600000;
	
	private final Map<String,UserInfoHolder> knownUsers = new ConcurrentHashMap<>();
	
	private AuthorizedUsers() {

	}
	
	public static void setInactiveTimeout(long timeout) {
		users.inactiveTimeout = timeout;
	}
	
	public void addUserInfo(String token, UserInfo ui) {
		LOG.debug("Add token: {} {}",token,ui);
		knownUsers.put(token, new UserInfoHolder(ui, inactiveTimeout));
	}

	public UserInfo removeUserInfo(String token) {
		UserInfoHolder uih = knownUsers.remove(token);
		if(uih != null)  {
			return uih.userInfo;
		} else {
			return null;
		}
	}

	public void cleanup() {
		LOG.debug("cleaning user cache {} known users ...",knownUsers.size());
		for(String token: Lists.newArrayList(knownUsers.keySet())) {
			UserInfoHolder uih = knownUsers.get(token);			
			if(uih == null) {
				knownUsers.remove(token);
				LOG.debug("removed empty token {}",token);
			} else {
				if(uih.expired()) {
					knownUsers.remove(token);
					LOG.debug("removed expired handler {}",uih);
				}
			}
		}
	}
	
	public UserInfo getUserInfo(String token) {
		if(token == null) {
			return null;
		}
		UserInfoHolder uih = knownUsers.get(token);
		
		if(uih == null) {
			LOG.debug("Unknown token: {}",token);
			return null;
		} else {
			if(uih.expired()) {
				knownUsers.remove(token);
				LOG.debug("Expired token: {} {}",token,uih);
				return null;
			} else {
				uih.expires = System.currentTimeMillis() + inactiveTimeout;
			}
		}
		LOG.debug("Return token: {} {}",token,uih);
		return uih.userInfo;
	}
	
	public static AuthorizedUsers getInstance() {
		return users;
	}
	
}
