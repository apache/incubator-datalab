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

package com.epam.dlab.auth;

import io.dropwizard.auth.Authorizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SecurityAuthorizer implements Authorizer<UserInfo> {
    private final static Logger LOGGER = LoggerFactory.getLogger(SecurityRestAuthenticator.class);

    /*
    Add annotations to the service resource class:
@PermitAll. All authenticated users will have access to the method.
@RolesAllowed("awsUser"). Access will be granted to the users with the specified roles.
@DenyAll. No access will be granted to anyone.
    */

    @Override
    public boolean authorize(UserInfo userInfo, String role) {
        LOGGER.debug("authorize user = {} with role = {}", userInfo.getName(), role);
        if(role == null) {
            return true;
        }

        boolean authorized = false;

        switch(role.toUpperCase()) {
            case "AWSUSER":
                authorized = userInfo.isAwsUser();
                break;
            default:
                authorized = true;
        }

        return authorized;
    }
}
