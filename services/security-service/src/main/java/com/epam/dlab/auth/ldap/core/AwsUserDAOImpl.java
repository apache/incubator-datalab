/***************************************************************************

Copyright (c) 2016, EPAM SYSTEMS INC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

****************************************************************************/

package com.epam.dlab.auth.ldap.core;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagement;
import com.amazonaws.services.identitymanagement.AmazonIdentityManagementClient;
import com.amazonaws.services.identitymanagement.model.*;
import com.epam.dlab.auth.ldap.core.filter.AwsUserDAO;
import com.epam.dlab.auth.rest.ExpirableContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AwsUserDAOImpl implements AwsUserDAO {

    private final static Logger LOG = LoggerFactory.getLogger(AwsUserDAOImpl.class);

    private final ExpirableContainer<User> usersCache = new ExpirableContainer<>();
    private volatile AWSCredentials credentials;
    private volatile AmazonIdentityManagement aim;

    public AwsUserDAOImpl(AWSCredentials credentials) {

        this.credentials = credentials;
        this.aim = new AmazonIdentityManagementClient(credentials);
        try {
            ListUsersResult lur = aim.listUsers();
            lur.getUsers().forEach(u -> {
                usersCache.put(u.getUserName(), u, 3600000);
                LOG.debug("Initialized AWS user {}",u);
            });

        } catch(Exception e) {
            LOG.error("Failed AWS user initialization. Will keep trying. Error: {}",e.getMessage());
        }
    }

    @Override
    public User getAwsUser(String username) {
        User u = usersCache.get(username);
        if(u == null) {
            u = fetchAwsUser(username);
            usersCache.put(username,u,600000);
            LOG.debug("Fetched AWS user {}",u);
        }
        return u;
    }

    @Override
    public void updateCredentials(AWSCredentials credentials) {
        this.credentials = credentials;
        this.aim         = new AmazonIdentityManagementClient(credentials);
    }

    private User fetchAwsUser(String username) {
        User user = null;
        try {
            GetUserRequest r = new GetUserRequest().withUserName(username);
            GetUserResult ur = aim.getUser(r);
            user = ur.getUser();
        } catch (NoSuchEntityException e) {
            LOG.error("User {} not found: {}",username,e.getMessage());
        }
        return user;
    }
}
