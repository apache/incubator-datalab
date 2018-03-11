package com.epam.dlab.backendapi.service.impl;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.ComputationalDAO;
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.dao.GitCredsDAO;
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.util.RequestBuilder;
import com.epam.dlab.dto.StatusEnvBaseDTO;
import com.epam.dlab.dto.UserInstanceDTO;
import com.epam.dlab.dto.exploratory.ExploratoryActionDTO;
import com.epam.dlab.dto.exploratory.ExploratoryCreateDTO;
import com.epam.dlab.dto.exploratory.ExploratoryGitCredsDTO;
import com.epam.dlab.dto.exploratory.ExploratoryGitCredsUpdateDTO;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.model.exloratory.Exploratory;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ExploratoryServiceImplTest {

	private final String USER = "test";
	private final String TOKEN = "token";
	private final String EXPLORATORY_NAME = "expName";
	private final String UUID = "1234-56789765-4321";

	private UserInfo userInfo;
	private UserInstanceDTO userInstance;

	@Mock
	private ExploratoryDAO exploratoryDAO;
	@Mock
	private ComputationalDAO computationalDAO;
	@Mock
	private GitCredsDAO gitCredsDAO;
	@Mock
	private RESTService provisioningService;
	@Mock
	private RequestBuilder requestBuilder;
	@Mock
	private RequestId requestId;

	@InjectMocks
	private ExploratoryServiceImpl exploratoryService;

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Before
	public void setUp() {
		userInfo = getUserInfo();
		userInstance = getUserInstanceDto();
	}

	@Test
	public void start() {
		when(exploratoryDAO.updateExploratoryStatus(any(StatusEnvBaseDTO.class))).thenReturn(mock(UpdateResult.class));
		when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString())).thenReturn(userInstance);

		ExploratoryGitCredsDTO egcDtoMock = mock(ExploratoryGitCredsDTO.class);
		when(gitCredsDAO.findGitCreds(anyString())).thenReturn(egcDtoMock);

		ExploratoryActionDTO egcuDto = new ExploratoryGitCredsUpdateDTO();
		egcuDto.withExploratoryName(EXPLORATORY_NAME);
		when(requestBuilder.newExploratoryStart(any(UserInfo.class), any(UserInstanceDTO.class),
				any(ExploratoryGitCredsDTO.class))).thenReturn(egcuDto);

		String exploratoryStart = "exploratory/start";
		when(provisioningService.post(anyString(), anyString(), any(ExploratoryActionDTO.class), any())).thenReturn
				(UUID);
		when(requestId.put(anyString(), anyString())).thenReturn(UUID);

		String uuid = exploratoryService.start(userInfo, EXPLORATORY_NAME);
		assertNotNull(uuid);
		assertEquals(UUID, uuid);

		verify(exploratoryDAO).updateExploratoryStatus(any(StatusEnvBaseDTO.class));
		verify(exploratoryDAO).fetchExploratoryFields(USER, EXPLORATORY_NAME);

		verify(provisioningService).post(exploratoryStart, TOKEN, egcuDto, String.class);
		verifyNoMoreInteractions(provisioningService);

		verify(requestId).put(USER, UUID);
		verifyNoMoreInteractions(requestId);
	}

	@Test
	public void stop() {
		when(exploratoryDAO.updateExploratoryStatus(any(StatusEnvBaseDTO.class))).thenReturn(mock(UpdateResult.class));
		when(computationalDAO.updateComputationalStatusesForExploratory(any(StatusEnvBaseDTO.class))).thenReturn(1);
		when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString())).thenReturn(userInstance);

		ExploratoryActionDTO eaDto = new ExploratoryActionDTO();
		eaDto.withExploratoryName(EXPLORATORY_NAME);
		when(requestBuilder.newExploratoryStop(any(UserInfo.class), any(UserInstanceDTO.class))).thenReturn(eaDto);

		String exploratoryStop = "exploratory/stop";
		when(provisioningService.post(anyString(), anyString(), any(ExploratoryActionDTO.class), any())).thenReturn
				(UUID);
		when(requestId.put(anyString(), anyString())).thenReturn(UUID);

		String uuid = exploratoryService.stop(userInfo, EXPLORATORY_NAME);
		assertNotNull(uuid);
		assertEquals(UUID, uuid);

		verify(exploratoryDAO).updateExploratoryStatus(any(StatusEnvBaseDTO.class));
		verify(exploratoryDAO).fetchExploratoryFields(USER, EXPLORATORY_NAME);

		verify(provisioningService).post(exploratoryStop, TOKEN, eaDto, String.class);
		verifyNoMoreInteractions(provisioningService);

		verify(requestId).put(USER, UUID);
		verifyNoMoreInteractions(requestId);
	}

	@Test
	public void terminate() {
		when(exploratoryDAO.updateExploratoryStatus(any(StatusEnvBaseDTO.class))).thenReturn(mock(UpdateResult.class));
		when(computationalDAO.updateComputationalStatusesForExploratory(any(StatusEnvBaseDTO.class))).thenReturn(1);
		when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString())).thenReturn(userInstance);

		ExploratoryActionDTO eaDto = new ExploratoryActionDTO();
		eaDto.withExploratoryName(EXPLORATORY_NAME);
		when(requestBuilder.newExploratoryStop(any(UserInfo.class), any(UserInstanceDTO.class))).thenReturn(eaDto);

		String exploratoryTerminate = "exploratory/terminate";
		when(provisioningService.post(anyString(), anyString(), any(ExploratoryActionDTO.class), any())).thenReturn
				(UUID);
		when(requestId.put(anyString(), anyString())).thenReturn(UUID);

		String uuid = exploratoryService.terminate(userInfo, EXPLORATORY_NAME);
		assertNotNull(uuid);
		assertEquals(UUID, uuid);

		verify(exploratoryDAO).updateExploratoryStatus(any(StatusEnvBaseDTO.class));
		verify(exploratoryDAO).fetchExploratoryFields(USER, EXPLORATORY_NAME);

		verify(computationalDAO).updateComputationalStatusesForExploratory(any(StatusEnvBaseDTO.class));
		verifyNoMoreInteractions(computationalDAO);

		verify(requestBuilder).newExploratoryStop(userInfo, userInstance);
		verifyNoMoreInteractions(requestBuilder);

		verify(provisioningService).post(exploratoryTerminate, TOKEN, eaDto, String.class);
		verifyNoMoreInteractions(provisioningService);

		verify(requestId).put(USER, UUID);
		verifyNoMoreInteractions(requestId);
	}

	@Test
	public void create() {
		ExploratoryGitCredsDTO egcDto = new ExploratoryGitCredsDTO();
		when(gitCredsDAO.findGitCreds(anyString())).thenReturn(egcDto);

		ExploratoryCreateDTO ecDto = new ExploratoryCreateDTO();
		Exploratory exploratory = Exploratory.builder().name(EXPLORATORY_NAME).build();
		when(requestBuilder.newExploratoryCreate(any(Exploratory.class), any(UserInfo.class), any
				(ExploratoryGitCredsDTO.class))).thenReturn(ecDto);
		String exploratoryCreate = "exploratory/create";
		when(provisioningService.post(anyString(), anyString(), any(ExploratoryCreateDTO.class), any())).thenReturn
				(UUID);
		when(requestId.put(anyString(), anyString())).thenReturn(UUID);

		String uuid = exploratoryService.create(userInfo, exploratory);
		assertNotNull(uuid);
		assertEquals(UUID, uuid);

		verify(gitCredsDAO).findGitCreds(USER);
		verifyNoMoreInteractions(gitCredsDAO);

		verify(requestBuilder).newExploratoryCreate(exploratory, userInfo, egcDto);
		verifyNoMoreInteractions(requestBuilder);

		verify(provisioningService).post(exploratoryCreate, TOKEN, ecDto, String.class);
		verifyNoMoreInteractions(provisioningService);

		verify(requestId).put(USER, UUID);
		verifyNoMoreInteractions(requestId);
	}

	@Test
	public void createWithException() {
		doThrow(new RuntimeException()).when(exploratoryDAO).insertExploratory(any(UserInstanceDTO.class));
		expectedException.expect(DlabException.class);

		Exploratory exploratory = Exploratory.builder().name(EXPLORATORY_NAME).build();
		exploratoryService.create(userInfo, exploratory);
	}

	private UserInfo getUserInfo() {
		return new UserInfo(USER, TOKEN);
	}

	private UserInstanceDTO getUserInstanceDto() {
		return new UserInstanceDTO().withUser(USER).withExploratoryName(EXPLORATORY_NAME);
	}

}
