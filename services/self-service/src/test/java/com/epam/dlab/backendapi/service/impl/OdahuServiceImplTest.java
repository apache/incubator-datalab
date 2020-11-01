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

package com.epam.dlab.backendapi.service.impl;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.annotation.BudgetLimited;
import com.epam.dlab.backendapi.annotation.Project;
import com.epam.dlab.backendapi.dao.OdahuDAO;
import com.epam.dlab.backendapi.domain.EndpointDTO;
import com.epam.dlab.backendapi.domain.OdahuCreateDTO;
import com.epam.dlab.backendapi.domain.OdahuDTO;
import com.epam.dlab.backendapi.domain.ProjectDTO;
import com.epam.dlab.backendapi.domain.ProjectEndpointDTO;
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.service.EndpointService;
import com.epam.dlab.backendapi.service.ProjectService;
import com.epam.dlab.backendapi.util.RequestBuilder;
import com.epam.dlab.cloud.CloudProvider;
import com.epam.dlab.dto.UserInstanceStatus;
import com.epam.dlab.dto.base.edge.EdgeInfo;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.exceptions.ResourceConflictException;
import com.epam.dlab.rest.client.RESTService;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.*;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class OdahuServiceImplTest {

    private static final String USER = "testUser";
    private static final String TOKEN = "testToken";
    private static final String PROJECT = "testProject";
    private static final String END_POINT_URL = "https://localhsot:8080/";
    private static final String END_POINT_NAME = "endpoint";
    private static final String tag = "testTag";
    private static final String uuid = "34dsr324";
    private static final String ODAHU_NAME = "odahuTest";
    private static UserInfo userInfo;

    @Mock
    private OdahuDAO odahuDAO;

    @Mock
    private ProjectService projectService;

    @Mock
    private EndpointService endpointService;

    @Mock
    private RequestId requestId;

    @Mock
    private RESTService provisioningService;

    @Mock
    private RequestBuilder requestBuilder;

    @InjectMocks
    private OdahuServiceImpl odahuService;

    @BeforeClass
    public static void setUp(){
        userInfo = new UserInfo(USER, TOKEN);
    }

    @Test
    public void findOdahuTest() {
        List<OdahuDTO> odahuDTOList = odahuService.findOdahu();
        assertNotNull(odahuDTOList);
    }

    @Test
    public void createTest() {
        EndpointDTO endpointDTO = getEndpointDTO();
        ProjectDTO projectDTO = getProjectDTO();
        OdahuCreateDTO odahuCreateDTO = getOdahuCreateDTO();

        when(projectService.get(PROJECT)).thenReturn(projectDTO);
        when(odahuDAO.create(new OdahuDTO(odahuCreateDTO.getName(), odahuCreateDTO.getProject(),
                odahuCreateDTO.getEndpoint(), UserInstanceStatus.CREATING, getTags()))).thenReturn(true);
        when(endpointService.get(odahuCreateDTO.getEndpoint())).thenReturn(endpointDTO);
        when(requestId.put(userInfo.getName(), uuid)).thenReturn(uuid);

        odahuService.create(PROJECT, odahuCreateDTO, userInfo);

        verify(odahuDAO).findOdahuClusters(odahuCreateDTO.getProject(), odahuCreateDTO.getEndpoint());
        verify(odahuDAO).create(new OdahuDTO(odahuCreateDTO.getName(), odahuCreateDTO.getProject(),
                odahuCreateDTO.getEndpoint(), UserInstanceStatus.CREATING, getTags()));
        verify(endpointService).get(odahuCreateDTO.getEndpoint());
        verify(projectService).get(PROJECT);
        verify(provisioningService).post(END_POINT_URL + "infrastructure/odahu" , userInfo.getAccessToken(),
                requestBuilder.newOdahuCreate(userInfo, odahuCreateDTO, projectDTO, endpointDTO), String.class);
        verifyNoMoreInteractions(odahuDAO, provisioningService, endpointService, projectService);
    }

    @Test(expected = ResourceConflictException.class)
    public void createIfClusterActive() {
        OdahuCreateDTO odahuCreateDTO = getOdahuCreateDTO();
        OdahuDTO failedOdahu = getOdahuDTO(UserInstanceStatus.RUNNING);
        List<OdahuDTO> runningOdahuList = singletonList(failedOdahu);
        when(odahuDAO.findOdahuClusters(odahuCreateDTO.getProject(), odahuCreateDTO.getEndpoint())).thenReturn(runningOdahuList);

        odahuService.create(PROJECT, odahuCreateDTO, userInfo);
        verify(odahuDAO).findOdahuClusters(odahuCreateDTO.getProject(), odahuCreateDTO.getEndpoint());
        verifyNoMoreInteractions(odahuDAO);
    }

    @Test(expected = DlabException.class)
    public void createIfDBissue() {
        EndpointDTO endpointDTO = getEndpointDTO();
        ProjectDTO projectDTO = getProjectDTO();
        OdahuCreateDTO odahuCreateDTO = getOdahuCreateDTO();

        when(projectService.get(PROJECT)).thenReturn(projectDTO);
        when(odahuDAO.create(new OdahuDTO(odahuCreateDTO.getName(), odahuCreateDTO.getProject(),
                odahuCreateDTO.getEndpoint(), UserInstanceStatus.CREATING, getTags()))).thenReturn(false);
        when(endpointService.get(odahuCreateDTO.getEndpoint())).thenReturn(endpointDTO);
        when(requestId.put(userInfo.getName(), uuid)).thenReturn(uuid);

        odahuService.create(PROJECT, odahuCreateDTO, userInfo);

        verify(odahuDAO).findOdahuClusters(odahuCreateDTO.getProject(), odahuCreateDTO.getEndpoint());
        verify(odahuDAO).create(new OdahuDTO(odahuCreateDTO.getName(), odahuCreateDTO.getProject(),
                odahuCreateDTO.getEndpoint(), UserInstanceStatus.CREATING, getTags()));
        verify(endpointService).get(odahuCreateDTO.getEndpoint());
        verify(projectService).get(PROJECT);
        verify(provisioningService).post(END_POINT_URL + "infrastructure/odahu" , userInfo.getAccessToken(),
                requestBuilder.newOdahuCreate(userInfo, odahuCreateDTO, projectDTO, endpointDTO), String.class);
        verifyNoMoreInteractions(odahuDAO, provisioningService, endpointService, projectService);
    }

    @Test
    public void startTest() {
        when(endpointService.get(END_POINT_URL)).thenReturn(getEndpointDTO());

        odahuService.start(ODAHU_NAME, PROJECT, END_POINT_URL, userInfo);
        verify(endpointService).get(END_POINT_URL);
        verify(odahuDAO).updateStatus(ODAHU_NAME, PROJECT, END_POINT_URL, UserInstanceStatus.STARTING);
        verify(odahuDAO).getFields(ODAHU_NAME, PROJECT, END_POINT_URL);
        verifyNoMoreInteractions(endpointService, odahuDAO);
    }

    @Test
    public void stopTest() {
        when(endpointService.get(END_POINT_URL)).thenReturn(getEndpointDTO());

        odahuService.stop(ODAHU_NAME, PROJECT, END_POINT_URL, userInfo);

        verify(endpointService).get(END_POINT_URL);
        verify(odahuDAO).updateStatus(ODAHU_NAME, PROJECT, END_POINT_URL, UserInstanceStatus.STOPPING);
        verify(odahuDAO).getFields(ODAHU_NAME, PROJECT, END_POINT_URL);
        verifyNoMoreInteractions(endpointService, odahuDAO);
    }

    private Map<String, String> getTags() {
        Map<String, String> tags = new HashMap<>();
        tags.put("custom_tag", getOdahuCreateDTO().getCustomTag());
        tags.put("project_tag", getOdahuCreateDTO().getProject());
        tags.put("endpoint_tag", getOdahuCreateDTO().getEndpoint());
        return tags;
    }

    private EndpointDTO getEndpointDTO() {
        return new EndpointDTO(END_POINT_NAME, END_POINT_URL, "testAccount", tag, EndpointDTO.EndpointStatus.ACTIVE, CloudProvider.GCP);
    }

    private ProjectDTO getProjectDTO() {
        return new ProjectDTO(PROJECT,
                Collections.emptySet(),
                "ssh-testKey\n", tag, 200,
                singletonList(new ProjectEndpointDTO(END_POINT_NAME, UserInstanceStatus.RUNNING, new EdgeInfo())),
                true);
    }

    private OdahuDTO getOdahuDTO(UserInstanceStatus instanceStatus) {
        return new OdahuDTO(ODAHU_NAME, PROJECT, END_POINT_NAME, instanceStatus, getTags());
    }

    private OdahuCreateDTO getOdahuCreateDTO() {
        return new OdahuCreateDTO(ODAHU_NAME, PROJECT, END_POINT_URL, tag);
    }
}
