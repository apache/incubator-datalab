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
import com.epam.datalab.backendapi.resources.dto.HealthStatusPageDTO;
import com.epam.datalab.backendapi.service.InfrastructureInfoService;
import com.epam.datalab.dto.InfrastructureMetaInfoDTO;
import com.epam.datalab.exceptions.DatalabException;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.refEq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class InfrastructureInfoResourceTest extends TestBase {

    private InfrastructureInfoService infrastructureInfoService = mock(InfrastructureInfoService.class);

    @Rule
    public final ResourceTestRule resources =
            getResourceTestRuleInstance(new InfrastructureInfoResource(infrastructureInfoService));

    @Before
    public void setup() throws AuthenticationException {
        authSetup();
    }

    @Test
    public void status() {
        final Response response = resources.getJerseyTest()
                .target("/infrastructure")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .get();

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertNull(response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verifyZeroInteractions(infrastructureInfoService);
    }

    @Test
    public void statusWithFailedAuth() throws AuthenticationException {
        authFailSetup();
        final Response response = resources.getJerseyTest()
                .target("/infrastructure")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .get();

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertNull(response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verifyZeroInteractions(infrastructureInfoService);
    }

    @Test
    public void healthStatus() {
        HealthStatusPageDTO hspDto = getHealthStatusPageDTO();
        when(infrastructureInfoService.getHeathStatus(any(UserInfo.class))).thenReturn(hspDto);
        final Response response = resources.getJerseyTest()
                .target("/infrastructure/status")
                .queryParam("full", "1")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .get();

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertEquals(hspDto.getStatus(), response.readEntity(HealthStatusPageDTO.class).getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(infrastructureInfoService).getHeathStatus(refEq(getUserInfo()));
        verifyNoMoreInteractions(infrastructureInfoService);
    }

    @Test
    public void healthStatusWithFailedAuth() throws AuthenticationException {
        authFailSetup();
        HealthStatusPageDTO hspDto = getHealthStatusPageDTO();
        when(infrastructureInfoService.getHeathStatus(any(UserInfo.class))).thenReturn(hspDto);
        final Response response = resources.getJerseyTest()
                .target("/infrastructure/status")
                .queryParam("full", "1")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .get();

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertEquals(hspDto.getStatus(), response.readEntity(HealthStatusPageDTO.class).getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(infrastructureInfoService).getHeathStatus(refEq(getUserInfo()));
        verifyNoMoreInteractions(infrastructureInfoService);
    }

    @Test
    public void healthStatusWithDefaultQueryParam() {
        HealthStatusPageDTO hspDto = getHealthStatusPageDTO();
        when(infrastructureInfoService.getHeathStatus(any(UserInfo.class))).thenReturn(hspDto);
        final Response response = resources.getJerseyTest()
                .target("/infrastructure/status")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .get();

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertEquals(hspDto.getStatus(), response.readEntity(HealthStatusPageDTO.class).getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(infrastructureInfoService).getHeathStatus(refEq(getUserInfo()));
        verifyNoMoreInteractions(infrastructureInfoService);
    }

    @Test
    public void healthStatusWithException() {
        doThrow(new DatalabException("Could not return status of resources for user"))
                .when(infrastructureInfoService).getHeathStatus(any(UserInfo.class));
        final Response response = resources.getJerseyTest()
                .target("/infrastructure/status")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .get();

        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(infrastructureInfoService).getHeathStatus(refEq(getUserInfo()));
        verifyNoMoreInteractions(infrastructureInfoService);
    }


    @Test
    public void getUserResourcesWithException() {
        doThrow(new DatalabException("Could not load list of provisioned resources for user"))
                .when(infrastructureInfoService).getUserResources(any(UserInfo.class));
        final Response response = resources.getJerseyTest()
                .target("/infrastructure/info")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .get();

        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(infrastructureInfoService).getUserResources(any());
        verifyNoMoreInteractions(infrastructureInfoService);
    }

    @Test
    public void getInfrastructureMeta() {

        when(infrastructureInfoService.getInfrastructureMetaInfo()).thenReturn(
                InfrastructureMetaInfoDTO.builder()
                        .version("1.0").build());
        final Response response = resources.getJerseyTest()
                .target("/infrastructure/meta")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .get();

        final InfrastructureMetaInfoDTO infrastructureMetaInfoDTO =
                response.readEntity(InfrastructureMetaInfoDTO.class);
        assertEquals("1.0", infrastructureMetaInfoDTO.getVersion());
    }

    private HealthStatusPageDTO getHealthStatusPageDTO() {
        return HealthStatusPageDTO.builder()
                .status("someStatus")
                .build();
    }
}
