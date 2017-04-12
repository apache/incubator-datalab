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

import com.epam.dlab.ModuleBase;
import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.SelfServiceApplicationConfiguration;
import com.epam.dlab.mongo.MongoService;
import com.epam.dlab.rest.client.RESTService;
import com.epam.dlab.rest.contracts.DockerAPI;
import com.epam.dlab.rest.contracts.EdgeAPI;
import com.epam.dlab.rest.contracts.ExploratoryAPI;
import com.epam.dlab.rest.contracts.SecurityAPI;
import com.epam.dlab.utils.ResourceUtils;
import com.google.inject.name.Names;
import io.dropwizard.setup.Environment;

import javax.ws.rs.core.Response;
import java.util.UUID;
import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.dto.imagemetadata.ComputationalMetadataDTO;
import com.epam.dlab.dto.imagemetadata.ExploratoryMetadataDTO;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Mock class for an application configuration of SelfService for tests.
 */
public class MockModule extends ModuleBase<SelfServiceApplicationConfiguration> implements SecurityAPI, DockerAPI, EdgeAPI, ExploratoryAPI {
	
	/** Instantiates an application configuration of SelfService for tests.
     * @param configuration application configuration of SelfService.
     * @param environment environment of SelfService.
     */
    public MockModule(SelfServiceApplicationConfiguration configuration, Environment environment) {
        super(configuration, environment);
    }

    @Override
    protected void configure() {
        bind(SelfServiceApplicationConfiguration.class).toInstance(configuration);
        bind(MongoService.class).toInstance(configuration.getMongoFactory().build(environment));
        bind(RESTService.class).annotatedWith(Names.named(ServiceConsts.SECURITY_SERVICE_NAME))
                .toInstance(createAuthenticationService());
        bind(RESTService.class).annotatedWith(Names.named(ServiceConsts.PROVISIONING_SERVICE_NAME))
                .toInstance(createProvisioningService());
        bind(RESTService.class).annotatedWith(Names.named(ServiceConsts.BILLING_SERVICE_NAME))
        		.toInstance(createBillingService());
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

    /** Creates and returns the mock object for provisioning service.
     */
    private RESTService createProvisioningService() {
        RESTService result = mock(RESTService.class);
        when(result.post(eq(EDGE_CREATE), any(), eq(Response.class)))
                .then(invocationOnMock -> Response.accepted().build());
        when(result.get(eq(DOCKER_EXPLORATORY), any(), eq(ExploratoryMetadataDTO[].class)))
                .thenReturn(new ExploratoryMetadataDTO[]{
                        prepareJupiterImage()
                });
        when(result.get(eq(DOCKER_COMPUTATIONAL), any(), eq(ComputationalMetadataDTO[].class)))
                .thenReturn(new ComputationalMetadataDTO[]{
                        prepareEmrImage()
                });
        when(result.post(eq(EXPLORATORY_CREATE), any(), eq(String.class))).thenReturn(UUID.randomUUID().toString());
        return result;
    }

    /** Creates and returns the computational metadata for EMR. */
    private ComputationalMetadataDTO prepareEmrImage() {
        try {
            ComputationalMetadataDTO dto = ResourceUtils.readResourceAsClass(getClass(),
                    "/metadata/computational_mock.json",
                    ComputationalMetadataDTO.class);
            return dto;
        }
        catch (Exception e) {
            return null;
        }
    }

    /** Creates and returns the exploratory metadata for Jupiter. */
    private ExploratoryMetadataDTO prepareJupiterImage() {
        try {
            ExploratoryMetadataDTO dto = ResourceUtils.readResourceAsClass(getClass(),
                    "/metadata/exploratory_mock.json",
                    ExploratoryMetadataDTO.class);
            return dto;
        }
        catch (Exception e) {
            return null;
        }
    }

    /** Creates and returns the mock object for billing service.
     */
    private RESTService createBillingService() {
        RESTService result = mock(RESTService.class);
        when(result.post(eq("billing"), any(), eq(Response.class)))
                .then(invocationOnMock -> Response.accepted().build());
        return result;
    }

}
