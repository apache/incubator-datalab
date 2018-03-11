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
import com.epam.dlab.dto.base.DataEngineType;
import com.epam.dlab.dto.base.computational.ComputationalBase;
import com.epam.dlab.dto.computational.ComputationalStatusDTO;
import com.epam.dlab.dto.computational.ComputationalTerminateDTO;
import com.epam.dlab.dto.computational.SparkStandaloneClusterResource;
import com.epam.dlab.dto.computational.UserComputationalResource;
import com.epam.dlab.rest.client.RESTService;
import com.epam.dlab.rest.contracts.ComputationalAPI;
import com.mongodb.client.result.UpdateResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;


@RunWith(MockitoJUnitRunner.class)
public class ComputationalServiceImplTest {

	private final String USER = "test";
	private final String TOKEN = "token";
	private final String EXPLORATORY_NAME = "expName";
	private final String UUID = "1234-56789765-4321";

	private UserInfo userInfo;
	private List<ComputationalCreateFormDTO> formList;
	private UserInstanceDTO userInstance;

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

	@Before
	public void setUp() {
		userInfo = getUserInfo();
		formList = getFormList();
		userInstance = getUserInstanceDto();
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

		boolean creationResult =
				computationalService.createSparkCluster(userInfo, (SparkStandaloneClusterCreateForm) formList.get(0));
		assertEquals(true, creationResult);

		verify(configuration).getMinSparkInstanceCount();
		verify(configuration).getMaxSparkInstanceCount();

		verify(computationalDAO)
				.addComputational(eq(USER), eq(EXPLORATORY_NAME), any(SparkStandaloneClusterResource.class));
		verifyNoMoreInteractions(computationalDAO);

		verify(exploratoryDAO).fetchExploratoryFields(USER, EXPLORATORY_NAME);
		verifyNoMoreInteractions(exploratoryDAO);

		verify(requestBuilder).newComputationalCreate(
				refEq(userInfo), refEq(userInstance), any(SparkStandaloneClusterCreateForm.class));
		verifyNoMoreInteractions(requestBuilder);

		verify(provisioningService)
				.post(ComputationalAPI.COMPUTATIONAL_CREATE_SPARK, TOKEN, compBaseMocked, String.class);
		verifyNoMoreInteractions(provisioningService);

		verify(requestId).put(USER, UUID);
		verifyNoMoreInteractions(requestId);
	}

	@Test
	public void terminateComputationalEnvironment() {
		when(computationalDAO.updateComputationalStatus(any(ComputationalStatusDTO.class)))
				.thenReturn(mock(UpdateResult.class));
		String explId = "explId";
		when(exploratoryDAO.fetchExploratoryId(anyString(), anyString())).thenReturn(explId);

		String compId = "compId";
		UserComputationalResource ucResource = new UserComputationalResource();
		String compName = "compName";
		ucResource.setComputationalName(compName);
		ucResource.setImageName("dataengine-service");
		ucResource.setComputationalId(compId);
		when(computationalDAO.fetchComputationalFields(anyString(), anyString(), anyString())).thenReturn(ucResource);

		ComputationalTerminateDTO ctDto = new ComputationalTerminateDTO();
		ctDto.setComputationalName(compName);
		ctDto.setExploratoryName(EXPLORATORY_NAME);
		when(requestBuilder.newComputationalTerminate(any(UserInfo.class), anyString(), anyString(), anyString(),
				anyString(), any(DataEngineType.class))).thenReturn(ctDto);

		when(provisioningService.post(anyString(), anyString(), any(ComputationalTerminateDTO.class), any()))
				.thenReturn(UUID);
		when(requestId.put(anyString(), anyString())).thenReturn(UUID);

		computationalService.terminateComputationalEnvironment(userInfo, EXPLORATORY_NAME, compName);

		verify(computationalDAO).updateComputationalStatus(any(ComputationalStatusDTO.class));
		verify(computationalDAO).fetchComputationalFields(USER, EXPLORATORY_NAME, compName);

		verify(exploratoryDAO).fetchExploratoryId(USER, EXPLORATORY_NAME);
		verifyNoMoreInteractions(exploratoryDAO);

		verify(requestBuilder).newComputationalTerminate(userInfo, EXPLORATORY_NAME, explId, compName, compId,
				DataEngineType.CLOUD_SERVICE);
		verifyNoMoreInteractions(requestBuilder);

		verify(provisioningService).post(ComputationalAPI.COMPUTATIONAL_TERMINATE_CLOUD_SPECIFIC, TOKEN, ctDto,
				String.class);
		verifyNoMoreInteractions(provisioningService);

		verify(requestId).put(USER, UUID);
		verifyNoMoreInteractions(requestId);
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

		UserComputationalResource ucResource = new UserComputationalResource();
		ucResource.setImageName("des");
		boolean creationResult =
				computationalService.createDataEngineService(userInfo, formList.get(1), ucResource);
		assertEquals(true, creationResult);

		verify(computationalDAO)
				.addComputational(eq(USER), eq(EXPLORATORY_NAME), any(UserComputationalResource.class));
		verifyNoMoreInteractions(computationalDAO);

		verify(exploratoryDAO).fetchExploratoryFields(USER, EXPLORATORY_NAME);
		verifyNoMoreInteractions(exploratoryDAO);

		verify(requestBuilder).newComputationalCreate(
				refEq(userInfo), refEq(userInstance), any(ComputationalCreateFormDTO.class));
		verifyNoMoreInteractions(requestBuilder);

		verify(provisioningService)
				.post(ComputationalAPI.COMPUTATIONAL_CREATE_CLOUD_SPECIFIC, TOKEN, compBaseMocked, String.class);
		verifyNoMoreInteractions(provisioningService);

		verify(requestId).put(USER, UUID);
		verifyNoMoreInteractions(requestId);
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
		sparkClusterForm.setDataEngineInstanceCount(String.valueOf(2));
		sparkClusterForm.setImage("dataengine");
		ComputationalCreateFormDTO desClusterForm = new ComputationalCreateFormDTO();
		desClusterForm.setNotebookName(EXPLORATORY_NAME);
		return Arrays.asList(sparkClusterForm, desClusterForm);
	}

}
