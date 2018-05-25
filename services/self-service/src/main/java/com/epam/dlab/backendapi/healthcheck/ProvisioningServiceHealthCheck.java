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
