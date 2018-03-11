package com.epam.dlab.backendapi.service.impl;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.dao.ExploratoryLibDAO;
import com.epam.dlab.backendapi.resources.dto.LibInfoRecord;
import com.epam.dlab.backendapi.resources.dto.LibInstallFormDTO;
import com.epam.dlab.backendapi.util.RequestBuilder;
import com.epam.dlab.dto.UserInstanceDTO;
import com.epam.dlab.dto.computational.UserComputationalResource;
import com.epam.dlab.dto.exploratory.LibInstallDTO;
import com.epam.dlab.dto.exploratory.LibStatus;
import com.epam.dlab.dto.exploratory.LibraryInstallDTO;
import org.bson.Document;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class LibraryServiceImplTest {

	private final String USER = "test";
	private final String EXPLORATORY_NAME = "explName";
	private final String COMPUTATIONAL_NAME = "compName";

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

	@InjectMocks
	private LibraryServiceImpl libraryService;

	@Before
	public void setUp() {
		prepareForTesting();
	}

	@Test
	public void getLibs() {
		Document document = new Document();
		when(libraryDAO.findExploratoryLibraries(anyString(), anyString())).thenReturn(document);

		List<Document> expectedList = new ArrayList<>();
		List<Document> actualList = libraryService.getLibs(USER, EXPLORATORY_NAME, "");
		assertNotNull(actualList);
		assertEquals(expectedList, actualList);

		verify(libraryDAO).findExploratoryLibraries(USER, EXPLORATORY_NAME);
		verifyNoMoreInteractions(libraryDAO);
	}

	@Test
	public void getLibInfo() {
		Document document = new Document();
		when(libraryDAO.findAllLibraries(anyString(), anyString())).thenReturn(document);

		List<LibInfoRecord> expectedList = new ArrayList<>();
		List<LibInfoRecord> actualList = libraryService.getLibInfo(USER, EXPLORATORY_NAME);
		assertNotNull(actualList);
		assertEquals(expectedList, actualList);

		verify(libraryDAO).findAllLibraries(USER, EXPLORATORY_NAME);
		verifyNoMoreInteractions(libraryDAO);
	}

	@Test
	public void generateLibraryInstallDTO() {
		UserComputationalResource userComputationalResource = new UserComputationalResource();
		userComputationalResource.setComputationalName(COMPUTATIONAL_NAME);
		userComputationalResource.setComputationalId("someId");
		userComputationalResource.setImageName("someImageName");
		UserInstanceDTO userInstanceDTO = new UserInstanceDTO().withUser(USER).withExploratoryName(EXPLORATORY_NAME)
				.withResources(Collections.singletonList(userComputationalResource));
		when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString(), anyString())).thenReturn(userInstanceDTO);

		UserInfo userInfo = new UserInfo(USER, "token");
		LibraryInstallDTO expectedLiDTO = new LibraryInstallDTO().withExploratoryName(EXPLORATORY_NAME)
				.withComputationalName(COMPUTATIONAL_NAME).withApplicationName("");
		when(requestBuilder.newLibInstall(any(UserInfo.class), any(UserInstanceDTO.class),
				any(UserComputationalResource.class))).thenReturn(expectedLiDTO);

		LibraryInstallDTO actualLiDto = libraryService.generateLibraryInstallDTO(userInfo, libInstallFormDTO);
		assertNotNull(actualLiDto);
		assertEquals(expectedLiDTO, actualLiDto);

		verify(exploratoryDAO).fetchExploratoryFields(USER, EXPLORATORY_NAME, COMPUTATIONAL_NAME);
		verifyNoMoreInteractions(exploratoryDAO);

		verify(requestBuilder).newLibInstall(userInfo, userInstanceDTO, userComputationalResource);
		verifyNoMoreInteractions(requestBuilder);
	}

	@Test
	public void prepareExploratoryLibInstallation() {
		when(libraryDAO.fetchLibraryStatus(anyString(), anyString(), anyString(), anyString(), anyString()))
				.thenReturn(LibStatus.INSTALLED);

		when(libraryDAO.addLibrary(anyString(), anyString(), any(LibInstallDTO.class), anyBoolean()))
				.thenReturn(true);

		LibraryInstallDTO actualLiDto =
				libraryService.prepareExploratoryLibInstallation(USER, libInstallFormDTO, libraryInstallDto);
		libraryInstallDto.setLibs(libs);
		assertNotNull(actualLiDto);
		assertEquals(libraryInstallDto, actualLiDto);

		verify(libraryDAO).fetchLibraryStatus(USER, EXPLORATORY_NAME, "someGroup", "someName", "someVersion");
		verify(libraryDAO).addLibrary(USER, EXPLORATORY_NAME, liDto, false);
	}

	@Test
	public void prepareComputationalLibInstallation() {
		when(libraryDAO.fetchLibraryStatus(anyString(), anyString(), anyString(), anyString(), anyString(), anyString
				()))
				.thenReturn(LibStatus.INSTALLED);

		when(libraryDAO.addLibrary(anyString(), anyString(), any(LibInstallDTO.class), anyBoolean()))
				.thenReturn(true);

		LibraryInstallDTO actualLiDto =
				libraryService.prepareComputationalLibInstallation(USER, libInstallFormDTO, libraryInstallDto);
		libraryInstallDto.setLibs(libs);
		assertNotNull(actualLiDto);
		assertEquals(libraryInstallDto, actualLiDto);

		verify(libraryDAO).fetchLibraryStatus(USER, EXPLORATORY_NAME, COMPUTATIONAL_NAME, "someGroup", "someName",
				"someVersion");
		verify(libraryDAO).addLibrary(USER, EXPLORATORY_NAME, COMPUTATIONAL_NAME, liDto, false);
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

}
