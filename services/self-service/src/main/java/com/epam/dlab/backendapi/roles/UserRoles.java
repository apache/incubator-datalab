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

package com.epam.dlab.backendapi.roles;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.SecurityDAO;
import com.epam.dlab.exceptions.DlabException;
import com.google.common.base.MoreObjects;
import com.mongodb.client.FindIterable;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

/**
 * Provides user roles access to features.
 */
public class UserRoles {
	private static final Logger LOGGER = LoggerFactory.getLogger(UserRoles.class);

	private static final String ANY_USER = "$anyuser";
	/**
	 * Node name of groups.
	 */
	private static final String GROUPS = "groups";
	/**
	 * Node name of user.
	 */
	private static final String USERS = "users";
	private static final String ADMIN_ROLE_NAME = "admin";
	/**
	 * Single instance of the user roles.
	 */
	private static UserRoles userRoles = null;
	/**
	 * List of roles.
	 */
	private List<UserRole> roles = null;
	private Map<String, Set<String>> userGroups;

	/**
	 * Default access to features if the role is not defined.
	 */
	private boolean defaultAccess = false;

	/**
	 * Initialize user roles for all users.
	 *
	 * @param dao security DAO.
	 */
	public static void initialize(SecurityDAO dao, boolean defaultAccess) {
		LOGGER.trace("Loading roles from database...");
		if (userRoles == null) {
			userRoles = new UserRoles();
		}
		userRoles.load(dao, defaultAccess);
		LOGGER.trace("New roles are	: {}", getRoles());
	}

	/**
	 * Return the list of roles for all users.
	 */
	public static List<UserRole> getRoles() {
		return (userRoles == null ? null : userRoles.roles());
	}

	/**
	 * Check access for user to the role.
	 *
	 * @param userInfo user info.
	 * @param type     the type of role.
	 * @param name     the name of role.
	 * @param roles
	 * @return boolean value
	 */
	public static boolean checkAccess(UserInfo userInfo, RoleType type, String name, Collection<String> roles) {
		return checkAccess(userInfo, type, name, true, roles);
	}

	public static boolean isAdmin(UserInfo userInfo) {
		final List<UserRole> roles = UserRoles.getRoles();
		return roles == null || roles.stream().anyMatch(r -> ADMIN_ROLE_NAME.equals(r.getId()) &&
				(userRoles.hasAccessByGroup(userInfo, r, userInfo.getRoles()) || userRoles.hasAccessByUserName(userInfo, r)));
	}

	/**
	 * Check access for user to the role.
	 *
	 * @param roles
	 * @param userInfo user info.
	 * @param type     the type of role.
	 * @param name     the name of role.
	 * @return boolean value
	 */
	public static boolean checkAccess(UserInfo userInfo, RoleType type, String name, boolean useDefault,
									  Collection<String> roles) {
		return (userRoles == null || userRoles.hasAccess(userInfo, type, name, useDefault, roles));
	}

	/**
	 * Loading the user roles for all users from Mongo database.
	 *
	 * @param dao security DAO.
	 */
	private synchronized void load(SecurityDAO dao, boolean defaultAccess) {
		this.defaultAccess = defaultAccess;
		try {
			FindIterable<Document> docs = dao.getRoles();
			roles = new ArrayList<>();
			for (Document d : docs) {
				Set<String> groups = getAndRemoveSet(d, GROUPS);
				Set<String> users = getAndRemoveSet(d, USERS);
				String id = d.getString("_id");
				for (RoleType type : RoleType.values()) {
					@SuppressWarnings("unchecked")
					List<String> names = d.get(type.getNodeName(), ArrayList.class);
					if (names != null) {
						for (String name : names) {
							append(type, name, groups, users, id);
						}
					}
				}
			}
			userGroups = dao.getGroups();
		} catch (Exception e) {
			throw new DlabException("Cannot load roles from database. " + e.getLocalizedMessage(), e);
		}
	}

	private synchronized List<UserRole> roles() {
		return roles;
	}

	/**
	 * Append new role to the list if role not exists in list an return it, otherwise return
	 * existence role.
	 *
	 * @param type   type of role.
	 * @param name   the name of role.
	 * @param groups the names of external groups.
	 * @param users  the name of DLab's users.
	 * @param id
	 * @return role.
	 */
	private UserRole append(RoleType type, String name, Set<String> groups, Set<String> users, String id) {
		UserRole item = new UserRole(id, type, name, groups, users);
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

	/**
	 * Find and return role by type and name.
	 *
	 * @param type type of role.
	 * @param name the name of role.
	 */
	private UserRole get(RoleType type, String name) {
		UserRole item = new UserRole(type, name, null, null);
		synchronized (roles) {
			int i = Collections.binarySearch(roles, item);
			return (i < 0 ? null : roles.get(i));
		}
	}

	/**
	 * Find and return a list by key from JSON document, otherwise return <b>null</b>.
	 *
	 * @param document the document.
	 * @param key      the name of node.
	 */
	private Set<String> getAndRemoveSet(Document document, String key) {
		Object o = document.get(key);
		if (!(o instanceof ArrayList)) {
			return Collections.emptySet();
		}

		@SuppressWarnings("unchecked")
		List<String> list = (List<String>) o;
		if (list.isEmpty()) {
			return Collections.emptySet();
		}

		Set<String> set = new HashSet<>();
		for (String value : list) {
			set.add(value.toLowerCase());
		}
		document.remove(key);
		return set;
	}

	/**
	 * Check access for user to the role.
	 *
	 * @param userInfo   user info.
	 * @param type       the type of role.
	 * @param name       the name of role.
	 * @param useDefault true/false
	 * @param roles
	 * @return boolean value
	 */
	private boolean hasAccess(UserInfo userInfo, RoleType type, String name, boolean useDefault,
							  Collection<String> roles) {
		if (userRoles == null) {
			return true;
		}
		LOGGER.trace("Check access for user {} with groups {} to {}/{}", userInfo.getName(), userInfo.getRoles(),
				type, name);
		UserRole role = get(type, name);
		if (role == null) {
			return checkDefault(useDefault);
		}
		if (hasAccessByGroup(userInfo, role, roles)) return true;
		LOGGER.trace("Access denied for user {} to {}/{}", userInfo.getName(), type, name);
		return false;
	}

	private boolean hasAccessByGroup(UserInfo userInfo, UserRole role, Collection<String> userRoles) {
		Set<String> groups = role.getGroups();
		if (groups != null) {
			if (groups.contains(ANY_USER)) {
				return true;
			}
			for (String group : userRoles) {
				if (group != null && groups.contains(group.toLowerCase())) {
					LOGGER.trace("Got access by group {}", group);
					return true;
				}
			}

			final Optional<String> group = role.getGroups()
					.stream()
					.filter(g -> userGroups.getOrDefault(g, Collections.emptySet()).contains(userInfo.getName()))
					.findAny();
			if (group.isPresent()) {
				LOGGER.trace("Got access by local group {}", group.get());
				return true;
			}
		}
		return false;
	}

	private boolean hasAccessByUserName(UserInfo userInfo, UserRole role) {
		if (role.getUsers() != null &&
				userInfo.getName() != null &&
				(role.getUsers().contains(ANY_USER) ||
						role.getUsers().contains(userInfo.getName().toLowerCase()))) {
			LOGGER.trace("Got access by name");
			return true;
		}
		return false;
	}

	private boolean checkDefault(boolean useDefault) {
		if (useDefault) {
			LOGGER.trace("Got default access {}", defaultAccess);
			return defaultAccess;
		} else {
			return false;
		}
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(roles)
				.addValue(roles)
				.toString();
	}


}
