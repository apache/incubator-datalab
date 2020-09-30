/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.epam.datalab.backendapi.resources;


import com.epam.datalab.backendapi.service.ApplicationSettingService;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ApplicationSettingResourceTest extends TestBase {

    private ApplicationSettingService applicationSettingService = mock(ApplicationSettingService.class);

    @Rule
    public final ResourceTestRule resources =
            getResourceTestRuleInstance(new ApplicationSettingResource(applicationSettingService));

    @Before
    public void setup() throws AuthenticationException {
        authSetup();
    }


    @Test
    public void setMaxBudget() {
        final Response response = resources.getJerseyTest()
                .target("/settings/budget/12")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .put(Entity.entity("dummy", MediaType.TEXT_PLAIN));

        assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatus());

        verify(applicationSettingService).setMaxBudget(12L);
        verifyNoMoreInteractions(applicationSettingService);
    }

    @Test
    public void removeMaxBudget() {
        final Response response = resources.getJerseyTest()
                .target("/settings/budget")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .delete();

        assertEquals(HttpStatus.SC_NO_CONTENT, response.getStatus());

        verify(applicationSettingService).removeMaxBudget();
        verifyNoMoreInteractions(applicationSettingService);
    }

    @Test
    public void getSettings() {

        when(applicationSettingService.getSettings()).thenReturn(Collections.singletonMap("key", "value"));
        final Response response = resources.getJerseyTest()
                .target("/settings")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .get();
        final Map map = response.readEntity(Map.class);

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getMediaType());
        assertEquals(1, map.size());
        assertEquals("value", map.get("key"));

        verify(applicationSettingService).getSettings();
        verifyNoMoreInteractions(applicationSettingService);


    }
}