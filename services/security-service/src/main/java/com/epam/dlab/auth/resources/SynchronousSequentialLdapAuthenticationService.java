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

package com.epam.dlab.auth.resources;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.identitymanagement.model.AccessKeyMetadata;
import com.amazonaws.services.identitymanagement.model.User;
import com.epam.dlab.auth.SecurityServiceConfiguration;
import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.auth.UserInfoDAO;
import com.epam.dlab.auth.dao.AwsUserDAOImpl;
import com.epam.dlab.auth.dao.LdapUserDAO;
import com.epam.dlab.auth.dao.UserInfoDAODumbImpl;
import com.epam.dlab.auth.dao.UserInfoDAOMongoImpl;
import com.epam.dlab.auth.dao.filter.AwsUserDAO;
import com.epam.dlab.auth.rest.AbstractAuthenticationService;
import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.dto.UserCredentialDTO;
import com.epam.dlab.exceptions.DlabException;
import com.google.common.collect.Lists;
import io.dropwizard.setup.Environment;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Used for authentication against LDAP server
 */
@Path("/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class SynchronousSequentialLdapAuthenticationService extends AbstractAuthenticationService<SecurityServiceConfiguration> {

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private final LdapUserDAO ldapUserDAO;
    private final UserInfoDAO userInfoDao;
    private AwsUserDAO awsUserDAO;

    public SynchronousSequentialLdapAuthenticationService(SecurityServiceConfiguration config, Environment env) {
        super(config);

        if (config.isUserInfoPersistenceEnabled()) {
            this.userInfoDao = new UserInfoDAOMongoImpl(config.getMongoFactory().build(env), config.getInactiveUserTimeoutMillSec());
        } else {
            this.userInfoDao = new UserInfoDAODumbImpl();
        }

        if (config.isAwsUserIdentificationEnabled()) {
            DefaultAWSCredentialsProviderChain providerChain = new DefaultAWSCredentialsProviderChain();
            awsUserDAO = new AwsUserDAOImpl(providerChain.getCredentials());
            scheduler.scheduleAtFixedRate(() -> {
                try {
                    providerChain.refresh();
                    awsUserDAO.updateCredentials(providerChain.getCredentials());
                    log.debug("provider credentials refreshed");
                } catch (Exception e) {
                    log.error("AWS provider error", e);
                    throw e;
                }
            }, 5, 5, TimeUnit.MINUTES);
        }

        this.ldapUserDAO = new LdapUserDAO(config, false);
    }

    @Override
    @POST
    @Path("/login")
    public Response login(UserCredentialDTO credential, @Context HttpServletRequest request) {

        String username = credential.getUsername();
        String password = credential.getPassword();
        String accessToken = credential.getAccessToken();
        String remoteIp = request.getRemoteAddr();
        String userAgent = request.getHeader(HttpHeaders.USER_AGENT);

        log.debug("validating username:{} password:****** token:{} ip:{}", username, accessToken, remoteIp);

        if (accessToken != null && !accessToken.isEmpty()) {
            UserInfo ui = getUserInfo(accessToken, userAgent, remoteIp);
            if (ui != null) {
                return Response.ok(accessToken).build();
            } else{
                return Response.status(Response.Status.UNAUTHORIZED).build();
            }
        }

        try {

            login(username, password);
            UserInfo enriched = enrichUser(username);
            verifyAwsUser(username, enriched);
            verifyAwsKeys(username, enriched);

            enriched.setRemoteIp(remoteIp);
            log.info("User authenticated is {}", enriched);
            String token = getRandomToken();

            userInfoDao.saveUserInfo(enriched.withToken(token));
            return Response.ok(token).build();

        } catch (Exception e) {
            return Response.status(Response.Status.UNAUTHORIZED).entity(e.getMessage()).build();
        }
    }

    @Override
    @POST
    @Path("/getuserinfo")
    public UserInfo getUserInfo(String accessToken, @Context HttpServletRequest request) {
        String userAgent = request.getHeader(HttpHeaders.USER_AGENT);
        String remoteIp = request.getRemoteAddr();

        UserInfo ui = getUserInfo(accessToken, userAgent, remoteIp);

        if (ui != null) {
            return ui;
        }

        log.debug("Session {} is expired", accessToken);

        return null;
    }

    private UserInfo getUserInfo(String accessToken, String userAgent, String remoteIp) {

        UserInfo ui = userInfoDao.getUserInfoByAccessToken(accessToken);

        if (ui != null) {
            ui = ui.withToken(accessToken);
            updateTTL(accessToken, ui, userAgent);
            log.debug("restored UserInfo from DB {}", ui);

            log.debug("Authorized {} {} {}", accessToken, ui, remoteIp);
            return ui;
        }

        return null;

    }


    @Override
    @POST
    @Path("/logout")
    public Response logout(String accessToken) {
        userInfoDao.deleteUserInfo(accessToken);
        log.info("Logged out user {}", accessToken);
        return Response.ok().build();
    }

    private UserInfo login(String username, String password) {
        try {
            UserInfo userInfo = ldapUserDAO.getUserInfo(username, password);
            log.debug("User Authenticated: {}", username);
            return userInfo;
        } catch (Exception e) {
            log.error("Authentication error", e);
            throw new DlabException("Username or password are not valid", e);
        }
    }

    private UserInfo enrichUser(String username) {

        try {
            UserInfo userInfo = ldapUserDAO.enrichUserInfo(new UserInfo(username, null));
            log.debug("User Enriched: {}", username);
            return userInfo;
        } catch (Exception e) {
            log.error("Authentication error", e);
            throw new DlabException("User not authorized. Please contact DLAB administrator.");
        }
    }


    private User verifyAwsUser(String username, UserInfo userInfo) {

        if (config.isAwsUserIdentificationEnabled()) {
            try {
                User awsUser = awsUserDAO.getAwsUser(username);
                if (awsUser != null) {
                    userInfo.setAwsUser(true);
                    return awsUser;
                } else {
                    throw new DlabException("Please contact AWS administrator to create corresponding IAM User");
                }
            } catch (RuntimeException e) {
                throw new DlabException("Please contact AWS administrator to create corresponding IAM User", e);
            }
        }

        return new User();
    }

    private List<AccessKeyMetadata> verifyAwsKeys(String username, UserInfo userInfo) {

        userInfo.getKeys().clear();

        if (config.isAwsUserIdentificationEnabled()) {
            try {
                List<AccessKeyMetadata> keys = awsUserDAO.getAwsAccessKeys(username);
                if (keys == null || keys.isEmpty()
                        || keys.stream().filter(k -> "Active".equalsIgnoreCase(k.getStatus())).count() == 0) {

                    throw new DlabException("Cannot get aws access key for user " + username);
                }
                keys.forEach(e -> userInfo.addKey(e.getAccessKeyId(), e.getStatus()));

                return keys;
            } catch (RuntimeException e) {
                throw new DlabException("Please contact AWS administrator to activate your Access Key", e);
            }
        }
        return Lists.newArrayList();
    }


    private void updateTTL(String accessToken, UserInfo ui, String userAgent) {
        log.debug("updating TTL agent {} {}", userAgent, ui);
        if (ServiceConsts.PROVISIONING_USER_AGENT.equals(userAgent)) {
            return;
        }

        userInfoDao.updateUserInfoTTL(accessToken, ui);
    }
}
