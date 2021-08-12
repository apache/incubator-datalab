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

import com.epam.datalab.backendapi.resources.dto.UserRoleDTO;
import com.epam.datalab.backendapi.service.UserRoleService;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.refEq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class UserRoleResourceTest extends TestBase {


    private static final String USER = "user";
    private static final String ROLE_ID = "id";

    private UserRoleService rolesService = mock(UserRoleService.class);

    @Before
    public void setup() throws AuthenticationException {
        authSetup();
    }

    @Rule
    public final ResourceTestRule resources =
            getResourceTestRuleInstance(new UserRoleResource(rolesService));


    @Test
    public void getRoles() {
        when(rolesService.getUserRoles()).thenReturn(Collections.singletonList(getUserRole()));

        final Response response = resources.getJerseyTest()
                .target("/role")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .get();

        final List<UserRoleDTO> actualRoles = response.readEntity(new GenericType<List<UserRoleDTO>>() {
        });

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertEquals(ROLE_ID, actualRoles.get(0).getId());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(rolesService).getUserRoles();
        verifyNoMoreInteractions(rolesService);
    }

    @Test
    public void createRole() {

        final Response response = resources.getJerseyTest()
                .target("/role")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .post(Entity.json(getUserRole()));

        assertEquals(HttpStatus.SC_OK, response.getStatus());

        verify(rolesService).createRole(refEq(getUserRole()));
        verifyNoMoreInteractions(rolesService);
    }

    private UserRoleDTO getUserRole() {
        final UserRoleDTO userRoleDto = new UserRoleDTO();
        userRoleDto.setId(ROLE_ID);
        return userRoleDto;
    }

}