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

package com.epam.dlab.backendapi.resources;

import com.epam.dlab.backendapi.domain.AuditCreateDTO;
import com.epam.dlab.backendapi.domain.AuditResourceTypeEnum;
import com.epam.dlab.backendapi.service.AuditService;
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

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.refEq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.when;

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
        final Response response;
        response = resources.getJerseyTest()
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

    private AuditCreateDTO prepareAuditCreateDTO() {
        return new AuditCreateDTO(RESOURCE, INFO,AuditResourceTypeEnum.COMPUTE);
    }
}
