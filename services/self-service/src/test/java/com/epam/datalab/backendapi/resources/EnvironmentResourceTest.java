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
import com.epam.datalab.backendapi.service.EnvironmentService;
import com.epam.datalab.exceptions.ResourceConflictException;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class EnvironmentResourceTest extends TestBase {

    private EnvironmentService environmentService = mock(EnvironmentService.class);

    @Rule
    public final ResourceTestRule resources = getResourceTestRuleInstance(new EnvironmentResource(environmentService));

    @Before
    public void setup() throws AuthenticationException {
        authSetup();
    }

    @Test
    public void getAllEnv() {
        UserInfo userInfo = getUserInfo();
        when(environmentService.getAllEnv(userInfo)).thenReturn(Collections.emptyList());
        final Response response = resources.getJerseyTest()
                .target("/environment/all")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .get();

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(environmentService).getAllEnv(eq(userInfo));
        verifyNoMoreInteractions(environmentService);
    }

    @Test
    public void getAllEnvWithFailedAuth() throws AuthenticationException {
        authFailSetup();
        when(environmentService.getAllEnv(getUserInfo())).thenReturn(Collections.emptyList());
        final Response response = resources.getJerseyTest()
                .target("/environment/all")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .get();

        assertEquals(HttpStatus.SC_FORBIDDEN, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verifyZeroInteractions(environmentService);
    }

    @Test
    public void stopNotebook() {
        doNothing().when(environmentService).stopExploratory(any(UserInfo.class), anyString(), anyString(), anyString());
        final Response response = resources.getJerseyTest()
                .target("/environment/stop/projectName/explName")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .post(Entity.text(USER));

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertNull(response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(environmentService).stopExploratory(new UserInfo(USER, TOKEN), USER, "projectName", "explName");
        verifyNoMoreInteractions(environmentService);
    }

    @Test
    public void stopNotebookWithFailedAuth() throws AuthenticationException {
        authFailSetup();
        doNothing().when(environmentService).stopExploratory(any(UserInfo.class), anyString(), anyString(), anyString());
        final Response response = resources.getJerseyTest()
                .target("/environment/stop/projectName/explName")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .post(Entity.text(USER));

        assertEquals(HttpStatus.SC_FORBIDDEN, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verifyZeroInteractions(environmentService);
    }

    @Test
    public void stopNotebookWithResourceConflictException() {
        doThrow(new ResourceConflictException("Can not stop notebook because its status is CREATING or STARTING"))
                .when(environmentService).stopExploratory(any(UserInfo.class), anyString(), anyString(), anyString());
        final Response response = resources.getJerseyTest()
                .target("/environment/stop/projectName/explName")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .post(Entity.text(USER));

        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(environmentService).stopExploratory(new UserInfo(USER, TOKEN), USER, "projectName", "explName");
        verifyNoMoreInteractions(environmentService);
    }

    @Test
    public void stopCluster() {
        doNothing().when(environmentService).stopComputational(any(UserInfo.class), anyString(), anyString(), anyString(), anyString());
        final Response response = resources.getJerseyTest()
                .target("/environment/stop/projectName/explName/compName")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .post(Entity.text(USER));

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertNull(response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(environmentService).stopComputational(new UserInfo(USER, TOKEN), USER, "projectName", "explName", "compName");
        verifyNoMoreInteractions(environmentService);
    }

    @Test
    public void stopClusterWithFailedAuth() throws AuthenticationException {
        authFailSetup();
        doNothing().when(environmentService).stopComputational(any(UserInfo.class), anyString(), anyString(), anyString(), anyString());
        final Response response = resources.getJerseyTest()
                .target("/environment/stop/projectName/explName/compName")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .post(Entity.text(USER));

        assertEquals(HttpStatus.SC_FORBIDDEN, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verifyZeroInteractions(environmentService);
    }

    @Test
    public void stopClusterWithResourceConflictException() {
        doThrow(new ResourceConflictException("Can not stop cluster because its status is CREATING or STARTING"))
                .when(environmentService).stopComputational(any(UserInfo.class), anyString(), anyString(), anyString(), anyString());
        final Response response = resources.getJerseyTest()
                .target("/environment/stop/projectName/explName/compName")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .post(Entity.text(USER));

        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(environmentService).stopComputational(new UserInfo(USER, TOKEN), USER, "projectName", "explName", "compName");
        verifyNoMoreInteractions(environmentService);
    }

    @Test
    public void terminateNotebook() {
        doNothing().when(environmentService).terminateExploratory(any(UserInfo.class), anyString(), anyString(), anyString());
        final Response response = resources.getJerseyTest()
                .target("/environment/terminate/projectName/explName")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .post(Entity.text(USER));

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertNull(response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(environmentService).terminateExploratory(new UserInfo(USER, TOKEN), USER, "projectName", "explName");
        verifyNoMoreInteractions(environmentService);
    }

    @Test
    public void terminateNotebookWithFailedAuth() throws AuthenticationException {
        authFailSetup();
        doNothing().when(environmentService).terminateExploratory(any(UserInfo.class), anyString(), anyString(), anyString());
        final Response response = resources.getJerseyTest()
                .target("/environment/terminate/projectName/explName")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .post(Entity.text(USER));

        assertEquals(HttpStatus.SC_FORBIDDEN, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verifyZeroInteractions(environmentService);
    }

    @Test
    public void terminateNotebookWithResourceConflictException() {
        doThrow(new ResourceConflictException("Can not terminate notebook because its status is CREATING or STARTING"))
                .when(environmentService).terminateExploratory(any(UserInfo.class), anyString(), anyString(), anyString());
        final Response response = resources.getJerseyTest()
                .target("/environment/terminate/projectName/explName")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .post(Entity.text(USER));

        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(environmentService).terminateExploratory(new UserInfo(USER, TOKEN), USER, "projectName", "explName");
        verifyNoMoreInteractions(environmentService);
    }

    @Test
    public void terminateCluster() {
        doNothing().when(environmentService).terminateComputational(any(UserInfo.class), anyString(), anyString(), anyString(), anyString());
        final Response response = resources.getJerseyTest()
                .target("/environment/terminate/projectName/explName/compName")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .post(Entity.text(USER));

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertNull(response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(environmentService).terminateComputational(new UserInfo(USER, TOKEN), USER, "projectName", "explName", "compName");
        verifyNoMoreInteractions(environmentService);
    }

    @Test
    public void terminateClusterWithFailedAuth() throws AuthenticationException {
        authFailSetup();
        doNothing().when(environmentService).terminateComputational(any(UserInfo.class), anyString(), anyString(), anyString(), anyString());
        final Response response = resources.getJerseyTest()
                .target("/environment/terminate/projectName/explName/compName")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .post(Entity.text(USER));

        assertEquals(HttpStatus.SC_FORBIDDEN, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verifyZeroInteractions(environmentService);
    }

    @Test
    public void terminateClusterWithResourceConflictException() {
        doThrow(new ResourceConflictException("Can not terminate cluster because its status is CREATING or STARTING"))
                .when(environmentService).terminateComputational(any(UserInfo.class), anyString(), anyString(), anyString(), anyString());
        final Response response = resources.getJerseyTest()
                .target("/environment/terminate/projectName/explName/compName")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .post(Entity.text(USER));

        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(environmentService).terminateComputational(new UserInfo(USER, TOKEN), USER, "projectName", "explName", "compName");
        verifyNoMoreInteractions(environmentService);
    }
}
