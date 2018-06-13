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
import com.epam.dlab.backendapi.SelfServiceApplicationConfiguration;
import com.epam.dlab.backendapi.dao.ComputationalDAO;
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.resources.dto.ComputationalCreateFormDTO;
import com.epam.dlab.backendapi.resources.dto.SparkStandaloneClusterCreateForm;
import com.epam.dlab.backendapi.util.RequestBuilder;
import com.epam.dlab.dto.UserInstanceDTO;
import com.epam.dlab.dto.UserInstanceStatus;
import com.epam.dlab.dto.base.DataEngineType;
import com.epam.dlab.dto.base.computational.ComputationalBase;
import com.epam.dlab.dto.computational.*;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.exceptions.ResourceNotFoundException;
import com.epam.dlab.rest.client.RESTService;
import com.epam.dlab.rest.contracts.ComputationalAPI;
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

import static com.epam.dlab.dto.UserInstanceStatus.CREATING;
import static com.epam.dlab.dto.UserInstanceStatus.RUNNING;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class ComputationalServiceImplTest {

	private final String USER = "test";
	private final String TOKEN = "token";
	private final String EXPLORATORY_NAME = "expName";
	private final String COMP_NAME = "compName";
	private final String UUID = "1234-56789765-4321";

	private UserInfo userInfo;
	private List<ComputationalCreateFormDTO> formList;
	private UserInstanceDTO userInstance;
	private ComputationalStatusDTO computationalStatusDTOWithStatusTerminating;
	private ComputationalStatusDTO computationalStatusDTOWithStatusFailed;
	private ComputationalStatusDTO computationalStatusDTOWithStatusStopping;
	private ComputationalStatusDTO computationalStatusDTOWithStatusStarting;
	private SparkStandaloneClusterResource sparkClusterResource;
	private UserComputationalResource ucResource;

	@Mock
	private ExploratoryDAO exploratoryDAO;
	@Mock
	private ComputationalDAO computationalDAO;
	@Mock
	private RESTService provisioningService;
	@Mock
	private SelfServiceApplicationConfiguration configuration;
	@Mock
	private RequestBuilder requestBuilder;
	@Mock
	private RequestId requestId;

	@InjectMocks
	private ComputationalServiceImpl computationalService;

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Before
	public void setUp() {
		userInfo = getUserInfo();
		userInstance = getUserInstanceDto();
		formList = getFormList();
		computationalStatusDTOWithStatusTerminating = getComputationalStatusDTOWithStatus("terminating");
		computationalStatusDTOWithStatusFailed = getComputationalStatusDTOWithStatus("failed");
		computationalStatusDTOWithStatusStopping = getComputationalStatusDTOWithStatus("stopping");
		computationalStatusDTOWithStatusStarting = getComputationalStatusDTOWithStatus("starting");
		sparkClusterResource = getSparkClusterResource();
		ucResource = getUserComputationalResource();
	}

	@Test
	public void createSparkCluster() {
		when(configuration.getMinSparkInstanceCount()).thenReturn(2);
		when(configuration.getMaxSparkInstanceCount()).thenReturn(1000);
		when(computationalDAO.addComputational(anyString(), anyString(),
				any(SparkStandaloneClusterResource.class))).thenReturn(true);
		when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString())).thenReturn(userInstance);

		ComputationalBase compBaseMocked = mock(ComputationalBase.class);
		when(requestBuilder.newComputationalCreate(any(UserInfo.class), any(UserInstanceDTO.class),
				any(SparkStandaloneClusterCreateForm.class))).thenReturn(compBaseMocked);
		when(provisioningService.post(anyString(), anyString(), any(ComputationalBase.class), any())).thenReturn(UUID);
		when(requestId.put(anyString(), anyString())).thenReturn(UUID);

		SparkStandaloneClusterCreateForm sparkClusterCreateForm = (SparkStandaloneClusterCreateForm) formList.get(0);
		boolean creationResult =
				computationalService.createSparkCluster(userInfo, sparkClusterCreateForm);
		assertTrue(creationResult);

		verify(configuration).getMinSparkInstanceCount();
		verify(configuration).getMaxSparkInstanceCount();

		verify(computationalDAO)
				.addComputational(eq(USER), eq(EXPLORATORY_NAME), refEq(sparkClusterResource));

		verify(exploratoryDAO).fetchExploratoryFields(USER, EXPLORATORY_NAME);
		verify(requestBuilder).newComputationalCreate(
				refEq(userInfo), refEq(userInstance), refEq(sparkClusterCreateForm));

		verify(provisioningService)
				.post(ComputationalAPI.COMPUTATIONAL_CREATE_SPARK, TOKEN, compBaseMocked, String.class);

		verify(requestId).put(USER, UUID);
		verifyNoMoreInteractions(configuration, computationalDAO, requestBuilder, provisioningService, requestId);
	}

	@Test
	public void createSparkClusterWithInappropriateDataEngineInstanceCount() {
		when(configuration.getMinSparkInstanceCount()).thenReturn(2);
		when(configuration.getMaxSparkInstanceCount()).thenReturn(1000);

		expectedException.expect(IllegalArgumentException.class);
		expectedException.expectMessage("Instance count should be in range ");

		SparkStandaloneClusterCreateForm sparkClusterForm = (SparkStandaloneClusterCreateForm) formList.get(0);
		sparkClusterForm.setDataEngineInstanceCount(String.valueOf(1));
		computationalService.createSparkCluster(userInfo, sparkClusterForm);
	}

	@Test
	public void createSparkClusterWithInappropriateDataEngineImageType() {
		when(configuration.getMinSparkInstanceCount()).thenReturn(2);
		when(configuration.getMaxSparkInstanceCount()).thenReturn(1000);

		expectedException.expect(IllegalArgumentException.class);
		expectedException.expectMessage("Unknown data engine ");

		SparkStandaloneClusterCreateForm sparkClusterForm = (SparkStandaloneClusterCreateForm) formList.get(0);
		sparkClusterForm.setImage("someImageType");
		computationalService.createSparkCluster(userInfo, sparkClusterForm);
	}

	@Test
	public void createSparkClusterWhenResourceAlreadyExists() {
		when(configuration.getMinSparkInstanceCount()).thenReturn(2);
		when(configuration.getMaxSparkInstanceCount()).thenReturn(1000);
		when(computationalDAO.addComputational(anyString(), anyString(),
				any(SparkStandaloneClusterResource.class))).thenReturn(false);

		boolean creationResult =
				computationalService.createSparkCluster(userInfo, (SparkStandaloneClusterCreateForm) formList.get(0));
		assertFalse(creationResult);

		verify(configuration).getMinSparkInstanceCount();
		verify(configuration).getMaxSparkInstanceCount();

		verify(computationalDAO).addComputational(eq(USER), eq(EXPLORATORY_NAME), refEq(sparkClusterResource));
		verifyNoMoreInteractions(configuration, computationalDAO);
	}

	@Test
	public void createSparkClusterWhenMethodFetchExploratoryFieldsThrowsException() {
		when(configuration.getMinSparkInstanceCount()).thenReturn(2);
		when(configuration.getMaxSparkInstanceCount()).thenReturn(1000);
		when(computationalDAO.addComputational(anyString(), anyString(),
				any(SparkStandaloneClusterResource.class))).thenReturn(true);
		doThrow(new ResourceNotFoundException("Exploratory for user with name not found"))
				.when(exploratoryDAO).fetchExploratoryFields(anyString(), anyString());

		when(computationalDAO.updateComputationalStatus(any(ComputationalStatusDTO.class)))
				.thenReturn(mock(UpdateResult.class));

		SparkStandaloneClusterCreateForm sparkClusterCreateForm = (SparkStandaloneClusterCreateForm) formList.get(0);
		try {
			computationalService.createSparkCluster(userInfo, sparkClusterCreateForm);
		} catch (ResourceNotFoundException e) {
			assertEquals("Exploratory for user with name not found", e.getMessage());
		}

		verify(configuration).getMinSparkInstanceCount();
		verify(configuration).getMaxSparkInstanceCount();
		verify(computationalDAO).addComputational(USER, EXPLORATORY_NAME, sparkClusterResource);
		verify(computationalDAO).updateComputationalStatus(refEq(computationalStatusDTOWithStatusFailed, "self"));
		verify(exploratoryDAO).fetchExploratoryFields(USER, EXPLORATORY_NAME);
		verifyNoMoreInteractions(configuration, computationalDAO, exploratoryDAO);
	}

	@Test
	public void createSparkClusterWhenMethodNewComputationalCreateThrowsException() {
		when(configuration.getMinSparkInstanceCount()).thenReturn(2);
		when(configuration.getMaxSparkInstanceCount()).thenReturn(1000);
		when(computationalDAO.addComputational(anyString(), anyString(),
				any(SparkStandaloneClusterResource.class))).thenReturn(true);
		when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString())).thenReturn(userInstance);

		doThrow(new DlabException("Cannot create instance of resource class "))
				.when(requestBuilder).newComputationalCreate(any(UserInfo.class), any(UserInstanceDTO.class),
				any(SparkStandaloneClusterCreateForm.class));

		when(computationalDAO.updateComputationalStatus(any(ComputationalStatusDTO.class)))
				.thenReturn(mock(UpdateResult.class));

		SparkStandaloneClusterCreateForm sparkClusterCreateForm = (SparkStandaloneClusterCreateForm) formList.get(0);
		try {
			computationalService.createSparkCluster(userInfo, sparkClusterCreateForm);
		} catch (DlabException e) {
			assertEquals("Cannot create instance of resource class ", e.getMessage());
		}

		verify(configuration).getMinSparkInstanceCount();
		verify(configuration).getMaxSparkInstanceCount();
		verify(computationalDAO).addComputational(USER, EXPLORATORY_NAME, sparkClusterResource);
		verify(computationalDAO).updateComputationalStatus(refEq(computationalStatusDTOWithStatusFailed, "self"));
		verify(exploratoryDAO).fetchExploratoryFields(USER, EXPLORATORY_NAME);
		verify(requestBuilder).newComputationalCreate(userInfo, userInstance, sparkClusterCreateForm);
		verifyNoMoreInteractions(configuration, computationalDAO, exploratoryDAO, requestBuilder);
	}

	@Test
	public void terminateComputationalEnvironment() {
		when(computationalDAO.updateComputationalStatus(any(ComputationalStatusDTO.class)))
				.thenReturn(mock(UpdateResult.class));
		String explId = "explId";
		when(exploratoryDAO.fetchExploratoryId(anyString(), anyString())).thenReturn(explId);

		String compId = "compId";
		UserComputationalResource ucResource = new UserComputationalResource();
		ucResource.setComputationalName(COMP_NAME);
		ucResource.setImageName("dataengine-service");
		ucResource.setComputationalId(compId);
		when(computationalDAO.fetchComputationalFields(anyString(), anyString(), anyString())).thenReturn(ucResource);

		ComputationalTerminateDTO ctDto = new ComputationalTerminateDTO();
		ctDto.setComputationalName(COMP_NAME);
		ctDto.setExploratoryName(EXPLORATORY_NAME);
		when(requestBuilder.newComputationalTerminate(any(UserInfo.class), anyString(), anyString(), anyString(),
				anyString(), any(DataEngineType.class))).thenReturn(ctDto);

		when(provisioningService.post(anyString(), anyString(), any(ComputationalTerminateDTO.class), any()))
				.thenReturn(UUID);
		when(requestId.put(anyString(), anyString())).thenReturn(UUID);

		computationalService.terminateComputationalEnvironment(userInfo, EXPLORATORY_NAME, COMP_NAME);

		verify(computationalDAO).updateComputationalStatus(refEq(computationalStatusDTOWithStatusTerminating, "self"));
		verify(computationalDAO).fetchComputationalFields(USER, EXPLORATORY_NAME, COMP_NAME);

		verify(exploratoryDAO).fetchExploratoryId(USER, EXPLORATORY_NAME);

		verify(requestBuilder).newComputationalTerminate(userInfo, EXPLORATORY_NAME, explId, COMP_NAME, compId,
				DataEngineType.CLOUD_SERVICE);

		verify(provisioningService).post(ComputationalAPI.COMPUTATIONAL_TERMINATE_CLOUD_SPECIFIC, TOKEN, ctDto,
				String.class);

		verify(requestId).put(USER, UUID);
		verifyNoMoreInteractions(computationalDAO, exploratoryDAO, requestBuilder, provisioningService, requestId);
	}

	@Test
	public void terminateComputationalEnvironmentWhenMethodUpdateComputationalStatusThrowsException() {
		doThrow(new DlabException("Could not update computational resource status"))
				.when(computationalDAO).updateComputationalStatus(refEq(computationalStatusDTOWithStatusTerminating,
				"self"));

		when(computationalDAO.updateComputationalStatus(refEq(computationalStatusDTOWithStatusFailed, "self")))
				.thenReturn(mock(UpdateResult.class));

		try {
			computationalService.terminateComputationalEnvironment(userInfo, EXPLORATORY_NAME, COMP_NAME);
		} catch (DlabException e) {
			assertEquals("Could not update computational resource status", e.getMessage());
		}

		verify(computationalDAO).updateComputationalStatus(refEq(computationalStatusDTOWithStatusTerminating, "self"));
		verify(computationalDAO).updateComputationalStatus(refEq(computationalStatusDTOWithStatusFailed, "self"));
		verifyNoMoreInteractions(computationalDAO);
	}

	@Test
	public void terminateComputationalEnvironmentWhenMethodFetchComputationalFieldsThrowsException() {
		when(computationalDAO.updateComputationalStatus(any(ComputationalStatusDTO.class)))
				.thenReturn(mock(UpdateResult.class));
		String explId = "explId";
		when(exploratoryDAO.fetchExploratoryId(anyString(), anyString())).thenReturn(explId);

		doThrow(new DlabException("Computational resource for user with exploratory name not found."))
				.when(computationalDAO).fetchComputationalFields(anyString(), anyString(), anyString());
		when(computationalDAO.updateComputationalStatus(any(ComputationalStatusDTO.class)))
				.thenReturn(mock(UpdateResult.class));

		try {
			computationalService.terminateComputationalEnvironment(userInfo, EXPLORATORY_NAME, COMP_NAME);
		} catch (DlabException e) {
			assertEquals("Computational resource for user with exploratory name not found.", e.getMessage());
		}

		verify(computationalDAO).updateComputationalStatus(refEq(computationalStatusDTOWithStatusTerminating, "self"));
		verify(computationalDAO).fetchComputationalFields(USER, EXPLORATORY_NAME, COMP_NAME);
		verify(computationalDAO).updateComputationalStatus(refEq(computationalStatusDTOWithStatusFailed, "self"));
		verify(exploratoryDAO).fetchExploratoryId(USER, EXPLORATORY_NAME);
		verifyNoMoreInteractions(computationalDAO, exploratoryDAO);
	}

	@Test
	public void terminateComputationalEnvironmentWhenMethodNewComputationalTerminateThrowsException() {
		when(computationalDAO.updateComputationalStatus(any(ComputationalStatusDTO.class)))
				.thenReturn(mock(UpdateResult.class));
		String explId = "explId";
		when(exploratoryDAO.fetchExploratoryId(anyString(), anyString())).thenReturn(explId);

		String compId = "compId";
		UserComputationalResource ucResource = new UserComputationalResource();
		ucResource.setComputationalName(COMP_NAME);
		ucResource.setImageName("dataengine-service");
		ucResource.setComputationalId(compId);
		when(computationalDAO.fetchComputationalFields(anyString(), anyString(), anyString())).thenReturn(ucResource);

		doThrow(new DlabException("Cannot create instance of resource class "))
				.when(requestBuilder).newComputationalTerminate(any(UserInfo.class), anyString(), anyString(),
				anyString(), anyString(), any(DataEngineType.class));

		when(computationalDAO.updateComputationalStatus(any(ComputationalStatusDTO.class)))
				.thenReturn(mock(UpdateResult.class));

		try {
			computationalService.terminateComputationalEnvironment(userInfo, EXPLORATORY_NAME, COMP_NAME);
		} catch (DlabException e) {
			assertEquals("Cannot create instance of resource class ", e.getMessage());
		}

		verify(computationalDAO).updateComputationalStatus(refEq(computationalStatusDTOWithStatusTerminating, "self"));
		verify(computationalDAO).fetchComputationalFields(USER, EXPLORATORY_NAME, COMP_NAME);

		verify(exploratoryDAO).fetchExploratoryId(USER, EXPLORATORY_NAME);

		verify(requestBuilder).newComputationalTerminate(userInfo, EXPLORATORY_NAME, explId, COMP_NAME, compId,
				DataEngineType.CLOUD_SERVICE);
		verify(computationalDAO).updateComputationalStatus(refEq(computationalStatusDTOWithStatusFailed, "self"));
		verifyNoMoreInteractions(computationalDAO, exploratoryDAO, requestBuilder);
	}

	@Test
	public void createDataEngineService() {
		when(computationalDAO.addComputational(anyString(), anyString(), any(UserComputationalResource.class)))
				.thenReturn(true);
		when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString())).thenReturn(userInstance);

		ComputationalBase compBaseMocked = mock(ComputationalBase.class);
		when(requestBuilder.newComputationalCreate(any(UserInfo.class), any(UserInstanceDTO.class),
				any(ComputationalCreateFormDTO.class))).thenReturn(compBaseMocked);

		when(provisioningService.post(anyString(), anyString(), any(ComputationalBase.class), any())).thenReturn(UUID);
		when(requestId.put(anyString(), anyString())).thenReturn(UUID);

		boolean creationResult =
				computationalService.createDataEngineService(userInfo, formList.get(1), ucResource);
		assertTrue(creationResult);

		verify(computationalDAO).addComputational(eq(USER), eq(EXPLORATORY_NAME), refEq(ucResource));

		verify(exploratoryDAO).fetchExploratoryFields(USER, EXPLORATORY_NAME);

		verify(requestBuilder).newComputationalCreate(
				refEq(userInfo), refEq(userInstance), any(ComputationalCreateFormDTO.class));

		verify(provisioningService)
				.post(ComputationalAPI.COMPUTATIONAL_CREATE_CLOUD_SPECIFIC, TOKEN, compBaseMocked, String.class);

		verify(requestId).put(USER, UUID);
		verifyNoMoreInteractions(computationalDAO, exploratoryDAO, requestBuilder, provisioningService, requestId);
	}

	@Test
	public void createDataEngineServiceWhenComputationalResourceNotAdded() {
		when(computationalDAO.addComputational(anyString(), anyString(), any(UserComputationalResource.class)))
				.thenReturn(false);

		boolean creationResult = computationalService.createDataEngineService(userInfo, formList.get(1), ucResource);
		assertFalse(creationResult);

		verify(computationalDAO).addComputational(eq(USER), eq(EXPLORATORY_NAME), refEq(ucResource));
		verifyNoMoreInteractions(computationalDAO);
	}

	@Test
	public void createDataEngineServiceWhenMethodFetchExploratoryFieldsThrowsException() {
		when(computationalDAO.addComputational(anyString(), anyString(), any(UserComputationalResource.class)))
				.thenReturn(true);
		doThrow(new ResourceNotFoundException("Exploratory for user with name not found"))
				.when(exploratoryDAO).fetchExploratoryFields(anyString(), anyString());

		when(computationalDAO.updateComputationalStatus(any(ComputationalStatusDTO.class)))
				.thenReturn(mock(UpdateResult.class));

		try {
			computationalService.createDataEngineService(userInfo, formList.get(1), ucResource);
		} catch (DlabException e) {
			assertEquals("Could not send request for creation the computational resource compName: " +
					"Exploratory for user with name not found", e.getMessage());
		}

		verify(computationalDAO)
				.addComputational(eq(USER), eq(EXPLORATORY_NAME), refEq(ucResource));

		verify(exploratoryDAO).fetchExploratoryFields(USER, EXPLORATORY_NAME);

		verify(computationalDAO).updateComputationalStatus(refEq(computationalStatusDTOWithStatusFailed, "self"));
		verifyNoMoreInteractions(computationalDAO, exploratoryDAO);
	}

	@Test
	public void createDataEngineServiceWhenMethodNewComputationalCreateThrowsException() {
		when(computationalDAO.addComputational(anyString(), anyString(), any(UserComputationalResource.class)))
				.thenReturn(true);
		when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString())).thenReturn(userInstance);

		doThrow(new DlabException("Cannot create instance of resource class "))
				.when(requestBuilder).newComputationalCreate(any(UserInfo.class), any(UserInstanceDTO.class),
				any(ComputationalCreateFormDTO.class));

		when(computationalDAO.updateComputationalStatus(any(ComputationalStatusDTO.class)))
				.thenReturn(mock(UpdateResult.class));

		ComputationalCreateFormDTO computationalCreateFormDTO = formList.get(1);
		try {
			computationalService.createDataEngineService(userInfo, computationalCreateFormDTO, ucResource);
		} catch (DlabException e) {
			assertEquals("Could not send request for creation the computational resource compName: " +
					"Cannot create instance of resource class ", e.getMessage());
		}

		verify(computationalDAO).addComputational(eq(USER), eq(EXPLORATORY_NAME), refEq(ucResource));
		verify(exploratoryDAO).fetchExploratoryFields(USER, EXPLORATORY_NAME);
		verify(requestBuilder).newComputationalCreate(
				refEq(userInfo), refEq(userInstance), refEq(computationalCreateFormDTO));
		verify(computationalDAO).updateComputationalStatus(refEq(computationalStatusDTOWithStatusFailed, "self"));

		verifyNoMoreInteractions(computationalDAO, exploratoryDAO, requestBuilder);
	}

	@Test
	public void stopSparkCluster() {
		final UserInstanceDTO exploratory = new UserInstanceDTO();
		exploratory.setExploratoryId("someId");
		exploratory.setExploratoryName(EXPLORATORY_NAME);
		when(computationalDAO.fetchComputationalFields(anyString(), anyString(), anyString())).thenReturn(ucResource);
		when(computationalDAO.updateComputationalStatus(any(ComputationalStatusDTO.class)))
				.thenReturn(mock(UpdateResult.class));
		when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString())).thenReturn(exploratory);

		ComputationalStopDTO computationalStopDTO = new ComputationalStopDTO();
		when(requestBuilder.newComputationalStop(any(UserInfo.class), any(UserInstanceDTO.class), anyString()))
				.thenReturn(computationalStopDTO);
		when(provisioningService.post(anyString(), anyString(), any(ComputationalBase.class), any()))
				.thenReturn("someUuid");
		when(requestId.put(anyString(), anyString())).thenReturn("someUuid");

		computationalService.stopSparkCluster(userInfo, EXPLORATORY_NAME, COMP_NAME);

		verify(computationalDAO).fetchComputationalFields(USER, EXPLORATORY_NAME, COMP_NAME);
		verify(computationalDAO).updateComputationalStatus(refEq(computationalStatusDTOWithStatusStopping, "self"));
		verify(exploratoryDAO).fetchExploratoryFields(USER, EXPLORATORY_NAME);
		verify(requestBuilder).newComputationalStop(refEq(userInfo), refEq(exploratory), eq(COMP_NAME));
		verify(provisioningService)
				.post(eq("computational/stop/spark"), eq(TOKEN), refEq(computationalStopDTO), eq(String.class));
		verify(requestId).put(USER, "someUuid");
		verifyNoMoreInteractions(computationalDAO, exploratoryDAO, requestBuilder,
				provisioningService, requestId);
	}

	@Test
	public void stopSparkClusterWhenDataengineTypeIsAnother() {
		ucResource.setImageName("dataengine-service");
		when(computationalDAO.fetchComputationalFields(anyString(), anyString(), anyString())).thenReturn(ucResource);

		expectedException.expect(UnsupportedOperationException.class);
		expectedException.expectMessage("Operation for data engine service is not supported");

		computationalService.stopSparkCluster(userInfo, EXPLORATORY_NAME, COMP_NAME);
	}

	@Test
	public void startSparkCluster() {
		final UserInstanceDTO exploratory = new UserInstanceDTO();
		exploratory.setExploratoryId("someId");
		exploratory.setExploratoryName(EXPLORATORY_NAME);
		when(computationalDAO.fetchComputationalFields(anyString(), anyString(), anyString())).thenReturn(ucResource);
		when(computationalDAO.updateComputationalStatus(any(ComputationalStatusDTO.class)))
				.thenReturn(mock(UpdateResult.class));
		when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString())).thenReturn(exploratory);

		ComputationalStartDTO computationalStartDTO = new ComputationalStartDTO();
		when(requestBuilder.newComputationalStart(any(UserInfo.class), any(UserInstanceDTO.class), anyString()))
				.thenReturn(computationalStartDTO);
		when(provisioningService.post(anyString(), anyString(), any(ComputationalBase.class), any()))
				.thenReturn("someUuid");
		when(requestId.put(anyString(), anyString())).thenReturn("someUuid");

		computationalService.startSparkCluster(userInfo, EXPLORATORY_NAME, COMP_NAME);

		verify(computationalDAO).fetchComputationalFields(USER, EXPLORATORY_NAME, COMP_NAME);
		verify(computationalDAO).updateComputationalStatus(refEq(computationalStatusDTOWithStatusStarting, "self"));
		verify(exploratoryDAO).fetchExploratoryFields(USER, EXPLORATORY_NAME);
		verify(requestBuilder).newComputationalStart(refEq(userInfo), refEq(exploratory), eq(COMP_NAME));
		verify(provisioningService)
				.post(eq("computational/start/spark"), eq(TOKEN), refEq(computationalStartDTO), eq(String.class));
		verify(requestId).put(USER, "someUuid");
		verifyNoMoreInteractions(computationalDAO, exploratoryDAO, requestBuilder,
				provisioningService, requestId);
	}

	@Test
	public void startSparkClusterWhenDataengineTypeIsAnother() {
		ucResource.setImageName("dataengine-service");
		when(computationalDAO.fetchComputationalFields(anyString(), anyString(), anyString())).thenReturn(ucResource);

		expectedException.expect(UnsupportedOperationException.class);
		expectedException.expectMessage("Operation for data engine service is not supported");

		computationalService.startSparkCluster(userInfo, EXPLORATORY_NAME, COMP_NAME);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void updateComputationalsReuploadKeyFlag() {
		doNothing().when(computationalDAO).updateReuploadKeyFlagForComputationalResources(anyString(), any(List.class),
				any(List.class), anyBoolean(), anyVararg());

		computationalService.updateComputationalsReuploadKeyFlag(USER, Collections.singletonList(RUNNING),
				Collections.singletonList(DataEngineType.SPARK_STANDALONE), true, RUNNING);

		verify(computationalDAO).updateReuploadKeyFlagForComputationalResources(USER, Collections.singletonList
						(RUNNING),
				Collections.singletonList(DataEngineType.SPARK_STANDALONE), true, RUNNING);
		verifyNoMoreInteractions(computationalDAO);
	}

	@Test
	public void getComputationalResource() {
		when(computationalDAO.fetchComputationalFields(anyString(), anyString(), anyString())).thenReturn(ucResource);

		Optional<UserComputationalResource> expectedResource = Optional.of(ucResource);
		Optional<UserComputationalResource> actualResource =
				computationalService.getComputationalResource(USER, EXPLORATORY_NAME, COMP_NAME);
		assertEquals(expectedResource, actualResource);

		verify(computationalDAO).fetchComputationalFields(USER, EXPLORATORY_NAME, COMP_NAME);
		verifyNoMoreInteractions(computationalDAO);
	}

	@Test
	public void getComputationalResourceWithException() {
		doThrow(new DlabException("Computational resource not found"))
				.when(computationalDAO).fetchComputationalFields(anyString(), anyString(), anyString());

		Optional<UserComputationalResource> expectedResource = Optional.empty();
		Optional<UserComputationalResource> actualResource =
				computationalService.getComputationalResource(USER, EXPLORATORY_NAME, COMP_NAME);
		assertEquals(expectedResource, actualResource);

		verify(computationalDAO).fetchComputationalFields(USER, EXPLORATORY_NAME, COMP_NAME);
		verifyNoMoreInteractions(computationalDAO);
	}

	private UserInfo getUserInfo() {
		return new UserInfo(USER, TOKEN);
	}

	private UserInstanceDTO getUserInstanceDto() {
		return new UserInstanceDTO().withUser(USER).withExploratoryName(EXPLORATORY_NAME);
	}

	private List<ComputationalCreateFormDTO> getFormList() {
		SparkStandaloneClusterCreateForm sparkClusterForm = new SparkStandaloneClusterCreateForm();
		sparkClusterForm.setNotebookName(EXPLORATORY_NAME);
		sparkClusterForm.setName(COMP_NAME);
		sparkClusterForm.setDataEngineInstanceCount(String.valueOf(2));
		sparkClusterForm.setImage("dataengine");
		ComputationalCreateFormDTO desClusterForm = new ComputationalCreateFormDTO();
		desClusterForm.setNotebookName(EXPLORATORY_NAME);
		desClusterForm.setName(COMP_NAME);
		return Arrays.asList(sparkClusterForm, desClusterForm);
	}

	private ComputationalStatusDTO getComputationalStatusDTOWithStatus(String status) {
		return new ComputationalStatusDTO()
				.withUser(USER)
				.withExploratoryName(EXPLORATORY_NAME)
				.withComputationalName(COMP_NAME)
				.withStatus(UserInstanceStatus.of(status));
	}

	private SparkStandaloneClusterResource getSparkClusterResource() {
		return SparkStandaloneClusterResource.builder()
				.computationalName(COMP_NAME)
				.imageName("dataengine")
				.status(CREATING.toString())
				.dataEngineInstanceCount(String.valueOf(2))
				.build();
	}

	private UserComputationalResource getUserComputationalResource() {
		UserComputationalResource ucResource = new UserComputationalResource();
		ucResource.setImageName("dataengine");
		return ucResource;
	}

}
