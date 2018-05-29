package com.epam.dlab.auth.azure.service;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.auth.UserInfoDAO;
import com.epam.dlab.auth.azure.AuthorizationSupplier;
import com.epam.dlab.auth.azure.AzureLocalAuthResponse;
import com.epam.dlab.auth.azure.RoleAssignment;
import com.epam.dlab.auth.azure.RoleAssignmentResponse;
import com.epam.dlab.exceptions.DlabAuthenticationException;
import com.epam.dlab.exceptions.DlabException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static com.epam.dlab.auth.rest.AbstractAuthenticationService.getRandomToken;

@Slf4j
public class AzureAuthorizationCodeServiceImpl implements AzureAuthorizationCodeService {
	@Inject
	private UserInfoDAO userInfoDao;

	private final String authority;
	private String clientId;
	private String tenantId;
	private String clientSecret;
	private final String permissionScope;
	private final boolean validatePermissionScope;

	public AzureAuthorizationCodeServiceImpl(String authority, String permissionScope, String managementApiAuthFile,
											 boolean validatePermissionScope) throws IOException {
		this.authority = authority;
		this.permissionScope = permissionScope;
		this.validatePermissionScope = validatePermissionScope;

		if (validatePermissionScope) {
			Map<String, String> authenticationParameters = new ObjectMapper()
					.readValue(new File(managementApiAuthFile),
							new TypeReference<HashMap<String, String>>() {
							});

			this.clientId = authenticationParameters.get("clientId");
			this.tenantId = authenticationParameters.get("tenantId");
			this.clientSecret = authenticationParameters.get("clientSecret");

			if (clientId == null || tenantId == null || clientSecret == null) {
				throw new DlabException("Authentication information not configured to use Management API");
			}
		}
	}

	@Override
	public AzureLocalAuthResponse authenticateAndLogin(AuthorizationSupplier authorizationSupplier) {
		AuthenticationResult authenticationResult = authenticate(authorizationSupplier,
				AzureEnvironment.AZURE.dataLakeEndpointResourceId());

		if (validatePermissions(authenticationResult)) {

			UserInfo userInfo = prepareUserInfo(authenticationResult);
			userInfoDao.saveUserInfo(userInfo);

			return new AzureLocalAuthResponse(userInfo.getAccessToken(),
					userInfo.getName(), null);
		}
		throw new DlabAuthenticationException("You do not have proper permissions to use DLab. Please contact your " +
				"administrator");
	}

	@Override
	public boolean validatePermissions(AuthenticationResult authenticationResult) {
		if (!validatePermissionScope) {
			log.info("Verification of user permissions is disabled");
			return true;
		}

		Client client = null;
		RoleAssignmentResponse roleAssignmentResponse;
		try {
			client = ClientBuilder.newClient();

			roleAssignmentResponse = client
					.target(AzureEnvironment.AZURE.resourceManagerEndpoint()
							+ permissionScope + "roleAssignments")
					.queryParam("api-version", "2015-07-01")
					.queryParam("$filter", String.format("assignedTo('%s')",
							authenticationResult.getUserInfo().getUniqueId()))
					.request(MediaType.APPLICATION_JSON_TYPE)
					.header("Authorization", String.format("Bearer %s", getManagementApiToken()))
					.get(RoleAssignmentResponse.class);

		} catch (ClientErrorException e) {
			log.error("Cannot validate permissions due to {}", (e.getResponse() != null && e.getResponse().hasEntity())
					? e.getResponse().readEntity(String.class) : "");
			log.error("Error during permission validation", e);
			throw e;
		} catch (RuntimeException e) {
			log.error("Cannot validate permissions due to", e);
			throw e;
		} finally {
			if (client != null) {
				client.close();
			}
		}

		return checkRoles(roleAssignmentResponse, authenticationResult);
	}

	private String getManagementApiToken() {
		try {

			log.info("Requesting authentication token ... ");

			ApplicationTokenCredentials applicationTokenCredentials = new ApplicationTokenCredentials(
					clientId, tenantId, clientSecret,
					AzureEnvironment.AZURE);

			return applicationTokenCredentials.getToken(AzureEnvironment.AZURE.resourceManagerEndpoint());
		} catch (IOException e) {
			log.error("Cannot retrieve authentication token due to", e);
			throw new DlabException("Cannot retrieve authentication token", e);
		}
	}


	private AuthenticationResult authenticate(AuthorizationSupplier authorizationSupplier, String resource) {
		AuthenticationResult result;
		ExecutorService executorService = Executors.newFixedThreadPool(1);

		try {

			AuthenticationContext context = new AuthenticationContext(authority, true, executorService);
			Future<AuthenticationResult> future = authorizationSupplier.get(context, resource);

			result = future.get();

		} catch (MalformedURLException | InterruptedException e) {
			log.error("Authentication to {} is failed", resource, e);
			throw new DlabException(String.format("Cannot get token to %s", resource), e);

		} catch (ExecutionException e) {
			if (e.getCause() instanceof RuntimeException) {
				throw (RuntimeException) e.getCause();
			}

			throw new DlabException(String.format("Cannot get token to %s", resource), e);

		} finally {
			executorService.shutdown();
		}

		if (result == null) {
			throw new DlabException("Authentication result was null");
		}

		return result;
	}

	private UserInfo prepareUserInfo(AuthenticationResult authenticationResult) {
		com.microsoft.aad.adal4j.UserInfo ui = authenticationResult.getUserInfo();
		log.info("Extracted user info display id {}, {} {}", ui.getDisplayableId(), ui.getGivenName(),
				ui.getFamilyName());

		if (ui.getDisplayableId() != null && !ui.getDisplayableId().isEmpty()) {
			UserInfo userInfo = new UserInfo(ui.getDisplayableId(), getRandomToken());
			userInfo.setFirstName(ui.getGivenName());
			userInfo.setLastName(ui.getFamilyName());
			userInfo.getKeys().put("refresh_token", authenticationResult.getRefreshToken());
			userInfo.getKeys().put("created_date_of_refresh_token", Long.toString(System.currentTimeMillis()));
			return userInfo;
		}

		throw new DlabException("Cannot verify user identity");
	}

	private boolean checkRoles(RoleAssignmentResponse response, AuthenticationResult authenticationResult) {

		List<RoleAssignment> roles = (response != null) ? response.getValue() : null;
		if (roles != null && !roles.isEmpty()) {
			log.info("User {} has {} roles in configured scope for security",
					authenticationResult.getUserInfo().getDisplayableId(), roles.size());

			log.debug("User's({}) roles are {}", authenticationResult.getUserInfo().getDisplayableId(),
					roles.stream().map(RoleAssignment::getName).collect(Collectors.toList()));
			return true;

		} else {
			log.info("User {} does not have any roles in configured scope for security",
					authenticationResult.getUserInfo().getDisplayableId());

			throw new DlabException("User does not have any roles in pre-configured security scope for DLab");
		}
	}
}
