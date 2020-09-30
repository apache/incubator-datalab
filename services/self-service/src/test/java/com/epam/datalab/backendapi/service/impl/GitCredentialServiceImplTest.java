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

package com.epam.datalab.backendapi.service.impl;

import com.epam.datalab.auth.UserInfo;
import com.epam.datalab.backendapi.dao.ExploratoryDAO;
import com.epam.datalab.backendapi.dao.GitCredsDAO;
import com.epam.datalab.backendapi.domain.EndpointDTO;
import com.epam.datalab.backendapi.domain.RequestId;
import com.epam.datalab.backendapi.service.EndpointService;
import com.epam.datalab.backendapi.util.RequestBuilder;
import com.epam.datalab.cloud.CloudProvider;
import com.epam.datalab.dto.UserInstanceDTO;
import com.epam.datalab.dto.exploratory.ExploratoryGitCredsDTO;
import com.epam.datalab.dto.exploratory.ExploratoryGitCredsUpdateDTO;
import com.epam.datalab.exceptions.DatalabException;
import com.epam.datalab.rest.client.RESTService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class GitCredentialServiceImplTest {

    private final String USER = "test";

    @Mock
    private GitCredsDAO gitCredsDAO;
    @Mock
    private ExploratoryDAO exploratoryDAO;
    @Mock
    private RESTService provisioningService;
    @Mock
    private RequestBuilder requestBuilder;
    @Mock
    private RequestId requestId;
    @Mock
    private EndpointService endpointService;

    @InjectMocks
    private GitCredentialServiceImpl gitCredentialService;

    @Test
    public void updateGitCredentials() {
        String token = "token";
        UserInfo userInfo = new UserInfo(USER, token);
        doNothing().when(gitCredsDAO).updateGitCreds(anyString(), any(ExploratoryGitCredsDTO.class));
        when(endpointService.get(anyString())).thenReturn(endpointDTO());

        String exploratoryName = "explName";
        UserInstanceDTO uiDto = new UserInstanceDTO().withExploratoryName(exploratoryName).withUser(USER);
        when(exploratoryDAO.fetchRunningExploratoryFields(anyString())).thenReturn(Collections.singletonList(uiDto));

        ExploratoryGitCredsUpdateDTO egcuDto = new ExploratoryGitCredsUpdateDTO().withExploratoryName(exploratoryName);
        when(requestBuilder.newGitCredentialsUpdate(any(UserInfo.class), any(UserInstanceDTO.class), any(EndpointDTO.class),
                any(ExploratoryGitCredsDTO.class))).thenReturn(egcuDto);

        String uuid = "someUuid";
        when(provisioningService.post(anyString(), anyString(), any(ExploratoryGitCredsUpdateDTO.class), any()))
                .thenReturn(uuid);
        when(requestId.put(anyString(), anyString())).thenReturn(uuid);

        ExploratoryGitCredsDTO egcDto = new ExploratoryGitCredsDTO();
        gitCredentialService.updateGitCredentials(userInfo, egcDto);

        verify(gitCredsDAO).updateGitCreds(USER, egcDto);
        verify(exploratoryDAO).fetchRunningExploratoryFields(USER);
        verify(requestBuilder).newGitCredentialsUpdate(userInfo, uiDto, endpointDTO(), egcDto);
        verify(provisioningService).post(endpointDTO().getUrl() + "exploratory/git_creds", token, egcuDto,
                String.class);
        verify(requestId).put(USER, uuid);
        verifyNoMoreInteractions(gitCredsDAO, exploratoryDAO, requestBuilder, provisioningService, requestId);
    }

    @Test
    public void updateGitCredentialsWhenMethodUpdateGitCredsThrowsException() {
        String token = "token";
        UserInfo userInfo = new UserInfo(USER, token);
        doThrow(new NullPointerException())
                .when(gitCredsDAO).updateGitCreds(anyString(), any(ExploratoryGitCredsDTO.class));

        ExploratoryGitCredsDTO egcDto = new ExploratoryGitCredsDTO();
        try {
            gitCredentialService.updateGitCredentials(userInfo, egcDto);
        } catch (DatalabException e) {
            assertEquals("Cannot update the GIT credentials: null", e.getMessage());
        }

        verify(gitCredsDAO).updateGitCreds(USER, egcDto);
        verifyNoMoreInteractions(gitCredsDAO);
    }

    @Test
    public void updateGitCredentialsWithFailedNotebooks() {
        when(endpointService.get(anyString())).thenReturn(endpointDTO());
        String token = "token";
        UserInfo userInfo = new UserInfo(USER, token);
        doNothing().when(gitCredsDAO).updateGitCreds(anyString(), any(ExploratoryGitCredsDTO.class));

        String exploratoryName = "explName";
        UserInstanceDTO uiDto = new UserInstanceDTO().withExploratoryName(exploratoryName).withUser(USER);
        when(exploratoryDAO.fetchRunningExploratoryFields(anyString())).thenReturn(Collections.singletonList(uiDto));

        doThrow(new DatalabException("Cannot create instance of resource class "))
                .when(requestBuilder).newGitCredentialsUpdate(any(UserInfo.class), any(UserInstanceDTO.class),
                any(EndpointDTO.class), any(ExploratoryGitCredsDTO.class));

        ExploratoryGitCredsDTO egcDto = new ExploratoryGitCredsDTO();
        try {
            gitCredentialService.updateGitCredentials(userInfo, egcDto);
        } catch (DatalabException e) {
            assertEquals("Cannot update the GIT credentials: Requests for notebooks failed: explName",
                    e.getMessage());
        }

        verify(gitCredsDAO).updateGitCreds(USER, egcDto);
        verify(exploratoryDAO).fetchRunningExploratoryFields(USER);
        verify(requestBuilder).newGitCredentialsUpdate(userInfo, uiDto, endpointDTO(), egcDto);
        verifyNoMoreInteractions(gitCredsDAO, exploratoryDAO, requestBuilder);
    }

    @Test
    public void getGitCredentials() {
        ExploratoryGitCredsDTO expectedEgcDto = new ExploratoryGitCredsDTO();
        when(gitCredsDAO.findGitCreds(anyString(), anyBoolean())).thenReturn(expectedEgcDto);

        ExploratoryGitCredsDTO actualEgcDto = gitCredentialService.getGitCredentials(USER);
        assertNotNull(actualEgcDto);
        assertEquals(expectedEgcDto, actualEgcDto);

        verify(gitCredsDAO).findGitCreds(USER, true);
        verifyNoMoreInteractions(gitCredsDAO);
    }

    @Test
    public void getGitCredentialsWhenMethodFindGitCredsThrowsException() {
        doThrow(new NullPointerException()).when(gitCredsDAO).findGitCreds(anyString(), anyBoolean());
        try {
            gitCredentialService.getGitCredentials(USER);
        } catch (DatalabException e) {
            assertEquals("Cannot load GIT credentials for user test: null", e.getMessage());
        }
        verify(gitCredsDAO).findGitCreds(USER, true);
        verifyNoMoreInteractions(gitCredsDAO);
    }

    private EndpointDTO endpointDTO() {
        return new EndpointDTO("test", "url", "", null, EndpointDTO.EndpointStatus.ACTIVE, CloudProvider.AWS);
    }
}
