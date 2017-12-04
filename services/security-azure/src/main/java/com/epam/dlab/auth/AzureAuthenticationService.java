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

package com.epam.dlab.auth;

import com.epam.dlab.auth.rest.AbstractAuthenticationService;
import com.epam.dlab.config.azure.AzureLoginConfiguration;
import com.epam.dlab.dto.UserCredentialDTO;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.contracts.SecurityAPI;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.aad.adal4j.AuthenticationContext;
import com.microsoft.aad.adal4j.AuthenticationResult;
import io.dropwizard.Configuration;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URI;
import java.util.Base64;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Path("/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AzureAuthenticationService<C extends Configuration> extends AbstractAuthenticationService<C>
        implements AzureAuthorizationCodeService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final UserInfoDAO userInfoDao;
    private final String authority;
    private final String tenant;
    private final String redirectUrl;
    private final String clientId;

    public AzureAuthenticationService(C config, UserInfoDAO userInfoDao,
                                      AzureLoginConfiguration azureLoginConfiguration) {
        super(config);
        this.userInfoDao = userInfoDao;
        this.clientId = azureLoginConfiguration.getClientId();
        this.tenant = azureLoginConfiguration.getTenant();
        this.authority = azureLoginConfiguration.getAuthority() + tenant + "/";
        this.redirectUrl = azureLoginConfiguration.getRedirectUrl();
    }

    @Path(SecurityAPI.LOGIN)
    @POST
    @Override
    public Response login(UserCredentialDTO credential, @Context HttpServletRequest request) {
        return authenticateAndLogin(credential);
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
    @Override
    public Response authenticateAndLogin(AuthorizationCodeFlowResponse response) {

        log.info("Try to login {}", response);

        if (validatePermissions(response)) {
            AuthenticationResult authenticationResult = authenticate(new AuthorizationCodeSupplier(this, response),
                    "https://datalake.azure.net/");

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
    public Response authenticateAndLogin(UserCredentialDTO credentialDTO) {
        log.info("Try to login {}", credentialDTO);

        if (validatePermissions(credentialDTO)) {


            AuthenticationResult authenticationResult = authenticate(new UsernamePasswordSupplier(this, credentialDTO),
                    "https://datalake.azure.net/");

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
    public boolean validatePermissions(AuthorizationCodeFlowResponse response) {

        /* TODO To be implemented

        try {

            String managementToken = authenticate(response, "https://management.azure.com/").getAccessToken();
            String graphToken = authenticate(response, "https://graph.windows.net/").getAccessToken();

            log.info("Management token {}", managementToken);
            log.info("Graph token {}", graphToken);

            // CHECK permissions

        } catch (Exception e) {
            log.error("Cannot validate permissions", e);
            return false;
        }
        */

        return true;
    }

    @Override
    public boolean validatePermissions(UserCredentialDTO credentialDTO) {
        return true;
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
        String idToken = authenticationResult.getIdToken();
        String[] parts = idToken.split("\\.");

        try {

            JwtPayload jwtPayload = objectMapper.readValue(Base64.getUrlDecoder().decode(parts[1]), JwtPayload.class);

            log.info("Extracted jwt {}", jwtPayload);

            if (jwtPayload.getUpn() != null && !jwtPayload.getUpn().isEmpty()) {

                UserInfo userInfo = new UserInfo(jwtPayload.getUpn(), getRandomToken());
                userInfo.setFirstName(jwtPayload.getFirstName());
                userInfo.setLastName(jwtPayload.getLastName());

                userInfo.getKeys().put("refresh_token", authenticationResult.getRefreshToken());

                return userInfo;
            }


        } catch (IOException e) {
            log.info("Cannot extract info from payload", e);
        }

        throw new DlabException("Cannot verify user identity");
    }

    private abstract static class AuthorizationSupplier {
        final AzureAuthenticationService azureAuthenticationService;

        AuthorizationSupplier(AzureAuthenticationService service) {
            this.azureAuthenticationService = service;
        }

        abstract Future<AuthenticationResult> get(AuthenticationContext context, String resource);
    }

    private static class AuthorizationCodeSupplier extends AuthorizationSupplier {
        private final AuthorizationCodeFlowResponse response;

        AuthorizationCodeSupplier(AzureAuthenticationService service,
                                  AuthorizationCodeFlowResponse response) {
            super(service);
            this.response = response;
        }

        public Future<AuthenticationResult> get(AuthenticationContext context, String resource) {
            return context
                    .acquireTokenByAuthorizationCode(response.getCode(), resource, azureAuthenticationService.clientId,
                            URI.create(azureAuthenticationService.redirectUrl), null);
        }
    }

    private static class UsernamePasswordSupplier extends AuthorizationSupplier {
        private final UserCredentialDTO credentialDTO;

        UsernamePasswordSupplier(AzureAuthenticationService service,
                                 UserCredentialDTO credentialDTO) {
            super(service);
            this.credentialDTO = credentialDTO;
        }

        public Future<AuthenticationResult> get(AuthenticationContext context, String resource) {
            return context
                    .acquireToken(resource, azureAuthenticationService.clientId, credentialDTO.getUsername(),
                            credentialDTO.getPassword(), null);
        }
    }
}
