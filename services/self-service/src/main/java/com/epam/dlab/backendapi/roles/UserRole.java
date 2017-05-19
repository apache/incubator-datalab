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

import java.util.Set;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;

/** Describe role.
 */
public class UserRole implements Comparable<UserRole> {

	/** Type of role. */
	private final RoleType type;
	
	/** Name of role. */
	private final String name;
	
	/** Names of external groups. */
	private final Set<String> groups;
	
	/** Name of DLab's users. */
	private final Set<String> users;
	
	/** Instantiate the role.
	 * @param type type of role.
	 * @param name the name of role.
	 * @param groups the names of external groups.
	 * @param users the name of DLab's users.
	 */
	public UserRole(RoleType type, String name, Set<String> groups, Set<String> users) {
		this.type = type;
		this.name = name;
		this.groups = groups;
		this.users = users;
	}
	
	/** Return the type of role.
	 */
	public RoleType getType() {
		return type;
	}

	/** Return the name of role.
	 */
	public String getName() {
		return name;
	}
	
	/** Return the names of external groups.
	 */
	public Set<String> getGroups() {
		return groups;
	}

	/** Return the name of DLab's users.
	 */
	public Set<String> getUsers() {
		return users;
	}
	
	@Override
	public int compareTo(UserRole o) {
		int result = type.compareTo(o.type);
		return (result == 0 ? name.compareTo(o.name) : result);
	}
	
    public ToStringHelper toStringHelper(Object self) {
    	return MoreObjects.toStringHelper(self)
    	        .add("type", type)
    	        .add("name", name)
    	        .add("groups", groups)
    	        .add("users", users);
    }
    
    @Override
    public String toString() {
    	return toStringHelper(this).toString();
    }
}
