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
import com.epam.datalab.backendapi.dao.ExploratoryLibDAO;
import com.epam.datalab.backendapi.domain.EndpointDTO;
import com.epam.datalab.backendapi.domain.NotebookTemplate;
import com.epam.datalab.backendapi.domain.RequestId;
import com.epam.datalab.backendapi.resources.dto.LibInfoRecord;
import com.epam.datalab.backendapi.resources.dto.LibInstallFormDTO;
import com.epam.datalab.backendapi.resources.dto.LibKey;
import com.epam.datalab.backendapi.resources.dto.LibraryStatus;
import com.epam.datalab.backendapi.service.EndpointService;
import com.epam.datalab.backendapi.util.RequestBuilder;
import com.epam.datalab.cloud.CloudProvider;
import com.epam.datalab.dto.UserInstanceDTO;
import com.epam.datalab.dto.computational.UserComputationalResource;
import com.epam.datalab.dto.exploratory.LibInstallDTO;
import com.epam.datalab.dto.exploratory.LibStatus;
import com.epam.datalab.dto.exploratory.LibraryInstallDTO;
import com.epam.datalab.exceptions.DatalabException;
import com.epam.datalab.model.library.Library;
import com.epam.datalab.rest.client.RESTService;
import org.bson.Document;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyListOf;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.refEq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LibraryServiceImplTest {

    private static final String LIB_NAME = "name";
    private static final String LIB_GROUP = "group";
    private static final String LIB_VERSION = "version";
    private static final String UUID = "id";
    private final String USER = "test";
    private final String EXPLORATORY_NAME = "explName";
    private final String PROJECT = "projectName";
    private final String COMPUTATIONAL_NAME = "compName";

    private static final String GROUP_JAVA = "java";
    private static final String GROUP_PIP3 = "pip3";
    private static final String GROUP_R_PKG = "r_pkg";
    private static final String GROUP_OS_PKG = "os_pkg";
    private static final String GROUP_OTHERS = "others";

    private LibInstallDTO liDto;
    private List<LibInstallDTO> libs;
    private LibInstallFormDTO libInstallFormDTO;
    private LibraryInstallDTO libraryInstallDto;

    @Mock
    private ExploratoryDAO exploratoryDAO;
    @Mock
    private ExploratoryLibDAO libraryDAO;
    @Mock
    private RequestBuilder requestBuilder;
    @Mock
    private RequestId requestId;
    @Mock
    private RESTService provisioningService;
    @Mock
    private EndpointService endpointService;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @InjectMocks
    private LibraryServiceImpl libraryService;

    @Before
    public void setUp() {
        prepareForTesting();
    }

    @Test
    public void testGetLibs() {
        Document document = new Document();
        when(libraryDAO.findExploratoryLibraries(anyString(), anyString(), anyString())).thenReturn(document);

        List<Document> expectedList = new ArrayList<>();
        List<Document> actualList = libraryService.getLibs(USER, PROJECT, EXPLORATORY_NAME, "");
        assertNotNull(actualList);
        assertEquals(expectedList, actualList);

        verify(libraryDAO).findExploratoryLibraries(USER, PROJECT, EXPLORATORY_NAME);
        verifyNoMoreInteractions(libraryDAO);
    }

    @Test
    public void getLibInfo() {
        Document document = new Document();
        when(libraryDAO.findAllLibraries(anyString(), anyString(), anyString())).thenReturn(document);

        List<LibInfoRecord> expectedList = new ArrayList<>();
        List<LibInfoRecord> actualList = libraryService.getLibInfo(USER, PROJECT, EXPLORATORY_NAME);
        assertNotNull(actualList);
        assertEquals(expectedList, actualList);

        verify(libraryDAO).findAllLibraries(USER, PROJECT, EXPLORATORY_NAME);
        verifyNoMoreInteractions(libraryDAO);
    }

    @Test
    public void getLibInfoWhenListsOfExploratoryAndComputationalLibsAreNotEmpty() {
        when(libraryDAO.findAllLibraries(anyString(), anyString(), anyString()))
                .thenReturn(getDocumentWithExploratoryAndComputationalLibs());

        List<LibInfoRecord> expectedList = getLibInfoRecordList();
        List<LibInfoRecord> actualList = libraryService.getLibInfo(USER, PROJECT, EXPLORATORY_NAME);
        assertNotNull(actualList);
        assertEquals(expectedList, actualList);

        verify(libraryDAO).findAllLibraries(USER, PROJECT, EXPLORATORY_NAME);
        verifyNoMoreInteractions(libraryDAO);
    }

    @Test
    public void installComputationalLibsWithoutOverride() {
        final LibraryInstallDTO libraryInstallDTO = new LibraryInstallDTO();
        final List<LibInstallDTO> libsToInstall = getLibs("installing");
        libraryInstallDTO.setLibs(libsToInstall);
        final UserInfo user = getUser();

        when(endpointService.get(anyString())).thenReturn(endpointDTO());
        when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString(), anyString(), anyString())).thenReturn(getUserInstanceDto());
        when(provisioningService.post(anyString(), anyString(), any(LibraryInstallDTO.class), any())).thenReturn(UUID);
        when(requestBuilder.newLibInstall(any(UserInfo.class), any(UserInstanceDTO.class),
                any(UserComputationalResource.class), anyListOf(LibInstallDTO.class), any(EndpointDTO.class))).thenReturn(libraryInstallDTO);


        final String uuid = libraryService.installComputationalLibs(user, PROJECT, EXPLORATORY_NAME,
                COMPUTATIONAL_NAME, getLibs(null), null);

        assertEquals(UUID, uuid);

        verify(libraryDAO).getLibrary(USER, PROJECT, EXPLORATORY_NAME, COMPUTATIONAL_NAME, LIB_GROUP, LIB_NAME);
        verify(requestBuilder).newLibInstall(refEq(user), refEq(getUserInstanceDto()),
                refEq(getUserComputationalResourceWithName(COMPUTATIONAL_NAME)), eq(libsToInstall), refEq(endpointDTO()));
        verify(provisioningService).post(eq(endpointDTO().getUrl() + "library/computational/lib_install"), eq(user.getAccessToken()),
                refEq(libraryInstallDTO), eq(String.class));
        verify(libraryDAO).addLibrary(eq(USER), eq(PROJECT), eq(EXPLORATORY_NAME),
                eq(COMPUTATIONAL_NAME), refEq(libsToInstall.get(0)), eq(false));
        verify(requestId).put(user.getName(), UUID);
        verify(exploratoryDAO).fetchExploratoryFields(USER, PROJECT, EXPLORATORY_NAME, COMPUTATIONAL_NAME);
        verifyNoMoreInteractions(libraryDAO, requestBuilder, provisioningService, requestId, exploratoryDAO);
    }

    @Test
    public void installComputationalLibsWhenComputationalNotFound() {
        final LibraryInstallDTO libraryInstallDTO = new LibraryInstallDTO();
        final List<LibInstallDTO> libsToInstall = getLibs("installing");
        libraryInstallDTO.setLibs(libsToInstall);
        libraryInstallDTO.setProject(PROJECT);
        final UserInfo user = getUser();

        when(endpointService.get(anyString())).thenReturn(endpointDTO());
        when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString(), anyString(), anyString())).thenReturn(getUserInstanceDto());
        when(provisioningService.post(anyString(), anyString(), any(LibraryInstallDTO.class), any())).thenReturn(UUID);
        when(requestBuilder.newLibInstall(any(UserInfo.class), any(UserInstanceDTO.class),
                any(UserComputationalResource.class), anyListOf(LibInstallDTO.class), any(EndpointDTO.class)))
                .thenReturn(libraryInstallDTO);


        expectedException.expect(DatalabException.class);
        expectedException.expectMessage("Computational with name " + COMPUTATIONAL_NAME + "X was not found");

        libraryService.installComputationalLibs(user, PROJECT, EXPLORATORY_NAME, COMPUTATIONAL_NAME + "X", getLibs(null), null);
    }

    @Test
    public void installComputationalLibsWithOverride() {
        final LibraryInstallDTO libraryInstallDTO = new LibraryInstallDTO();
        final List<LibInstallDTO> libsToInstall = getLibs("installing");
        libraryInstallDTO.setProject(PROJECT);
        libraryInstallDTO.setLibs(libsToInstall);
        final UserInfo user = getUser();

        when(endpointService.get(anyString())).thenReturn(endpointDTO());
        when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString(), anyString(), anyString())).thenReturn(getUserInstanceDto());
        when(provisioningService.post(anyString(), anyString(), any(LibraryInstallDTO.class), any())).thenReturn(UUID);
        when(requestBuilder.newLibInstall(any(UserInfo.class), any(UserInstanceDTO.class),
                any(UserComputationalResource.class), anyListOf(LibInstallDTO.class), any(EndpointDTO.class)))
                .thenReturn(libraryInstallDTO);
        when(libraryDAO.getLibrary(anyString(), anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(getLibrary(LibStatus.INSTALLED));

        final String uuid = libraryService.installComputationalLibs(user, PROJECT, EXPLORATORY_NAME,
                COMPUTATIONAL_NAME, getLibs(null), null);

        assertEquals(UUID, uuid);

        libsToInstall.get(0).setOverride(true);
        verify(exploratoryDAO).fetchExploratoryFields(USER, PROJECT, EXPLORATORY_NAME, COMPUTATIONAL_NAME);
        verify(libraryDAO).getLibrary(USER, PROJECT, EXPLORATORY_NAME, COMPUTATIONAL_NAME, LIB_GROUP, LIB_NAME);
        verify(libraryDAO).addLibrary(eq(USER), eq(PROJECT), eq(EXPLORATORY_NAME),
                eq(COMPUTATIONAL_NAME), refEq(libsToInstall.get(0)), eq(true));
        verify(requestBuilder).newLibInstall(refEq(user), refEq(getUserInstanceDto()),
                refEq(getUserComputationalResourceWithName(COMPUTATIONAL_NAME)), eq(libsToInstall), refEq(endpointDTO()));
        verify(provisioningService).post(eq(endpointDTO().getUrl() + "library/computational/lib_install"),
                eq(user.getAccessToken()),
                refEq(libraryInstallDTO), eq(String.class));
        verify(requestId).put(user.getName(), UUID);
        verifyNoMoreInteractions(libraryDAO, requestBuilder, provisioningService, requestId, exploratoryDAO);

    }


    @Test
    public void installComputationalLibsWhenLibraryIsAlreadyInstalling() {
        final LibraryInstallDTO libraryInstallDTO = new LibraryInstallDTO();
        final List<LibInstallDTO> libsToInstall = getLibs("installing");
        libraryInstallDTO.setLibs(libsToInstall);
        final UserInfo user = getUser();

        when(endpointService.get(anyString())).thenReturn(endpointDTO());
        when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString(), anyString(), anyString())).thenReturn(getUserInstanceDto());
        when(provisioningService.post(anyString(), anyString(), any(LibraryInstallDTO.class), any())).thenReturn(UUID);
        when(requestBuilder.newLibInstall(any(UserInfo.class), any(UserInstanceDTO.class),
                any(UserComputationalResource.class), anyListOf(LibInstallDTO.class), any(EndpointDTO.class)))
                .thenReturn(libraryInstallDTO);
        when(libraryDAO.getLibrary(anyString(), anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(getLibrary(LibStatus.INSTALLING));

        try {
            libraryService.installComputationalLibs(user, PROJECT, EXPLORATORY_NAME,
                    COMPUTATIONAL_NAME, getLibs(null), null);
        } catch (DatalabException e) {
            assertEquals("Library name is already installing", e.getMessage());
        }
        verify(exploratoryDAO).fetchExploratoryFields(USER, PROJECT, EXPLORATORY_NAME, COMPUTATIONAL_NAME);
        verify(libraryDAO).getLibrary(USER, PROJECT, EXPLORATORY_NAME, COMPUTATIONAL_NAME, LIB_GROUP, LIB_NAME);
        verifyNoMoreInteractions(libraryDAO, requestBuilder, provisioningService, requestId, exploratoryDAO);
    }

    @Test
    public void installExploratoryLibsWithoutOverride() {
        final LibraryInstallDTO libraryInstallDTO = new LibraryInstallDTO();
        final List<LibInstallDTO> libsToInstall = getLibs("installing");
        libraryInstallDTO.setLibs(libsToInstall);
        final UserInfo user = getUser();

        when(endpointService.get(anyString())).thenReturn(endpointDTO());
        when(exploratoryDAO.fetchRunningExploratoryFields(anyString(), anyString(), anyString())).thenReturn(getUserInstanceDto());
        when(provisioningService.post(anyString(), anyString(), any(LibraryInstallDTO.class), any())).thenReturn(UUID);
        when(requestBuilder.newLibInstall(any(UserInfo.class), any(UserInstanceDTO.class), any(EndpointDTO.class),
                anyListOf(LibInstallDTO.class))).thenReturn(libraryInstallDTO);


        final String uuid = libraryService.installExploratoryLibs(user, PROJECT, EXPLORATORY_NAME, getLibs(null), null);

        assertEquals(UUID, uuid);

        verify(libraryDAO).getLibrary(USER, PROJECT, EXPLORATORY_NAME, LIB_GROUP, LIB_NAME);
        verify(requestBuilder).newLibInstall(refEq(user), refEq(getUserInstanceDto()), eq(endpointDTO()), eq(libsToInstall));
        verify(provisioningService).post(eq(endpointDTO().getUrl() + "library/exploratory/lib_install"), eq(user.getAccessToken()),
                refEq(libraryInstallDTO), eq(String.class));
        verify(libraryDAO).addLibrary(eq(USER), eq(PROJECT), eq(EXPLORATORY_NAME), refEq(libsToInstall.get(0)), eq(false));
        verify(requestId).put(user.getName(), UUID);
        verify(exploratoryDAO).fetchRunningExploratoryFields(USER, PROJECT, EXPLORATORY_NAME);
        verifyNoMoreInteractions(libraryDAO, requestBuilder, provisioningService, requestId, exploratoryDAO);
    }

    @Test
    public void installExploratoryLibsWithOverride() {
        final LibraryInstallDTO libraryInstallDTO = new LibraryInstallDTO();
        final List<LibInstallDTO> libsToInstall = getLibs("installing");
        libraryInstallDTO.setLibs(libsToInstall);
        final UserInfo user = getUser();

        when(endpointService.get(anyString())).thenReturn(endpointDTO());
        when(exploratoryDAO.fetchRunningExploratoryFields(anyString(), anyString(), anyString())).thenReturn(getUserInstanceDto());
        when(provisioningService.post(anyString(), anyString(), any(LibraryInstallDTO.class), any())).thenReturn(UUID);
        when(requestBuilder.newLibInstall(any(UserInfo.class), any(UserInstanceDTO.class), any(EndpointDTO.class),
                anyListOf(LibInstallDTO.class))).thenReturn(libraryInstallDTO);
        when(libraryDAO.getLibrary(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(getLibrary(LibStatus.INSTALLED));

        final String uuid = libraryService.installExploratoryLibs(user, PROJECT, EXPLORATORY_NAME, getLibs(null), null);

        assertEquals(UUID, uuid);

        libsToInstall.get(0).setOverride(true);
        verify(libraryDAO).getLibrary(USER, PROJECT, EXPLORATORY_NAME, LIB_GROUP, LIB_NAME);
        verify(libraryDAO).addLibrary(eq(USER), eq(PROJECT), eq(EXPLORATORY_NAME), refEq(libsToInstall.get(0)), eq(true));
        verify(requestBuilder).newLibInstall(refEq(user), refEq(getUserInstanceDto()), eq(endpointDTO()), eq(libsToInstall));
        verify(exploratoryDAO).fetchRunningExploratoryFields(USER, PROJECT, EXPLORATORY_NAME);
        verify(provisioningService).post(eq(endpointDTO().getUrl() + "library/exploratory/lib_install"), eq(user.getAccessToken()),
                refEq(libraryInstallDTO), eq(String.class));
        verify(requestId).put(USER, uuid);
        verifyNoMoreInteractions(libraryDAO, requestBuilder, provisioningService, requestId, exploratoryDAO);
    }

    @Test
    public void installExploratoryLibsWhenLibIsAlreadyInstalling() {
        final LibraryInstallDTO libraryInstallDTO = new LibraryInstallDTO();
        final List<LibInstallDTO> libsToInstall = getLibs("installing");
        libraryInstallDTO.setLibs(libsToInstall);
        final UserInfo user = getUser();

        when(endpointService.get(anyString())).thenReturn(endpointDTO());
        when(exploratoryDAO.fetchRunningExploratoryFields(anyString(), anyString(), anyString())).thenReturn(getUserInstanceDto());
        when(provisioningService.post(anyString(), anyString(), any(LibraryInstallDTO.class), any())).thenReturn(UUID);
        when(requestBuilder.newLibInstall(any(UserInfo.class), any(UserInstanceDTO.class), any(EndpointDTO.class),
                anyListOf(LibInstallDTO.class))).thenReturn(libraryInstallDTO);
        when(libraryDAO.getLibrary(anyString(), anyString(), anyString(), anyString(), anyString())).thenReturn(getLibrary(LibStatus.INSTALLING));

        try {
            libraryService.installExploratoryLibs(user, PROJECT, EXPLORATORY_NAME, getLibs(null), null);
        } catch (DatalabException e) {
            assertEquals("Library name is already installing", e.getMessage());
        }

        verify(libraryDAO).getLibrary(USER, PROJECT, EXPLORATORY_NAME, LIB_GROUP, LIB_NAME);
        verify(exploratoryDAO).fetchRunningExploratoryFields(USER, PROJECT, EXPLORATORY_NAME);
        verifyNoMoreInteractions(libraryDAO, requestBuilder, provisioningService, requestId, exploratoryDAO);

    }

    @Test
    public void getComputeLibGroups() {
        List<Object> computeGroups = Arrays.asList(GROUP_PIP3, GROUP_OTHERS, GROUP_R_PKG, GROUP_OS_PKG, GROUP_JAVA);

        List<String> computeGroupsResult = libraryService.getComputeLibGroups();

        assertEquals("lists are not equal", computeGroups, computeGroupsResult);
    }

    @Test
    public void getExploratoryJupyterLibGroups() {
        List<Object> exploratoryGroups = Arrays.asList(GROUP_PIP3, GROUP_OTHERS, GROUP_OS_PKG, GROUP_R_PKG, GROUP_JAVA);
        when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString(), anyString())).thenReturn(getJupyterUserInstanceDtoForLibGroups());

        List<String> exploratoryGroupsResult = libraryService.getExploratoryLibGroups(getUser(), PROJECT, EXPLORATORY_NAME);

        assertEquals("lists are not equal", exploratoryGroups, exploratoryGroupsResult);
        verify(exploratoryDAO).fetchExploratoryFields(USER, PROJECT, EXPLORATORY_NAME);
    }

    @Test
    public void getExploratoryRstudioLibGroups() {
        List<Object> exploratoryGroups = Arrays.asList(GROUP_PIP3, GROUP_OTHERS, GROUP_OS_PKG, GROUP_R_PKG);
        when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString(), anyString())).thenReturn(getRstudioUserInstanceDtoForLibGroups());

        List<String> exploratoryGroupsResult = libraryService.getExploratoryLibGroups(getUser(), PROJECT, EXPLORATORY_NAME);

        assertEquals("lists are not equal", exploratoryGroups, exploratoryGroupsResult);
        verify(exploratoryDAO).fetchExploratoryFields(USER, PROJECT, EXPLORATORY_NAME);
    }

    private Library getLibrary(LibStatus status) {
        return new Library(LIB_GROUP, LIB_NAME, "1", status, "");
    }

    private List<LibInstallDTO> getLibs(String status) {
        final LibInstallDTO libInstallDTO = new LibInstallDTO(LIB_GROUP, LIB_NAME, LIB_VERSION);
        libInstallDTO.setStatus(status);
        return Collections.singletonList(libInstallDTO);
    }

    private UserInfo getUser() {
        return new UserInfo(USER, "token123");
    }

    private void prepareForTesting() {
        liDto = new LibInstallDTO("someGroup", "someName", "someVersion");
        libs = Collections.singletonList(liDto);
        libInstallFormDTO = new LibInstallFormDTO();
        libInstallFormDTO.setNotebookName(EXPLORATORY_NAME);
        libInstallFormDTO.setComputationalName(COMPUTATIONAL_NAME);
        libInstallFormDTO.setLibs(libs);
        libraryInstallDto = new LibraryInstallDTO().withExploratoryName(EXPLORATORY_NAME)
                .withComputationalName(COMPUTATIONAL_NAME).withApplicationName("");
        libraryInstallDto.setLibs(new ArrayList<>());
    }

    private UserComputationalResource getUserComputationalResourceWithName(String name) {
        UserComputationalResource resource = new UserComputationalResource();
        resource.setComputationalName(name);
        resource.setComputationalId("someId");
        resource.setImageName("someImageName");
        return resource;
    }

    private UserInstanceDTO getUserInstanceDto() {
        final UserInstanceDTO userInstanceDTO = new UserInstanceDTO().withUser(USER).withExploratoryName(EXPLORATORY_NAME);
        userInstanceDTO.getResources().add(getUserComputationalResourceWithName(COMPUTATIONAL_NAME));
        return userInstanceDTO;
    }

    private UserInstanceDTO getJupyterUserInstanceDtoForLibGroups() {
        return new UserInstanceDTO()
                .withUser(USER)
                .withExploratoryName(EXPLORATORY_NAME)
                .withTemplateName(NotebookTemplate.JUPYTER.getName());
    }

    private UserInstanceDTO getRstudioUserInstanceDtoForLibGroups() {
        return new UserInstanceDTO()
                .withUser(USER)
                .withExploratoryName(EXPLORATORY_NAME)
                .withTemplateName(NotebookTemplate.RSTUDIO.getName());
    }

    private List<Document> getExpLibsList() {
        Document explLibsDoc = new Document();
        explLibsDoc.append(ExploratoryLibDAO.LIB_NAME, "expLibName");
        explLibsDoc.append(ExploratoryLibDAO.LIB_VERSION, "expLibVersion");
        explLibsDoc.append(ExploratoryLibDAO.LIB_GROUP, "expLibGroup");
        explLibsDoc.append(ExploratoryLibDAO.STATUS, "expLibStatus");
        explLibsDoc.append(ExploratoryLibDAO.ERROR_MESSAGE, "expLibErrorMessage");
        return Collections.singletonList(explLibsDoc);
    }

    private Document getCompLibs() {
        Document compLibs = new Document();
        compLibs.append(ExploratoryLibDAO.LIB_NAME, "compLibName");
        compLibs.append(ExploratoryLibDAO.LIB_VERSION, "compLibVersion");
        compLibs.append(ExploratoryLibDAO.LIB_GROUP, "compLibGroup");
        compLibs.append(ExploratoryLibDAO.STATUS, "compLibStatus");
        compLibs.append(ExploratoryLibDAO.ERROR_MESSAGE, "compLibErrorMessage");

        Document compResourcesAndLibs = new Document();
        compResourcesAndLibs.append("compName", Collections.singletonList(compLibs));
        return compResourcesAndLibs;
    }

    private Document getDocumentWithExploratoryAndComputationalLibs() {
        return new Document().append(ExploratoryLibDAO.EXPLORATORY_LIBS, getExpLibsList())
                .append(ExploratoryLibDAO.COMPUTATIONAL_LIBS, getCompLibs());
    }

    private List<LibInfoRecord> getLibInfoRecordList() {
        LibKey explLibKey = new LibKey("expLibName", "expLibVersion", "expLibGroup");
        List<LibraryStatus> explLibStatuses = Collections.singletonList(
                new LibraryStatus(EXPLORATORY_NAME, "notebook", "expLibStatus", "expLibErrorMessage", null, null));

        LibKey compLibKey = new LibKey("compLibName", "compLibVersion", "compLibGroup");
        List<LibraryStatus> compLibStatuses = Collections.singletonList(
                new LibraryStatus("compName", "cluster", "compLibStatus", "compLibErrorMessage", null, null));

        return Arrays.asList(
                new LibInfoRecord(compLibKey, compLibStatuses),
                new LibInfoRecord(explLibKey, explLibStatuses)
        );
    }

    private EndpointDTO endpointDTO() {
        return new EndpointDTO("test", "url", "", null, EndpointDTO.EndpointStatus.ACTIVE, CloudProvider.AWS);
    }

}
