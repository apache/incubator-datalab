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

package com.epam.dlab.auth.aws.service;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.epam.dlab.auth.aws.dao.AwsUserDAO;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import io.dropwizard.lifecycle.Managed;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Singleton
@Slf4j
public class AwsCredentialRefreshService implements Managed {

	private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor();
	private final AwsUserDAO awsUserDAO;
	private final AWSCredentialsProvider credentialsProvider;

	@Inject
	public AwsCredentialRefreshService(AwsUserDAO awsUserDAO, AWSCredentialsProvider credentialsProvider) {
		this.awsUserDAO = awsUserDAO;
		this.credentialsProvider = credentialsProvider;
	}

	@Override
	public void start() {
		executor.scheduleAtFixedRate(() -> refresh(credentialsProvider), 5, 5,
				TimeUnit.MINUTES);
	}

	@Override
	public void stop() {
		executor.shutdown();
	}

	private void refresh(AWSCredentialsProvider credentialsProvider) {
		try {
			credentialsProvider.refresh();
			this.awsUserDAO.updateCredentials(credentialsProvider.getCredentials());
			log.debug("provider credentials refreshed");
		} catch (Exception e) {
			log.error("AWS provider error", e);
			throw e;
		}
	}
}
