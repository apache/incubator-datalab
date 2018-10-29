package com.epam.dlab.backendapi.dao;

import java.util.Set;

public interface UserGroupDao {
	void addUsers(String group, Set<String> users);

	void removeUser(String group, String user);

	void removeGroup(String groupId);

}
