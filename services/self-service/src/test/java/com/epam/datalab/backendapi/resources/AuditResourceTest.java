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

import com.epam.datalab.backendapi.domain.AuditCreateDTO;
import com.epam.datalab.backendapi.domain.AuditResourceTypeEnum;
import com.epam.datalab.backendapi.resources.dto.AuditFilter;
import com.epam.datalab.backendapi.service.AuditService;
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

import java.util.ArrayList;
import java.util.StringJoiner;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class AuditResourceTest extends TestBase {

    private final static String USER = "testuser";
    private final static String INFO = "testInfo";
    private final static String RESOURCE = "testResource";

    private final AuditService auditService = mock(AuditService.class);

    @Rule
    public final ResourceTestRule resources = getResourceTestRuleInstance(new AuditResource(auditService));

    @Before
    public void setup() throws AuthenticationException {
        authSetup();
    }

    @Test
    public void saveAudit() {
        final Response response = resources.getJerseyTest()
                .target("/audit")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .post(Entity.json(prepareAuditCreateDTO()));

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        verify(auditService).save(eq(USER), refEq(prepareAuditCreateDTO()));
        verifyNoMoreInteractions(auditService);
    }

    @Test
    public void getAudit() {
        final Response response = resources.getJerseyTest()
                .target("/audit")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .get();

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));
    }

    @Test
    public void downloadAuditReport() {
        when(auditService.downloadAuditReport(getAuditFilter())).thenReturn(getAuditReport());
        final Response response = resources.getJerseyTest()
                .target("/audit/report/download")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .post(Entity.json(getAuditFilter()));

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertEquals(getAuditReport(), response.readEntity(String.class));
        assertEquals(MediaType.APPLICATION_OCTET_STREAM, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(auditService).downloadAuditReport(refEq(getAuditFilter()));
        verifyNoMoreInteractions(auditService);
    }

    private AuditCreateDTO prepareAuditCreateDTO() {
        return new AuditCreateDTO(RESOURCE, INFO, AuditResourceTypeEnum.COMPUTE);
    }

    private String getAuditReport() {
        StringBuilder auiditReport = new StringBuilder();
        auiditReport.append("\"Available reporting period from: ").append("17-Mar-2022 ").append("to: ").append("20-Mar-2022").append("\"\n");
        auiditReport.append(new StringJoiner(",").add("Date").add("User").add("Action").add("Project").add("Resource type").add("Resource\n"));
        auiditReport.append(new StringJoiner(",").add("2022-03-20").add("test").add("LOG_IN").add("").add("").add("\n"));
        auiditReport.append(new StringJoiner(",").add("2022-03-20").add("test").add("LOG_IN").add("").add("").add("\n"));
        auiditReport.append(new StringJoiner(",").add("2022-03-19").add("test").add("LOG_IN").add("").add("").add("\n"));
        auiditReport.append(new StringJoiner(",").add("2022-03-17").add("test").add("LOG_IN").add("").add("").add("\n"));

        return auiditReport.toString();
    }

    private AuditFilter getAuditFilter() {
        AuditFilter auditFilter = new AuditFilter();
        auditFilter.setUsers(new ArrayList<>());
        auditFilter.setResourceNames(new ArrayList<>());
        auditFilter.setResourceTypes(new ArrayList<>());
        auditFilter.setProjects(new ArrayList<>());
        auditFilter.setLocale("en-GB");
        auditFilter.setPageSize(50);
        auditFilter.setPageNumber(1);
        auditFilter.setDateStart("2022-03-17");
        auditFilter.setDateEnd("2022-03-20");
        return auditFilter;
    }
}
