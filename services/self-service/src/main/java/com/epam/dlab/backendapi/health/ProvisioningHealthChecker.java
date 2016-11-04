/******************************************************************************************************

 Copyright (c) 2016 EPAM Systems Inc.

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 *****************************************************************************************************/

package com.epam.dlab.backendapi.health;

import com.epam.dlab.client.restclient.RESTService;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.Response;

import static com.epam.dlab.backendapi.SelfServiceApplicationConfiguration.PROVISIONING_SERVICE;

public class ProvisioningHealthChecker implements HealthChecker {
    private static final Logger LOGGER = LoggerFactory.getLogger(ProvisioningHealthChecker.class);

    @Inject
    @Named(PROVISIONING_SERVICE)
    private RESTService provisioningService;

    @Override
    public boolean isAlive() {
        try {
            Response response = provisioningService.get("/infrastructure/status", Response.class);
            boolean alive = response.getStatusInfo() == Response.Status.OK;
            if (!alive) {
                LOGGER.error("Provisioning service is not available");
            }
            return alive;
        } catch (Throwable t) {
            LOGGER.error("Provisioning service is not available", t);
            return false;
        }
    }
}
