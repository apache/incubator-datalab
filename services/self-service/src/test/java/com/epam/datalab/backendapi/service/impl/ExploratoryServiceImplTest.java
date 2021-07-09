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
import com.epam.datalab.backendapi.dao.GitCredsDAO;
import com.epam.datalab.backendapi.domain.EndpointDTO;
import com.epam.datalab.backendapi.domain.ProjectDTO;
import com.epam.datalab.backendapi.domain.ProjectEndpointDTO;
import com.epam.datalab.backendapi.domain.RequestId;
import com.epam.datalab.backendapi.service.EndpointService;
import com.epam.datalab.backendapi.service.ProjectService;
import com.epam.datalab.backendapi.service.TagService;
import com.epam.datalab.backendapi.util.RequestBuilder;
import com.epam.datalab.cloud.CloudProvider;
import com.epam.datalab.dto.StatusEnvBaseDTO;
import com.epam.datalab.dto.UserInstanceDTO;
import com.epam.datalab.dto.UserInstanceStatus;
import com.epam.datalab.dto.aws.computational.ClusterConfig;
import com.epam.datalab.dto.base.edge.EdgeInfo;
import com.epam.datalab.dto.computational.UserComputationalResource;
import com.epam.datalab.dto.exploratory.ExploratoryActionDTO;
import com.epam.datalab.dto.exploratory.ExploratoryCreateDTO;
import com.epam.datalab.dto.exploratory.ExploratoryGitCredsDTO;
import com.epam.datalab.dto.exploratory.ExploratoryGitCredsUpdateDTO;
import com.epam.datalab.dto.exploratory.ExploratoryReconfigureSparkClusterActionDTO;
import com.epam.datalab.dto.exploratory.ExploratoryStatusDTO;
import com.epam.datalab.exceptions.DatalabException;
import com.epam.datalab.exceptions.ResourceNotFoundException;
import com.epam.datalab.model.exploratory.Exploratory;
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

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyListOf;
import static org.mockito.Mockito.anyMapOf;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.anyVararg;
import static org.mockito.Mockito.doNothing;
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
public class ExploratoryServiceImplTest {

    private final String USER = "test";
    private final String TOKEN = "token";
    private final String PROJECT = "project";
    private final String EXPLORATORY_NAME = "expName";
    private final String UUID = "1234-56789765-4321";
    private static final String ENDPOINT_NAME = "endpointName";


    private UserInfo userInfo;
    private UserInstanceDTO userInstance;
    private StatusEnvBaseDTO statusEnvBaseDTO;

    @Mock
    private ProjectService projectService;
    @Mock
    private ExploratoryDAO exploratoryDAO;
    @Mock
    private ComputationalDAO computationalDAO;
    @Mock
    private GitCredsDAO gitCredsDAO;
    @Rule
    public ExpectedException expectedException = ExpectedException.none();
    @Mock
    private RESTService provisioningService;
    @Mock
    private RequestBuilder requestBuilder;
    @Mock
    private RequestId requestId;
    @Mock
    private TagService tagService;
    @Mock
    private EndpointService endpointService;
    @Mock
    private SelfServiceApplicationConfiguration configuration;
    @InjectMocks
    private ExploratoryServiceImpl exploratoryService;

    @Before
    public void setUp() {
        when(configuration.isAuditEnabled()).thenReturn(false);
        userInfo = getUserInfo();
        userInstance = getUserInstanceDto();
    }

