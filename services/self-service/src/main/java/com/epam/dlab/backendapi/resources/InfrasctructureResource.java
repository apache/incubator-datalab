/******************************************************************************************************

 Copyright (c) 2016 EPAM Systems Inc.

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 *****************************************************************************************************/

package com.epam.dlab.backendapi.resources;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.health.HealthChecker;
import com.epam.dlab.backendapi.health.HealthStatusDTO;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.dropwizard.auth.Auth;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import static com.epam.dlab.backendapi.health.HealthChecks.MONGO_HEALTH_CHECKER;
import static com.epam.dlab.backendapi.health.HealthChecks.PROVISIONING_HEALTH_CHECKER;

@Path("/infrastructure")
@Produces(MediaType.APPLICATION_JSON)
public class InfrasctructureResource {
    @Inject
    @Named(MONGO_HEALTH_CHECKER)
    private HealthChecker mongoHealthChecker;
    @Inject
    @Named(PROVISIONING_HEALTH_CHECKER)
    private HealthChecker provisioningHealthChecker;

    @GET
    @Path("/status")
    public HealthStatusDTO status(@Auth UserInfo userInfo) {
        return new HealthStatusDTO()
                .withMongoAlive(mongoHealthChecker.isAlive())
                .withProvisioningAlive(provisioningHealthChecker.isAlive());
    }
}
