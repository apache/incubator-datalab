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


package com.epam.dlab.backendapi.domain;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.conf.SelfServiceApplicationConfiguration;
import com.epam.dlab.backendapi.dao.EnvDAO;
import com.epam.dlab.backendapi.util.RequestBuilder;
import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.dto.UserEnvironmentResources;
import com.epam.dlab.dto.status.EnvResourceList;
import com.epam.dlab.rest.client.RESTService;
import com.epam.dlab.rest.contracts.InfrasctructureAPI;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalNotification;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.dropwizard.lifecycle.Managed;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Send requests to the docker to check environment status.
 */
@Singleton
@Slf4j
public class EnvStatusListener implements Managed {

	private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

	private final Cache<String, UserInfo> sessions;
	private final EnvDAO dao;
	private final RESTService provisioningService;
	private final StatusChecker statusChecker = new StatusChecker();
	private final long checkEnvStatusTimeout;
	private final RequestBuilder requestBuilder;

	@Inject
	private RequestId requestId;

	@Inject
	public EnvStatusListener(SelfServiceApplicationConfiguration configuration, EnvDAO dao,
							 @Named(ServiceConsts.PROVISIONING_SERVICE_NAME) RESTService provisioningService,
							 RequestBuilder requestBuilder) {

		this.sessions = CacheBuilder.newBuilder()
				.expireAfterAccess(configuration.getInactiveUserTimeoutMillSec(), TimeUnit.MILLISECONDS)
				.removalListener((RemovalNotification<String, Object> notification) ->
						log.info("User {} session is removed", notification.getKey()))
				.build();

		this.dao = dao;
		this.provisioningService = provisioningService;
		this.checkEnvStatusTimeout = configuration.getCheckEnvStatusTimeout().toMilliseconds();
		this.requestBuilder = requestBuilder;
	}

	@Override
	public void start() {
		executorService.scheduleAtFixedRate(new StatusChecker(), checkEnvStatusTimeout, checkEnvStatusTimeout,
				TimeUnit.MILLISECONDS);
	}

	@Override
	public void stop() throws Exception {
		statusChecker.shouldStop = true;
		if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
			executorService.shutdownNow();
		}
	}

	public void registerSession(UserInfo userInfo) {
		UserInfo ui = getSession(userInfo.getName());
		log.info("Register session(existing = {}) for {}", ui != null, userInfo.getName());
		sessions.put(userInfo.getName(), userInfo);
	}

	public void unregisterSession(UserInfo userInfo) {
		log.info("Invalidate session for {}", userInfo.getName());
		sessions.invalidate(userInfo.getName());
	}

	public UserInfo getSession(String username) {
		return sessions.getIfPresent(username);
	}

	/**
	 * Scheduled @{@link Runnable} that verifies status of users' resources
	 */
	private class StatusChecker implements Runnable {
		private volatile boolean shouldStop = false;

		@Override
		public void run() {

			log.debug("Start checking environment statuses");

			sessions.cleanUp();

			for (Map.Entry<String, UserInfo> entry : sessions.asMap().entrySet()) {
				try {
					if (!shouldStop) {
						checkStatusThroughProvisioningService(entry.getValue());
					} else {
						log.info("Stopping env status listener");
					}
				} catch (RuntimeException e) {
					log.error("Cannot check env status for user {}", entry.getKey(), e);
				}
			}
		}

		/**
		 * Sends request to docker to check the status of user environment.
		 *
		 * @param userInfo username
		 * @return UUID associated with async operation
		 */
		private String checkStatusThroughProvisioningService(UserInfo userInfo) {

			String uuid = null;
			EnvResourceList resourceList = dao.findEnvResources(userInfo.getName());
			UserEnvironmentResources dto = requestBuilder.newUserEnvironmentStatus(userInfo);

			log.trace("EnvStatus listener check status for user {} with resource list {}", userInfo.getName(),
					resourceList);

			if (resourceList.getHostList() != null || resourceList.getClusterList() != null) {
				dto.withResourceList(resourceList);
				log.trace("Ask docker for the status of resources for user {}: {}", userInfo.getName(), dto);
				uuid = provisioningService.post(InfrasctructureAPI.INFRASTRUCTURE_STATUS, userInfo.getAccessToken(),
						dto, String.class);
				requestId.put(userInfo.getName(), uuid);
			}

			return uuid;
		}
	}
}
