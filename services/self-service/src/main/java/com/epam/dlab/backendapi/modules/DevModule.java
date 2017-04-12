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

package com.epam.dlab.backendapi.modules;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import javax.ws.rs.core.Response;

import com.epam.dlab.ModuleBase;
import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.SelfServiceApplicationConfiguration;
import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.mongo.MongoService;
import com.epam.dlab.rest.client.RESTService;
import com.epam.dlab.rest.contracts.DockerAPI;
import com.epam.dlab.rest.contracts.SecurityAPI;
import com.google.inject.name.Names;

import io.dropwizard.setup.Environment;

/** Mock class for an application configuration of SelfService for developer mode.
 */
public class DevModule extends ModuleBase<SelfServiceApplicationConfiguration> implements SecurityAPI, DockerAPI {
	
	/** Instantiates an application configuration of SelfService for developer mode.
     * @param configuration application configuration of SelfService.
     * @param environment environment of SelfService.
     */
    public DevModule(SelfServiceApplicationConfiguration configuration, Environment environment) {
        super(configuration, environment);
    }

    @Override
    protected void configure() {
        bind(SelfServiceApplicationConfiguration.class).toInstance(configuration);
        bind(MongoService.class).toInstance(configuration.getMongoFactory().build(environment));
        bind(RESTService.class).annotatedWith(Names.named(ServiceConsts.SECURITY_SERVICE_NAME))
                .toInstance(createAuthenticationService());
        bind(RESTService.class).annotatedWith(Names.named(ServiceConsts.PROVISIONING_SERVICE_NAME))
        		.toInstance(configuration.getProvisioningFactory().build(environment, ServiceConsts.PROVISIONING_SERVICE_NAME));
    }

    /** Creates and returns the mock object for authentication service.
     */
    private RESTService createAuthenticationService() {
        RESTService result = mock(RESTService.class);
        when(result.post(eq(LOGIN), any(), any())).then(invocationOnMock -> Response.ok("token123").build());
        when(result.post(eq(GET_USER_INFO), eq("token123"), eq(UserInfo.class)))
                .thenReturn(new UserInfo("test", "token123"));
        return result;
    }
}
