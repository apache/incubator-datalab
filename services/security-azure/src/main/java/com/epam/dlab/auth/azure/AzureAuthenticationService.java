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
import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.azure.AzureEnvironment;
import io.dropwizard.Configuration;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
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

    public AzureAuthenticationService(C config, UserInfoDAO userInfoDao,
                                      AzureLoginConfiguration azureLoginConfiguration) {
        super(config);
        this.userInfoDao = userInfoDao;
        this.authority = azureLoginConfiguration.getAuthority() + azureLoginConfiguration.getTenant() + "/";
        this.azureLoginConfiguration = azureLoginConfiguration;
    }

    @Path(SecurityAPI.LOGIN)
    @POST
    public Response login(UserCredentialDTO credential, @Context HttpServletRequest request) {

        log.info("Basic authentication {}", credential);

        return authenticateAndLogin(new UsernamePasswordSupplier(azureLoginConfiguration, credential));
    }

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

    @Override
    @Path(SecurityAPI.LOGOUT)
    @POST
    public Response logout(String accessToken) {
        userInfoDao.deleteUserInfo(accessToken);
        log.info("Logged out user {}", accessToken);
        return Response.ok().build();
    }

    @Path(SecurityAPI.LOGIN_OAUTH)
    @POST
    public Response authenticateOAuth(AuthorizationCodeFlowResponse response) {

        log.info("Try to login using authorization code {}", response);

        return authenticateAndLogin(new AuthorizationCodeSupplier(azureLoginConfiguration, response));
    }

    @Override
    public Response authenticateAndLogin(AuthorizationSupplier authorizationSupplier) {

        if (validatePermissions(authorizationSupplier)) {
            AuthenticationResult authenticationResult = authenticate(authorizationSupplier,
                    AzureEnvironment.AZURE.dataLakeEndpointResourceId());

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
    public boolean validatePermissions(AuthorizationSupplier authorizationSupplier) {

        if (!azureLoginConfiguration.isValidatePermissionScope()) {
            log.info("Verification of user permissions is disabled");

            return true;
        }

        Client client = null;
        RoleAssignmentResponse roleAssignmentResponse;
        AuthenticationResult authenticationResult;
        try {
            client = ClientBuilder.newClient();

            authenticationResult = authenticate(authorizationSupplier,
                    AzureEnvironment.AZURE.resourceManagerEndpoint());

            roleAssignmentResponse = client
                    .target(AzureEnvironment.AZURE.resourceManagerEndpoint()
                            + azureLoginConfiguration.getPermissionScope() + "roleAssignments")
                    .queryParam("api-version", "2015-07-01")
                    .queryParam("$filter", String.format("assignedTo('%s')",
                            authenticationResult.getUserInfo().getUniqueId()))
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .header("Authorization", String.format("Bearer %s", authenticationResult.getUserInfo()))
                    .get(RoleAssignmentResponse.class);

        } catch (ClientErrorException e) {
            log.error("Cannot get rate card due to {}", (e.getResponse() != null && e.getResponse().hasEntity())
                    ? e.getResponse().readEntity(String.class) : "");
            log.error("Error during using RateCard API", e);
            throw e;
        } catch (RuntimeException e) {
            log.error("Cannot retrieve rate card due to ", e);
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

        } catch (Exception e) {
            log.error("Authentication to {} is failed", resource, e);
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
            return userInfo;
        }

        throw new DlabException("Cannot verify user identity");
    }
}
