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

package com.epam.dlab.backendapi.healthcheck;

import com.codahale.metrics.health.HealthCheck;
import com.epam.dlab.auth.SystemUserInfoService;
import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.rest.client.RESTService;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import javax.ws.rs.core.Response;

public class ProvisioningServiceHealthCheck extends HealthCheck {

	public static final String USER = "healthChecker";
	public static final String INFRASTRUCTURE_URL = "/infrastructure";
	@Inject
	@Named(ServiceConsts.PROVISIONING_SERVICE_NAME)
	private RESTService provisioningService;

	@Inject
	private SystemUserInfoService systemUserInfoService;

	@Override
	protected Result check() {
		final String accessToken = systemUserInfoService.create(USER).getAccessToken();
		final Response response = provisioningService.get(INFRASTRUCTURE_URL, accessToken, Response.class);
		return isSuccess(response) ? Result.healthy() : Result.unhealthy(response.getStatusInfo()
				.getReasonPhrase());
	}

	private boolean isSuccess(Response response) {
		return response.getStatus() == 200;
	}
}
