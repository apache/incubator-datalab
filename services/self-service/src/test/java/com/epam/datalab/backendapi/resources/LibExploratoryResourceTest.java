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
import com.epam.datalab.backendapi.dao.ExploratoryDAO;
import com.epam.datalab.backendapi.domain.RequestId;
import com.epam.datalab.backendapi.resources.dto.LibInfoRecord;
import com.epam.datalab.backendapi.resources.dto.LibInstallFormDTO;
import com.epam.datalab.backendapi.resources.dto.LibKey;
import com.epam.datalab.backendapi.resources.dto.LibraryDTO;
import com.epam.datalab.backendapi.resources.dto.LibraryStatus;
import com.epam.datalab.backendapi.resources.dto.SearchLibsFormDTO;
import com.epam.datalab.backendapi.service.ExternalLibraryService;
import com.epam.datalab.backendapi.service.LibraryService;
import com.epam.datalab.dto.UserInstanceDTO;
import com.epam.datalab.dto.computational.UserComputationalResource;
import com.epam.datalab.dto.exploratory.LibInstallDTO;
import com.epam.datalab.dto.exploratory.LibraryInstallDTO;
import com.epam.datalab.exceptions.DatalabException;
import com.epam.datalab.rest.client.RESTService;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.apache.http.HttpStatus;
import org.bson.Document;
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
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.anyListOf;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.refEq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class LibExploratoryResourceTest extends TestBase {
    private static final String AUDIT_MESSAGE = "Install libs: %s";
    private static final String LIB_GROUP = "group";
    private static final String LIB_NAME = "name";
    private static final String LIB_VERSION = "version";
    private static final String EXPLORATORY_NAME = "explName";
    private static final String PROJECT = "projectName";
    private static final String COMPUTATIONAL_NAME = "compName";
    private static final String UUID = "uid";
    private ExploratoryDAO exploratoryDAO = mock(ExploratoryDAO.class);
    private LibraryService libraryService = mock(LibraryService.class);
    private RESTService provisioningService = mock(RESTService.class);
    private ExternalLibraryService externalLibraryService = mock(ExternalLibraryService.class);
    private RequestId requestId = mock(RequestId.class);

    @Rule
    public final ResourceTestRule resources = getResourceTestRuleInstance(
            new LibExploratoryResource(exploratoryDAO, libraryService, externalLibraryService));

    @Before
    public void setup() throws AuthenticationException {
        authSetup();
    }

    @Test
    public void getComputeLibGroupList() {
        when(libraryService.getComputeLibGroups()).thenReturn(Collections.emptyList());

        final Response response = resources.getJerseyTest()
                .target("/infrastructure_provision/exploratory_environment/lib-groups/compute")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .get();

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(libraryService).getComputeLibGroups();
        verifyNoMoreInteractions(libraryService);
    }

    @Test
    public void getExploratoryLibGroupList() {
        when(libraryService.getExploratoryLibGroups(any(UserInfo.class), anyString(), anyString())).thenReturn(Collections.emptyList());

        final Response response = resources.getJerseyTest()
                .target("/infrastructure_provision/exploratory_environment/lib-groups/exploratory")
                .queryParam("project", "projectName")
                .queryParam("exploratory", "explName")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .get();

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(libraryService).getExploratoryLibGroups(getUserInfo(), "projectName", "explName");
        verifyNoMoreInteractions(libraryService);
    }

    @Test
    public void getLibList() {
        when(libraryService.getLibs(anyString(), anyString(), anyString(), anyString())).thenReturn(getDocuments());
        final Response response = resources.getJerseyTest()
                .target("/infrastructure_provision/exploratory_environment/lib_list")
                .queryParam("project_name", "projectName")
                .queryParam("exploratory_name", "explName")
                .queryParam("computational_name", "compName")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .get();

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertEquals(getDocuments(), response.readEntity(new GenericType<List<Document>>() {
        }));
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(libraryService).getLibs(USER.toLowerCase(), "projectName", "explName", "compName");
        verifyNoMoreInteractions(libraryService);
    }

    @Test
    public void getLibListWithFailedAuth() throws AuthenticationException {
        authFailSetup();
        when(libraryService.getLibs(anyString(), anyString(), anyString(), anyString())).thenReturn(getDocuments());
        final Response response = resources.getJerseyTest()
                .target("/infrastructure_provision/exploratory_environment/lib_list")
                .queryParam("project_name", "projectName")
                .queryParam("exploratory_name", "explName")
                .queryParam("computational_name", "compName")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .get();

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertEquals(getDocuments(), response.readEntity(new GenericType<List<Document>>() {
        }));
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(libraryService).getLibs(USER.toLowerCase(), "projectName", "explName", "compName");
        verifyNoMoreInteractions(libraryService);
    }

    @Test
    public void getLibListWithException() {
        doThrow(new DatalabException("Cannot load installed libraries"))
                .when(libraryService).getLibs(anyString(), anyString(), anyString(), anyString());
        final Response response = resources.getJerseyTest()
                .target("/infrastructure_provision/exploratory_environment/lib_list")
                .queryParam("project_name", "projectName")
                .queryParam("exploratory_name", "explName")
                .queryParam("computational_name", "compName")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .get();

        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(libraryService).getLibs(USER.toLowerCase(), "projectName", "explName", "compName");
        verifyNoMoreInteractions(libraryService);
    }

    @Test
    public void getLibListFormatted() {
        when(libraryService.getLibInfo(anyString(), anyString(), anyString())).thenReturn(getLibInfoRecords());
        final Response response = resources.getJerseyTest()
                .target("/infrastructure_provision/exploratory_environment/lib_list/formatted")
                .queryParam("exploratory_name", "explName")
                .queryParam("project_name", "projectName")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .get();

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(libraryService).getLibInfo(USER.toLowerCase(), "projectName", "explName");
        verifyNoMoreInteractions(libraryService);
    }

    @Test
    public void getLibListFormattedWithFailedAuth() throws AuthenticationException {
        authFailSetup();
        when(libraryService.getLibInfo(anyString(), anyString(), anyString())).thenReturn(getLibInfoRecords());
        final Response response = resources.getJerseyTest()
                .target("/infrastructure_provision/exploratory_environment/lib_list/formatted")
                .queryParam("exploratory_name", "explName")
                .queryParam("project_name", "projectName")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .get();

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(libraryService).getLibInfo(USER.toLowerCase(), "projectName", "explName");
        verifyNoMoreInteractions(libraryService);
    }

    @Test
    public void getLibListFormattedWithException() {
        doThrow(new DatalabException("Cannot load  formatted list of installed libraries"))
                .when(libraryService).getLibInfo(anyString(), anyString(), anyString());
        final Response response = resources.getJerseyTest()
                .target("/infrastructure_provision/exploratory_environment/lib_list/formatted")
                .queryParam("exploratory_name", "explName")
                .queryParam("project_name", "projectName")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .get();

        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(libraryService).getLibInfo(USER.toLowerCase(), "projectName", "explName");
        verifyNoMoreInteractions(libraryService);
    }

    @Test
    public void libInstall() {
        List<LibInstallDTO> libInstallDTOS = singletonList(new LibInstallDTO(LIB_GROUP, LIB_NAME, LIB_VERSION));
        when(libraryService.installComputationalLibs(any(UserInfo.class), anyString(), anyString(),
                anyString(), anyListOf(LibInstallDTO.class), anyString())).thenReturn(UUID);
        LibInstallFormDTO libInstallFormDTO = new LibInstallFormDTO();
        libInstallFormDTO.setComputationalName(COMPUTATIONAL_NAME);
        libInstallFormDTO.setNotebookName(EXPLORATORY_NAME);
        libInstallFormDTO.setProject(PROJECT);
        libInstallFormDTO.setLibs(libInstallDTOS);
        final Response response = resources.getJerseyTest()
                .target("/infrastructure_provision/exploratory_environment/lib_install")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .post(Entity.json(libInstallFormDTO));

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));
        assertEquals(UUID, response.readEntity(String.class));

        verify(libraryService).installComputationalLibs(refEq(getUserInfo()), eq(PROJECT),
                eq(EXPLORATORY_NAME), eq(COMPUTATIONAL_NAME), eq(libInstallDTOS), eq(getAuditInfo(libInstallDTOS)));
        verifyNoMoreInteractions(libraryService);
        verifyZeroInteractions(provisioningService, requestId);
    }


    @Test
    public void libInstallWithoutComputational() {
        List<LibInstallDTO> libInstallDTOS = singletonList(new LibInstallDTO(LIB_GROUP, LIB_NAME, LIB_VERSION));
        when(libraryService.installExploratoryLibs(any(UserInfo.class), anyString(), anyString(), anyListOf(LibInstallDTO.class), anyString())).thenReturn(UUID);
        LibInstallFormDTO libInstallFormDTO = new LibInstallFormDTO();
        libInstallFormDTO.setNotebookName(EXPLORATORY_NAME);
        libInstallFormDTO.setLibs(libInstallDTOS);
        libInstallFormDTO.setProject(PROJECT);
        final Response response = resources.getJerseyTest()
                .target("/infrastructure_provision/exploratory_environment/lib_install")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .post(Entity.json(libInstallFormDTO));

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));
        assertEquals(UUID, response.readEntity(String.class));

        verify(libraryService).installExploratoryLibs(refEq(getUserInfo()), eq(PROJECT),
                eq(EXPLORATORY_NAME), eq(libInstallDTOS), eq(getAuditInfo(libInstallDTOS)));
        verifyNoMoreInteractions(libraryService);
        verifyZeroInteractions(provisioningService, requestId);
    }

    @Test
    public void getLibraryListWithFailedAuth() throws AuthenticationException {
        authFailSetup();
        when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(getUserInstanceDto());
        SearchLibsFormDTO searchLibsFormDTO = new SearchLibsFormDTO();
        searchLibsFormDTO.setComputationalName("compName");
        searchLibsFormDTO.setNotebookName("explName");
        searchLibsFormDTO.setGroup("someGroup");
        searchLibsFormDTO.setStartWith("someText");
        searchLibsFormDTO.setProjectName("projectName");
        final Response response = resources.getJerseyTest()
                .target("/infrastructure_provision/exploratory_environment/search/lib_list")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .post(Entity.json(searchLibsFormDTO));

        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(exploratoryDAO).fetchExploratoryFields(USER.toLowerCase(), "projectName", "explName", "compName");
        verifyNoMoreInteractions(exploratoryDAO);
    }

    @Test
    public void getLibraryListWithException() {
        when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(getUserInstanceDto());
        SearchLibsFormDTO searchLibsFormDTO = new SearchLibsFormDTO();
        searchLibsFormDTO.setComputationalName("compName");
        searchLibsFormDTO.setNotebookName("explName");
        searchLibsFormDTO.setGroup("someGroup");
        searchLibsFormDTO.setStartWith("someText");
        searchLibsFormDTO.setProjectName("projectName");
        final Response response = resources.getJerseyTest()
                .target("/infrastructure_provision/exploratory_environment/search/lib_list")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .post(Entity.json(searchLibsFormDTO));

        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(exploratoryDAO).fetchExploratoryFields(USER.toLowerCase(), "projectName", "explName", "compName");
        verifyNoMoreInteractions(exploratoryDAO);
    }

    @Test
    public void getLibraryListWithoutComputationalWithException() {
        when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString(), anyString()))
                .thenReturn(getUserInstanceDto());
        SearchLibsFormDTO searchLibsFormDTO = new SearchLibsFormDTO();
        searchLibsFormDTO.setComputationalName("");
        searchLibsFormDTO.setNotebookName("explName");
        searchLibsFormDTO.setGroup("someGroup");
        searchLibsFormDTO.setStartWith("someText");
        searchLibsFormDTO.setProjectName("projectName");
        final Response response = resources.getJerseyTest()
                .target("/infrastructure_provision/exploratory_environment/search/lib_list")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .post(Entity.json(searchLibsFormDTO));

        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(exploratoryDAO).fetchExploratoryFields(USER.toLowerCase(), "projectName", "explName");
        verifyNoMoreInteractions(exploratoryDAO);
    }

    @Test
    public void getMavenArtifact() {
        when(externalLibraryService.getLibrary(anyString(), anyString(), anyString())).thenReturn(libraryDto());
        final Response response = resources.getJerseyTest()
                .target("/infrastructure_provision/exploratory_environment/search/lib_list/maven")
                .queryParam("artifact", "group:artifact:version")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .get();

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));
        final LibraryDTO libraryDTO = response.readEntity(LibraryDTO.class);
        assertEquals("test", libraryDTO.getName());
        assertEquals("1.0", libraryDTO.getVersion());

        verify(externalLibraryService).getLibrary("group", "artifact", "version");
        verifyNoMoreInteractions(externalLibraryService);
    }

    @Test
    public void getMavenArtifactWithValidationException() {
        when(externalLibraryService.getLibrary(anyString(), anyString(), anyString())).thenReturn(libraryDto());
        final Response response = resources.getJerseyTest()
                .target("/infrastructure_provision/exploratory_environment/search/lib_list/maven")
                .queryParam("artifact", "group:artifact")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .get();

        assertEquals(HttpStatus.SC_BAD_REQUEST, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        assertEquals("{\"errors\":[\"query param artifact Wrong library name format. Should be <groupId>:<artifactId>:<versionId>\"]}",
                response.readEntity(String.class));

        verifyZeroInteractions(externalLibraryService);
    }

    private LibraryDTO libraryDto() {
        return new LibraryDTO(
                "test", "1.0");
    }

    private UserInstanceDTO getUserInstanceDto() {
        UserComputationalResource ucResource = new UserComputationalResource();
        ucResource.setComputationalName("compName");
        return new UserInstanceDTO()
                .withUser(USER)
                .withExploratoryName("explName")
                .withProject(PROJECT)
                .withResources(singletonList(ucResource));
    }

    private List<Document> getDocuments() {
        return singletonList(new Document());
    }

    private List<LibInfoRecord> getLibInfoRecords() {
        return singletonList(new LibInfoRecord(
                new LibKey(), singletonList(new LibraryStatus())));
    }

    private LibraryInstallDTO getLibraryInstallDTO() {
        return new LibraryInstallDTO().withComputationalName("compName");
    }

    private String getAuditInfo(List<LibInstallDTO> libs) {
        return String.format(AUDIT_MESSAGE, libs
                .stream()
                .map(LibInstallDTO::getName)
                .collect(Collectors.joining(", ")));
    }
}
