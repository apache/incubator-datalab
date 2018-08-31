/*
 *
 *  * Copyright (c) 2018, EPAM SYSTEMS INC
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */
package com.epam.dlab.backendapi.dao;

import com.epam.dlab.backendapi.resources.dto.UserRoleDto;
import org.bson.Document;

import java.util.Date;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;

public class UserRoleDao extends BaseDAO {

	public List<UserRoleDto> getUserRoles() {
		return find(MongoCollections.ROLES, UserRoleDto.class);
	}

	public void createRole(UserRoleDto dto) {
		insertOne(MongoCollections.ROLES, dto, dto.getId());
	}

	public void updateRole(UserRoleDto dto) {
		final Document userRoleDocument = convertToBson(dto).append(TIMESTAMP, new Date());
		updateOne(MongoCollections.ROLES,
				eq(ID, dto.getId()),
				new Document(SET, userRoleDocument));
	}

	public void updateRoleField(String field, Object obj, String roleId){
		final Document userRoleDocument = new Document(field, obj).append(TIMESTAMP, new Date());
		updateOne(MongoCollections.ROLES,
				eq(ID, roleId),
				new Document(SET, userRoleDocument));
	}

	public void removeRoleById(String roleId) {
		deleteOne(MongoCollections.ROLES, eq(ID, roleId));
	}

}
