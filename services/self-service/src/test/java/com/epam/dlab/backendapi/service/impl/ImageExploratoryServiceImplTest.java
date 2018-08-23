/*
 * Copyright (c) 2018, EPAM SYSTEMS INC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.dlab.backendapi.service.impl;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.dao.ExploratoryLibDAO;
import com.epam.dlab.backendapi.dao.ImageExploratoryDao;
import com.epam.dlab.backendapi.resources.dto.ImageInfoRecord;
import com.epam.dlab.backendapi.util.RequestBuilder;
import com.epam.dlab.dto.UserInstanceDTO;
import com.epam.dlab.dto.exploratory.ExploratoryImageDTO;
import com.epam.dlab.dto.exploratory.ExploratoryStatusDTO;
import com.epam.dlab.dto.exploratory.ImageStatus;
import com.epam.dlab.dto.exploratory.LibStatus;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.exceptions.ResourceAlreadyExistException;
import com.epam.dlab.exceptions.ResourceNotFoundException;
import com.epam.dlab.model.ResourceType;
import com.epam.dlab.model.exploratory.Image;
import com.epam.dlab.model.library.Library;
import com.epam.dlab.rest.client.RESTService;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ImageExploratoryServiceImplTest {

	private final String USER = "test";
	private final String TOKEN = "token";
	private final String EXPLORATORY_NAME = "expName";

	private UserInfo userInfo;
	private UserInstanceDTO userInstance;
	private Image image;

	@Mock
	private ExploratoryDAO exploratoryDAO;
	@Mock
	private ImageExploratoryDao imageExploratoryDao;
	@Mock
	private ExploratoryLibDAO libDAO;
	@Mock
	private RESTService provisioningService;
	@Mock
	private RequestBuilder requestBuilder;

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
		when(exploratoryDAO.fetchRunningExploratoryFields(anyString(), anyString())).thenReturn(userInstance);
		when(imageExploratoryDao.exist(anyString(), anyString())).thenReturn(false);

		when(libDAO.getLibraries(anyString(), anyString())).thenReturn(Collections.singletonList(getLibrary()));
		doNothing().when(imageExploratoryDao).save(any(Image.class));
		when(exploratoryDAO.updateExploratoryStatus(any(ExploratoryStatusDTO.class)))
				.thenReturn(mock(UpdateResult.class));
		ExploratoryImageDTO eiDto = new ExploratoryImageDTO();
		when(requestBuilder.newExploratoryImageCreate(any(UserInfo.class), any(UserInstanceDTO.class), anyString()))
				.thenReturn(eiDto);

		String expectedUuid = "someUuid";
		when(provisioningService.post(anyString(), anyString(), any(ExploratoryImageDTO.class), any()))
				.thenReturn(expectedUuid);

		String imageName = "someImageName", imageDescription = "someDescription";
		String actualUuid = imageExploratoryService.createImage(userInfo, EXPLORATORY_NAME, imageName,
				imageDescription);
		assertNotNull(actualUuid);
		assertEquals(expectedUuid, actualUuid);

		verify(exploratoryDAO).fetchRunningExploratoryFields(USER, EXPLORATORY_NAME);
		verify(exploratoryDAO).updateExploratoryStatus(any(ExploratoryStatusDTO.class));
		verify(imageExploratoryDao).exist(USER, imageName);
		verify(imageExploratoryDao).save(any(Image.class));
		verify(libDAO).getLibraries(USER, EXPLORATORY_NAME);
		verify(requestBuilder).newExploratoryImageCreate(userInfo, userInstance, imageName);
		verify(provisioningService).post("exploratory/image", TOKEN, eiDto, String.class);
		verifyNoMoreInteractions(exploratoryDAO, imageExploratoryDao, libDAO, requestBuilder, provisioningService);
	}

	@Test
	public void createImageWhenMethodFetchRunningExploratoryFieldsThrowsException() {
		doThrow(new DlabException("Running exploratory instance for user with name not found."))
				.when(exploratoryDAO).fetchRunningExploratoryFields(anyString(), anyString());

		String imageName = "someImageName", imageDescription = "someDescription";

		try {
			imageExploratoryService.createImage(userInfo, EXPLORATORY_NAME, imageName, imageDescription);
		} catch (DlabException e) {
			assertEquals("Running exploratory instance for user with name not found.", e.getMessage());
		}
		verify(exploratoryDAO).fetchRunningExploratoryFields(USER, EXPLORATORY_NAME);
		verifyNoMoreInteractions(exploratoryDAO);
	}

	@Test
	public void createImageWhenResourceAlreadyExists() {
		when(exploratoryDAO.fetchRunningExploratoryFields(anyString(), anyString())).thenReturn(userInstance);
		when(imageExploratoryDao.exist(anyString(), anyString())).thenReturn(true);

		expectedException.expect(ResourceAlreadyExistException.class);
		expectedException.expectMessage("Image with name someImageName is already exist");

		String imageName = "someImageName", imageDescription = "someDescription";
		imageExploratoryService.createImage(userInfo, EXPLORATORY_NAME, imageName, imageDescription);
	}

	@Test
	public void createImageWhenMethodNewExploratoryImageCreateThrowsException() {
		when(exploratoryDAO.fetchRunningExploratoryFields(anyString(), anyString())).thenReturn(userInstance);
		when(imageExploratoryDao.exist(anyString(), anyString())).thenReturn(false);

		when(libDAO.getLibraries(anyString(), anyString())).thenReturn(Collections.singletonList(getLibrary()));
		doNothing().when(imageExploratoryDao).save(any(Image.class));
		when(exploratoryDAO.updateExploratoryStatus(any(ExploratoryStatusDTO.class)))
				.thenReturn(mock(UpdateResult.class));
		doThrow(new DlabException("Cannot create instance of resource class")).when(requestBuilder)
				.newExploratoryImageCreate(any(UserInfo.class), any(UserInstanceDTO.class), anyString());

		String imageName = "someImageName", imageDescription = "someDescription";
		try {
			imageExploratoryService.createImage(userInfo, EXPLORATORY_NAME, imageName, imageDescription);
		} catch (DlabException e) {
			assertEquals("Cannot create instance of resource class", e.getMessage());
		}

		verify(exploratoryDAO).fetchRunningExploratoryFields(USER, EXPLORATORY_NAME);
		verify(exploratoryDAO).updateExploratoryStatus(any(ExploratoryStatusDTO.class));
		verify(imageExploratoryDao).exist(USER, imageName);
		verify(imageExploratoryDao).save(any(Image.class));
		verify(libDAO).getLibraries(USER, EXPLORATORY_NAME);
		verify(requestBuilder).newExploratoryImageCreate(userInfo, userInstance, imageName);
		verifyNoMoreInteractions(exploratoryDAO, imageExploratoryDao, libDAO, requestBuilder);
	}

	@Test
	public void finishImageCreate() {
		when(exploratoryDAO.updateExploratoryStatus(any(ExploratoryStatusDTO.class)))
				.thenReturn(mock(UpdateResult.class));
		doNothing().when(imageExploratoryDao).updateImageFields(any(Image.class));
		doNothing().when(exploratoryDAO).updateExploratoryIp(anyString(), anyString(), anyString());

		String notebookIp = "someIp";
		imageExploratoryService.finishImageCreate(image, EXPLORATORY_NAME, notebookIp);

		verify(exploratoryDAO).updateExploratoryStatus(any(ExploratoryStatusDTO.class));
		verify(exploratoryDAO).updateExploratoryIp(USER, notebookIp, EXPLORATORY_NAME);
		verify(imageExploratoryDao).updateImageFields(image);
		verifyNoMoreInteractions(exploratoryDAO, imageExploratoryDao);
	}

	@Test
	public void finishImageCreateWhenMethodUpdateExploratoryIpThrowsException() {
		when(exploratoryDAO.updateExploratoryStatus(any(ExploratoryStatusDTO.class)))
				.thenReturn(mock(UpdateResult.class));
		doNothing().when(imageExploratoryDao).updateImageFields(any(Image.class));
		doThrow(new ResourceNotFoundException("Exploratory for user with name not found"))
				.when(exploratoryDAO).updateExploratoryIp(anyString(), anyString(), anyString());

		String notebookIp = "someIp";
		try {
			imageExploratoryService.finishImageCreate(image, EXPLORATORY_NAME, notebookIp);
		} catch (ResourceNotFoundException e) {
			assertEquals("Exploratory for user with name not found", e.getMessage());
		}

		verify(exploratoryDAO).updateExploratoryStatus(any(ExploratoryStatusDTO.class));
		verify(exploratoryDAO).updateExploratoryIp(USER, notebookIp, EXPLORATORY_NAME);
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
		verify(exploratoryDAO, never()).updateExploratoryIp(USER, null, EXPLORATORY_NAME);
		verify(imageExploratoryDao).updateImageFields(image);
		verifyNoMoreInteractions(exploratoryDAO, imageExploratoryDao);
	}

	@Test
	public void getCreatedImages() {
		ImageInfoRecord imageInfoRecord = getImageInfoRecord();
		List<ImageInfoRecord> expectedRecordList = Collections.singletonList(imageInfoRecord);
		when(imageExploratoryDao.getImages(anyString(), anyString(), anyVararg()))
				.thenReturn(expectedRecordList);

		List<ImageInfoRecord> actualRecordList = imageExploratoryService.getNotFailedImages(USER,
				"someImage");
		assertNotNull(actualRecordList);
		assertEquals(1, actualRecordList.size());
		assertEquals(expectedRecordList, actualRecordList);

		verify(imageExploratoryDao).getImages(USER, "someImage", ImageStatus.CREATED, ImageStatus.CREATING);
		verifyNoMoreInteractions(imageExploratoryDao);
	}

	@Test
	public void getImage() {
		ImageInfoRecord expectedImageInfoRecord = getImageInfoRecord();
		when(imageExploratoryDao.getImage(anyString(), anyString())).thenReturn(Optional.of(expectedImageInfoRecord));

		ImageInfoRecord actualImageInfoRecord = imageExploratoryService.getImage(USER, "someName");
		assertNotNull(actualImageInfoRecord);
		assertEquals(expectedImageInfoRecord, actualImageInfoRecord);

		verify(imageExploratoryDao).getImage(USER, "someName");
		verifyNoMoreInteractions(imageExploratoryDao);
	}

	@Test
	public void getImageWhenMethodGetImageReturnsOptionalEmpty() {
		when(imageExploratoryDao.getImage(anyString(), anyString())).thenReturn(Optional.empty());
		expectedException.expect(ResourceNotFoundException.class);
		expectedException.expectMessage(String.format("Image with name %s was not found for user %s",
				"someImageName", USER));
		imageExploratoryService.getImage(USER, "someImageName");
	}

	private ImageInfoRecord getImageInfoRecord() {
		return new ImageInfoRecord("someName", "someDescription", "someApp",
				"someFullName", ImageStatus.CREATED);
	}

	private Image fetchImage() {
		return Image.builder()
				.name("someImageName")
				.description("someDescription")
				.status(ImageStatus.CREATING)
				.user(USER)
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
		return new UserInstanceDTO().withUser(USER).withExploratoryName(EXPLORATORY_NAME)
				.withExploratoryId("explId");
	}

	private UserInfo getUserInfo() {
		return new UserInfo(USER, TOKEN);
	}
}
