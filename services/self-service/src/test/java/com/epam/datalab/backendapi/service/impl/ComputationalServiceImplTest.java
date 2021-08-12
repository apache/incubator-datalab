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
import com.epam.datalab.backendapi.conf.SelfServiceApplicationConfiguration;
import com.epam.datalab.backendapi.dao.ComputationalDAO;
import com.epam.datalab.backendapi.dao.ExploratoryDAO;
import com.epam.datalab.backendapi.domain.EndpointDTO;
import com.epam.datalab.backendapi.domain.ProjectDTO;
import com.epam.datalab.backendapi.domain.ProjectEndpointDTO;
import com.epam.datalab.backendapi.domain.RequestId;
import com.epam.datalab.backendapi.resources.dto.ComputationalCreateFormDTO;
import com.epam.datalab.backendapi.resources.dto.SparkStandaloneClusterCreateForm;
import com.epam.datalab.backendapi.service.EndpointService;
import com.epam.datalab.backendapi.service.ProjectService;
import com.epam.datalab.backendapi.service.TagService;
import com.epam.datalab.backendapi.util.RequestBuilder;
import com.epam.datalab.cloud.CloudProvider;
import com.epam.datalab.dto.SchedulerJobDTO;
import com.epam.datalab.dto.UserInstanceDTO;
import com.epam.datalab.dto.UserInstanceStatus;
import com.epam.datalab.dto.aws.computational.ClusterConfig;
import com.epam.datalab.dto.base.computational.ComputationalBase;
import com.epam.datalab.dto.base.edge.EdgeInfo;
import com.epam.datalab.dto.computational.ComputationalClusterConfigDTO;
import com.epam.datalab.dto.computational.ComputationalStartDTO;
import com.epam.datalab.dto.computational.ComputationalStatusDTO;
import com.epam.datalab.dto.computational.ComputationalStopDTO;
import com.epam.datalab.dto.computational.ComputationalTerminateDTO;
import com.epam.datalab.dto.computational.SparkStandaloneClusterResource;
import com.epam.datalab.dto.computational.UserComputationalResource;
import com.epam.datalab.exceptions.DatalabException;
import com.epam.datalab.exceptions.ResourceNotFoundException;
import com.epam.datalab.rest.client.RESTService;
import com.epam.datalab.rest.contracts.ComputationalAPI;
import com.mongodb.client.result.UpdateResult;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.epam.datalab.dto.UserInstanceStatus.CREATING;
import static com.epam.datalab.dto.UserInstanceStatus.RUNNING;
import static com.epam.datalab.dto.UserInstanceStatus.STOPPED;
import static com.epam.datalab.rest.contracts.ComputationalAPI.AUDIT_COMPUTATIONAL_RECONFIGURE_MESSAGE;
import static com.epam.datalab.rest.contracts.ComputationalAPI.AUDIT_MESSAGE;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyBoolean;
import static org.mockito.Mockito.anyListOf;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.refEq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;


@RunWith(MockitoJUnitRunner.class)
public class ComputationalServiceImplTest {

    private static final long MAX_INACTIVITY = 10L;
    private static final String DOCKER_DATALAB_DATAENGINE = "docker.datalab-dataengine";
    private static final String DOCKER_DATALAB_DATAENGINE_SERVICE = "docker.datalab-dataengine-service";
    private static final String COMP_ID = "compId";
    private final String USER = "test";
    private final String TOKEN = "token";
    private final String EXPLORATORY_NAME = "expName";
    private final String PROJECT = "project";
    private final String COMP_NAME = "compName";
    private final String NOTE_BOOK_NAME = "notebookName";
    private final String UUID = "1234-56789765-4321";
    private final LocalDateTime LAST_ACTIVITY = LocalDateTime.now().minusMinutes(MAX_INACTIVITY);

    private UserInfo userInfo;
    private List<ComputationalCreateFormDTO> formList;
    private UserInstanceDTO userInstance;
    private ComputationalStatusDTO computationalStatusDTOWithStatusTerminating;
    private ComputationalStatusDTO computationalStatusDTOWithStatusFailed;
    private ComputationalStatusDTO computationalStatusDTOWithStatusStopping;
    private ComputationalStatusDTO computationalStatusDTOWithStatusStarting;
    private SparkStandaloneClusterResource sparkClusterResource;
    private UserComputationalResource ucResource;

    @Mock
    private ProjectService projectService;
    @Mock
    private ExploratoryDAO exploratoryDAO;
    @Mock
    private ComputationalDAO computationalDAO;
    @Mock
    private RESTService provisioningService;
    @Mock
    private SelfServiceApplicationConfiguration configuration;
    @Mock
    private RequestBuilder requestBuilder;
    @Mock
    private RequestId requestId;
    @Mock
    private TagService tagService;
    @Mock
    private EndpointService endpointService;

