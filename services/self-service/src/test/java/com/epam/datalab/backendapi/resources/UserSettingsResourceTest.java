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

import com.epam.datalab.auth.UserInfo;
import com.epam.datalab.backendapi.resources.dto.UserDTO;
import com.epam.datalab.backendapi.service.UserSettingService;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class UserSettingsResourceTest extends TestBase {

    private UserSettingService userSettingService = mock(UserSettingService.class);

    @Rule
    public final ResourceTestRule resources =
            getResourceTestRuleInstance(new UserSettingsResource(userSettingService));

    @Before
    public void setup() throws AuthenticationException {
        authSetup();
    }

    @Test
    public void getSettings() {
        when(userSettingService.getUISettings(any(UserInfo.class))).thenReturn("someSettings");
        final Response response = resources.getJerseyTest()
                .target("/user/settings")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .get();

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertEquals("someSettings", response.readEntity(String.class));
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(userSettingService).getUISettings(refEq(getUserInfo()));
        verifyNoMoreInteractions(userSettingService);
    }

    @Test
    public void getSettingsWithFailedAuth() throws AuthenticationException {
        authFailSetup();
        when(userSettingService.getUISettings(any(UserInfo.class))).thenReturn("someSettings");
        final Response response = resources.getJerseyTest()
                .target("/user/settings")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .get();

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertEquals("someSettings", response.readEntity(String.class));
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(userSettingService).getUISettings(refEq(getUserInfo()));
        verifyNoMoreInteractions(userSettingService);
    }

    @Test
    public void saveSettings() {
        doNothing().when(userSettingService).saveUISettings(any(UserInfo.class), anyString());
        final Response response = resources.getJerseyTest()
                .target("/user/settings")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .post(Entity.json("someSettings"));

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertNull(response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(userSettingService).saveUISettings(refEq(getUserInfo()), eq("someSettings"));
        verifyNoMoreInteractions(userSettingService);
    }

    @Test
    public void saveSettingsWithFailedAuth() throws AuthenticationException {
        authFailSetup();
        doNothing().when(userSettingService).saveUISettings(any(UserInfo.class), anyString());
        final Response response = resources.getJerseyTest()
                .target("/user/settings")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .post(Entity.json("someSettings"));

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertNull(response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(userSettingService).saveUISettings(refEq(getUserInfo()), eq("someSettings"));
        verifyNoMoreInteractions(userSettingService);
    }

    @Test
    public void saveSettingsWithException() {
        doThrow(new RuntimeException()).when(userSettingService).saveUISettings(any(UserInfo.class), anyString());
        final Response response = resources.getJerseyTest()
                .target("/user/settings")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .post(Entity.json("someSettings"));

        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
        assertTrue(response.readEntity(String.class).contains("{\"code\":500,\"message\":\"There was an error " +
                "processing your request. It has been logged"));
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(userSettingService).saveUISettings(refEq(getUserInfo()), eq("someSettings"));
        verifyNoMoreInteractions(userSettingService);
    }

    @Test
    public void saveAllowedBudget() {
        doNothing().when(userSettingService).saveUISettings(any(UserInfo.class), anyString());
        final Response response = resources.getJerseyTest()
                .target("/user/settings/budget")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .put(Entity.json(singletonList(new UserDTO(USER, 10, UserDTO.Status.ACTIVE))));

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertNull(response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(userSettingService).updateUsersBudget(singletonList(new UserDTO(USER, 10, UserDTO.Status.ACTIVE)));
        verifyNoMoreInteractions(userSettingService);
    }
}
