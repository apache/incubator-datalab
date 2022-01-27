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
import com.epam.datalab.backendapi.domain.BudgetDTO;
import com.epam.datalab.backendapi.domain.CreateProjectDTO;
import com.epam.datalab.backendapi.domain.ProjectDTO;
import com.epam.datalab.backendapi.domain.ProjectEndpointDTO;
import com.epam.datalab.backendapi.domain.UpdateProjectBudgetDTO;
import com.epam.datalab.backendapi.domain.UpdateProjectDTO;
import com.epam.datalab.backendapi.resources.dto.KeysDTO;
import com.epam.datalab.backendapi.resources.dto.ProjectActionFormDTO;
import com.epam.datalab.backendapi.service.AccessKeyService;
import com.epam.datalab.backendapi.service.ProjectService;
import com.epam.datalab.dto.UserInstanceStatus;
import com.epam.datalab.exceptions.DatalabException;
import com.epam.datalab.exceptions.ResourceConflictException;
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
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

public class ProjectResourceTest extends TestBase {

    private static final String PROJECT_NAME = "DATALAB";

    private final ProjectService projectService = mock(ProjectService.class);
    private final AccessKeyService keyService = mock(AccessKeyService.class);

    @Rule
    public final ResourceTestRule resources = getResourceTestRuleInstance(
            new ProjectResource(projectService, keyService));

    @Before
    public void setup() throws AuthenticationException {
        authSetup();
    }

    @Test
    public void createProject() {
        CreateProjectDTO createProjectDTO = returnCreateProjectDTO();
        final Response response = resources.getJerseyTest()
                .target("project")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .post(Entity.json(createProjectDTO));

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        verify(projectService).create(getUserInfo(), prepareProjectDTO(createProjectDTO), createProjectDTO.getName());
        verifyNoMoreInteractions(projectService);
    }

    @Test
    public void createExistingProject() {
        CreateProjectDTO createProjectDTO = returnCreateProjectDTO();
        doThrow(new ResourceConflictException("Project with passed name already exist in system"))
                .when(projectService).create(any(UserInfo.class), any(ProjectDTO.class), anyString());
        final Response response = resources.getJerseyTest()
                .target("project")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .post(Entity.json(createProjectDTO));

        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
        verify(projectService).create(getUserInfo(), prepareProjectDTO(createProjectDTO), createProjectDTO.getName());
        verifyNoMoreInteractions(projectService);
    }

    @Test
    public void recreateProject() {
        final Response response = resources.getJerseyTest()
                .target("project/recreate")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .post(Entity.json(getProjectActionDTO()));

        assertEquals(HttpStatus.SC_ACCEPTED, response.getStatus());
        verify(projectService).recreate(getUserInfo(), ENDPOINT_NAME, PROJECT_NAME);
        verifyNoMoreInteractions(projectService);
    }

    @Test
    public void startProject() {
        final Response response = resources.getJerseyTest()
                .target("project/start")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .post(Entity.json(getProjectActionDTO()));

        assertEquals(HttpStatus.SC_ACCEPTED, response.getStatus());
        verify(projectService).start(getUserInfo(), Collections.singletonList(ENDPOINT_NAME), PROJECT_NAME);
        verifyNoMoreInteractions(projectService);
    }

    @Test
    public void stopProject() {
        final Response response = resources.getJerseyTest()
                .target("project/stop")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .post(Entity.json(getProjectActionDTO()));

        assertEquals(HttpStatus.SC_ACCEPTED, response.getStatus());
        verify(projectService).stopWithResources(getUserInfo(), Collections.singletonList(ENDPOINT_NAME), PROJECT_NAME);
        verifyNoMoreInteractions(projectService);
    }

