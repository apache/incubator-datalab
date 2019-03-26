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

package com.epam.dlab.auth.aws.dao;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.identitymanagement.model.*;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;

@Singleton
@Slf4j
public class AwsUserDAOImpl implements AwsUserDAO {

	private final String endpoint;
	private final String region;
	private AmazonIdentityManagement aim;

	@Inject
	public AwsUserDAOImpl(AWSCredentials credentials,
						  @Nullable @Named("iamEndpoint") String endpoint,
						  @Nullable @Named("iamRegion") String region) {
		this.endpoint = endpoint;
		this.region = region;
		this.aim = getClient(credentials);
	}

	@Override
	public User getAwsUser(String username) {
		User u = fetchAwsUser(username);
		log.debug("Fetched AWS user {}", u);
		return u;
	}

	@Override
	public void updateCredentials(AWSCredentials credentials) {
		this.aim = getClient(credentials);
	}

	@Override
	public List<AccessKeyMetadata> getAwsAccessKeys(String username) {
		List<AccessKeyMetadata> data = null;
		try {
			ListAccessKeysRequest request = new ListAccessKeysRequest().withUserName(username);
			ListAccessKeysResult result = aim.listAccessKeys(request);
			data = result.getAccessKeyMetadata();
		} catch (Exception e) {
			log.error("AccessKeyMetadata for {} request failed: {}", username, e.getMessage());
		}
		return data;
	}

	private User fetchAwsUser(String username) {
		User user = null;
		try {
			GetUserRequest r = new GetUserRequest().withUserName(username);
			GetUserResult ur = aim.getUser(r);
			user = ur.getUser();
		} catch (NoSuchEntityException e) {
			log.error("User {} not found: {}", username, e.getMessage());
		}
		return user;
	}

	private AmazonIdentityManagementClient getClient(AWSCredentials credentials) {
		final AmazonIdentityManagementClient client =
				new AmazonIdentityManagementClient(credentials);
		Optional.ofNullable(region)
				.ifPresent(r -> client.setRegion(Region.getRegion(Regions.fromName(r))));
		Optional.ofNullable(endpoint)
				.ifPresent(client::setEndpoint);
		return client;
	}
}
