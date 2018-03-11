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
import com.epam.dlab.exceptions.ResourceNotFoundException;
import com.epam.dlab.model.ResourceType;
import com.epam.dlab.model.exloratory.Image;
import com.epam.dlab.model.library.Library;
import com.epam.dlab.rest.client.RESTService;
import com.mongodb.client.result.UpdateResult;
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

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ImageExploratoryServiceImplTest {

	private final String USER = "test";
	private final String EXPLORATORY_NAME = "expName";

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

	@Test
	public void createImage() {
		String explId = "explId";
		UserInstanceDTO uiDto = new UserInstanceDTO().withUser(USER).withExploratoryName(EXPLORATORY_NAME)
				.withExploratoryId(explId);
		when(exploratoryDAO.fetchRunningExploratoryFields(anyString(), anyString())).thenReturn(uiDto);
		when(imageExploratoryDao.exist(anyString(), anyString())).thenReturn(false);

		Library library = new Library("someGroup", "someName", "someVersion", LibStatus.INSTALLED,
				"someErrorMessage").withType(ResourceType.EXPLORATORY);
		when(libDAO.getLibraries(anyString(), anyString())).thenReturn(Collections.singletonList(library));
		doNothing().when(imageExploratoryDao).save(any(Image.class));
		when(exploratoryDAO.updateExploratoryStatus(any(ExploratoryStatusDTO.class)))
				.thenReturn(mock(UpdateResult.class));
		ExploratoryImageDTO eiDto = new ExploratoryImageDTO();
		when(requestBuilder.newExploratoryImageCreate(any(UserInfo.class), any(UserInstanceDTO.class), anyString()))
				.thenReturn(eiDto);

		String expectedUuid = "someUuid";
		when(provisioningService.post(anyString(), anyString(), any(ExploratoryImageDTO.class), any()))
				.thenReturn(expectedUuid);

		String token = "token";
		UserInfo userInfo = new UserInfo(USER, token);
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
		verifyNoMoreInteractions(libDAO);

		verify(requestBuilder).newExploratoryImageCreate(userInfo, uiDto, imageName);
		verifyNoMoreInteractions(requestBuilder);

		verify(provisioningService).post("exploratory/image", token, eiDto, String.class);
		verifyNoMoreInteractions(provisioningService);
	}

	@Test
	public void finishImageCreate() {
		when(exploratoryDAO.updateExploratoryStatus(any(ExploratoryStatusDTO.class)))
				.thenReturn(mock(UpdateResult.class));
		doNothing().when(imageExploratoryDao).updateImageFields(any(Image.class));
		doNothing().when(exploratoryDAO).updateExploratoryIp(anyString(), anyString(), anyString());

		String imageName = "someImageName", imageDescription = "someDescription", explId = "explId";
		Library library = new Library("someGroup", "someName", "someVersion", LibStatus.INSTALLED,
				"someErrorMessage").withType(ResourceType.EXPLORATORY);
		Image image = Image.builder()
				.name(imageName)
				.description(imageDescription)
				.status(ImageStatus.CREATING)
				.user(USER)
				.libraries(Collections.singletonList(library))
				.computationalLibraries(Collections.emptyMap())
				.dockerImage(imageName)
				.exploratoryId(explId).build();
		String notebookIp = "someIp";
		imageExploratoryService.finishImageCreate(image, EXPLORATORY_NAME, notebookIp);

		verify(exploratoryDAO).updateExploratoryStatus(any(ExploratoryStatusDTO.class));
		verify(exploratoryDAO).updateExploratoryIp(USER, notebookIp, EXPLORATORY_NAME);

		verify(imageExploratoryDao).updateImageFields(image);
		verifyNoMoreInteractions(imageExploratoryDao);
	}

	@Test
	public void getCreatedImages() {
		String name = "someName", description = "someDescription", application = "someApp", fullName = "someFullName";
		ImageInfoRecord imageInfoRecord =
				new ImageInfoRecord(name, description, application, fullName, ImageStatus.CREATED);
		List<ImageInfoRecord> expectedRecordList = Collections.singletonList(imageInfoRecord);
		when(imageExploratoryDao.getImages(anyString(), any(ImageStatus.class), anyString()))
				.thenReturn(expectedRecordList);

		List<ImageInfoRecord> actualRecordList = imageExploratoryService.getCreatedImages(USER, "someImage");
		assertNotNull(actualRecordList);
		assertTrue(actualRecordList.size() == 1);
		assertEquals(expectedRecordList, actualRecordList);

		verify(imageExploratoryDao).getImages(USER, ImageStatus.CREATED, "someImage");
		verifyNoMoreInteractions(imageExploratoryDao);
	}

	@Test
	public void getImage() {
		String name = "someName", description = "someDescription", application = "someApp", fullName = "someFullName";
		ImageInfoRecord expectedImageInfoRecord =
				new ImageInfoRecord(name, description, application, fullName, ImageStatus.CREATED);
		when(imageExploratoryDao.getImage(anyString(), anyString())).thenReturn(Optional.of(expectedImageInfoRecord));

		ImageInfoRecord actualImageInfoRecord = imageExploratoryService.getImage(USER, name);
		assertNotNull(actualImageInfoRecord);
		assertEquals(expectedImageInfoRecord, actualImageInfoRecord);

		verify(imageExploratoryDao).getImage(USER, name);
		verifyNoMoreInteractions(imageExploratoryDao);
	}

	@Test
	public void getImageWithException() {
		doThrow(new ResourceNotFoundException(String.format("Image with name %s was not found for user %s",
				"someImageName", USER))).when(imageExploratoryDao).getImage(USER, "someImageName");
		expectedException.expect(ResourceNotFoundException.class);
		expectedException.expectMessage(String.format("Image with name %s was not found for user %s",
				"someImageName", USER));
		imageExploratoryService.getImage(USER, "someImageName");
	}
}
