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

package com.epam.dlab.backendapi.core.health;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.contracts.HealthChecker;
import com.epam.dlab.rest.client.RESTService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;

/** Health checker for provisioning service.
 */
public class ProvisioningHealthChecker implements HealthChecker {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProvisioningHealthChecker.class);

    private RESTService provisioningService;

    /** Creates checker instance for provisioning service.
     * @param provisioningService provisioning service.
     */
    public ProvisioningHealthChecker(RESTService provisioningService) {
        this.provisioningService = provisioningService;
    }

    @Override
    public boolean isAlive(UserInfo userInfo) {
        try {
            Response response = provisioningService.get("infrastructure/status",userInfo.getAccessToken(), Response.class);
            boolean alive = response.getStatusInfo().getStatusCode() == Response.Status.OK.getStatusCode();
            if (!alive) {
                LOGGER.error("Provisioning service is not available");
            }
            return alive;
        } catch (Exception t) {
            LOGGER.error("Provisioning service is not available", t);
            return false;
        }
    }
}
