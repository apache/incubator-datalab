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
import com.epam.datalab.backendapi.dao.ComputationalDAO;
import com.epam.datalab.backendapi.dao.ExploratoryDAO;
import com.epam.datalab.backendapi.domain.RequestId;
import com.epam.datalab.backendapi.service.ExploratoryService;
import com.epam.datalab.backendapi.util.RequestBuilder;
import com.epam.datalab.dto.UserInstanceStatus;
import com.epam.datalab.dto.reuploadkey.ReuploadKeyCallbackDTO;
import com.epam.datalab.dto.reuploadkey.ReuploadKeyStatus;
import com.epam.datalab.dto.reuploadkey.ReuploadKeyStatusDTO;
import com.epam.datalab.model.ResourceData;
import com.epam.datalab.model.ResourceType;
import com.epam.datalab.rest.client.RESTService;
import com.mongodb.client.result.UpdateResult;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static com.epam.datalab.dto.UserInstanceStatus.RUNNING;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ReuploadKeyServiceImplTest {

    private final String USER = "test";
    private final String TOKEN = "token";
    private final String EXPLORATORY_NAME = "explName";

    private UserInfo userInfo;

    @Mock
    private RESTService provisioningService;
    @Mock
    private RequestBuilder requestBuilder;
    @Mock
    private RequestId requestId;
    @Mock
    private ExploratoryService exploratoryService;
    @Mock
    private ComputationalDAO computationalDAO;
    @Mock
    private ExploratoryDAO exploratoryDAO;

    @InjectMocks
    private ReuploadKeyServiceImpl reuploadKeyService;


    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() {
        userInfo = getUserInfo();
    }

    @Test
    public void updateResourceDataForEdgeWhenStatusCompleted() {
        ResourceData resource = new ResourceData(ResourceType.EDGE, "someId", null, null);
        ReuploadKeyStatusDTO dto = getReuploadKeyStatusDTO(resource, ReuploadKeyStatus.COMPLETED);

        reuploadKeyService.updateResourceData(dto);

        verifyZeroInteractions(exploratoryDAO, computationalDAO);
    }

    @Test
    public void updateResourceDataForEdgeWhenStatusFailed() {
        ResourceData resource = new ResourceData(ResourceType.EDGE, "someId", null, null);

        ReuploadKeyStatusDTO dto = getReuploadKeyStatusDTO(resource, ReuploadKeyStatus.FAILED);
        reuploadKeyService.updateResourceData(dto);

        verifyZeroInteractions(exploratoryDAO, computationalDAO);
    }

    @Test
    public void updateResourceDataForExploratoryWhenStatusCompleted() {
        ResourceData resource = new ResourceData(ResourceType.EXPLORATORY, "someId", EXPLORATORY_NAME, null);
        when(exploratoryDAO.updateStatusForExploratory(anyString(), anyString(), anyString(),
                any(UserInstanceStatus.class))).thenReturn(mock(UpdateResult.class));
        doNothing().when(exploratoryDAO).updateReuploadKeyForExploratory(anyString(), anyString(), anyString(), anyBoolean());

        ReuploadKeyStatusDTO dto = getReuploadKeyStatusDTO(resource, ReuploadKeyStatus.COMPLETED);

        reuploadKeyService.updateResourceData(dto);

        verify(exploratoryDAO).updateStatusForExploratory(USER, null, EXPLORATORY_NAME, RUNNING);
        verify(exploratoryDAO).updateReuploadKeyForExploratory(USER, null, EXPLORATORY_NAME, false);
        verifyNoMoreInteractions(exploratoryDAO);
        verifyZeroInteractions(computationalDAO);
    }

    @Test
    public void updateResourceDataForExploratoryWhenStatusFailed() {
        ResourceData resource = new ResourceData(ResourceType.EXPLORATORY, "someId", EXPLORATORY_NAME, null);
        when(exploratoryDAO.updateStatusForExploratory(anyString(), anyString(), anyString(),
                any(UserInstanceStatus.class))).thenReturn(mock(UpdateResult.class));

        ReuploadKeyStatusDTO dto = getReuploadKeyStatusDTO(resource, ReuploadKeyStatus.FAILED);

        reuploadKeyService.updateResourceData(dto);

        verify(exploratoryDAO).updateStatusForExploratory(USER, null, EXPLORATORY_NAME, RUNNING);
        verifyNoMoreInteractions(exploratoryDAO);
        verifyZeroInteractions(computationalDAO);
    }

    @Test
    public void updateResourceDataForClusterWhenStatusCompleted() {
        ResourceData resource = new ResourceData(ResourceType.COMPUTATIONAL, "someId", EXPLORATORY_NAME, "compName");
        doNothing().when(computationalDAO).updateStatusForComputationalResource(anyString(), anyString(), anyString(),
                anyString(), any(UserInstanceStatus.class));
        doNothing().when(computationalDAO).updateReuploadKeyFlagForComputationalResource(anyString(), anyString(),
                anyString(), anyString(), anyBoolean());
        ReuploadKeyStatusDTO dto = getReuploadKeyStatusDTO(resource, ReuploadKeyStatus.COMPLETED);

        reuploadKeyService.updateResourceData(dto);

        verify(computationalDAO).updateStatusForComputationalResource(USER, null, EXPLORATORY_NAME, "compName", RUNNING);
        verify(computationalDAO).updateReuploadKeyFlagForComputationalResource(USER, null, EXPLORATORY_NAME,
                "compName", false);
        verifyNoMoreInteractions(computationalDAO);
        verifyZeroInteractions(exploratoryDAO);
    }

    @Test
    public void updateResourceDataForClusterWhenStatusFailed() {
        ResourceData resource = new ResourceData(ResourceType.COMPUTATIONAL, "someId", EXPLORATORY_NAME, "compName");
        doNothing().when(computationalDAO).updateStatusForComputationalResource(anyString(), anyString(), anyString(),
                anyString(), any(UserInstanceStatus.class));
        ReuploadKeyStatusDTO dto = getReuploadKeyStatusDTO(resource, ReuploadKeyStatus.FAILED);

        reuploadKeyService.updateResourceData(dto);

        verify(computationalDAO).updateStatusForComputationalResource(USER, null, EXPLORATORY_NAME, "compName", RUNNING);
        verifyNoMoreInteractions(computationalDAO);
        verifyZeroInteractions(exploratoryDAO);
    }

    private UserInfo getUserInfo() {
        return new UserInfo(USER, TOKEN);
    }

    private ReuploadKeyStatusDTO getReuploadKeyStatusDTO(ResourceData resource, ReuploadKeyStatus status) {
        return new ReuploadKeyStatusDTO().withReuploadKeyCallbackDto(
                new ReuploadKeyCallbackDTO().withResource(resource)).withReuploadKeyStatus(status).withUser(USER);
    }

}
