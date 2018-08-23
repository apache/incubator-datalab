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
import com.epam.dlab.backendapi.dao.ComputationalDAO;
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.dao.GitCredsDAO;
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.util.RequestBuilder;
import com.epam.dlab.dto.StatusEnvBaseDTO;
import com.epam.dlab.dto.UserInstanceDTO;
import com.epam.dlab.dto.UserInstanceStatus;
import com.epam.dlab.dto.computational.UserComputationalResource;
import com.epam.dlab.dto.exploratory.*;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.exceptions.ResourceNotFoundException;
import com.epam.dlab.model.exploratory.Exploratory;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

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
	private StatusEnvBaseDTO statusEnvBaseDTO;

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
		when(provisioningService.post(anyString(), anyString(), any(ExploratoryActionDTO.class), any()))
				.thenReturn(UUID);
		when(requestId.put(anyString(), anyString())).thenReturn(UUID);

		String uuid = exploratoryService.start(userInfo, EXPLORATORY_NAME);
		assertNotNull(uuid);
		assertEquals(UUID, uuid);

		statusEnvBaseDTO = getStatusEnvBaseDTOWithStatus("starting");

		verify(exploratoryDAO).updateExploratoryStatus(refEq(statusEnvBaseDTO, "self"));
		verify(exploratoryDAO).fetchExploratoryFields(USER, EXPLORATORY_NAME);
		verify(provisioningService).post(exploratoryStart, TOKEN, egcuDto, String.class);
		verify(requestId).put(USER, UUID);
		verifyNoMoreInteractions(exploratoryDAO, provisioningService, requestId);
	}

	@Test
	public void startWhenMethodFetchExploratoryFieldsThrowsException() {
		when(exploratoryDAO.updateExploratoryStatus(any(StatusEnvBaseDTO.class))).thenReturn(mock(UpdateResult.class));
		doThrow(new ResourceNotFoundException("Exploratory for user with name not found"))
				.when(exploratoryDAO).fetchExploratoryFields(anyString(), anyString());
		try {
			exploratoryService.start(userInfo, EXPLORATORY_NAME);
		} catch (DlabException e) {
			assertEquals("Could not exploratory/start exploratory environment expName: Exploratory for user with " +
					"name not found", e.getMessage());
		}
		statusEnvBaseDTO = getStatusEnvBaseDTOWithStatus("starting");
		verify(exploratoryDAO).updateExploratoryStatus(refEq(statusEnvBaseDTO, "self"));
		verify(exploratoryDAO).fetchExploratoryFields(USER, EXPLORATORY_NAME);

		statusEnvBaseDTO = getStatusEnvBaseDTOWithStatus("failed");
		verify(exploratoryDAO).updateExploratoryStatus(refEq(statusEnvBaseDTO, "self"));
		verifyNoMoreInteractions(exploratoryDAO);
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

		statusEnvBaseDTO = getStatusEnvBaseDTOWithStatus("stopping");

		verify(exploratoryDAO).updateExploratoryStatus(refEq(statusEnvBaseDTO, "self"));
		verify(exploratoryDAO).fetchExploratoryFields(USER, EXPLORATORY_NAME);
		verify(provisioningService).post(exploratoryStop, TOKEN, eaDto, String.class);
		verify(computationalDAO).updateComputationalStatusesForExploratory(userInfo.getName(), EXPLORATORY_NAME,
				UserInstanceStatus.STOPPING, UserInstanceStatus.TERMINATING, UserInstanceStatus.FAILED,
				UserInstanceStatus.TERMINATED, UserInstanceStatus.STOPPED);
		verify(requestId).put(USER, UUID);
		verifyNoMoreInteractions(exploratoryDAO, provisioningService, requestId);
	}

	@Test
	public void stopWhenMethodFetchExploratoryFieldsThrowsException() {
		when(exploratoryDAO.updateExploratoryStatus(any(StatusEnvBaseDTO.class))).thenReturn(mock(UpdateResult.class));
		doThrow(new ResourceNotFoundException("Exploratory for user with name not found"))
				.when(exploratoryDAO).fetchExploratoryFields(anyString(), anyString());
		try {
			exploratoryService.stop(userInfo, EXPLORATORY_NAME);
		} catch (DlabException e) {
			assertEquals("Could not exploratory/stop exploratory environment expName: Exploratory for user with " +
					"name not found", e.getMessage());
		}
		statusEnvBaseDTO = getStatusEnvBaseDTOWithStatus("stopping");
		verify(exploratoryDAO).updateExploratoryStatus(refEq(statusEnvBaseDTO, "self"));
		verify(exploratoryDAO).fetchExploratoryFields(USER, EXPLORATORY_NAME);

		statusEnvBaseDTO = getStatusEnvBaseDTOWithStatus("failed");
		verify(exploratoryDAO).updateExploratoryStatus(refEq(statusEnvBaseDTO, "self"));
		verifyNoMoreInteractions(exploratoryDAO);
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

		statusEnvBaseDTO = getStatusEnvBaseDTOWithStatus("terminating");

		verify(exploratoryDAO).updateExploratoryStatus(refEq(statusEnvBaseDTO, "self"));
		verify(exploratoryDAO).fetchExploratoryFields(USER, EXPLORATORY_NAME);
		verify(computationalDAO).updateComputationalStatusesForExploratory(USER, EXPLORATORY_NAME, UserInstanceStatus
				.TERMINATING, UserInstanceStatus.TERMINATING, UserInstanceStatus.TERMINATED, UserInstanceStatus.FAILED);
		verify(requestBuilder).newExploratoryStop(userInfo, userInstance);
		verify(provisioningService).post(exploratoryTerminate, TOKEN, eaDto, String.class);
		verify(requestId).put(USER, UUID);
		verifyNoMoreInteractions(exploratoryDAO, computationalDAO, requestBuilder, provisioningService, requestId);
	}

	@Test
	public void terminateWhenMethodFetchExploratoryFieldsThrowsException() {
		when(exploratoryDAO.updateExploratoryStatus(any(StatusEnvBaseDTO.class))).thenReturn(mock(UpdateResult.class));
		doThrow(new ResourceNotFoundException("Exploratory for user with name not found"))
				.when(exploratoryDAO).fetchExploratoryFields(anyString(), anyString());
		try {
			exploratoryService.terminate(userInfo, EXPLORATORY_NAME);
		} catch (DlabException e) {
			assertEquals("Could not exploratory/terminate exploratory environment expName: Exploratory for user " +
					"with name not found", e.getMessage());
		}
		statusEnvBaseDTO = getStatusEnvBaseDTOWithStatus("terminating");
		verify(exploratoryDAO).updateExploratoryStatus(refEq(statusEnvBaseDTO, "self"));
		verify(exploratoryDAO).fetchExploratoryFields(USER, EXPLORATORY_NAME);

		statusEnvBaseDTO = getStatusEnvBaseDTOWithStatus("failed");
		verify(exploratoryDAO).updateExploratoryStatus(refEq(statusEnvBaseDTO, "self"));
		verifyNoMoreInteractions(exploratoryDAO);
	}

	@Test
	public void create() {
		doNothing().when(exploratoryDAO).insertExploratory(any(UserInstanceDTO.class));
		ExploratoryGitCredsDTO egcDto = new ExploratoryGitCredsDTO();
		when(gitCredsDAO.findGitCreds(anyString())).thenReturn(egcDto);

		ExploratoryCreateDTO ecDto = new ExploratoryCreateDTO();
		Exploratory exploratory = Exploratory.builder().name(EXPLORATORY_NAME).build();
		when(requestBuilder.newExploratoryCreate(any(Exploratory.class), any(UserInfo.class),
				any(ExploratoryGitCredsDTO.class))).thenReturn(ecDto);
		String exploratoryCreate = "exploratory/create";
		when(provisioningService.post(anyString(), anyString(), any(ExploratoryCreateDTO.class), any()))
				.thenReturn(UUID);
		when(requestId.put(anyString(), anyString())).thenReturn(UUID);

		String uuid = exploratoryService.create(userInfo, exploratory);
		assertNotNull(uuid);
		assertEquals(UUID, uuid);

		userInstance.withStatus("creating");
		userInstance.withResources(Collections.emptyList());
		verify(exploratoryDAO).insertExploratory(userInstance);
		verify(gitCredsDAO).findGitCreds(USER);
		verify(requestBuilder).newExploratoryCreate(exploratory, userInfo, egcDto);
		verify(provisioningService).post(exploratoryCreate, TOKEN, ecDto, String.class);
		verify(requestId).put(USER, UUID);
		verifyNoMoreInteractions(exploratoryDAO, gitCredsDAO, requestBuilder, provisioningService, requestId);
	}

	@Test
	public void createWhenMethodInsertExploratoryThrowsException() {
		doThrow(new RuntimeException("Exploratory for user with name not found"))
				.when(exploratoryDAO).insertExploratory(any(UserInstanceDTO.class));
		expectedException.expect(DlabException.class);
		expectedException.expectMessage("Could not create exploratory environment expName for user test: " +
				"Exploratory for user with name not found");

		Exploratory exploratory = Exploratory.builder().name(EXPLORATORY_NAME).build();
		exploratoryService.create(userInfo, exploratory);
	}

	@Test
	public void createWhenMethodInsertExploratoryThrowsExceptionWithItsCatching() {
		doThrow(new RuntimeException()).when(exploratoryDAO).insertExploratory(any(UserInstanceDTO.class));
		Exploratory exploratory = Exploratory.builder().name(EXPLORATORY_NAME).build();
		try {
			exploratoryService.create(userInfo, exploratory);
		} catch (DlabException e) {
			assertEquals("Could not create exploratory environment expName for user test: null",
					e.getMessage());
		}
		userInstance.withStatus("creating");
		userInstance.withResources(Collections.emptyList());
		verify(exploratoryDAO).insertExploratory(userInstance);
		verify(exploratoryDAO, never()).updateExploratoryStatus(any(StatusEnvBaseDTO.class));
		verifyNoMoreInteractions(exploratoryDAO);
	}

	@Test
	public void createWhenMethodNewExploratoryCreateThrowsException() {
		doNothing().when(exploratoryDAO).insertExploratory(any(UserInstanceDTO.class));
		ExploratoryGitCredsDTO egcDto = new ExploratoryGitCredsDTO();
		when(gitCredsDAO.findGitCreds(anyString())).thenReturn(egcDto);

		Exploratory exploratory = Exploratory.builder().name(EXPLORATORY_NAME).build();

		doThrow(new DlabException("Cannot create instance of resource class ")).when(requestBuilder)
				.newExploratoryCreate(any(Exploratory.class), any(UserInfo.class), any(ExploratoryGitCredsDTO.class));

		when(exploratoryDAO.updateExploratoryStatus(any(StatusEnvBaseDTO.class))).thenReturn(mock(UpdateResult.class));
		try {
			exploratoryService.create(userInfo, exploratory);
		} catch (DlabException e) {
			assertEquals("Could not create exploratory environment expName for user test: Cannot create instance " +
					"of resource class ", e.getMessage());
		}

		statusEnvBaseDTO = getStatusEnvBaseDTOWithStatus("failed");

		userInstance.withStatus("creating");
		userInstance.withResources(Collections.emptyList());
		verify(exploratoryDAO).insertExploratory(userInstance);
		verify(gitCredsDAO).findGitCreds(USER);
		verify(requestBuilder).newExploratoryCreate(exploratory, userInfo, egcDto);
		verify(exploratoryDAO).updateExploratoryStatus(refEq(statusEnvBaseDTO, "self"));
		verifyNoMoreInteractions(exploratoryDAO, gitCredsDAO, requestBuilder);
	}

	@Test
	public void updateExploratoryStatusesWithRunningStatus() {
		when(exploratoryDAO.fetchUserExploratoriesWhereStatusNotIn(anyString(), anyVararg()))
				.thenReturn(Collections.singletonList(userInstance));
		when(exploratoryDAO.updateExploratoryStatus(any(StatusEnvBaseDTO.class)))
				.thenReturn(mock(UpdateResult.class));

		exploratoryService.updateExploratoryStatuses(USER, UserInstanceStatus.RUNNING);

		statusEnvBaseDTO = getStatusEnvBaseDTOWithStatus("running");

		verify(exploratoryDAO).fetchUserExploratoriesWhereStatusNotIn(USER,
				UserInstanceStatus.TERMINATED, UserInstanceStatus.FAILED);
		verify(exploratoryDAO).updateExploratoryStatus(refEq(statusEnvBaseDTO, "self"));
		verifyNoMoreInteractions(exploratoryDAO);
	}

	@Test
	public void updateExploratoryStatusesWithStoppingStatus() {
		userInstance.setStatus("stopping");
		when(exploratoryDAO.fetchUserExploratoriesWhereStatusNotIn(anyString(), anyVararg()))
				.thenReturn(Collections.singletonList(userInstance));
		when(exploratoryDAO.updateExploratoryStatus(any(StatusEnvBaseDTO.class)))
				.thenReturn(mock(UpdateResult.class));
		doNothing().when(computationalDAO).updateComputationalStatusesForExploratory(anyString(), anyString(),
				any(UserInstanceStatus.class), any(UserInstanceStatus.class));

		exploratoryService.updateExploratoryStatuses(USER, UserInstanceStatus.STOPPING);

		statusEnvBaseDTO = getStatusEnvBaseDTOWithStatus("stopping");

		verify(exploratoryDAO).fetchUserExploratoriesWhereStatusNotIn(USER,
				UserInstanceStatus.TERMINATED, UserInstanceStatus.FAILED);
		verify(exploratoryDAO).updateExploratoryStatus(refEq(statusEnvBaseDTO, "self"));
		verify(computationalDAO).updateComputationalStatusesForExploratory(USER, EXPLORATORY_NAME,
				UserInstanceStatus.STOPPING, UserInstanceStatus.TERMINATING, UserInstanceStatus.FAILED,
				UserInstanceStatus.TERMINATED, UserInstanceStatus.STOPPED);
		verifyNoMoreInteractions(exploratoryDAO, computationalDAO);
	}

	@Test
	public void updateExploratoryStatusesWithTerminatingStatus() {
		userInstance.setStatus("terminating");
		when(exploratoryDAO.fetchUserExploratoriesWhereStatusNotIn(anyString(), anyVararg()))
				.thenReturn(Collections.singletonList(userInstance));
		when(exploratoryDAO.updateExploratoryStatus(any(StatusEnvBaseDTO.class)))
				.thenReturn(mock(UpdateResult.class));
		when(computationalDAO.updateComputationalStatusesForExploratory(any(StatusEnvBaseDTO.class)))
				.thenReturn(10);

		exploratoryService.updateExploratoryStatuses(USER, UserInstanceStatus.TERMINATING);

		statusEnvBaseDTO = getStatusEnvBaseDTOWithStatus("terminating");

		verify(exploratoryDAO).fetchUserExploratoriesWhereStatusNotIn(USER,
				UserInstanceStatus.TERMINATED, UserInstanceStatus.FAILED);
		verify(exploratoryDAO).updateExploratoryStatus(refEq(statusEnvBaseDTO, "self"));
		verify(computationalDAO).updateComputationalStatusesForExploratory(USER, EXPLORATORY_NAME, UserInstanceStatus
				.TERMINATING, UserInstanceStatus.TERMINATING, UserInstanceStatus.TERMINATED, UserInstanceStatus
				.FAILED);
		verifyNoMoreInteractions(exploratoryDAO, computationalDAO);
	}

	@Test
	public void updateUserExploratoriesReuploadKeyFlag() {
		doNothing().when(exploratoryDAO).updateReuploadKeyForExploratories(anyString(), anyBoolean(),
				any(UserInstanceStatus.class));

		exploratoryService.updateExploratoriesReuploadKeyFlag(USER, true, UserInstanceStatus.RUNNING);

		verify(exploratoryDAO).updateReuploadKeyForExploratories(USER, true, UserInstanceStatus.RUNNING);
		verifyNoMoreInteractions(exploratoryDAO);
	}

	@Test
	public void getInstancesWithStatuses() {
		when(exploratoryDAO.fetchUserExploratoriesWhereStatusIn(anyString(), anyBoolean(), anyVararg()))
				.thenReturn(Collections.singletonList(userInstance));
		exploratoryService.getInstancesWithStatuses(USER, UserInstanceStatus.RUNNING, UserInstanceStatus.RUNNING);

		verify(exploratoryDAO).fetchUserExploratoriesWhereStatusIn(USER, true, UserInstanceStatus.RUNNING);
		verifyNoMoreInteractions(exploratoryDAO);
	}

	@Test
	public void getUserInstance() {
		when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString())).thenReturn(userInstance);

		Optional<UserInstanceDTO> expectedInstance = Optional.of(userInstance);
		Optional<UserInstanceDTO> actualInstance = exploratoryService.getUserInstance(USER, EXPLORATORY_NAME);
		assertEquals(expectedInstance, actualInstance);

		verify(exploratoryDAO).fetchExploratoryFields(USER, EXPLORATORY_NAME);
		verifyNoMoreInteractions(exploratoryDAO);
	}

	@Test
	public void getUserInstanceWithException() {
		doThrow(new ResourceNotFoundException("Exploratory for user not found"))
				.when(exploratoryDAO).fetchExploratoryFields(anyString(), anyString());

		Optional<UserInstanceDTO> expectedInstance = Optional.empty();
		Optional<UserInstanceDTO> actualInstance = exploratoryService.getUserInstance(USER, EXPLORATORY_NAME);
		assertEquals(expectedInstance, actualInstance);

		verify(exploratoryDAO).fetchExploratoryFields(USER, EXPLORATORY_NAME);
		verifyNoMoreInteractions(exploratoryDAO);
	}

	@Test
	public void getInstancesByComputationalIds() {
		UserComputationalResource compResource1 = new UserComputationalResource();
		compResource1.setImageName("YYYY.dataengine");
		compResource1.setComputationalName("compName1");
		compResource1.setStatus("stopped");
		compResource1.setComputationalId("compId1");

		UserComputationalResource compResource2 = new UserComputationalResource();
		compResource2.setImageName("YYYY.dataengine");
		compResource2.setComputationalName("compName2");
		compResource2.setStatus("running");
		compResource2.setComputationalId("compId2");

		userInstance.setResources(Arrays.asList(compResource1, compResource2));
		when(exploratoryDAO.getInstances()).thenReturn(Collections.singletonList(userInstance));

		userInstance.setResources(Collections.singletonList(compResource1));
		userInstance.setStatus(null);
		List<UserInstanceDTO> expected = Collections.singletonList(userInstance);
		List<UserInstanceDTO> actual = exploratoryService.getInstancesByComputationalIds(Collections.singletonList(
				"compId1"));
		assertEquals(actual, expected);

		verify(exploratoryDAO).getInstances();
		verifyNoMoreInteractions(exploratoryDAO);
	}

	private UserInfo getUserInfo() {
		return new UserInfo(USER, TOKEN);
	}

	private UserInstanceDTO getUserInstanceDto() {
		UserComputationalResource compResource = new UserComputationalResource();
		compResource.setImageName("YYYY.dataengine");
		compResource.setComputationalName("compName");
		compResource.setStatus("stopped");
		compResource.setComputationalId("compId");
		return new UserInstanceDTO().withUser(USER).withExploratoryName(EXPLORATORY_NAME).withStatus("running")
				.withResources(Collections.singletonList(compResource));
	}

	private StatusEnvBaseDTO getStatusEnvBaseDTOWithStatus(String status) {
		return new ExploratoryStatusDTO()
				.withUser(USER)
				.withExploratoryName(EXPLORATORY_NAME)
				.withStatus(status);
	}

}
