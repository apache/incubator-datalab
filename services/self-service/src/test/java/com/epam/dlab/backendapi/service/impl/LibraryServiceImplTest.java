package com.epam.dlab.backendapi.service.impl;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.dao.ExploratoryLibDAO;
import com.epam.dlab.backendapi.resources.dto.LibInfoRecord;
import com.epam.dlab.backendapi.resources.dto.LibInstallFormDTO;
import com.epam.dlab.backendapi.resources.dto.LibKey;
import com.epam.dlab.backendapi.resources.dto.LibraryStatus;
import com.epam.dlab.backendapi.util.RequestBuilder;
import com.epam.dlab.dto.UserInstanceDTO;
import com.epam.dlab.dto.computational.UserComputationalResource;
import com.epam.dlab.dto.exploratory.LibInstallDTO;
import com.epam.dlab.dto.exploratory.LibStatus;
import com.epam.dlab.dto.exploratory.LibraryInstallDTO;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.exceptions.ResourceNotFoundException;
import org.bson.Document;
import org.junit.Before;
import org.junit.Test;
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
	private UserInfo userInfo;
	private UserInstanceDTO userInstance;

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
		userInfo = new UserInfo(USER, "token");
		userInstance = getUserInstanceDto();
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
	public void getLibInfoWhenListsOfExploratoryAndComputationalLibsAreNotEmpty() {
		when(libraryDAO.findAllLibraries(anyString(), anyString()))
				.thenReturn(getDocumentWithExploratoryAndComputationalLibs());

		List<LibInfoRecord> expectedList = getLibInfoRecordList();
		List<LibInfoRecord> actualList = libraryService.getLibInfo(USER, EXPLORATORY_NAME);
		assertNotNull(actualList);
		assertEquals(expectedList, actualList);

		verify(libraryDAO).findAllLibraries(USER, EXPLORATORY_NAME);
		verifyNoMoreInteractions(libraryDAO);
	}

	@Test
	public void generateLibraryInstallDTO() {
		UserComputationalResource userComputationalResource = getUserComputationalResourceWithName(COMPUTATIONAL_NAME);
		userInstance.withResources(Collections.singletonList(userComputationalResource));
		when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString(), anyString())).thenReturn(userInstance);

		LibraryInstallDTO expectedLiDTO = new LibraryInstallDTO().withExploratoryName(EXPLORATORY_NAME)
				.withComputationalName(COMPUTATIONAL_NAME).withApplicationName("");
		when(requestBuilder.newLibInstall(any(UserInfo.class), any(UserInstanceDTO.class),
				any(UserComputationalResource.class))).thenReturn(expectedLiDTO);

		LibraryInstallDTO actualLiDto = libraryService.generateLibraryInstallDTO(userInfo, libInstallFormDTO);
		assertNotNull(actualLiDto);
		assertEquals(expectedLiDTO, actualLiDto);

		verify(exploratoryDAO).fetchExploratoryFields(USER, EXPLORATORY_NAME, COMPUTATIONAL_NAME);
		verify(requestBuilder).newLibInstall(userInfo, userInstance, userComputationalResource);
		verifyNoMoreInteractions(exploratoryDAO, requestBuilder);
	}

	@Test
	public void generateLibraryInstallDTOWhenClusterAbsent() {
		userInstance.withStatus("running");
		when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString())).thenReturn(userInstance);

		LibraryInstallDTO expectedLiDTO = new LibraryInstallDTO().withExploratoryName(EXPLORATORY_NAME);
		when(requestBuilder.newLibInstall(any(UserInfo.class), any(UserInstanceDTO.class))).thenReturn(expectedLiDTO);

		LibInstallFormDTO lifDTO = new LibInstallFormDTO();
		lifDTO.setNotebookName(EXPLORATORY_NAME);
		lifDTO.setLibs(libs);
		LibraryInstallDTO actualLiDto = libraryService.generateLibraryInstallDTO(userInfo, lifDTO);
		assertNotNull(actualLiDto);
		assertEquals(expectedLiDTO, actualLiDto);

		verify(exploratoryDAO).fetchExploratoryFields(USER, EXPLORATORY_NAME);
		verify(requestBuilder).newLibInstall(userInfo, userInstance);
		verifyNoMoreInteractions(exploratoryDAO, requestBuilder);
	}

	@Test
	public void generateLibraryInstallDTOWhenClusterAbsentAndMethodFetchExploratoryFieldsThrowsException() {
		doThrow(new ResourceNotFoundException("Exploratory for user with name not found"))
				.when(exploratoryDAO).fetchExploratoryFields(anyString(), anyString());

		LibInstallFormDTO lifDTO = new LibInstallFormDTO();
		lifDTO.setNotebookName(EXPLORATORY_NAME);
		lifDTO.setLibs(libs);
		try {
			libraryService.generateLibraryInstallDTO(userInfo, lifDTO);
		} catch (ResourceNotFoundException e) {
			assertEquals("Exploratory for user with name not found", e.getMessage());
		}
		verify(exploratoryDAO).fetchExploratoryFields(USER, EXPLORATORY_NAME);
		verifyNoMoreInteractions(exploratoryDAO);
	}

	@Test
	public void generateLibraryInstallDTOWhenClusterAbsentAndNotebookStatusIsNotRunning() {
		when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString())).thenReturn(userInstance);

		LibInstallFormDTO lifDTO = new LibInstallFormDTO();
		lifDTO.setNotebookName(EXPLORATORY_NAME);
		lifDTO.setLibs(libs);
		try {
			libraryService.generateLibraryInstallDTO(userInfo, lifDTO);
		} catch (DlabException e) {
			assertEquals("Exploratory explName is not running", e.getMessage());
		}
		verify(exploratoryDAO).fetchExploratoryFields(USER, EXPLORATORY_NAME);
		verifyNoMoreInteractions(exploratoryDAO);
	}

	@Test
	public void generateLibraryInstallDTOWhenClusterAbsentAndMethodNewLibInstallThrowsException() {
		userInstance.withStatus("running");
		when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString())).thenReturn(userInstance);

		doThrow(new DlabException("Cannot create instance of resource class"))
				.when(requestBuilder).newLibInstall(any(UserInfo.class), any(UserInstanceDTO.class));

		LibInstallFormDTO lifDTO = new LibInstallFormDTO();
		lifDTO.setNotebookName(EXPLORATORY_NAME);
		lifDTO.setLibs(libs);
		try {
			libraryService.generateLibraryInstallDTO(userInfo, lifDTO);
		} catch (DlabException e) {
			assertEquals("Cannot create instance of resource class", e.getMessage());
		}
		verify(exploratoryDAO).fetchExploratoryFields(USER, EXPLORATORY_NAME);
		verify(requestBuilder).newLibInstall(userInfo, userInstance);
		verifyNoMoreInteractions(exploratoryDAO, requestBuilder);
	}

	@Test
	public void generateLibraryInstallDTOWhenClusterPresentButRunningNotebookNotFound() {
		doThrow(new DlabException("Running notebook with running cluster not found for user"))
				.when(exploratoryDAO).fetchExploratoryFields(anyString(), anyString(), anyString());

		try {
			libraryService.generateLibraryInstallDTO(userInfo, libInstallFormDTO);
		} catch (DlabException e) {
			assertEquals("Running notebook with running cluster not found for user", e.getMessage());
		}
		verify(exploratoryDAO).fetchExploratoryFields(USER, EXPLORATORY_NAME, COMPUTATIONAL_NAME);
		verifyNoMoreInteractions(exploratoryDAO);
	}

	@Test
	public void generateLibraryInstallDTOWhenClusterPresentButHasInaproprietaryName() {
		userInstance.withResources(Collections.singletonList(getUserComputationalResourceWithName
				("inaproprietaryName")));
		when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString(), anyString())).thenReturn(userInstance);
		try {
			libraryService.generateLibraryInstallDTO(userInfo, libInstallFormDTO);
		} catch (DlabException e) {
			assertEquals("Computational with name compName is not unique or absent", e.getMessage());
		}
		verify(exploratoryDAO).fetchExploratoryFields(USER, EXPLORATORY_NAME, COMPUTATIONAL_NAME);
		verifyNoMoreInteractions(exploratoryDAO);
	}

	@Test
	public void generateLibraryInstallDTOWhenTwoClustersHaveEqualNames() {
		List<UserComputationalResource> listOfCompResources = Arrays.asList(
				getUserComputationalResourceWithName("compName"),
				getUserComputationalResourceWithName("compName")
		);
		userInstance.withResources(listOfCompResources);
		when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString(), anyString())).thenReturn(userInstance);
		try {
			libraryService.generateLibraryInstallDTO(userInfo, libInstallFormDTO);
		} catch (DlabException e) {
			assertEquals("Computational with name compName is not unique or absent", e.getMessage());
		}
		verify(exploratoryDAO).fetchExploratoryFields(USER, EXPLORATORY_NAME, COMPUTATIONAL_NAME);
		verifyNoMoreInteractions(exploratoryDAO);
	}

	@Test
	public void generateLibraryInstallDTOWhenMethodNewLibInstallThrowsException() {
		UserComputationalResource userComputationalResource = getUserComputationalResourceWithName(COMPUTATIONAL_NAME);
		userInstance.withResources(Collections.singletonList(userComputationalResource));
		when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString(), anyString())).thenReturn(userInstance);

		doThrow(new DlabException("Cannot create instance of resource class"))
				.when(requestBuilder).newLibInstall(any(UserInfo.class), any(UserInstanceDTO.class),
				any(UserComputationalResource.class));
		try {
			libraryService.generateLibraryInstallDTO(userInfo, libInstallFormDTO);
		} catch (DlabException e) {
			assertEquals("Cannot create instance of resource class", e.getMessage());
		}
		verify(exploratoryDAO).fetchExploratoryFields(USER, EXPLORATORY_NAME, COMPUTATIONAL_NAME);
		verify(requestBuilder).newLibInstall(userInfo, userInstance, userComputationalResource);
		verifyNoMoreInteractions(exploratoryDAO, requestBuilder);
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
		verifyNoMoreInteractions(libraryDAO);
	}

	@Test
	public void prepareExploratoryLibInstallationWhenLibraryStatusIsInstalling() {
		when(libraryDAO.fetchLibraryStatus(anyString(), anyString(), anyString(), anyString(), anyString()))
				.thenReturn(LibStatus.INSTALLING);
		try {
			libraryService.prepareExploratoryLibInstallation(USER, libInstallFormDTO, libraryInstallDto);
		} catch (DlabException e) {
			assertEquals("Library someName is already installing", e.getMessage());
		}
		verify(libraryDAO)
				.fetchLibraryStatus(USER, EXPLORATORY_NAME, "someGroup", "someName", "someVersion");
		verifyNoMoreInteractions(libraryDAO);
	}

	@Test
	public void prepareComputationalLibInstallation() {
		when(libraryDAO.fetchLibraryStatus(
				anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
				.thenReturn(LibStatus.INSTALLED);
		when(libraryDAO.addLibrary(anyString(), anyString(), any(LibInstallDTO.class), anyBoolean())).thenReturn(true);

		LibraryInstallDTO actualLiDto =
				libraryService.prepareComputationalLibInstallation(USER, libInstallFormDTO, libraryInstallDto);
		libraryInstallDto.setLibs(libs);
		assertNotNull(actualLiDto);
		assertEquals(libraryInstallDto, actualLiDto);

		verify(libraryDAO)
				.fetchLibraryStatus(USER, EXPLORATORY_NAME, COMPUTATIONAL_NAME, "someGroup", "someName",
						"someVersion");
		verify(libraryDAO).addLibrary(USER, EXPLORATORY_NAME, COMPUTATIONAL_NAME, liDto, false);
		verifyNoMoreInteractions(libraryDAO);
	}

	@Test
	public void prepareComputationalLibInstallationWhenLibraryStatusIsInstalling() {
		when(libraryDAO.fetchLibraryStatus(
				anyString(), anyString(), anyString(), anyString(), anyString(), anyString()))
				.thenReturn(LibStatus.INSTALLING);
		try {
			libraryService.prepareComputationalLibInstallation(USER, libInstallFormDTO, libraryInstallDto);
		} catch (DlabException e) {
			assertEquals("Library someName is already installing", e.getMessage());
		}
		verify(libraryDAO)
				.fetchLibraryStatus(USER, EXPLORATORY_NAME, COMPUTATIONAL_NAME, "someGroup", "someName",
						"someVersion");
		verifyNoMoreInteractions(libraryDAO);
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
		return new UserInstanceDTO().withUser(USER).withExploratoryName(EXPLORATORY_NAME);
	}

	private List<Document> getExplLibsList() {
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
		return new Document().append(ExploratoryLibDAO.EXPLORATORY_LIBS, getExplLibsList())
				.append(ExploratoryLibDAO.COMPUTATIONAL_LIBS, getCompLibs());
	}

	private List<LibInfoRecord> getLibInfoRecordList() {
		LibKey explLibKey = new LibKey("expLibName", "expLibVersion", "expLibGroup");
		List<LibraryStatus> explLibStatuses = Collections.singletonList(
				new LibraryStatus(EXPLORATORY_NAME, "notebook", "expLibStatus", "expLibErrorMessage"));

		LibKey compLibKey = new LibKey("compLibName", "compLibVersion", "compLibGroup");
		List<LibraryStatus> compLibStatuses = Collections.singletonList(
				new LibraryStatus("compName", "cluster", "compLibStatus", "compLibErrorMessage"));

		return Arrays.asList(
				new LibInfoRecord(compLibKey, compLibStatuses),
				new LibInfoRecord(explLibKey, explLibStatuses)
		);
	}

}
