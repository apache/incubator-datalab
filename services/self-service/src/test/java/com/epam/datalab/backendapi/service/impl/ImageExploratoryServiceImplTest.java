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
import com.epam.datalab.backendapi.dao.ImageExploratoryDAO;
import com.epam.datalab.backendapi.domain.EndpointDTO;
import com.epam.datalab.backendapi.domain.ProjectDTO;
import com.epam.datalab.backendapi.resources.dto.ImageInfoDTO;
import com.epam.datalab.backendapi.resources.dto.ImageInfoRecord;
import com.epam.datalab.backendapi.service.EndpointService;
import com.epam.datalab.backendapi.service.ProjectService;
import com.epam.datalab.backendapi.util.RequestBuilder;
import com.epam.datalab.cloud.CloudProvider;
import com.epam.datalab.dto.UserInstanceDTO;
import com.epam.datalab.dto.exploratory.ExploratoryImageDTO;
import com.epam.datalab.dto.exploratory.ExploratoryStatusDTO;
import com.epam.datalab.dto.exploratory.ImageStatus;
import com.epam.datalab.dto.exploratory.LibStatus;
import com.epam.datalab.exceptions.DatalabException;
import com.epam.datalab.exceptions.ResourceAlreadyExistException;
import com.epam.datalab.exceptions.ResourceNotFoundException;
import com.epam.datalab.model.ResourceType;
import com.epam.datalab.model.exploratory.Image;
import com.epam.datalab.model.library.Library;
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
import java.util.Date;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.anyVararg;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ImageExploratoryServiceImplTest {
    private final String USER = "test";
    private final String TOKEN = "token";
    private final String EXPLORATORY_NAME = "expName";
    private final String PROJECT = "project";

    private UserInfo userInfo;
    private UserInstanceDTO userInstance;
    private Image image;

    @Mock
    private ExploratoryDAO exploratoryDAO;
    @Mock
    private ImageExploratoryDAO imageExploratoryDao;
    @Mock
    private ExploratoryLibDAO libDAO;
    @Mock
    private RESTService provisioningService;
    @Mock
    private RequestBuilder requestBuilder;
    @Mock
    private EndpointService endpointService;
    @Mock
    private ProjectService projectService;

    @InjectMocks
    private ImageExploratoryServiceImpl imageExploratoryService;

    @Rule
    public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setUp() {
        userInfo = getUserInfo();
        userInstance = getUserInstanceDto();
        image = fetchImage();
    }

    @Test
    public void createImage() {
        when(projectService.get(anyString())).thenReturn(getProjectDTO());
        when(exploratoryDAO.fetchRunningExploratoryFields(anyString(), anyString(), anyString())).thenReturn(userInstance);
        when(imageExploratoryDao.exist(anyString(), anyString())).thenReturn(false);

        when(libDAO.getLibraries(anyString(), anyString(), anyString())).thenReturn(Collections.singletonList(getLibrary()));
        doNothing().when(imageExploratoryDao).save(any(Image.class));
        when(exploratoryDAO.updateExploratoryStatus(any(ExploratoryStatusDTO.class)))
                .thenReturn(mock(UpdateResult.class));
        ExploratoryImageDTO eiDto = new ExploratoryImageDTO();
        when(endpointService.get(anyString())).thenReturn(endpointDTO());
        when(requestBuilder.newExploratoryImageCreate(any(UserInfo.class), any(UserInstanceDTO.class), anyString(),
                any(EndpointDTO.class), any(ProjectDTO.class))).thenReturn(eiDto);

        String expectedUuid = "someUuid";
        when(provisioningService.post(anyString(), anyString(), any(ExploratoryImageDTO.class), any()))
                .thenReturn(expectedUuid);

        String imageName = "someImageName", imageDescription = "someDescription";
        String actualUuid = imageExploratoryService.createImage(userInfo, PROJECT, EXPLORATORY_NAME,
                imageName, imageDescription);
        assertNotNull(actualUuid);
        assertEquals(expectedUuid, actualUuid);

        verify(projectService).get(PROJECT);
        verify(exploratoryDAO).fetchRunningExploratoryFields(USER, PROJECT, EXPLORATORY_NAME);
        verify(exploratoryDAO).updateExploratoryStatus(any(ExploratoryStatusDTO.class));
        verify(imageExploratoryDao).exist(imageName, PROJECT);
        verify(imageExploratoryDao).save(any(Image.class));
        verify(libDAO).getLibraries(USER, PROJECT, EXPLORATORY_NAME);
        verify(requestBuilder).newExploratoryImageCreate(userInfo, userInstance, imageName, endpointDTO(), getProjectDTO());
        verify(endpointService).get(anyString());
        verify(provisioningService).post(endpointDTO().getUrl() + "exploratory/image", TOKEN, eiDto, String.class);
        verifyNoMoreInteractions(projectService, exploratoryDAO, imageExploratoryDao, libDAO, requestBuilder, endpointService, provisioningService);
    }

    @Test
    public void createImageWhenMethodFetchRunningExploratoryFieldsThrowsException() {
        doThrow(new DatalabException("Running exploratory instance for user with name not found."))
                .when(exploratoryDAO).fetchRunningExploratoryFields(anyString(), anyString(), anyString());

        String imageName = "someImageName", imageDescription = "someDescription";

        try {
            imageExploratoryService.createImage(userInfo, PROJECT, EXPLORATORY_NAME, imageName, imageDescription);
        } catch (DatalabException e) {
            assertEquals("Running exploratory instance for user with name not found.", e.getMessage());
        }
        verify(exploratoryDAO).fetchRunningExploratoryFields(USER, PROJECT, EXPLORATORY_NAME);
        verifyNoMoreInteractions(exploratoryDAO);
    }

    @Test
    public void createImageWhenResourceAlreadyExists() {
        when(exploratoryDAO.fetchRunningExploratoryFields(anyString(), anyString(), anyString())).thenReturn(userInstance);
        when(imageExploratoryDao.exist(anyString(), anyString())).thenReturn(true);

        expectedException.expect(ResourceAlreadyExistException.class);
        expectedException.expectMessage("Image with name someImageName is already exist");

        String imageName = "someImageName", imageDescription = "someDescription";
        imageExploratoryService.createImage(userInfo, PROJECT, EXPLORATORY_NAME, imageName, imageDescription);
    }

    @Test
    public void createImageWhenMethodNewExploratoryImageCreateThrowsException() {
        when(projectService.get(anyString())).thenReturn(getProjectDTO());
        when(exploratoryDAO.fetchRunningExploratoryFields(anyString(), anyString(), anyString())).thenReturn(userInstance);
        when(imageExploratoryDao.exist(anyString(), anyString())).thenReturn(false);

        when(libDAO.getLibraries(anyString(), anyString(), anyString())).thenReturn(Collections.singletonList(getLibrary()));
        doNothing().when(imageExploratoryDao).save(any(Image.class));
        when(exploratoryDAO.updateExploratoryStatus(any(ExploratoryStatusDTO.class)))
                .thenReturn(mock(UpdateResult.class));
        doThrow(new DatalabException("Cannot create instance of resource class")).when(requestBuilder)
                .newExploratoryImageCreate(any(UserInfo.class), any(UserInstanceDTO.class), anyString(), any(EndpointDTO.class), any(ProjectDTO.class));
        when(endpointService.get(anyString())).thenReturn(endpointDTO());

        String imageName = "someImageName", imageDescription = "someDescription";
        try {
            imageExploratoryService.createImage(userInfo, PROJECT, EXPLORATORY_NAME, imageName, imageDescription);
        } catch (DatalabException e) {
            assertEquals("Cannot create instance of resource class", e.getMessage());
        }

        verify(projectService).get(PROJECT);
        verify(exploratoryDAO).fetchRunningExploratoryFields(USER, PROJECT, EXPLORATORY_NAME);
        verify(exploratoryDAO).updateExploratoryStatus(any(ExploratoryStatusDTO.class));
        verify(imageExploratoryDao).exist(imageName, PROJECT);
        verify(imageExploratoryDao).save(any(Image.class));
        verify(libDAO).getLibraries(USER, PROJECT, EXPLORATORY_NAME);
        verify(requestBuilder).newExploratoryImageCreate(userInfo, userInstance, imageName, endpointDTO(), getProjectDTO());
        verify(endpointService).get(anyString());
        verifyNoMoreInteractions(projectService, exploratoryDAO, imageExploratoryDao, libDAO, requestBuilder, endpointService);
    }

    @Test
    public void finishImageCreate() {
        when(exploratoryDAO.updateExploratoryStatus(any(ExploratoryStatusDTO.class)))
                .thenReturn(mock(UpdateResult.class));
        doNothing().when(imageExploratoryDao).updateImageFields(any(Image.class));
        doNothing().when(exploratoryDAO).updateExploratoryIp(anyString(), anyString(), anyString(), anyString());

        String notebookIp = "someIp";
        imageExploratoryService.finishImageCreate(image, EXPLORATORY_NAME, notebookIp);

        verify(exploratoryDAO).updateExploratoryStatus(any(ExploratoryStatusDTO.class));
        verify(exploratoryDAO).updateExploratoryIp(USER, PROJECT, notebookIp, EXPLORATORY_NAME);
        verify(imageExploratoryDao).updateImageFields(image);
        verifyNoMoreInteractions(exploratoryDAO, imageExploratoryDao);
    }

    @Test
    public void finishImageCreateWhenMethodUpdateExploratoryIpThrowsException() {
        when(exploratoryDAO.updateExploratoryStatus(any(ExploratoryStatusDTO.class)))
                .thenReturn(mock(UpdateResult.class));
        doNothing().when(imageExploratoryDao).updateImageFields(any(Image.class));
        doThrow(new ResourceNotFoundException("Exploratory for user with name not found"))
                .when(exploratoryDAO).updateExploratoryIp(anyString(), anyString(), anyString(), anyString());

        String notebookIp = "someIp";
        try {
            imageExploratoryService.finishImageCreate(image, EXPLORATORY_NAME, notebookIp);
        } catch (ResourceNotFoundException e) {
            assertEquals("Exploratory for user with name not found", e.getMessage());
        }

        verify(exploratoryDAO).updateExploratoryStatus(any(ExploratoryStatusDTO.class));
        verify(exploratoryDAO).updateExploratoryIp(USER, PROJECT, notebookIp, EXPLORATORY_NAME);
        verify(imageExploratoryDao).updateImageFields(image);
        verifyNoMoreInteractions(exploratoryDAO, imageExploratoryDao);
    }

    @Test
    public void finishImageCreateWhenNotebookIpIsNull() {
        when(exploratoryDAO.updateExploratoryStatus(any(ExploratoryStatusDTO.class)))
                .thenReturn(mock(UpdateResult.class));
        doNothing().when(imageExploratoryDao).updateImageFields(any(Image.class));

        imageExploratoryService.finishImageCreate(image, EXPLORATORY_NAME, null);

        verify(exploratoryDAO).updateExploratoryStatus(any(ExploratoryStatusDTO.class));
        verify(exploratoryDAO, never()).updateExploratoryIp(USER, PROJECT, null, EXPLORATORY_NAME);
        verify(imageExploratoryDao).updateImageFields(image);
        verifyNoMoreInteractions(exploratoryDAO, imageExploratoryDao);
    }

//    @Test
//    public void getCreatedImages() {
//        ImageInfoDTO imageInfoDTO = getImageInfoDTO();
//        List<ImageInfoDTO> expectedRecordList = Collections.singletonList(imageInfoDTO);
//        when(imageExploratoryDao.getImages(anyString(), anyString(), anyString(), anyString(), anyVararg()))
//                .thenReturn(expectedRecordList);
//
//        List<ImageInfoDTO> actualRecordList = imageExploratoryService.getNotFailedImages(getUserInfo(),
//                "someImage", "someProject", "someEndpoint");
//        assertNotNull(actualRecordList);
//        assertEquals(1, actualRecordList.size());
//        assertEquals(expectedRecordList, actualRecordList);
//
//        verify(imageExploratoryDao).getImages(USER, "someImage", "someProject", "someEndpoint", ImageStatus.ACTIVE, ImageStatus.CREATING);
//        //verifyNoMoreInteractions(imageExploratoryDao);
//    }

    @Test
    public void getImage() {
        ImageInfoRecord expectedImageInfoRecord = getImageInfoRecord();
        when(imageExploratoryDao.getImage(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(Optional.of(expectedImageInfoRecord));

        ImageInfoRecord actualImageInfoRecord = imageExploratoryService.getImage(USER, "someName", "someProject", "someEndpoint");
        assertNotNull(actualImageInfoRecord);
        assertEquals(expectedImageInfoRecord, actualImageInfoRecord);

        verify(imageExploratoryDao).getImage(USER, "someName", "someProject", "someEndpoint");
        verifyNoMoreInteractions(imageExploratoryDao);
    }

    @Test
    public void getImageWhenMethodGetImageReturnsOptionalEmpty() {
        when(imageExploratoryDao.getImage(anyString(), anyString(), anyString(), anyString())).thenReturn(Optional.empty());
        expectedException.expect(ResourceNotFoundException.class);
        expectedException.expectMessage(String.format("Image with name %s was not found for user %s",
                "someImageName", USER));
        imageExploratoryService.getImage(USER, "someImageName", "someProject", "someEndpoint");
    }

    @Test
    public void getImagesForProject() {
        when(imageExploratoryDao.getImagesForProject(anyString())).thenReturn(Collections.singletonList(getImageInfoRecord()));

        imageExploratoryService.getImagesForProject(PROJECT);

        verify(imageExploratoryDao).getImagesForProject(PROJECT);
        verifyNoMoreInteractions(imageExploratoryDao);
    }

    private ImageInfoRecord getImageInfoRecord() {
        return new ImageInfoRecord("someName",
                new Date(),
                "someDescription",
                "someProject",
                "someEndpoint",
                "someUser",
                "someApp",
                "someTemplate",
                "someInstance",
                CloudProvider.GENERAL,
                "someDockerImage",
                "someFullName",
                ImageStatus.ACTIVE,
                null,
                null,
                null,
                null,
                null);
    }

    private Image fetchImage() {
        return Image.builder()
                .name("someImageName")
                .description("someDescription")
                .status(ImageStatus.CREATING)
                .user(USER)
                .project(PROJECT)
                .libraries(Collections.singletonList(getLibrary()))
                .computationalLibraries(Collections.emptyMap())
                .dockerImage("someImageName")
                .exploratoryId("explId").build();
    }

    private Library getLibrary() {
        return new Library("someGroup", "someName", "someVersion", LibStatus.INSTALLED,
                "someErrorMessage").withType(ResourceType.EXPLORATORY);
    }

    private UserInstanceDTO getUserInstanceDto() {
        return new UserInstanceDTO()
                .withUser(USER)
                .withExploratoryName(EXPLORATORY_NAME)
                .withExploratoryId("explId")
                .withProject(PROJECT);
    }

    private UserInfo getUserInfo() {
        return new UserInfo(USER, TOKEN);
    }

    private EndpointDTO endpointDTO() {
        return new EndpointDTO("test", "url", "", null, EndpointDTO.EndpointStatus.ACTIVE, CloudProvider.AWS);
    }

    private ProjectDTO getProjectDTO() {
        return ProjectDTO.builder().name(PROJECT).build();
    }
}
