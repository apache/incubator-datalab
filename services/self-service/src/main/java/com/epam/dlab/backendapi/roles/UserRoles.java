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

package com.epam.dlab.backendapi.roles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.SecurityDAO;
import com.epam.dlab.exceptions.DlabException;
import com.google.common.base.MoreObjects;
import com.mongodb.client.FindIterable;

public class UserRoles {
	private static final Logger LOGGER = LoggerFactory.getLogger(UserRoles.class);
	
	private static final String GROUPS = "groups";
	private static final String USERS = "users";
	
	private static List<UserRole> roles = null;
	
    public static synchronized void initialize(SecurityDAO dao) throws DlabException {
    	LOGGER.trace("Loading roles from database");
    	try {
			FindIterable<Document> docs = dao.getRoles();
    		roles = new ArrayList<>();
			for (Document d : docs) {
				Set<String> groups = getAndRemoveSet(d, GROUPS);
				Set<String> users = getAndRemoveSet(d, USERS);
				for (RoleType type : RoleType.values()) {
					@SuppressWarnings("unchecked")
					List<String> names = d.get(type.getNodeName(), ArrayList.class);
					if (names != null) {
						for (String name : names) {
							append(type, name, groups, users);
						}
					}
				}
			}
    	} catch (Exception e) {
    		throw new DlabException("Cannot load roles from database. " + e.getLocalizedMessage(), e);
    	}
    	LOGGER.trace("New roles is {}", roles);
    }
    
    public static List<UserRole> getRoles() {
    	return roles;
    }
    
	public static boolean checkAccess(UserInfo userInfo, RoleType type, String name) {
		if (roles == null) {
			return true;
		}
		UserRole role = get(type, name);
		if (role == null ||
			(role.getUsers() != null && role.getUsers().contains(userInfo.getSimpleName()))) {
			return true; // No restriction
		}
		Set<String> groups = role.getGroups();
		if (groups != null) {
			for (String group : userInfo.getRoles()) {
				if (groups.contains(group)) {
					return true;
				}
			}
		}
		return false;
	}

	private static UserRole append(RoleType type, String name, Set<String> groups, Set<String> users) {
		UserRole item = new UserRole(type, name, groups, users);
	    synchronized (roles) {
			int index = Collections.binarySearch(roles, item);
			if (index < 0) {
				index = -index;
				if (index > roles.size()) {
					roles.add(item);
				} else {
					roles.add(index - 1, item);
				}
			}
	    }
		return item;
	}
	
	private static UserRole get(RoleType type, String name) {
		UserRole item = new UserRole(type, name, null, null);
		synchronized (roles) {
			int i = Collections.binarySearch(roles, item);
			return (i < 0 ? null : roles.get(i));
		}
	}
	
	private static Set<String> getAndRemoveSet(Document document, String key) {
    	Object o = document.get(key);
    	if (o == null || !(o instanceof ArrayList)) {
    		return null;
    	}
    	
    	@SuppressWarnings("unchecked")
		List<String> list = (List<String>) o;
    	if (list.size() == 0) {
    		return null;
    	}
    	
    	Set<String> set = new HashSet<>();
    	for (String value : list) {
			set.add(value);
		}
		document.remove(key);
		return set;
    }
	
	@Override
	public String toString() {
		return MoreObjects.toStringHelper(roles)
				.addValue(roles)
				.toString();
	}
}