    @Test
    public void start() {
        when(endpointService.get(anyString())).thenReturn(endpointDTO());
        when(exploratoryDAO.updateExploratoryStatus(any(StatusEnvBaseDTO.class))).thenReturn(mock(UpdateResult.class));
        when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString(), anyString())).thenReturn(userInstance);

        ExploratoryGitCredsDTO egcDtoMock = mock(ExploratoryGitCredsDTO.class);
        when(gitCredsDAO.findGitCreds(anyString())).thenReturn(egcDtoMock);

        ExploratoryActionDTO egcuDto = new ExploratoryGitCredsUpdateDTO();
        egcuDto.withExploratoryName(EXPLORATORY_NAME);
        when(requestBuilder.newExploratoryStart(any(UserInfo.class), any(UserInstanceDTO.class), any(EndpointDTO.class),
                any(ExploratoryGitCredsDTO.class))).thenReturn(egcuDto);

        String exploratoryStart = "exploratory/start";
        when(provisioningService.post(anyString(), anyString(), any(ExploratoryActionDTO.class), any()))
                .thenReturn(UUID);
        when(requestId.put(anyString(), anyString())).thenReturn(UUID);

        String uuid = exploratoryService.start(userInfo, EXPLORATORY_NAME, "project", null);
        assertNotNull(uuid);
        assertEquals(UUID, uuid);

        statusEnvBaseDTO = getStatusEnvBaseDTOWithStatus("starting");

        verify(exploratoryDAO).updateExploratoryStatus(refEq(statusEnvBaseDTO, "self"));
        verify(exploratoryDAO).fetchExploratoryFields(USER, PROJECT, EXPLORATORY_NAME);
        verify(provisioningService).post(endpointDTO().getUrl() + exploratoryStart, TOKEN, egcuDto, String.class);
        verify(requestId).put(USER, UUID);
        verifyNoMoreInteractions(exploratoryDAO, provisioningService, requestId);
    }

    @Test
    public void startWhenMethodFetchExploratoryFieldsThrowsException() {
        when(exploratoryDAO.updateExploratoryStatus(any(StatusEnvBaseDTO.class))).thenReturn(mock(UpdateResult.class));
        doThrow(new ResourceNotFoundException("Exploratory for user with name not found"))
                .when(exploratoryDAO).fetchExploratoryFields(anyString(), anyString(), anyString());
        try {
            exploratoryService.start(userInfo, EXPLORATORY_NAME, PROJECT, null);
        } catch (DatalabException e) {
            assertEquals("Could not start exploratory environment expName: Exploratory for user with " +
                    "name not found", e.getMessage());
        }
        statusEnvBaseDTO = getStatusEnvBaseDTOWithStatus("starting");
        verify(exploratoryDAO).updateExploratoryStatus(refEq(statusEnvBaseDTO, "self"));
        verify(exploratoryDAO).fetchExploratoryFields(USER, PROJECT, EXPLORATORY_NAME);

        statusEnvBaseDTO = getStatusEnvBaseDTOWithStatus("failed");
        verify(exploratoryDAO).updateExploratoryStatus(refEq(statusEnvBaseDTO, "self"));
        verifyNoMoreInteractions(exploratoryDAO);
    }

    @Test
    public void stop() {
        when(endpointService.get(anyString())).thenReturn(endpointDTO());
        when(exploratoryDAO.updateExploratoryStatus(any(StatusEnvBaseDTO.class))).thenReturn(mock(UpdateResult.class));
        when(computationalDAO.updateComputationalStatusesForExploratory(any(StatusEnvBaseDTO.class))).thenReturn(1);
        when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString(), anyString())).thenReturn(userInstance);

        ExploratoryActionDTO eaDto = new ExploratoryActionDTO();
        eaDto.withExploratoryName(EXPLORATORY_NAME);
        when(requestBuilder.newExploratoryStop(anyString(), any(UserInstanceDTO.class), any(EndpointDTO.class)))
                .thenReturn(eaDto);

        String exploratoryStop = "exploratory/stop";
        when(provisioningService.post(anyString(), anyString(), any(ExploratoryActionDTO.class), any())).thenReturn
                (UUID);
        when(requestId.put(anyString(), anyString())).thenReturn(UUID);

        String uuid = exploratoryService.stop(userInfo, userInfo.getName(), PROJECT, EXPLORATORY_NAME, null);
        assertNotNull(uuid);
        assertEquals(UUID, uuid);

        statusEnvBaseDTO = getStatusEnvBaseDTOWithStatus("stopping");

        verify(exploratoryDAO).updateExploratoryStatus(refEq(statusEnvBaseDTO, "self"));
        verify(exploratoryDAO).fetchExploratoryFields(USER, PROJECT, EXPLORATORY_NAME);
        verify(provisioningService).post(endpointDTO().getUrl() + exploratoryStop, TOKEN, eaDto, String.class);
        verify(computationalDAO).updateComputationalStatusesForExploratory(userInfo.getName(), PROJECT,
                EXPLORATORY_NAME, UserInstanceStatus.STOPPING, UserInstanceStatus.TERMINATING,
                UserInstanceStatus.FAILED, UserInstanceStatus.TERMINATED, UserInstanceStatus.STOPPED);
        verify(requestId).put(USER, UUID);
        verifyNoMoreInteractions(exploratoryDAO, provisioningService, requestId);
    }

    @Test
    public void stopWhenMethodFetchExploratoryFieldsThrowsException() {
        when(exploratoryDAO.updateExploratoryStatus(any(StatusEnvBaseDTO.class))).thenReturn(mock(UpdateResult.class));
        doThrow(new ResourceNotFoundException("Exploratory for user with name not found"))
                .when(exploratoryDAO).fetchExploratoryFields(anyString(), anyString(), anyString());
        try {
            exploratoryService.stop(userInfo, userInfo.getName(), PROJECT, EXPLORATORY_NAME, null);
        } catch (DatalabException e) {
            assertEquals("Could not stop exploratory environment expName: Exploratory for user with " +
                    "name not found", e.getMessage());
        }
        statusEnvBaseDTO = getStatusEnvBaseDTOWithStatus("stopping");
        verify(exploratoryDAO).updateExploratoryStatus(refEq(statusEnvBaseDTO, "self"));
        verify(exploratoryDAO).fetchExploratoryFields(USER, PROJECT, EXPLORATORY_NAME);

        statusEnvBaseDTO = getStatusEnvBaseDTOWithStatus("failed");
        verify(exploratoryDAO).updateExploratoryStatus(refEq(statusEnvBaseDTO, "self"));
        verifyNoMoreInteractions(exploratoryDAO);
    }

    @Test
    public void terminate() {
        when(endpointService.get(anyString())).thenReturn(endpointDTO());
        when(exploratoryDAO.updateExploratoryStatus(any(StatusEnvBaseDTO.class))).thenReturn(mock(UpdateResult.class));
        when(computationalDAO.updateComputationalStatusesForExploratory(any(StatusEnvBaseDTO.class))).thenReturn(1);
        when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString(), anyString())).thenReturn(userInstance);

        ExploratoryActionDTO eaDto = new ExploratoryActionDTO();
        eaDto.withExploratoryName(EXPLORATORY_NAME);
        when(requestBuilder.newExploratoryStop(anyString(), any(UserInstanceDTO.class), any(EndpointDTO.class)))
                .thenReturn(eaDto);

        String exploratoryTerminate = "exploratory/terminate";
        when(provisioningService.post(anyString(), anyString(), any(ExploratoryActionDTO.class), any())).thenReturn
                (UUID);
        when(requestId.put(anyString(), anyString())).thenReturn(UUID);

        String uuid = exploratoryService.terminate(userInfo, userInfo.getName(), PROJECT, EXPLORATORY_NAME, null);
        assertNotNull(uuid);
        assertEquals(UUID, uuid);

        statusEnvBaseDTO = getStatusEnvBaseDTOWithStatus("terminating");

        verify(exploratoryDAO).updateExploratoryStatus(refEq(statusEnvBaseDTO, "self"));
        verify(exploratoryDAO).fetchExploratoryFields(USER, PROJECT, EXPLORATORY_NAME);
        verify(computationalDAO).updateComputationalStatusesForExploratory(USER, PROJECT, EXPLORATORY_NAME,
                UserInstanceStatus.TERMINATING, UserInstanceStatus.TERMINATING, UserInstanceStatus.TERMINATED,
                UserInstanceStatus.FAILED);
        verify(requestBuilder).newExploratoryStop(userInfo.getName(), userInstance, endpointDTO());
        verify(provisioningService).post(endpointDTO().getUrl() + exploratoryTerminate, TOKEN, eaDto, String.class);
        verify(requestId).put(USER, UUID);
        verifyNoMoreInteractions(exploratoryDAO, computationalDAO, requestBuilder, provisioningService, requestId);
    }

    @Test
    public void terminateWhenMethodFetchExploratoryFieldsThrowsException() {
        when(exploratoryDAO.updateExploratoryStatus(any(StatusEnvBaseDTO.class))).thenReturn(mock(UpdateResult.class));
        doThrow(new ResourceNotFoundException("Exploratory for user with name not found"))
                .when(exploratoryDAO).fetchExploratoryFields(anyString(), anyString(), anyString());
        try {
            exploratoryService.terminate(userInfo, userInfo.getName(), PROJECT, EXPLORATORY_NAME, null);
        } catch (DatalabException e) {
            assertEquals("Could not terminate exploratory environment expName: Exploratory for user " +
                    "with name not found", e.getMessage());
        }
        statusEnvBaseDTO = getStatusEnvBaseDTOWithStatus("terminating");
        verify(exploratoryDAO).updateExploratoryStatus(refEq(statusEnvBaseDTO, "self"));
        verify(exploratoryDAO).fetchExploratoryFields(USER, PROJECT, EXPLORATORY_NAME);

        statusEnvBaseDTO = getStatusEnvBaseDTOWithStatus("failed");
        verify(exploratoryDAO).updateExploratoryStatus(refEq(statusEnvBaseDTO, "self"));
        verifyNoMoreInteractions(exploratoryDAO);
    }

    @Test
    public void create() {
        ProjectDTO projectDTO = getProjectDTO();
        when(projectService.get(anyString())).thenReturn(projectDTO);
        when(endpointService.get(anyString())).thenReturn(endpointDTO());
        doNothing().when(exploratoryDAO).insertExploratory(any(UserInstanceDTO.class));
        ExploratoryGitCredsDTO egcDto = new ExploratoryGitCredsDTO();
        when(gitCredsDAO.findGitCreds(anyString())).thenReturn(egcDto);

        ExploratoryCreateDTO ecDto = new ExploratoryCreateDTO();
        Exploratory exploratory = Exploratory.builder()
                .name(EXPLORATORY_NAME)
                .endpoint("test")
                .enabledGPU(false)
                .version("someVersion")
                .build();
        when(requestBuilder.newExploratoryCreate(any(ProjectDTO.class), any(EndpointDTO.class),
                any(Exploratory.class), any(UserInfo.class), any(ExploratoryGitCredsDTO.class), anyMapOf(String.class, String.class))).thenReturn(ecDto);
        String exploratoryCreate = "exploratory/create";
        when(provisioningService.post(anyString(), anyString(), any(ExploratoryCreateDTO.class), any()))
                .thenReturn(UUID);
        when(requestId.put(anyString(), anyString())).thenReturn(UUID);

        String uuid = exploratoryService.create(userInfo, exploratory, "project", "exploratory");
        assertNotNull(uuid);
        assertEquals(UUID, uuid);

        userInstance.withStatus("creating");
        userInstance.withResources(Collections.emptyList());
        userInstance.withImageVersion("someVersion");
        verify(projectService).get("project");
        verify(exploratoryDAO).insertExploratory(userInstance);
        verify(gitCredsDAO).findGitCreds(USER);
        verify(requestBuilder).newExploratoryCreate(projectDTO, endpointDTO(), exploratory, userInfo, egcDto, Collections.emptyMap());
        verify(provisioningService).post(endpointDTO().getUrl() + exploratoryCreate, TOKEN, ecDto, String.class);
        verify(requestId).put(USER, UUID);
        verifyNoMoreInteractions(projectService, exploratoryDAO, gitCredsDAO, requestBuilder, provisioningService, requestId);
    }

    @Test
    public void createWhenMethodInsertExploratoryThrowsException() {
        when(endpointService.get(anyString())).thenReturn(endpointDTO());
        doThrow(new RuntimeException("Exploratory for user with name not found"))
                .when(exploratoryDAO).insertExploratory(any(UserInstanceDTO.class));
        expectedException.expect(DatalabException.class);
        expectedException.expectMessage("Could not create exploratory environment expName for user test: " +
                "Exploratory for user with name not found");

        Exploratory exploratory = Exploratory.builder()
                .name(EXPLORATORY_NAME)
                .enabledGPU(false)
                .version("someVersion")
                .build();
        exploratoryService.create(userInfo, exploratory, "project", "exploratory");
        verify(endpointService).get(anyString());
    }

    @Test
    public void createWhenMethodInsertExploratoryThrowsExceptionWithItsCatching() {
        when(endpointService.get(anyString())).thenReturn(endpointDTO());
        doThrow(new RuntimeException()).when(exploratoryDAO).insertExploratory(any(UserInstanceDTO.class));
        Exploratory exploratory = Exploratory
                .builder()
                .name(EXPLORATORY_NAME)
                .endpoint("test")
                .enabledGPU(false)
                .version("someVersion")
                .build();
        try {
            exploratoryService.create(userInfo, exploratory, "project", "exploratory");
        } catch (DatalabException e) {
            assertEquals("Could not create exploratory environment expName for user test: null",
                    e.getMessage());
        }
        userInstance.withStatus("creating");
        userInstance.withResources(Collections.emptyList());
        userInstance.withImageVersion("someVersion");
        verify(exploratoryDAO).insertExploratory(userInstance);
        verify(exploratoryDAO, never()).updateExploratoryStatus(any(StatusEnvBaseDTO.class));
        verify(endpointService).get("test");
        verifyNoMoreInteractions(exploratoryDAO);
    }

    @Test
    public void createWhenMethodNewExploratoryCreateThrowsException() {
        ProjectDTO projectDTO = getProjectDTO();
        when(projectService.get(anyString())).thenReturn(projectDTO);
        when(endpointService.get(anyString())).thenReturn(endpointDTO());
        doNothing().when(exploratoryDAO).insertExploratory(any(UserInstanceDTO.class));
        ExploratoryGitCredsDTO egcDto = new ExploratoryGitCredsDTO();
        when(gitCredsDAO.findGitCreds(anyString())).thenReturn(egcDto);

        Exploratory exploratory = Exploratory.builder()
                .name(EXPLORATORY_NAME)
                .endpoint("test")
                .version("someVersion")
                .enabledGPU(false)
                .build();

        doThrow(new DatalabException("Cannot create instance of resource class ")).when(requestBuilder)
                .newExploratoryCreate(any(ProjectDTO.class), any(EndpointDTO.class), any(Exploratory.class),
                        any(UserInfo.class), any(ExploratoryGitCredsDTO.class), anyMapOf(String.class, String.class));

        when(exploratoryDAO.updateExploratoryStatus(any(StatusEnvBaseDTO.class))).thenReturn(mock(UpdateResult.class));
        try {
            exploratoryService.create(userInfo, exploratory, "project", "exploratory");
        } catch (DatalabException e) {
            assertEquals("Could not create exploratory environment expName for user test: Cannot create instance " +
                    "of resource class ", e.getMessage());
        }

        statusEnvBaseDTO = getStatusEnvBaseDTOWithStatus("failed");

        userInstance.withStatus("creating");
        userInstance.withResources(Collections.emptyList());
        userInstance.withImageVersion("someVersion");
        verify(projectService).get("project");
        verify(exploratoryDAO).insertExploratory(userInstance);
        verify(exploratoryDAO).insertExploratory(userInstance);
        verify(gitCredsDAO).findGitCreds(USER);
        verify(requestBuilder).newExploratoryCreate(projectDTO, endpointDTO(), exploratory, userInfo, egcDto, Collections.emptyMap());
        verify(exploratoryDAO).updateExploratoryStatus(refEq(statusEnvBaseDTO, "self"));
        verifyNoMoreInteractions(projectService, exploratoryDAO, gitCredsDAO, requestBuilder);
    }

    @Test
    public void updateProjectExploratoryStatuses() {
        when(exploratoryDAO.fetchProjectExploratoriesWhereStatusNotIn(anyString(), anyString(), anyVararg()))
                .thenReturn(singletonList(userInstance));
        when(exploratoryDAO.updateExploratoryStatus(any(StatusEnvBaseDTO.class))).thenReturn(mock(UpdateResult.class));
        doNothing().when(computationalDAO).updateComputationalStatusesForExploratory(anyString(), anyString(),
                anyString(), any(UserInstanceStatus.class), any(UserInstanceStatus.class), anyVararg());

        exploratoryService.updateProjectExploratoryStatuses(userInfo, "project",
                "endpoint", UserInstanceStatus.TERMINATING);
        statusEnvBaseDTO = getStatusEnvBaseDTOWithStatus("terminating");

        verify(exploratoryDAO).fetchProjectExploratoriesWhereStatusNotIn("project", "endpoint",
                UserInstanceStatus.TERMINATED, UserInstanceStatus.FAILED);
        verify(exploratoryDAO).updateExploratoryStatus(refEq(statusEnvBaseDTO, "self"));
        verify(computationalDAO).updateComputationalStatusesForExploratory(USER, PROJECT,
                EXPLORATORY_NAME, UserInstanceStatus.TERMINATING, UserInstanceStatus.TERMINATING,
                UserInstanceStatus.TERMINATED, UserInstanceStatus.FAILED);

        verifyNoMoreInteractions(exploratoryDAO, computationalDAO);
    }

    @Test
    public void getUserInstance() {
        when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString(), anyString())).thenReturn(userInstance);

        Optional<UserInstanceDTO> expectedInstance = Optional.of(userInstance);
        Optional<UserInstanceDTO> actualInstance = exploratoryService.getUserInstance(USER, PROJECT, EXPLORATORY_NAME);
        assertEquals(expectedInstance, actualInstance);

        verify(exploratoryDAO).fetchExploratoryFields(USER, PROJECT, EXPLORATORY_NAME);
        verifyNoMoreInteractions(exploratoryDAO);
    }

    @Test
    public void getUserInstanceWithException() {
        doThrow(new ResourceNotFoundException("Exploratory for user not found"))
                .when(exploratoryDAO).fetchExploratoryFields(anyString(), anyString(), anyString());

        Optional<UserInstanceDTO> expectedInstance = Optional.empty();
        Optional<UserInstanceDTO> actualInstance = exploratoryService.getUserInstance(USER, PROJECT, EXPLORATORY_NAME);
        assertEquals(expectedInstance, actualInstance);

        verify(exploratoryDAO).fetchExploratoryFields(USER, PROJECT, EXPLORATORY_NAME);
        verifyNoMoreInteractions(exploratoryDAO);
    }

    @Test
    public void testUpdateExploratoryClusterConfig() {
        when(endpointService.get(anyString())).thenReturn(endpointDTO());
        when(exploratoryDAO.fetchRunningExploratoryFields(anyString(), anyString(), anyString())).thenReturn(getUserInstanceDto());
        when(requestBuilder.newClusterConfigUpdate(any(UserInfo.class), any(UserInstanceDTO.class),
                anyListOf(ClusterConfig.class), any(EndpointDTO.class))).thenReturn(new ExploratoryReconfigureSparkClusterActionDTO());
        when(provisioningService.post(anyString(), anyString(), any(ExploratoryReconfigureSparkClusterActionDTO.class)
                , any())).thenReturn(UUID);

        exploratoryService.updateClusterConfig(getUserInfo(), PROJECT, EXPLORATORY_NAME, singletonList(new ClusterConfig()));

        verify(exploratoryDAO).fetchRunningExploratoryFields(USER, PROJECT, EXPLORATORY_NAME);
        verify(requestBuilder).newClusterConfigUpdate(refEq(getUserInfo()), refEq(getUserInstanceDto()),
                refEq(singletonList(new ClusterConfig())), refEq(endpointDTO()));
        verify(requestId).put(USER, UUID);
        verify(provisioningService).post(eq(endpointDTO().getUrl() + "exploratory/reconfigure_spark"), eq(TOKEN),
                refEq(new ExploratoryReconfigureSparkClusterActionDTO(), "self"), eq(String.class));
        verify(exploratoryDAO).updateExploratoryFields(refEq(new ExploratoryStatusDTO()
                .withUser(USER)
                .withProject(PROJECT)
                .withConfig(singletonList(new ClusterConfig()))
                .withStatus(UserInstanceStatus.RECONFIGURING.toString())
                .withExploratoryName(EXPLORATORY_NAME), "self"));
        verifyNoMoreInteractions(requestBuilder, requestId, exploratoryDAO, provisioningService);
    }

    @Test
    public void testUpdateExploratoryClusterConfigWhenNotRunning() {

        when(exploratoryDAO.fetchRunningExploratoryFields(anyString(), anyString(), anyString())).thenThrow(new ResourceNotFoundException("EXCEPTION"));

        try {

            exploratoryService.updateClusterConfig(getUserInfo(), PROJECT, EXPLORATORY_NAME,
                    singletonList(new ClusterConfig()));
        } catch (ResourceNotFoundException e) {
            assertEquals("EXCEPTION", e.getMessage());
        }

        verify(exploratoryDAO).fetchRunningExploratoryFields(USER, PROJECT, EXPLORATORY_NAME);
        verifyNoMoreInteractions(exploratoryDAO);
        verifyZeroInteractions(requestBuilder, requestId, provisioningService);
    }

    @Test
    public void testGetClusterConfig() {
        when(exploratoryDAO.getClusterConfig(anyString(), anyString(), anyString())).thenReturn(Collections.singletonList(getClusterConfig()));
        final List<ClusterConfig> clusterConfig = exploratoryService.getClusterConfig(getUserInfo(), PROJECT, EXPLORATORY_NAME);

        assertEquals(1, clusterConfig.size());
        assertEquals("classification", clusterConfig.get(0).getClassification());

        verify(exploratoryDAO).getClusterConfig(getUserInfo().getName(), PROJECT, EXPLORATORY_NAME);
        verifyNoMoreInteractions(exploratoryDAO);
    }

    @Test
    public void testGetClusterConfigWithException() {
        when(exploratoryDAO.getClusterConfig(anyString(), anyString(), anyString())).thenThrow(new RuntimeException("Exception"));

        expectedException.expect(RuntimeException.class);
        expectedException.expectMessage("Exception");
        exploratoryService.getClusterConfig(getUserInfo(), PROJECT, EXPLORATORY_NAME);
    }

    private ClusterConfig getClusterConfig() {
        final ClusterConfig config = new ClusterConfig();
        config.setClassification("classification");
        return config;
    }

    private UserInfo getUserInfo() {
        return new UserInfo(USER, TOKEN);
    }

    private UserInstanceDTO getUserInstanceDto() {
        UserComputationalResource compResource = new UserComputationalResource();
        compResource.setImageName("YYYY.dataengine");
        compResource.setComputationalName("compName");
        compResource.setStatus("stopped");
        compResource.setComputationalId("compId");
        return new UserInstanceDTO()
                .withUser(USER)
                .withExploratoryName(EXPLORATORY_NAME)
                .withStatus("running")
                .withResources(singletonList(compResource))
                .withTags(Collections.emptyMap())
                .withProject(PROJECT)
                .withEndpoint("test")
                .withCloudProvider(CloudProvider.AWS.toString());
    }

    private StatusEnvBaseDTO getStatusEnvBaseDTOWithStatus(String status) {
        return new ExploratoryStatusDTO()
                .withProject(PROJECT)
                .withUser(USER)
                .withExploratoryName(EXPLORATORY_NAME)
                .withStatus(status);
    }

    private EndpointDTO endpointDTO() {
        return new EndpointDTO("test", "url", "", null, EndpointDTO.EndpointStatus.ACTIVE, CloudProvider.AWS);
    }

    private ProjectDTO getProjectDTO() {
        return new ProjectDTO("project", Collections.emptySet(), "", "", null,
                singletonList(new ProjectEndpointDTO(ENDPOINT_NAME, UserInstanceStatus.RUNNING,
                        new EdgeInfo())), true);
    }
}
