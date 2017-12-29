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
import com.epam.dlab.auth.dao.AwsUserDAOImpl;
import com.epam.dlab.auth.dao.filter.AwsUserDAO;
import com.epam.dlab.exceptions.DlabException;
import com.google.common.collect.Lists;
import io.dropwizard.setup.Environment;

import javax.ws.rs.Consumes;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Provides authentication against LDAP server and verify user identity and validity against AWS
 */
@Path("/")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AwsSynchronousLdapAuthenticationService extends SynchronousLdapAuthenticationService {
    private AwsUserDAO awsUserDAO;

    public AwsSynchronousLdapAuthenticationService(SecurityServiceConfiguration config, Environment env) {
        super(config, env);

        if (config.isAwsUserIdentificationEnabled()) {
            DefaultAWSCredentialsProviderChain providerChain = new DefaultAWSCredentialsProviderChain();
            awsUserDAO = new AwsUserDAOImpl(providerChain.getCredentials());
            Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(() -> {
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
    }

    @Override
    protected void verifyUser(String username, UserInfo userInfo, Object... params) {
        verifyAwsUser(username, userInfo);
        verifyAwsKeys(username, userInfo);
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
}
