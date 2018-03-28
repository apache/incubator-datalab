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

/**
 * Types of roles.
 */
public enum RoleType {
	COMPUTATIONAL("computationals"),
	EXPLORATORY("exploratories"),
	EXPLORATORY_SHAPES("exploratory_shapes"),
	PAGE("pages");

	private String nodeName;

	RoleType(String nodeName) {
		this.nodeName = nodeName;
	}

	public static RoleType of(String name) {
		if (name != null) {
			for (RoleType value : RoleType.values()) {
				if (name.equalsIgnoreCase(value.toString())) {
					return value;
				}
			}
		}
		return null;
	}

	/**
	 * Return name of node in JSON for type.
	 */
	public String getNodeName() {
		return nodeName;
	}
}
