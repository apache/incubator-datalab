/*
 * Copyright (c) 2018, EPAM SYSTEMS INC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.dlab.auth;

import com.epam.dlab.mongo.MongoService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.mongodb.client.MongoCollection;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;

import java.util.Optional;
import java.util.UUID;

import static com.mongodb.client.model.Filters.eq;


@Slf4j
@Singleton
public class SystemUserInfoServiceImpl implements SystemUserInfoService {

	private static final String USER_NAME_FIELD = "name";
	private static final String SYSTEM_USERS_COLLECTION = "systemUsers";
	private static final String ID_FIELD = "_id";
	@Inject
	private MongoService mongoService;

	@Override
	public Optional<UserInfo> getUser(String token) {
		final Document document = systemUserCollection().findOneAndDelete(eq(ID_FIELD,
				token));
		return document != null ? Optional.of(toUserInfo(document)) : Optional.empty();

	}

	@Override
	public UserInfo create(String name) {
		log.debug("Creating new system user with name {}", name);
		final String token = UUID.randomUUID().toString();
		systemUserCollection().insertOne(toSystemUserDocument(token, name));
		return new UserInfo(name, token);
	}


	private UserInfo toUserInfo(Document document) {
		return new UserInfo(document.getString(USER_NAME_FIELD), document.getString(ID_FIELD));
	}

	private Document toSystemUserDocument(String token, String name) {
		return new Document(ID_FIELD, token).append
				(USER_NAME_FIELD, name);
	}

	private MongoCollection<Document> systemUserCollection() {
		return mongoService.getCollection(SYSTEM_USERS_COLLECTION);
	}
}
