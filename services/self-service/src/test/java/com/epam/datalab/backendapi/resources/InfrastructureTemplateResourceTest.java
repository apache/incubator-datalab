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
import com.epam.datalab.backendapi.service.InfrastructureTemplateService;
import com.epam.datalab.dto.base.computational.FullComputationalTemplate;
import com.epam.datalab.dto.imagemetadata.ComputationalMetadataDTO;
import com.epam.datalab.dto.imagemetadata.ExploratoryMetadataDTO;
import com.epam.datalab.exceptions.DatalabException;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class InfrastructureTemplateResourceTest extends TestBase {

    private InfrastructureTemplateService infrastructureTemplateService = mock(InfrastructureTemplateService.class);

    @Rule
    public final ResourceTestRule resources =
            getResourceTestRuleInstance(new InfrastructureTemplateResource(infrastructureTemplateService));

    @Before
    public void setup() throws AuthenticationException {
        authSetup();
    }

    @Test
    public void getComputationalTemplates() {
        FullComputationalTemplate fullComputationalTemplate =
                new FullComputationalTemplate(new ComputationalMetadataDTO());
        when(infrastructureTemplateService.getComputationalTemplates(any(UserInfo.class), anyString(), anyString()))
                .thenReturn(Collections.singletonList(fullComputationalTemplate));
        final Response response = resources.getJerseyTest()
                .target("/infrastructure_templates/test/endpoint/computational_templates")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .get();

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(infrastructureTemplateService).getComputationalTemplates(getUserInfo(), "test", "endpoint");
        verifyNoMoreInteractions(infrastructureTemplateService);
    }

    @Test
    public void getComputationalTemplatesWithFailedAuth() throws AuthenticationException {
        authFailSetup();
        FullComputationalTemplate fullComputationalTemplate =
                new FullComputationalTemplate(new ComputationalMetadataDTO());
        when(infrastructureTemplateService.getComputationalTemplates(any(UserInfo.class), anyString(), anyString()))
                .thenReturn(Collections.singletonList(fullComputationalTemplate));
        final Response response = resources.getJerseyTest()
                .target("/infrastructure_templates/test/endpoint/computational_templates")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .get();

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(infrastructureTemplateService).getComputationalTemplates(getUserInfo(), "test", "endpoint");
        verifyNoMoreInteractions(infrastructureTemplateService);
    }

    @Test
    public void getComputationalTemplatesWithException() {
        doThrow(new DatalabException("Could not load list of computational templates for user"))
                .when(infrastructureTemplateService).getComputationalTemplates(any(UserInfo.class), anyString(), anyString());
        final Response response = resources.getJerseyTest()
                .target("/infrastructure_templates/test/endpoint/computational_templates")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .get();

        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(infrastructureTemplateService).getComputationalTemplates(getUserInfo(), "test", "endpoint");
        verifyNoMoreInteractions(infrastructureTemplateService);
    }

    @Test
    public void getExploratoryTemplates() {
        ExploratoryMetadataDTO exploratoryMetadataDTO =
                new ExploratoryMetadataDTO("someImageName");
        when(infrastructureTemplateService.getExploratoryTemplates(any(UserInfo.class), anyString(), anyString()))
                .thenReturn(Collections.singletonList(exploratoryMetadataDTO));
        final Response response = resources.getJerseyTest()
                .target("/infrastructure_templates/test/endpoint/exploratory_templates")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .get();

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertEquals(Collections.singletonList(exploratoryMetadataDTO),
                response.readEntity(new GenericType<List<ExploratoryMetadataDTO>>() {
                }));
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(infrastructureTemplateService).getExploratoryTemplates(getUserInfo(), "test", "endpoint");
        verifyNoMoreInteractions(infrastructureTemplateService);
    }

    @Test
    public void getExploratoryTemplatesWithFailedAuth() throws AuthenticationException {
        authFailSetup();
        ExploratoryMetadataDTO exploratoryMetadataDTO =
                new ExploratoryMetadataDTO("someImageName");
        when(infrastructureTemplateService.getExploratoryTemplates(any(UserInfo.class), anyString(), anyString()))
                .thenReturn(Collections.singletonList(exploratoryMetadataDTO));
        final Response response = resources.getJerseyTest()
                .target("/infrastructure_templates/test/endpoint/exploratory_templates")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .get();

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertEquals(Collections.singletonList(exploratoryMetadataDTO),
                response.readEntity(new GenericType<List<ExploratoryMetadataDTO>>() {
                }));
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(infrastructureTemplateService).getExploratoryTemplates(getUserInfo(), "test", "endpoint");
        verifyNoMoreInteractions(infrastructureTemplateService);
    }


    @Test
    public void getExploratoryTemplatesWithException() {
        doThrow(new DatalabException("Could not load list of exploratory templates for user"))
                .when(infrastructureTemplateService).getExploratoryTemplates(any(UserInfo.class), anyString(), anyString());
        final Response response = resources.getJerseyTest()
                .target("/infrastructure_templates/test/endpoint/exploratory_templates")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .get();

        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(infrastructureTemplateService).getExploratoryTemplates(getUserInfo(), "test", "endpoint");
        verifyNoMoreInteractions(infrastructureTemplateService);
    }
}
