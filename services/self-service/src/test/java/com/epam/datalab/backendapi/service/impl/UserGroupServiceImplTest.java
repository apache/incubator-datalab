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

import com.epam.datalab.backendapi.dao.ProjectDAO;
import com.epam.datalab.backendapi.dao.UserGroupDAO;
import com.epam.datalab.backendapi.dao.UserRoleDAO;
import com.epam.datalab.backendapi.domain.ProjectDTO;
import com.epam.datalab.backendapi.resources.TestBase;
import com.epam.datalab.backendapi.resources.dto.UserGroupDto;
import com.epam.datalab.dto.UserInstanceStatus;
import com.epam.datalab.exceptions.DatalabException;
import com.epam.datalab.exceptions.ResourceNotFoundException;
import io.dropwizard.auth.AuthenticationException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.anySet;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UserGroupServiceImplTest extends TestBase {

    private static final String ROLE_ID = "Role id";
    private static final String USER = "test";
    private static final String GROUP = "admin";
    @Mock
    private UserRoleDAO userRoleDao;
    @Mock
    private UserGroupDAO userGroupDao;
    @Mock
    private ProjectDAO projectDAO;
    @InjectMocks
    private UserGroupServiceImpl userGroupService;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setup() throws AuthenticationException {
        authSetup();
    }

    @Test
    public void createGroup() {
        when(userRoleDao.addGroupToRole(anySet(), anySet())).thenReturn(true);

        userGroupService.createGroup(getUserInfo(), GROUP, Collections.singleton(ROLE_ID), Collections.singleton(USER));

        verify(userRoleDao).addGroupToRole(Collections.singleton(GROUP), Collections.singleton(ROLE_ID));
        verify(userGroupDao).addUsers(GROUP, Collections.singleton(USER));
    }

    @Test
    public void createGroupWithNoUsers() {
        when(userRoleDao.addGroupToRole(anySet(), anySet())).thenReturn(true);

        userGroupService.createGroup(getUserInfo(), GROUP, Collections.singleton(ROLE_ID), Collections.emptySet());

        verify(userRoleDao).addGroupToRole(Collections.singleton(GROUP), Collections.singleton(ROLE_ID));
        verify(userGroupDao).addUsers(anyString(), anySet());
    }

    @Test
    public void createGroupWhenRoleNotFound() {
        when(userRoleDao.addGroupToRole(anySet(), anySet())).thenReturn(false);

        expectedException.expect(ResourceNotFoundException.class);
        userGroupService.createGroup(getUserInfo(), GROUP, Collections.singleton(ROLE_ID), Collections.singleton(USER));
    }

    @Test
    public void removeGroup() {

        when(userRoleDao.removeGroup(anyString())).thenReturn(true);
        final ProjectDTO projectDTO = new ProjectDTO(
                "name", Collections.emptySet(), "", "", null, Collections.emptyList(), true);
        when(projectDAO.getProjectsWithEndpointStatusNotIn(UserInstanceStatus.TERMINATED,
                UserInstanceStatus.TERMINATING)).thenReturn(Collections.singletonList(projectDTO));
        doNothing().when(userGroupDao).removeGroup(anyString());

        userGroupService.removeGroup(getUserInfo(), GROUP);

        verify(userRoleDao).removeGroup(GROUP);
        verify(userGroupDao).removeGroup(GROUP);
        verifyNoMoreInteractions(userGroupDao, userRoleDao);
    }

    @Test
    public void removeGroupWhenItIsUsedInProject() {

        when(userRoleDao.removeGroup(anyString())).thenReturn(true);
        when(projectDAO.getProjectsWithEndpointStatusNotIn(UserInstanceStatus.TERMINATED,
                UserInstanceStatus.TERMINATING)).thenReturn(Collections.singletonList(new ProjectDTO(
                "name", Collections.singleton(GROUP), "", "", null, Collections.emptyList(), true)));
        doNothing().when(userGroupDao).removeGroup(anyString());

        try {
            userGroupService.removeGroup(getUserInfo(), GROUP);
        } catch (Exception e) {
            assertEquals("Group can not be removed because it is used in some project", e.getMessage());
        }

        verify(userRoleDao, never()).removeGroup(GROUP);
        verify(userGroupDao, never()).removeGroup(GROUP);
        verifyNoMoreInteractions(userGroupDao, userRoleDao);
    }

    @Test
    public void removeGroupWhenGroupNotExist() {

        final ProjectDTO projectDTO = new ProjectDTO(
                "name", Collections.emptySet(), "", "", null, Collections.emptyList(), true);
        when(projectDAO.getProjectsWithEndpointStatusNotIn(UserInstanceStatus.TERMINATED,
                UserInstanceStatus.TERMINATING)).thenReturn(Collections.singletonList(projectDTO));
        when(userRoleDao.removeGroup(anyString())).thenReturn(false);
        doNothing().when(userGroupDao).removeGroup(anyString());

        userGroupService.removeGroup(getUserInfo(), GROUP);

        verify(userRoleDao).removeGroup(GROUP);
        verify(userGroupDao).removeGroup(GROUP);
        verifyNoMoreInteractions(userGroupDao, userRoleDao);
    }

    @Test
    public void removeGroupWithException() {
        final ProjectDTO projectDTO = new ProjectDTO(
                "name", Collections.emptySet(), "", "", null, Collections.emptyList(), true);
        when(projectDAO.getProjectsWithEndpointStatusNotIn(UserInstanceStatus.TERMINATED,
                UserInstanceStatus.TERMINATING)).thenReturn(Collections.singletonList(projectDTO));
        when(userRoleDao.removeGroup(anyString())).thenThrow(new DatalabException("Exception"));

        expectedException.expectMessage("Exception");
        expectedException.expect(DatalabException.class);

        userGroupService.removeGroup(getUserInfo(), GROUP);
    }

    private UserGroupDto getUserGroup() {
        return new UserGroupDto(GROUP, Collections.emptyList(), Collections.emptySet());
    }

    private List<ProjectDTO> getProjects() {
        return Collections.singletonList(ProjectDTO.builder()
                .groups(new HashSet<>(Collections.singletonList(GROUP)))
                .build());
    }
}