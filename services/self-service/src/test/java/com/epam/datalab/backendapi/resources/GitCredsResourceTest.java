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
import com.epam.datalab.backendapi.service.GitCredentialService;
import com.epam.datalab.dto.exploratory.ExploratoryGitCreds;
import com.epam.datalab.dto.exploratory.ExploratoryGitCredsDTO;
import com.epam.datalab.exceptions.DatalabException;
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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.refEq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class GitCredsResourceTest extends TestBase {

    private GitCredentialService gitCredentialService = mock(GitCredentialService.class);

    @Rule
    public final ResourceTestRule resources = getResourceTestRuleInstance(new GitCredsResource(gitCredentialService));

    @Before
    public void setup() throws AuthenticationException {
        authSetup();
    }

    @Test
    public void updateGitCreds() {
        doNothing().when(gitCredentialService).updateGitCredentials(any(UserInfo.class),
                any(ExploratoryGitCredsDTO.class));
        final Response response = resources.getJerseyTest()
                .target("/user/git_creds")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .put(Entity.json(getExploratoryGitCredsDTO()));

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertNull(response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(gitCredentialService)
                .updateGitCredentials(refEq(getUserInfo()), refEq(getExploratoryGitCredsDTO(), "self"));
        verifyNoMoreInteractions(gitCredentialService);
    }

    @Test
    public void updateGitCredsWithFailedAuth() throws AuthenticationException {
        authFailSetup();
        doNothing().when(gitCredentialService).updateGitCredentials(any(UserInfo.class),
                any(ExploratoryGitCredsDTO.class));
        final Response response = resources.getJerseyTest()
                .target("/user/git_creds")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .put(Entity.json(getExploratoryGitCredsDTO()));

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertNull(response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(gitCredentialService)
                .updateGitCredentials(refEq(getUserInfo()), refEq(getExploratoryGitCredsDTO(), "self"));
        verifyNoMoreInteractions(gitCredentialService);
    }

    @Test
    public void updateGitCredsWithException() {
        doThrow(new DatalabException("Cannot update the GIT credentials")).when(gitCredentialService)
                .updateGitCredentials(any(UserInfo.class), any(ExploratoryGitCredsDTO.class));
        final Response response = resources.getJerseyTest()
                .target("/user/git_creds")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .put(Entity.json(getExploratoryGitCredsDTO()));

        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(gitCredentialService).updateGitCredentials(refEq(getUserInfo()),
                refEq(getExploratoryGitCredsDTO(), "self"));
        verifyNoMoreInteractions(gitCredentialService);
    }

    @Test
    public void getGitCreds() {
        ExploratoryGitCredsDTO egcDto = getExploratoryGitCredsDTO();
        when(gitCredentialService.getGitCredentials(anyString())).thenReturn(egcDto);
        final Response response = resources.getJerseyTest()
                .target("/user/git_creds")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .get();

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertEquals(egcDto.getGitCreds(), response.readEntity(ExploratoryGitCredsDTO.class).getGitCreds());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(gitCredentialService).getGitCredentials(USER.toLowerCase());
        verifyNoMoreInteractions(gitCredentialService);
    }

    @Test
    public void getGitCredsWithFailedAuth() throws AuthenticationException {
        authFailSetup();
        ExploratoryGitCredsDTO egcDto = getExploratoryGitCredsDTO();
        when(gitCredentialService.getGitCredentials(anyString())).thenReturn(egcDto);
        final Response response = resources.getJerseyTest()
                .target("/user/git_creds")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .get();

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertEquals(egcDto.getGitCreds(), response.readEntity(ExploratoryGitCredsDTO.class).getGitCreds());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(gitCredentialService).getGitCredentials(USER.toLowerCase());
        verifyNoMoreInteractions(gitCredentialService);
    }

    @Test
    public void getGitCredsWithException() {
        doThrow(new DatalabException("Cannot load GIT credentials for user"))
                .when(gitCredentialService).getGitCredentials(anyString());
        final Response response = resources.getJerseyTest()
                .target("/user/git_creds")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .get();

        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(gitCredentialService).getGitCredentials(USER.toLowerCase());
        verifyNoMoreInteractions(gitCredentialService);
    }

    private ExploratoryGitCredsDTO getExploratoryGitCredsDTO() {
        ExploratoryGitCredsDTO exploratoryGitCredsDTO = new ExploratoryGitCredsDTO();
        final ExploratoryGitCreds exploratoryGitCreds = new ExploratoryGitCreds();
        exploratoryGitCreds.setHostname("host");
        exploratoryGitCredsDTO.setGitCreds(Collections.singletonList(exploratoryGitCreds));
        return exploratoryGitCredsDTO;
    }
}
