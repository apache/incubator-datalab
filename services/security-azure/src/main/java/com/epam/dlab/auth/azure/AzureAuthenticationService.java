/*
 * Copyright (c) 2017, EPAM SYSTEMS INC
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

package com.epam.dlab.auth.azure;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.auth.UserInfoDAO;
import com.epam.dlab.auth.rest.AbstractAuthenticationService;
import com.epam.dlab.config.azure.AzureLoginConfiguration;
import com.epam.dlab.dto.UserCredentialDTO;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.contracts.SecurityAPI;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationException;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import io.dropwizard.Configuration;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
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

@Path("/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AzureAuthenticationService<C extends Configuration> extends AbstractAuthenticationService<C>
        implements AzureAuthorizationCodeService {

    private final UserInfoDAO userInfoDao;
    private final String authority;
    private final AzureLoginConfiguration azureLoginConfiguration;
    private String clientId;
    private String tenantId;
    private String clientSecret;

    public AzureAuthenticationService(C config, UserInfoDAO userInfoDao,
                                      AzureLoginConfiguration azureLoginConfiguration) throws IOException {
        super(config);
        this.userInfoDao = userInfoDao;
        this.authority = azureLoginConfiguration.getAuthority() + azureLoginConfiguration.getTenant() + "/";
        this.azureLoginConfiguration = azureLoginConfiguration;

        if (azureLoginConfiguration.isValidatePermissionScope()) {
            Map<String, String> authenticationParameters = new ObjectMapper()
                    .readValue(new File(azureLoginConfiguration.getManagementApiAuthFile()),
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

    /**
     * Authenticates user by given <code>credential</code>
     *
     * @param credential contains username and password
     * @param request    http request
     * @return authentication result in {@link Response}
     */
    @Path(SecurityAPI.LOGIN)
    @POST
    public Response login(UserCredentialDTO credential, @Context HttpServletRequest request) {

        log.info("Basic authentication {}", credential);

        try {
            return authenticateAndLogin(new UsernamePasswordSupplier(azureLoginConfiguration, credential));
        } catch (AuthenticationException e) {
            log.error("Basic authentication failed", e);
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new AzureLocalAuthResponse(null, null,
                            "User authentication failed")).build();
        }
    }

    /**
     * Returns user info that is mapped with <code>accessToken</code>
     *
     * @param accessToken input access token
     * @param request     http request
     * @return user info
     */
    @Override
    @Path(SecurityAPI.GET_USER_INFO)
    @POST
    public UserInfo getUserInfo(String accessToken, @Context HttpServletRequest request) {
        String remoteIp = request.getRemoteAddr();

        UserInfo ui = userInfoDao.getUserInfoByAccessToken(accessToken);

        if (ui != null) {
            ui = ui.withToken(accessToken);
            userInfoDao.updateUserInfoTTL(accessToken, ui);
            log.debug("restored UserInfo from DB {}", ui);
        }

        log.debug("Authorized {} {} {}", accessToken, ui, remoteIp);
        return ui;
    }

    /**
     * Logs out user by input <code>accessToken</code>
     *
     * @param accessToken input access yoken
     * @return result of the operation
     */
    @Override
    @Path(SecurityAPI.LOGOUT)
    @POST
    public Response logout(String accessToken) {
        userInfoDao.deleteUserInfo(accessToken);
        log.info("Logged out user {}", accessToken);
        return Response.ok().build();
    }

    /**
     * Using OAuth2 authorization code grant approach authenticates user by given authorization code in <code>response</code>
     *
     * @param response contains username and passwrd
     * @return authentication result in {@link Response}
     */
    @Path(SecurityAPI.LOGIN_OAUTH)
    @POST
    public Response authenticateOAuth(AuthorizationCodeFlowResponse response) {

        log.info("Try to login using authorization code {}", response);

        try {
            return authenticateAndLogin(new AuthorizationCodeSupplier(azureLoginConfiguration, response));
        } catch (AuthenticationException e) {
            log.error("OAuth authentication failed", e);
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new AzureLocalAuthResponse(null, null,
                            "User authentication failed")).build();
        }
    }

    @Override
    public Response authenticateAndLogin(AuthorizationSupplier authorizationSupplier) {

        AuthenticationResult authenticationResult = authenticate(authorizationSupplier,
                AzureEnvironment.AZURE.dataLakeEndpointResourceId());

        if (validatePermissions(authenticationResult)) {

            UserInfo userInfo = prepareUserInfo(authenticationResult);
            userInfoDao.saveUserInfo(userInfo);

            AzureLocalAuthResponse localAuthResponse = new AzureLocalAuthResponse(userInfo.getAccessToken(),
                    userInfo.getName(), null);

            return Response.ok(localAuthResponse).build();
        }

        return Response.status(Response.Status.UNAUTHORIZED)
                .entity("You do not have proper permissions to use DLab. Please contact your administrator")
                .build();

    }

    @Override
    public boolean validatePermissions(AuthenticationResult authenticationResult) {

        if (!azureLoginConfiguration.isValidatePermissionScope()) {
            log.info("Verification of user permissions is disabled");

            return true;
        }

        Client client = null;
        RoleAssignmentResponse roleAssignmentResponse;
        try {
            client = ClientBuilder.newClient();

            roleAssignmentResponse = client
                    .target(AzureEnvironment.AZURE.resourceManagerEndpoint()
                            + azureLoginConfiguration.getPermissionScope() + "roleAssignments")
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
}
