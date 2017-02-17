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

package com.epam.dlab.backendapi.resources;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.resources.dto.HealthStatusDTO;
import com.epam.dlab.contracts.HealthChecker;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.dropwizard.auth.Auth;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import static com.epam.dlab.backendapi.core.health.HealthChecks.MONGO_HEALTH_CHECKER;
import static com.epam.dlab.backendapi.core.health.HealthChecks.PROVISIONING_HEALTH_CHECKER;

/** Provides the REST API for the basic information about infrastructure.
 */
@Path("/infrastructure")
@Produces(MediaType.APPLICATION_JSON)
public class InfrasctructureResource {
    @Inject
    @Named(MONGO_HEALTH_CHECKER)
    private HealthChecker mongoHealthChecker;
    @Inject
    @Named(PROVISIONING_HEALTH_CHECKER)
    private HealthChecker provisioningHealthChecker;

    /** Returns the status of infrastructure: database and provisioning service.
     * @param userInfo user info.
     */
    @GET
    @Path("/status")
    public HealthStatusDTO status(@Auth UserInfo userInfo) {
        return new HealthStatusDTO()
                .withMongoAlive(mongoHealthChecker.isAlive(userInfo))
                .withProvisioningAlive(provisioningHealthChecker.isAlive(userInfo));
    }
}