    @InjectMocks
    private ComputationalServiceImpl computationalService;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() {
        userInfo = getUserInfo();
        userInstance = getUserInstanceDto();
        formList = getFormList();
        computationalStatusDTOWithStatusTerminating = getComputationalStatusDTOWithStatus("terminating");
        computationalStatusDTOWithStatusFailed = getComputationalStatusDTOWithStatus("failed");
        computationalStatusDTOWithStatusStopping = getComputationalStatusDTOWithStatus("stopping");
        computationalStatusDTOWithStatusStarting = getComputationalStatusDTOWithStatus("starting");
        sparkClusterResource = getSparkClusterResource();
        ucResource = getUserComputationalResource(STOPPED, DOCKER_DATALAB_DATAENGINE);
    }

    @Test
    public void createSparkCluster() {
        ProjectDTO projectDTO = getProjectDTO();
        when(projectService.get(anyString())).thenReturn(projectDTO);
        when(endpointService.get(anyString())).thenReturn(endpointDTO());
        when(computationalDAO.addComputational(anyString(), anyString(), anyString(),
                any(SparkStandaloneClusterResource.class))).thenReturn(true);
        when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString(), anyString())).thenReturn(userInstance);

        ComputationalBase compBaseMocked = mock(ComputationalBase.class);
        when(requestBuilder.newComputationalCreate(any(UserInfo.class), any(ProjectDTO.class),
                any(UserInstanceDTO.class), any(SparkStandaloneClusterCreateForm.class), any(EndpointDTO.class)))
                .thenReturn(compBaseMocked);
        when(provisioningService.post(anyString(), anyString(), any(ComputationalBase.class), any())).thenReturn(UUID);
        when(requestId.put(anyString(), anyString())).thenReturn(UUID);

        SparkStandaloneClusterCreateForm form = (SparkStandaloneClusterCreateForm) formList.get(0);
        boolean creationResult = computationalService.createSparkCluster(userInfo, form.getName(), form, PROJECT,
                String.format(AUDIT_MESSAGE, form.getNotebookName()));
        assertTrue(creationResult);

        verify(projectService).get(PROJECT);
        verify(computationalDAO).addComputational(eq(USER), eq(EXPLORATORY_NAME), eq(PROJECT), refEq(sparkClusterResource));

        verify(exploratoryDAO).fetchExploratoryFields(USER, PROJECT, EXPLORATORY_NAME);
        verify(requestBuilder).newComputationalCreate(
                refEq(userInfo), refEq(projectDTO), refEq(userInstance), refEq(form), refEq(endpointDTO()));

        verify(provisioningService)
                .post(endpointDTO().getUrl() + ComputationalAPI.COMPUTATIONAL_CREATE_SPARK, TOKEN, compBaseMocked,
                        String.class);

        verify(requestId).put(USER, UUID);
        verifyNoMoreInteractions(projectService, configuration, computationalDAO, requestBuilder, provisioningService, requestId);
    }

    @Test
    public void createSparkClusterWhenResourceAlreadyExists() {
        when(computationalDAO.addComputational(anyString(), anyString(), anyString(),
                any(SparkStandaloneClusterResource.class))).thenReturn(false);
        when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString(), anyString())).thenReturn(userInstance);

        SparkStandaloneClusterCreateForm form = (SparkStandaloneClusterCreateForm) formList.get(0);
        boolean creationResult = computationalService.createSparkCluster(userInfo, form.getName(), form, PROJECT,
                String.format(AUDIT_MESSAGE, form.getNotebookName()));
        assertFalse(creationResult);
        verify(computationalDAO).addComputational(eq(USER), eq(EXPLORATORY_NAME), eq(PROJECT), refEq(sparkClusterResource));
        verifyNoMoreInteractions(configuration, computationalDAO);
    }

    @Test
    public void createSparkClusterWhenMethodFetchExploratoryFieldsThrowsException() {
        when(computationalDAO.addComputational(anyString(), anyString(), anyString(),
                any(SparkStandaloneClusterResource.class))).thenReturn(true);
        doThrow(new ResourceNotFoundException("Exploratory for user with name not found"))
                .when(exploratoryDAO).fetchExploratoryFields(anyString(), anyString(), anyString());

        when(computationalDAO.updateComputationalStatus(any(ComputationalStatusDTO.class)))
                .thenReturn(mock(UpdateResult.class));

        SparkStandaloneClusterCreateForm form = (SparkStandaloneClusterCreateForm) formList.get(0);
        try {
            computationalService.createSparkCluster(userInfo, form.getName(), form, PROJECT, String.format(AUDIT_MESSAGE, form.getNotebookName()));
        } catch (ResourceNotFoundException e) {
            assertEquals("Exploratory for user with name not found", e.getMessage());
        }

        verify(computationalDAO, never()).addComputational(USER, EXPLORATORY_NAME, PROJECT, sparkClusterResource);
        verify(computationalDAO, never()).updateComputationalStatus(refEq(computationalStatusDTOWithStatusFailed,
                "self"));
        verify(exploratoryDAO).fetchExploratoryFields(USER, PROJECT, EXPLORATORY_NAME);
        verifyNoMoreInteractions(configuration, computationalDAO, exploratoryDAO);
    }

    @Test
    public void createSparkClusterWhenMethodNewComputationalCreateThrowsException() {
        ProjectDTO projectDTO = getProjectDTO();
        when(endpointService.get(anyString())).thenReturn(endpointDTO());
        when(projectService.get(anyString())).thenReturn(projectDTO);
        when(computationalDAO.addComputational(anyString(), anyString(), anyString(),
                any(SparkStandaloneClusterResource.class))).thenReturn(true);
        when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString(), anyString())).thenReturn(userInstance);

        doThrow(new DatalabException("Cannot create instance of resource class "))
                .when(requestBuilder).newComputationalCreate(any(UserInfo.class), any(ProjectDTO.class),
                any(UserInstanceDTO.class), any(SparkStandaloneClusterCreateForm.class), any(EndpointDTO.class));

        when(computationalDAO.updateComputationalStatus(any(ComputationalStatusDTO.class)))
                .thenReturn(mock(UpdateResult.class));

        SparkStandaloneClusterCreateForm form = (SparkStandaloneClusterCreateForm) formList.get(0);
        try {
            computationalService.createSparkCluster(userInfo, form.getName(),
                    form, PROJECT, String.format(AUDIT_MESSAGE, form.getNotebookName()));
        } catch (DatalabException e) {
            assertEquals("Cannot create instance of resource class ", e.getMessage());
        }
        verify(projectService).get(PROJECT);
        verify(computationalDAO).addComputational(USER, EXPLORATORY_NAME, PROJECT, sparkClusterResource);
        verify(computationalDAO).updateComputationalStatus(refEq(computationalStatusDTOWithStatusFailed, "self"));
        verify(exploratoryDAO).fetchExploratoryFields(USER, PROJECT, EXPLORATORY_NAME);
        verify(requestBuilder).newComputationalCreate(userInfo, projectDTO, userInstance, form, endpointDTO());
        verifyNoMoreInteractions(projectService, configuration, computationalDAO, exploratoryDAO, requestBuilder);
    }

    @Test
    public void terminateComputationalEnvironment() {
        when(endpointService.get(anyString())).thenReturn(endpointDTO());
        when(computationalDAO.updateComputationalStatus(any(ComputationalStatusDTO.class)))
                .thenReturn(mock(UpdateResult.class));
        String explId = "explId";
        when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString(), anyString())).thenReturn(userInstance);

        String compId = "compId";
        UserComputationalResource ucResource = new UserComputationalResource();
        ucResource.setComputationalName(COMP_NAME);
        ucResource.setImageName("dataengine-service");
        ucResource.setComputationalId(compId);
        when(computationalDAO.fetchComputationalFields(anyString(), anyString(), anyString(), anyString())).thenReturn(ucResource);

        ComputationalTerminateDTO ctDto = new ComputationalTerminateDTO();
        ctDto.setComputationalName(COMP_NAME);
        ctDto.setExploratoryName(EXPLORATORY_NAME);
        when(requestBuilder.newComputationalTerminate(anyString(), any(UserInstanceDTO.class),
                any(UserComputationalResource.class), any(EndpointDTO.class))).thenReturn(ctDto);

        when(provisioningService.post(anyString(), anyString(), any(ComputationalTerminateDTO.class), any()))
                .thenReturn(UUID);
        when(requestId.put(anyString(), anyString())).thenReturn(UUID);

        computationalService.terminateComputational(userInfo, userInfo.getName(), PROJECT, EXPLORATORY_NAME, COMP_NAME, AUDIT_MESSAGE);

        verify(computationalDAO).updateComputationalStatus(refEq(computationalStatusDTOWithStatusTerminating, "self"));
        verify(computationalDAO).fetchComputationalFields(USER, PROJECT, EXPLORATORY_NAME, COMP_NAME);

        verify(requestBuilder).newComputationalTerminate(userInfo.getName(), userInstance, ucResource, endpointDTO());

        verify(provisioningService).post(endpointDTO().getUrl() + ComputationalAPI.COMPUTATIONAL_TERMINATE_CLOUD_SPECIFIC, TOKEN, ctDto,
                String.class);

        verify(requestId).put(USER, UUID);
        verify(exploratoryDAO).fetchExploratoryFields(USER, PROJECT, EXPLORATORY_NAME);
        verifyNoMoreInteractions(computationalDAO, exploratoryDAO, requestBuilder, provisioningService, requestId);
    }

    @Test
    public void terminateComputationalEnvironmentWhenMethodUpdateComputationalStatusThrowsException() {
        doThrow(new DatalabException("Could not update computational resource status"))
                .when(computationalDAO).updateComputationalStatus(refEq(computationalStatusDTOWithStatusTerminating,
                "self"));

        when(computationalDAO.updateComputationalStatus(refEq(computationalStatusDTOWithStatusFailed, "self")))
                .thenReturn(mock(UpdateResult.class));

        try {
            computationalService.terminateComputational(userInfo, userInfo.getName(), PROJECT, EXPLORATORY_NAME, COMP_NAME, AUDIT_MESSAGE);
        } catch (DatalabException e) {
            assertEquals("Could not update computational resource status", e.getMessage());
        }

        verify(computationalDAO).updateComputationalStatus(refEq(computationalStatusDTOWithStatusTerminating, "self"));
        verify(computationalDAO).updateComputationalStatus(refEq(computationalStatusDTOWithStatusFailed, "self"));
        verifyNoMoreInteractions(computationalDAO);
    }

    @Test
    public void terminateComputationalEnvironmentWhenMethodFetchComputationalFieldsThrowsException() {
        when(computationalDAO.updateComputationalStatus(any(ComputationalStatusDTO.class)))
                .thenReturn(mock(UpdateResult.class));

        doThrow(new DatalabException("Computational resource for user with exploratory name not found."))
                .when(computationalDAO).fetchComputationalFields(anyString(), anyString(), anyString(), anyString());
        when(computationalDAO.updateComputationalStatus(any(ComputationalStatusDTO.class)))
                .thenReturn(mock(UpdateResult.class));

        try {
            computationalService.terminateComputational(userInfo, userInfo.getName(), PROJECT, EXPLORATORY_NAME, COMP_NAME, AUDIT_MESSAGE);
        } catch (DatalabException e) {
            assertEquals("Computational resource for user with exploratory name not found.", e.getMessage());
        }

        verify(computationalDAO).updateComputationalStatus(refEq(computationalStatusDTOWithStatusTerminating, "self"));
        verify(computationalDAO).fetchComputationalFields(USER, PROJECT, EXPLORATORY_NAME, COMP_NAME);
        verify(computationalDAO).updateComputationalStatus(refEq(computationalStatusDTOWithStatusFailed, "self"));
        verify(exploratoryDAO).fetchExploratoryFields(USER, PROJECT, EXPLORATORY_NAME);
        verifyNoMoreInteractions(computationalDAO, exploratoryDAO);
    }

    @Test
    public void terminateComputationalEnvironmentWhenMethodNewComputationalTerminateThrowsException() {
        when(endpointService.get(anyString())).thenReturn(endpointDTO());
        when(computationalDAO.updateComputationalStatus(any(ComputationalStatusDTO.class)))
                .thenReturn(mock(UpdateResult.class));
        when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString(), anyString())).thenReturn(userInstance);

        String compId = "compId";
        UserComputationalResource ucResource = new UserComputationalResource();
        ucResource.setComputationalName(COMP_NAME);
        ucResource.setImageName("dataengine-service");
        ucResource.setComputationalId(compId);
        when(computationalDAO.fetchComputationalFields(anyString(), anyString(), anyString(), anyString())).thenReturn(ucResource);

        doThrow(new DatalabException("Cannot create instance of resource class "))
                .when(requestBuilder).newComputationalTerminate(anyString(), any(UserInstanceDTO.class),
                any(UserComputationalResource.class), any(EndpointDTO.class));

        when(computationalDAO.updateComputationalStatus(any(ComputationalStatusDTO.class)))
                .thenReturn(mock(UpdateResult.class));

        try {
            computationalService.terminateComputational(userInfo, userInfo.getName(), PROJECT, EXPLORATORY_NAME, COMP_NAME, AUDIT_MESSAGE);
        } catch (DatalabException e) {
            assertEquals("Cannot create instance of resource class ", e.getMessage());
        }

        verify(computationalDAO).updateComputationalStatus(refEq(computationalStatusDTOWithStatusTerminating, "self"));
        verify(computationalDAO).fetchComputationalFields(USER, PROJECT, EXPLORATORY_NAME, COMP_NAME);

        verify(exploratoryDAO).fetchExploratoryFields(USER, PROJECT, EXPLORATORY_NAME);

        verify(requestBuilder).newComputationalTerminate(userInfo.getName(), userInstance, ucResource, endpointDTO());
        verify(computationalDAO).updateComputationalStatus(refEq(computationalStatusDTOWithStatusFailed, "self"));
        verifyNoMoreInteractions(computationalDAO, exploratoryDAO, requestBuilder);
    }

    @Test
    public void createDataEngineService() {
        ProjectDTO projectDTO = getProjectDTO();
        when(projectService.get(anyString())).thenReturn(projectDTO);
        when(endpointService.get(anyString())).thenReturn(endpointDTO());
        when(computationalDAO.addComputational(anyString(), anyString(), anyString(), any(UserComputationalResource.class)))
                .thenReturn(true);
        when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString(), anyString())).thenReturn(userInstance);

        ComputationalBase compBaseMocked = mock(ComputationalBase.class);
        when(requestBuilder.newComputationalCreate(any(UserInfo.class), any(ProjectDTO.class),
                any(UserInstanceDTO.class), any(ComputationalCreateFormDTO.class), any(EndpointDTO.class)))
                .thenReturn(compBaseMocked);

        when(provisioningService.post(anyString(), anyString(), any(ComputationalBase.class), any())).thenReturn(UUID);
        when(requestId.put(anyString(), anyString())).thenReturn(UUID);

        ComputationalCreateFormDTO form = formList.get(1);
        boolean creationResult = computationalService.createDataEngineService(userInfo, form.getName(), form, ucResource, PROJECT,
                String.format(AUDIT_MESSAGE, form.getNotebookName()));
        assertTrue(creationResult);

        verify(projectService).get(PROJECT);

        verify(computationalDAO).addComputational(eq(USER), eq(EXPLORATORY_NAME), eq(PROJECT), refEq(ucResource));

        verify(exploratoryDAO).fetchExploratoryFields(USER, PROJECT, EXPLORATORY_NAME);

        verify(requestBuilder).newComputationalCreate(
                refEq(userInfo), refEq(projectDTO), refEq(userInstance), any(ComputationalCreateFormDTO.class), refEq(endpointDTO()));

        verify(provisioningService)
                .post(endpointDTO().getUrl() + ComputationalAPI.COMPUTATIONAL_CREATE_CLOUD_SPECIFIC, TOKEN,
                        compBaseMocked, String.class);

        verify(requestId).put(USER, UUID);
        verifyNoMoreInteractions(projectService, computationalDAO, exploratoryDAO, requestBuilder, provisioningService, requestId);
    }

    @Test
    public void createDataEngineServiceWhenComputationalResourceNotAdded() {
        when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString(), anyString())).thenReturn(userInstance);
        when(computationalDAO.addComputational(anyString(), anyString(), any(), any(UserComputationalResource.class)))
                .thenReturn(false);

        ComputationalCreateFormDTO form = formList.get(1);
        boolean creationResult = computationalService.createDataEngineService(userInfo, form.getName(), form, ucResource, PROJECT,
                String.format(AUDIT_MESSAGE, form.getNotebookName()));
        assertFalse(creationResult);

        verify(computationalDAO).addComputational(eq(USER), eq(EXPLORATORY_NAME), eq(PROJECT), refEq(ucResource));
        verifyNoMoreInteractions(computationalDAO);
    }

    @Test
    public void createDataEngineServiceWhenMethodFetchExploratoryFieldsThrowsException() {
        when(computationalDAO.addComputational(anyString(), anyString(), anyString(), any(UserComputationalResource.class)))
                .thenReturn(true);
        doThrow(new ResourceNotFoundException("Exploratory for user with name not found"))
                .when(exploratoryDAO).fetchExploratoryFields(anyString(), anyString(), anyString());

        when(computationalDAO.updateComputationalStatus(any(ComputationalStatusDTO.class)))
                .thenReturn(mock(UpdateResult.class));

        ComputationalCreateFormDTO form = formList.get(1);
        try {
            computationalService.createDataEngineService(userInfo, form.getName(), form, ucResource, PROJECT,
                    String.format(AUDIT_MESSAGE, form.getNotebookName()));
        } catch (DatalabException e) {
            assertEquals("Exploratory for user with name not found", e.getMessage());
        }

        verify(computationalDAO, never())
                .addComputational(eq(USER), eq(EXPLORATORY_NAME), eq(PROJECT), refEq(ucResource));

        verify(exploratoryDAO).fetchExploratoryFields(USER, PROJECT, EXPLORATORY_NAME);

        verify(computationalDAO, never()).updateComputationalStatus(refEq(computationalStatusDTOWithStatusFailed,
                "self"));
        verifyNoMoreInteractions(computationalDAO, exploratoryDAO);
    }

    @Test
    public void createDataEngineServiceWhenMethodNewComputationalCreateThrowsException() {
        ProjectDTO projectDTO = getProjectDTO();
        when(projectService.get(anyString())).thenReturn(projectDTO);
        when(endpointService.get(anyString())).thenReturn(endpointDTO());
        when(computationalDAO.addComputational(anyString(), anyString(), any(), any(UserComputationalResource.class)))
                .thenReturn(true);
        when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString(), anyString())).thenReturn(userInstance);

        doThrow(new DatalabException("Cannot create instance of resource class "))
                .when(requestBuilder).newComputationalCreate(any(UserInfo.class), any(ProjectDTO.class),
                any(UserInstanceDTO.class), any(ComputationalCreateFormDTO.class), any(EndpointDTO.class));

        when(computationalDAO.updateComputationalStatus(any(ComputationalStatusDTO.class)))
                .thenReturn(mock(UpdateResult.class));

        ComputationalCreateFormDTO form = formList.get(1);
        try {
            computationalService.createDataEngineService(userInfo, form.getName(), form, ucResource, PROJECT,
                    String.format(AUDIT_MESSAGE, form.getNotebookName()));
        } catch (DatalabException e) {
            assertEquals("Could not send request for creation the computational resource compName: " +
                    "Cannot create instance of resource class ", e.getMessage());
        }

        verify(projectService).get(PROJECT);
        verify(computationalDAO).addComputational(eq(USER), eq(EXPLORATORY_NAME), eq(PROJECT), refEq(ucResource));
        verify(exploratoryDAO).fetchExploratoryFields(USER, PROJECT, EXPLORATORY_NAME);
        verify(requestBuilder).newComputationalCreate(
                refEq(userInfo), refEq(projectDTO), refEq(userInstance), refEq(form), refEq(endpointDTO()));
        verify(computationalDAO).updateComputationalStatus(refEq(computationalStatusDTOWithStatusFailed, "self"));

        verifyNoMoreInteractions(projectService, computationalDAO, exploratoryDAO, requestBuilder);
    }

    @Test
    public void stopSparkCluster() {
        final UserInstanceDTO exploratory = getUserInstanceDto();
        exploratory.setResources(singletonList(getUserComputationalResource(RUNNING, DOCKER_DATALAB_DATAENGINE)));
        when(endpointService.get(anyString())).thenReturn(endpointDTO());
        when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString(), anyString(), anyBoolean())).thenReturn(exploratory);
        when(computationalDAO.updateComputationalStatus(any(ComputationalStatusDTO.class)))
                .thenReturn(mock(UpdateResult.class));

        ComputationalStopDTO computationalStopDTO = new ComputationalStopDTO();
        when(requestBuilder.newComputationalStop(anyString(), any(UserInstanceDTO.class), anyString(),
                any(EndpointDTO.class))).thenReturn(computationalStopDTO);
        when(provisioningService.post(anyString(), anyString(), any(ComputationalBase.class), any()))
                .thenReturn("someUuid");
        when(requestId.put(anyString(), anyString())).thenReturn("someUuid");

        computationalService.stopSparkCluster(userInfo, userInfo.getName(), PROJECT, EXPLORATORY_NAME, COMP_NAME, String.format(AUDIT_MESSAGE, EXPLORATORY_NAME));

        verify(computationalDAO).updateComputationalStatus(refEq(computationalStatusDTOWithStatusStopping, "self"));
        verify(exploratoryDAO).fetchExploratoryFields(USER, PROJECT, EXPLORATORY_NAME, true);
        verify(requestBuilder).newComputationalStop(eq(userInfo.getName()), refEq(exploratory), eq(COMP_NAME), refEq(endpointDTO()));
        verify(provisioningService)
                .post(eq(endpointDTO().getUrl() + "computational/stop/spark"), eq(TOKEN), refEq(computationalStopDTO),
                        eq(String.class));
        verify(requestId).put(USER, "someUuid");
        verifyNoMoreInteractions(computationalDAO, exploratoryDAO, requestBuilder,
                provisioningService, requestId);
    }

    @Test
    public void stopSparkClusterWhenDataengineTypeIsAnother() {
        final UserInstanceDTO exploratory = getUserInstanceDto();
        exploratory.setResources(singletonList(getUserComputationalResource(RUNNING, DOCKER_DATALAB_DATAENGINE_SERVICE)));
        when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString(), anyString(), anyBoolean())).thenReturn(exploratory);
        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("There is no running dataengine compName for exploratory expName");

        computationalService.stopSparkCluster(userInfo, userInfo.getName(), PROJECT, EXPLORATORY_NAME, COMP_NAME, String.format(AUDIT_MESSAGE, EXPLORATORY_NAME));
    }

    @Test
    public void startSparkCluster() {
        when(endpointService.get(anyString())).thenReturn(endpointDTO());
        final UserInstanceDTO exploratory = getUserInstanceDto();
        exploratory.setResources(singletonList(getUserComputationalResource(STOPPED, DOCKER_DATALAB_DATAENGINE)));
        when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString(), anyString(), anyBoolean())).thenReturn(exploratory);
        when(computationalDAO.updateComputationalStatus(any(ComputationalStatusDTO.class)))
                .thenReturn(mock(UpdateResult.class));

        ComputationalStartDTO computationalStartDTO = new ComputationalStartDTO();
        when(requestBuilder.newComputationalStart(any(UserInfo.class), any(UserInstanceDTO.class), anyString(),
                any(EndpointDTO.class))).thenReturn(computationalStartDTO);
        when(provisioningService.post(anyString(), anyString(), any(ComputationalBase.class), any()))
                .thenReturn("someUuid");
        when(requestId.put(anyString(), anyString())).thenReturn("someUuid");

        computationalService.startSparkCluster(userInfo, EXPLORATORY_NAME, COMP_NAME, PROJECT, String.format(AUDIT_MESSAGE, EXPLORATORY_NAME));

        verify(computationalDAO).updateComputationalStatus(refEq(computationalStatusDTOWithStatusStarting, "self"));
        verify(exploratoryDAO).fetchExploratoryFields(USER, PROJECT, EXPLORATORY_NAME, true);
        verify(requestBuilder).newComputationalStart(refEq(userInfo), refEq(exploratory), eq(COMP_NAME), refEq(endpointDTO()));
        verify(provisioningService)
                .post(eq(endpointDTO().getUrl() + "computational/start/spark"), eq(TOKEN),
                        refEq(computationalStartDTO),
                        eq(String.class));
        verify(requestId).put(USER, "someUuid");
        verifyNoMoreInteractions(computationalDAO, exploratoryDAO, requestBuilder,
                provisioningService, requestId);
    }

    @Test
    public void startSparkClusterWhenDataengineStatusIsRunning() {
        final UserInstanceDTO userInstanceDto = getUserInstanceDto();
        userInstanceDto.setResources(singletonList(getUserComputationalResource(RUNNING,
                DOCKER_DATALAB_DATAENGINE_SERVICE)));
        when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString(), anyString(), anyBoolean())).thenReturn(userInstanceDto);

        expectedException.expect(IllegalStateException.class);
        expectedException.expectMessage("There is no stopped dataengine compName for exploratory expName");

        computationalService.startSparkCluster(userInfo, EXPLORATORY_NAME, COMP_NAME, PROJECT, String.format(AUDIT_MESSAGE, EXPLORATORY_NAME));
    }

    @Test
    public void getComputationalResource() {
        when(computationalDAO.fetchComputationalFields(anyString(), anyString(), anyString(), anyString())).thenReturn(ucResource);

        Optional<UserComputationalResource> expectedResource = Optional.of(ucResource);
        Optional<UserComputationalResource> actualResource =
                computationalService.getComputationalResource(USER, PROJECT, EXPLORATORY_NAME, COMP_NAME);
        assertEquals(expectedResource, actualResource);

        verify(computationalDAO).fetchComputationalFields(USER, PROJECT, EXPLORATORY_NAME, COMP_NAME);
        verifyNoMoreInteractions(computationalDAO);
    }

    @Test
    public void getComputationalResourceWithException() {
        doThrow(new DatalabException("Computational resource not found"))
                .when(computationalDAO).fetchComputationalFields(anyString(), anyString(), anyString(), anyString());

        Optional<UserComputationalResource> expectedResource = Optional.empty();
        Optional<UserComputationalResource> actualResource =
                computationalService.getComputationalResource(USER, PROJECT, EXPLORATORY_NAME, COMP_NAME);
        assertEquals(expectedResource, actualResource);

        verify(computationalDAO).fetchComputationalFields(USER, PROJECT, EXPLORATORY_NAME, COMP_NAME);
        verifyNoMoreInteractions(computationalDAO);
    }

    @Test
    public void testUpdateSparkClusterConfig() {
        when(endpointService.get(anyString())).thenReturn(endpointDTO());
        final ComputationalClusterConfigDTO clusterConfigDTO = new ComputationalClusterConfigDTO();
        final UserInstanceDTO userInstanceDto = getUserInstanceDto();
        final List<ClusterConfig> config = Collections.singletonList(new ClusterConfig());
        userInstanceDto.setResources(Collections.singletonList(getUserComputationalResource(RUNNING, COMP_NAME)));
        when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString(), anyString(), anyBoolean())).thenReturn(userInstanceDto);
        when(requestBuilder.newClusterConfigUpdate(any(UserInfo.class), any(UserInstanceDTO.class),
                any(UserComputationalResource.class), anyListOf(ClusterConfig.class), any(EndpointDTO.class)))
                .thenReturn(clusterConfigDTO);
        when(provisioningService.post(anyString(), anyString(), any(ComputationalClusterConfigDTO.class), any()))
                .thenReturn("someUuid");
        computationalService.updateSparkClusterConfig(getUserInfo(), PROJECT, EXPLORATORY_NAME,
                COMP_NAME, config, String.format(AUDIT_COMPUTATIONAL_RECONFIGURE_MESSAGE, COMP_NAME, NOTE_BOOK_NAME));

        verify(exploratoryDAO).fetchExploratoryFields(USER, PROJECT, EXPLORATORY_NAME, true);
        verify(requestBuilder).newClusterConfigUpdate(refEq(getUserInfo()), refEq(userInstanceDto),
                refEq(getUserComputationalResource(RUNNING, COMP_NAME)),
                eq(Collections.singletonList(new ClusterConfig())), eq(endpointDTO()));
        verify(requestId).put(USER, "someUuid");
        verify(computationalDAO).updateComputationalFields(refEq(new ComputationalStatusDTO()
                .withProject(PROJECT)
                .withConfig(config)
                .withUser(USER)
                .withExploratoryName(EXPLORATORY_NAME)
                .withComputationalName(COMP_NAME)
                .withStatus(UserInstanceStatus.RECONFIGURING.toString()), "self"));
        verify(provisioningService).post(eq(endpointDTO().getUrl() + "computational/spark/reconfigure"),
                eq(getUserInfo().getAccessToken()),
                refEq(new ComputationalClusterConfigDTO()), eq(String.class));

    }

    @Test
    public void testUpdateSparkClusterConfigWhenClusterIsNotRunning() {
        final UserInstanceDTO userInstanceDto = getUserInstanceDto();
        final List<ClusterConfig> config = Collections.singletonList(new ClusterConfig());
        userInstanceDto.setResources(Collections.singletonList(getUserComputationalResource(STOPPED, COMP_NAME)));
        when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString(), anyString(), anyBoolean())).thenReturn(userInstanceDto);
        try {
            computationalService.updateSparkClusterConfig(getUserInfo(), PROJECT, EXPLORATORY_NAME,
                    COMP_NAME, config, String.format(AUDIT_COMPUTATIONAL_RECONFIGURE_MESSAGE, COMP_NAME, NOTE_BOOK_NAME));
        } catch (ResourceNotFoundException e) {
            assertEquals("Running computational resource with name compName for exploratory expName not found", e.getMessage());
        }

        verify(exploratoryDAO).fetchExploratoryFields(USER, PROJECT, EXPLORATORY_NAME, true);
        verifyNoMoreInteractions(exploratoryDAO);
        verifyZeroInteractions(provisioningService, requestBuilder, requestId);

    }

    @Test
    public void testUpdateSparkClusterConfigWhenClusterIsNotFound() {
        final UserInstanceDTO userInstanceDto = getUserInstanceDto();
        final List<ClusterConfig> config = Collections.singletonList(new ClusterConfig());
        userInstanceDto.setResources(Collections.singletonList(getUserComputationalResource(STOPPED, COMP_NAME)));
        when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString(), anyString(), anyBoolean())).thenReturn(userInstanceDto);
        try {
            computationalService.updateSparkClusterConfig(getUserInfo(), PROJECT, EXPLORATORY_NAME,
                    COMP_NAME + "X", config, String.format(AUDIT_COMPUTATIONAL_RECONFIGURE_MESSAGE, COMP_NAME, NOTE_BOOK_NAME));
        } catch (ResourceNotFoundException e) {
            assertEquals("Running computational resource with name compNameX for exploratory expName not found",
                    e.getMessage());
        }

        verify(exploratoryDAO).fetchExploratoryFields(USER, PROJECT, EXPLORATORY_NAME, true);
        verifyNoMoreInteractions(exploratoryDAO);
        verifyZeroInteractions(provisioningService, requestBuilder, requestId);

    }

    @Test
    public void testGetClusterConfig() {
        when(computationalDAO.getClusterConfig(anyString(), anyString(), anyString(), anyString())).thenReturn(Collections.singletonList(getClusterConfig()));

        final List<ClusterConfig> clusterConfig = computationalService.getClusterConfig(getUserInfo(), PROJECT,
                EXPLORATORY_NAME, COMP_NAME);
        final ClusterConfig config = clusterConfig.get(0);

        assertEquals(1, clusterConfig.size());
        assertEquals("test", config.getClassification());
        assertNull(config.getConfigurations());
        assertNull(config.getProperties());
    }


    @Test
    public void testGetClusterConfigWithException() {
        when(computationalDAO.getClusterConfig(anyString(), anyString(), anyString(), anyString())).thenThrow(new RuntimeException(
                "Exception"));

        expectedException.expectMessage("Exception");
        expectedException.expect(RuntimeException.class);
        computationalService.getClusterConfig(getUserInfo(), PROJECT, EXPLORATORY_NAME, COMP_NAME);
    }

    private ClusterConfig getClusterConfig() {
        final ClusterConfig config = new ClusterConfig();
        config.setClassification("test");
        return config;
    }

    private UserInfo getUserInfo() {
        return new UserInfo(USER, TOKEN);
    }

    private UserInstanceDTO getUserInstanceDto() {
        return new UserInstanceDTO().withUser(USER).withExploratoryName(EXPLORATORY_NAME)
                .withExploratoryId("explId")
                .withProject(PROJECT)
                .withTags(Collections.emptyMap());
    }

    private List<ComputationalCreateFormDTO> getFormList() {
        SparkStandaloneClusterCreateForm sparkClusterForm = new SparkStandaloneClusterCreateForm();
        sparkClusterForm.setNotebookName(EXPLORATORY_NAME);
        sparkClusterForm.setName(COMP_NAME);
        sparkClusterForm.setProject(PROJECT);
        sparkClusterForm.setDataEngineInstanceCount(String.valueOf(2));
        sparkClusterForm.setImage("dataengine");
        sparkClusterForm.setEnabledGPU(Boolean.FALSE);
        ComputationalCreateFormDTO desClusterForm = new ComputationalCreateFormDTO();
        desClusterForm.setNotebookName(EXPLORATORY_NAME);
        desClusterForm.setName(COMP_NAME);

        return Arrays.asList(sparkClusterForm, desClusterForm);
    }

    private ComputationalStatusDTO getComputationalStatusDTOWithStatus(String status) {
        return new ComputationalStatusDTO()
                .withUser(USER)
                .withProject(PROJECT)
                .withExploratoryName(EXPLORATORY_NAME)
                .withComputationalName(COMP_NAME)
                .withStatus(UserInstanceStatus.of(status));
    }

    private SparkStandaloneClusterResource getSparkClusterResource() {
        return SparkStandaloneClusterResource.builder()
                .computationalName(COMP_NAME)
                .imageName("dataengine")
                .status(CREATING.toString())
                .dataEngineInstanceCount(String.valueOf(2))
                .totalInstanceCount(2)
                .tags(Collections.emptyMap())
                .enabledGPU(Boolean.FALSE)
                .build();
    }

    private EndpointDTO endpointDTO() {
        return new EndpointDTO("test", "url", "", null, EndpointDTO.EndpointStatus.ACTIVE, CloudProvider.AWS);
    }


    private UserComputationalResource getUserComputationalResource(UserInstanceStatus status, String imageName) {
        UserComputationalResource ucResource = new UserComputationalResource();
        ucResource.setComputationalName(COMP_NAME);
        ucResource.setImageName("dataengine");
        ucResource.setImageName(imageName);
        ucResource.setStatus(status.toString());
        ucResource.setLastActivity(LAST_ACTIVITY);
        ucResource.setComputationalId(COMP_ID);
        ucResource.setTags(Collections.emptyMap());
        final SchedulerJobDTO schedulerData = new SchedulerJobDTO();
        schedulerData.setCheckInactivityRequired(true);
        schedulerData.setMaxInactivity(MAX_INACTIVITY);
        ucResource.setSchedulerData(schedulerData);
        return ucResource;
    }

    private ProjectDTO getProjectDTO() {
        return new ProjectDTO(PROJECT, Collections.emptySet(), "", "", null,
                singletonList(new ProjectEndpointDTO("endpoint", UserInstanceStatus.RUNNING,
                        new EdgeInfo())), true);
    }
}