    @Test
    public void getProject() {
        when(projectService.get(anyString())).thenReturn(ProjectDTO.builder().name(PROJECT_NAME).build());

        final Response response = resources.getJerseyTest()
                .target("project/" + PROJECT_NAME)
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .get();

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));
        verify(projectService).get(PROJECT_NAME);
        verifyNoMoreInteractions(projectService);
    }

    @Test
    public void getProjects() {
        when(projectService.getProjects(any(UserInfo.class))).thenReturn(Collections.singletonList(ProjectDTO.builder().name(PROJECT_NAME).build()));

        final Response response = resources.getJerseyTest()
                .target("project")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .get();

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));
        verify(projectService).getProjects(getUserInfo());
        verifyNoMoreInteractions(projectService);
    }

    @Test
    public void getUserProjects() {
        when(projectService.getUserProjects(getUserInfo(), false)).thenReturn(Collections.singletonList(ProjectDTO.builder().name(PROJECT_NAME).build()));

        final Response response = resources.getJerseyTest()
                .target("project/me")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .get();

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));
        verify(projectService).getUserProjects(getUserInfo(), Boolean.FALSE);
        verifyNoMoreInteractions(projectService);
    }

    @Test
    public void updateProject() {
        doNothing().when(projectService).update(any(UserInfo.class), any(UpdateProjectDTO.class), anyString());

        final Response response = resources.getJerseyTest()
                .target("project")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .put(Entity.json(prepareUpdateProjectDTO()));

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        verify(projectService).update(getUserInfo(), prepareUpdateProjectDTO(), PROJECT_NAME);
        verifyNoMoreInteractions(projectService);
    }

    @Test
    public void removeProjectEndpoint() {
        doNothing().when(projectService).terminateEndpoint(any(UserInfo.class), anyList(), anyString());

        final Response response = resources.getJerseyTest()
                .target("project/terminate")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .post(Entity.json(prepareProjectActionFormDTO()));

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        verify(projectService).terminateEndpoint(getUserInfo(), prepareProjectActionFormDTO().getEndpoints(), prepareProjectActionFormDTO().getProjectName());
        verifyNoMoreInteractions(projectService);
    }

    @Test
    public void updateBudget() {
        doNothing().when(projectService).updateBudget(any(UserInfo.class), anyList());

        final Response response = resources.getJerseyTest()
                .target("project/budget")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .put(Entity.json((prepareUpdateProjectBudgetDTOs())));

        assertEquals(HttpStatus.SC_OK, response.getStatus());

        verify(projectService).updateBudget(getUserInfo(), prepareUpdateProjectBudgetDTOs());
        verifyNoMoreInteractions(projectService);
    }

    @Test
    public void generate() {
        when(keyService.generateKeys(any(UserInfo.class))).thenReturn(new KeysDTO("somePublicKey", "somePrivateKey", "user"));

        final Response response = resources.getJerseyTest()
                .target("/project/keys")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .post(Entity.json(""));

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(keyService).generateKeys(getUserInfo());
        verifyNoMoreInteractions(keyService);
    }

    @Test
    public void generateKeysWithException() {
        doThrow(new DatalabException("Can not generate private/public key pair due to"))
                .when(keyService).generateKeys(any(UserInfo.class));

        final Response response = resources.getJerseyTest()
                .target("/project/keys")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .post(Entity.json(""));

        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(keyService).generateKeys(getUserInfo());
        verifyNoMoreInteractions(keyService);
    }

    private CreateProjectDTO returnCreateProjectDTO() {
        return new CreateProjectDTO(PROJECT_NAME, Collections.emptySet(), Collections.emptySet(), "ssh-testKey", "testTag");
    }

    private ProjectDTO prepareProjectDTO(CreateProjectDTO createProjectDTO) {
        List<ProjectEndpointDTO> projectEndpointDTOS = createProjectDTO.getEndpoints()
                .stream()
                .map(e -> new ProjectEndpointDTO(e, UserInstanceStatus.CREATING, null))
                .collect(Collectors.toList());

        return new ProjectDTO(createProjectDTO.getName(), createProjectDTO.getGroups(), createProjectDTO.getKey(), createProjectDTO.getTag(),
                new BudgetDTO(), projectEndpointDTOS, createProjectDTO.isSharedImageEnabled());
    }

    private ProjectActionFormDTO getProjectActionDTO() {
        return new ProjectActionFormDTO(PROJECT_NAME, Collections.singletonList(ENDPOINT_NAME),"RUNNING");
    }

    private UpdateProjectDTO prepareUpdateProjectDTO() {
        return new UpdateProjectDTO(PROJECT_NAME, Collections.emptySet(), Collections.emptySet(), Boolean.TRUE);
    }

    private ProjectActionFormDTO prepareProjectActionFormDTO() {
        return new ProjectActionFormDTO(PROJECT_NAME, Collections.singletonList(ENDPOINT_NAME),"RUNNING");
    }

    private List<UpdateProjectBudgetDTO> prepareUpdateProjectBudgetDTOs() {
        return Collections.singletonList(new UpdateProjectBudgetDTO(PROJECT_NAME, 123, Boolean.FALSE));
    }
}
