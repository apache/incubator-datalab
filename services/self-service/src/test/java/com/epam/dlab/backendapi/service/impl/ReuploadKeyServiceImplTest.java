package com.epam.dlab.backendapi.service.impl;


import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.ComputationalDAO;
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.dao.KeyDAO;
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.service.ExploratoryService;
import com.epam.dlab.backendapi.service.UserResourceService;
import com.epam.dlab.backendapi.util.RequestBuilder;
import com.epam.dlab.dto.UserInstanceDTO;
import com.epam.dlab.dto.UserInstanceStatus;
import com.epam.dlab.dto.aws.edge.EdgeInfoAws;
import com.epam.dlab.dto.base.DataEngineType;
import com.epam.dlab.dto.base.edge.EdgeInfo;
import com.epam.dlab.dto.reuploadkey.ReuploadKeyCallbackDTO;
import com.epam.dlab.dto.reuploadkey.ReuploadKeyDTO;
import com.epam.dlab.dto.reuploadkey.ReuploadKeyStatus;
import com.epam.dlab.dto.reuploadkey.ReuploadKeyStatusDTO;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.model.ResourceData;
import com.epam.dlab.model.ResourceType;
import com.epam.dlab.rest.client.RESTService;
import com.mongodb.client.result.UpdateResult;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.*;

import static com.epam.dlab.dto.UserInstanceStatus.REUPLOADING_KEY;
import static com.epam.dlab.dto.UserInstanceStatus.RUNNING;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ReuploadKeyServiceImplTest {

	private final String USER = "test";
	private final String TOKEN = "token";
	private final String EXPLORATORY_NAME = "explName";

	private UserInfo userInfo;

	@Mock
	private KeyDAO keyDAO;
	@Mock
	private RESTService provisioningService;
	@Mock
	private RequestBuilder requestBuilder;
	@Mock
	private RequestId requestId;
	@Mock
	private ExploratoryService exploratoryService;
	@Mock
	private ComputationalDAO computationalDAO;
	@Mock
	private ExploratoryDAO exploratoryDAO;
	@Mock
	private UserResourceService userResourceService;

	@InjectMocks
	private ReuploadKeyServiceImpl reuploadKeyService;


	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Before
	public void setUp() {
		userInfo = getUserInfo();
	}


	@Test
	@SuppressWarnings("unchecked")
	public void reuploadKey() {
		doNothing().when(userResourceService).updateReuploadKeyFlagForUserResources(anyString(), anyBoolean());
		List<UserInstanceDTO> instances = Collections.singletonList(getUserInstance());
		when(exploratoryService.getInstancesWithStatuses(anyString(), any(UserInstanceStatus.class),
				any(UserInstanceStatus.class))).thenReturn(instances);
		List<ResourceData> resourceList = new ArrayList<>();
		resourceList.add(new ResourceData(ResourceType.EXPLORATORY, "someId", EXPLORATORY_NAME, null));
		when(userResourceService.convertToResourceData(any(List.class))).thenReturn(resourceList);

		Optional<EdgeInfoAws> edgeInfo = Optional.of(new EdgeInfoAws());
		Mockito.<Optional<? extends EdgeInfo>>when(keyDAO.getEdgeInfoWhereStatusIn(anyString(), anyVararg()))
				.thenReturn(edgeInfo);
		doNothing().when(keyDAO).updateEdgeStatus(anyString(), anyString());

		doNothing().when(exploratoryDAO).updateStatusForExploratories(any(UserInstanceStatus.class), anyString(),
				any(UserInstanceStatus.class));
		doNothing().when(computationalDAO).updateStatusForComputationalResources(any(UserInstanceStatus.class),
				anyString(), any(List.class), any(List.class), any(UserInstanceStatus.class));
		ReuploadKeyDTO reuploadFile = mock(ReuploadKeyDTO.class);
		when(requestBuilder.newKeyReupload(any(UserInfo.class), anyString(), anyString(), any(List.class)))
				.thenReturn(reuploadFile);
		String expectedUuid = "someUuid";
		when(provisioningService.post(anyString(), anyString(), any(ReuploadKeyDTO.class), any()))
				.thenReturn(expectedUuid);

		String keyContent = "keyContent";
		String actualUuid = reuploadKeyService.reuploadKey(userInfo, keyContent);
		assertNotNull(actualUuid);
		assertEquals(expectedUuid, actualUuid);
		assertEquals(2, resourceList.size());

		verify(userResourceService).updateReuploadKeyFlagForUserResources(USER, true);
		verify(exploratoryService).getInstancesWithStatuses(USER, RUNNING, RUNNING);
		verify(userResourceService).convertToResourceData(instances);
		verify(keyDAO).getEdgeInfoWhereStatusIn(USER, RUNNING);
		verify(keyDAO).updateEdgeStatus(USER, "reuploading key");
		verify(exploratoryDAO).updateStatusForExploratories(REUPLOADING_KEY, USER, RUNNING);
		verify(computationalDAO).updateStatusForComputationalResources(REUPLOADING_KEY, USER,
				Arrays.asList(RUNNING, REUPLOADING_KEY), Arrays.asList(DataEngineType.SPARK_STANDALONE,
						DataEngineType.CLOUD_SERVICE), RUNNING);
		verify(requestBuilder).newKeyReupload(refEq(userInfo), anyString(), eq(keyContent), any(List.class));
		verify(provisioningService).post("/reupload_key", TOKEN, reuploadFile, String.class);
		verifyNoMoreInteractions(userResourceService, exploratoryService, keyDAO, exploratoryDAO, computationalDAO,
				requestBuilder, provisioningService);
		verifyZeroInteractions(requestId);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void reuploadKeyWithoutEdge() {
		doNothing().when(userResourceService).updateReuploadKeyFlagForUserResources(anyString(), anyBoolean());
		List<UserInstanceDTO> instances = Collections.singletonList(getUserInstance());
		when(exploratoryService.getInstancesWithStatuses(anyString(), any(UserInstanceStatus.class),
				any(UserInstanceStatus.class))).thenReturn(instances);
		List<ResourceData> resourceList = new ArrayList<>();
		resourceList.add(new ResourceData(ResourceType.EXPLORATORY, "someId", EXPLORATORY_NAME, null));
		when(userResourceService.convertToResourceData(any(List.class))).thenReturn(resourceList);
		when(keyDAO.getEdgeInfoWhereStatusIn(anyString(), anyVararg())).thenReturn(Optional.empty());
		doNothing().when(exploratoryDAO).updateStatusForExploratories(any(UserInstanceStatus.class), anyString(),
				any(UserInstanceStatus.class));
		doNothing().when(computationalDAO).updateStatusForComputationalResources(any(UserInstanceStatus.class),
				anyString(), any(List.class), any(List.class), any(UserInstanceStatus.class));
		ReuploadKeyDTO reuploadFile = mock(ReuploadKeyDTO.class);
		when(requestBuilder.newKeyReupload(any(UserInfo.class), anyString(), anyString(), any(List.class)))
				.thenReturn(reuploadFile);
		String expectedUuid = "someUuid";
		when(provisioningService.post(anyString(), anyString(), any(ReuploadKeyDTO.class), any()))
				.thenReturn(expectedUuid);

		String keyContent = "keyContent";
		String actualUuid = reuploadKeyService.reuploadKey(userInfo, keyContent);
		assertNotNull(actualUuid);
		assertEquals(expectedUuid, actualUuid);
		assertEquals(1, resourceList.size());

		verify(userResourceService).updateReuploadKeyFlagForUserResources(USER, true);
		verify(exploratoryService).getInstancesWithStatuses(USER, RUNNING, RUNNING);
		verify(userResourceService).convertToResourceData(instances);
		verify(keyDAO).getEdgeInfoWhereStatusIn(USER, RUNNING);
		verify(exploratoryDAO).updateStatusForExploratories(REUPLOADING_KEY, USER, RUNNING);
		verify(computationalDAO).updateStatusForComputationalResources(REUPLOADING_KEY, USER,
				Arrays.asList(RUNNING, REUPLOADING_KEY), Arrays.asList(DataEngineType.SPARK_STANDALONE,
						DataEngineType.CLOUD_SERVICE), RUNNING);
		verify(requestBuilder).newKeyReupload(refEq(userInfo), anyString(), eq(keyContent), any(List.class));
		verify(provisioningService).post("/reupload_key", TOKEN, reuploadFile, String.class);
		verifyNoMoreInteractions(userResourceService, exploratoryService, keyDAO, exploratoryDAO, computationalDAO,
				requestBuilder, provisioningService);
		verifyZeroInteractions(requestId);
	}

	@Test
	public void updateResourceDataForEdgeWhenStatusCompleted() {
		ResourceData resource = new ResourceData(ResourceType.EDGE, "someId", null, null);
		doNothing().when(keyDAO).updateEdgeStatus(anyString(), anyString());
		doNothing().when(keyDAO).updateEdgeReuploadKey(anyString(), anyBoolean(), anyVararg());
		ReuploadKeyStatusDTO dto = getReuploadKeyStatusDTO(resource, ReuploadKeyStatus.COMPLETED);

		reuploadKeyService.updateResourceData(dto);

		verify(keyDAO).updateEdgeStatus(USER, "running");
		verify(keyDAO).updateEdgeReuploadKey(USER, false, UserInstanceStatus.values());
		verifyNoMoreInteractions(keyDAO);
		verifyZeroInteractions(exploratoryDAO, computationalDAO);
	}

	@Test
	public void updateResourceDataForEdgeWhenStatusFailed() {
		ResourceData resource = new ResourceData(ResourceType.EDGE, "someId", null, null);
		doNothing().when(keyDAO).updateEdgeStatus(anyString(), anyString());

		ReuploadKeyStatusDTO dto = getReuploadKeyStatusDTO(resource, ReuploadKeyStatus.FAILED);
		reuploadKeyService.updateResourceData(dto);

		verify(keyDAO).updateEdgeStatus(USER, "running");
		verifyNoMoreInteractions(keyDAO);
		verifyZeroInteractions(exploratoryDAO, computationalDAO);
	}

	@Test
	public void updateResourceDataForExploratoryWhenStatusCompleted() {
		ResourceData resource = new ResourceData(ResourceType.EXPLORATORY, "someId", EXPLORATORY_NAME, null);
		when(exploratoryDAO.updateStatusForExploratory(anyString(), anyString(),
				any(UserInstanceStatus.class))).thenReturn(mock(UpdateResult.class));
		doNothing().when(exploratoryDAO).updateReuploadKeyForExploratory(anyString(), anyString(), anyBoolean());

		ReuploadKeyStatusDTO dto = getReuploadKeyStatusDTO(resource, ReuploadKeyStatus.COMPLETED);

		reuploadKeyService.updateResourceData(dto);

		verify(exploratoryDAO).updateStatusForExploratory(USER, EXPLORATORY_NAME, RUNNING);
		verify(exploratoryDAO).updateReuploadKeyForExploratory(USER, EXPLORATORY_NAME, false);
		verifyNoMoreInteractions(exploratoryDAO);
		verifyZeroInteractions(keyDAO, computationalDAO);
	}

	@Test
	public void updateResourceDataForExploratoryWhenStatusFailed() {
		ResourceData resource = new ResourceData(ResourceType.EXPLORATORY, "someId", EXPLORATORY_NAME, null);
		when(exploratoryDAO.updateStatusForExploratory(anyString(), anyString(),
				any(UserInstanceStatus.class))).thenReturn(mock(UpdateResult.class));

		ReuploadKeyStatusDTO dto = getReuploadKeyStatusDTO(resource, ReuploadKeyStatus.FAILED);

		reuploadKeyService.updateResourceData(dto);

		verify(exploratoryDAO).updateStatusForExploratory(USER, EXPLORATORY_NAME, RUNNING);
		verifyNoMoreInteractions(exploratoryDAO);
		verifyZeroInteractions(keyDAO, computationalDAO);
	}

	@Test
	public void updateResourceDataForClusterWhenStatusCompleted() {
		ResourceData resource = new ResourceData(ResourceType.COMPUTATIONAL, "someId", EXPLORATORY_NAME, "compName");
		doNothing().when(computationalDAO).updateStatusForComputationalResource(anyString(), anyString(), anyString(),
				any(UserInstanceStatus.class));
		doNothing().when(computationalDAO).updateReuploadKeyFlagForComputationalResource(anyString(), anyString(),
				anyString(), anyBoolean());
		ReuploadKeyStatusDTO dto = getReuploadKeyStatusDTO(resource, ReuploadKeyStatus.COMPLETED);

		reuploadKeyService.updateResourceData(dto);

		verify(computationalDAO).updateStatusForComputationalResource(USER, EXPLORATORY_NAME, "compName", RUNNING);
		verify(computationalDAO).updateReuploadKeyFlagForComputationalResource(USER, EXPLORATORY_NAME, "compName",
				false);
		verifyNoMoreInteractions(computationalDAO);
		verifyZeroInteractions(exploratoryDAO, keyDAO);
	}

	@Test
	public void updateResourceDataForClusterWhenStatusFailed() {
		ResourceData resource = new ResourceData(ResourceType.COMPUTATIONAL, "someId", EXPLORATORY_NAME, "compName");
		doNothing().when(computationalDAO).updateStatusForComputationalResource(anyString(), anyString(), anyString(),
				any(UserInstanceStatus.class));
		ReuploadKeyStatusDTO dto = getReuploadKeyStatusDTO(resource, ReuploadKeyStatus.FAILED);

		reuploadKeyService.updateResourceData(dto);

		verify(computationalDAO).updateStatusForComputationalResource(USER, EXPLORATORY_NAME, "compName", RUNNING);
		verifyNoMoreInteractions(computationalDAO);
		verifyZeroInteractions(exploratoryDAO, keyDAO);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void reuploadKeyActionForEdge() {
		doNothing().when(keyDAO).updateEdgeStatus(anyString(), anyString());
		ReuploadKeyDTO reuploadFile = mock(ReuploadKeyDTO.class);
		when(requestBuilder.newKeyReupload(any(UserInfo.class), anyString(), anyString(), any(List.class)))
				.thenReturn(reuploadFile);
		String expectedUuid = "someUuid";
		when(provisioningService.post(anyString(), anyString(), any(ReuploadKeyDTO.class), any(), any(Map.class)))
				.thenReturn(expectedUuid);
		when(requestId.put(anyString(), anyString())).thenReturn(expectedUuid);

		ResourceData resource = new ResourceData(ResourceType.EDGE, "someId", null, null);
		reuploadKeyService.reuploadKeyAction(userInfo, resource);

		verify(keyDAO).updateEdgeStatus(USER, "reuploading key");
		verify(requestBuilder).newKeyReupload(refEq(userInfo), anyString(), eq(""), any(List.class));
		verify(provisioningService).post("/reupload_key", TOKEN, reuploadFile, String.class,
				Collections.singletonMap("is_primary_reuploading", false));
		verify(requestId).put(USER, expectedUuid);
		verifyNoMoreInteractions(keyDAO, requestBuilder, provisioningService, requestId);
		verifyZeroInteractions(exploratoryDAO, computationalDAO);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void reuploadKeyActionForEdgeWithException() {
		doNothing().when(keyDAO).updateEdgeStatus(anyString(), eq("reuploading key"));
		doThrow(new DlabException("Couldn't reupload key to edge"))
				.when(requestBuilder).newKeyReupload(any(UserInfo.class), anyString(), anyString(), any(List.class));
		doNothing().when(keyDAO).updateEdgeStatus(anyString(), eq("running"));

		ResourceData resource = new ResourceData(ResourceType.EDGE, "someId", null, null);
		try {
			reuploadKeyService.reuploadKeyAction(userInfo, resource);
		} catch (DlabException e) {
			assertEquals("Couldn't reupload key to edge_node for user test:\tCouldn't reupload key to edge",
					e.getMessage());
		}

		verify(keyDAO).updateEdgeStatus(USER, "reuploading key");
		verify(requestBuilder).newKeyReupload(refEq(userInfo), anyString(), eq(""), any(List.class));
		verify(keyDAO).updateEdgeStatus(USER, "running");
		verifyNoMoreInteractions(keyDAO, requestBuilder);
		verifyZeroInteractions(exploratoryDAO, computationalDAO, provisioningService, requestId);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void reuploadKeyActionForExploratory() {
		when(exploratoryDAO.updateStatusForExploratory(anyString(), anyString(),
				any(UserInstanceStatus.class))).thenReturn(mock(UpdateResult.class));
		ReuploadKeyDTO reuploadFile = mock(ReuploadKeyDTO.class);
		when(requestBuilder.newKeyReupload(any(UserInfo.class), anyString(), anyString(), any(List.class)))
				.thenReturn(reuploadFile);
		String expectedUuid = "someUuid";
		when(provisioningService.post(anyString(), anyString(), any(ReuploadKeyDTO.class), any(), any(Map.class)))
				.thenReturn(expectedUuid);
		when(requestId.put(anyString(), anyString())).thenReturn(expectedUuid);

		ResourceData resource = new ResourceData(ResourceType.EXPLORATORY, "someId", EXPLORATORY_NAME, null);
		reuploadKeyService.reuploadKeyAction(userInfo, resource);

		verify(exploratoryDAO).updateStatusForExploratory(USER, EXPLORATORY_NAME, REUPLOADING_KEY);
		verify(requestBuilder).newKeyReupload(refEq(userInfo), anyString(), eq(""), any(List.class));
		verify(provisioningService).post("/reupload_key", TOKEN, reuploadFile, String.class,
				Collections.singletonMap("is_primary_reuploading", false));
		verify(requestId).put(USER, expectedUuid);
		verifyNoMoreInteractions(exploratoryDAO, requestBuilder, provisioningService, requestId);
		verifyZeroInteractions(keyDAO, computationalDAO);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void reuploadKeyActionForExploratoryWithException() {
		when(exploratoryDAO.updateStatusForExploratory(anyString(), anyString(),
				eq(REUPLOADING_KEY))).thenReturn(mock(UpdateResult.class));
		doThrow(new DlabException("Couldn't reupload key to exploratory"))
				.when(requestBuilder).newKeyReupload(any(UserInfo.class), anyString(), anyString(), any(List.class));
		when(exploratoryDAO.updateStatusForExploratory(anyString(), anyString(),
				eq(RUNNING))).thenReturn(mock(UpdateResult.class));

		ResourceData resource = new ResourceData(ResourceType.EXPLORATORY, "someId", EXPLORATORY_NAME, null);
		try {
			reuploadKeyService.reuploadKeyAction(userInfo, resource);
		} catch (DlabException e) {
			assertEquals("Couldn't reupload key to exploratory explName for user test:\tCouldn't reupload key to " +
					"exploratory", e.getMessage());
		}

		verify(exploratoryDAO).updateStatusForExploratory(USER, EXPLORATORY_NAME, REUPLOADING_KEY);
		verify(requestBuilder).newKeyReupload(refEq(userInfo), anyString(), eq(""), any(List.class));
		verify(exploratoryDAO).updateStatusForExploratory(USER, EXPLORATORY_NAME, RUNNING);
		verifyNoMoreInteractions(exploratoryDAO, requestBuilder);
		verifyZeroInteractions(keyDAO, computationalDAO, provisioningService, requestId);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void reuploadKeyActionForCluster() {
		doNothing().when(computationalDAO).updateStatusForComputationalResource(anyString(), anyString(), anyString(),
				any(UserInstanceStatus.class));
		ReuploadKeyDTO reuploadFile = mock(ReuploadKeyDTO.class);
		when(requestBuilder.newKeyReupload(any(UserInfo.class), anyString(), anyString(), any(List.class)))
				.thenReturn(reuploadFile);
		String expectedUuid = "someUuid";
		when(provisioningService.post(anyString(), anyString(), any(ReuploadKeyDTO.class), any(), any(Map.class)))
				.thenReturn(expectedUuid);
		when(requestId.put(anyString(), anyString())).thenReturn(expectedUuid);

		ResourceData resource = new ResourceData(ResourceType.COMPUTATIONAL, "someId", EXPLORATORY_NAME,
				"compName");
		reuploadKeyService.reuploadKeyAction(userInfo, resource);

		verify(computationalDAO).updateStatusForComputationalResource(USER, EXPLORATORY_NAME,
				"compName", REUPLOADING_KEY);
		verify(requestBuilder).newKeyReupload(refEq(userInfo), anyString(), eq(""), any(List.class));
		verify(provisioningService).post("/reupload_key", TOKEN, reuploadFile, String.class,
				Collections.singletonMap("is_primary_reuploading", false));
		verify(requestId).put(USER, expectedUuid);
		verifyNoMoreInteractions(computationalDAO, requestBuilder, provisioningService, requestId);
		verifyZeroInteractions(keyDAO, exploratoryDAO);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void reuploadKeyActionForClusterWithException() {
		doNothing().when(computationalDAO).updateStatusForComputationalResource(anyString(), anyString(), anyString(),
				eq(REUPLOADING_KEY));
		doThrow(new DlabException("Couldn't reupload key to cluster"))
				.when(requestBuilder).newKeyReupload(any(UserInfo.class), anyString(), anyString(), any(List.class));
		doNothing().when(computationalDAO).updateStatusForComputationalResource(anyString(), anyString(), anyString(),
				eq(RUNNING));

		ResourceData resource = new ResourceData(ResourceType.COMPUTATIONAL, "someId", EXPLORATORY_NAME,
				"compName");
		try {
			reuploadKeyService.reuploadKeyAction(userInfo, resource);
		} catch (DlabException e) {
			assertEquals("Couldn't reupload key to computational_resource compName affiliated with exploratory " +
					"explName for user test:\tCouldn't reupload key to cluster", e.getMessage());
		}

		verify(computationalDAO).updateStatusForComputationalResource(USER, EXPLORATORY_NAME,
				"compName", REUPLOADING_KEY);
		verify(requestBuilder).newKeyReupload(refEq(userInfo), anyString(), eq(""), any(List.class));
		verify(computationalDAO).updateStatusForComputationalResource(USER, EXPLORATORY_NAME,
				"compName", RUNNING);
		verifyNoMoreInteractions(computationalDAO, requestBuilder);
		verifyZeroInteractions(keyDAO, exploratoryDAO, provisioningService, requestId);
	}

	private UserInfo getUserInfo() {
		return new UserInfo(USER, TOKEN);
	}

	private UserInstanceDTO getUserInstance() {
		return new UserInstanceDTO().withUser(USER).withExploratoryName(EXPLORATORY_NAME);
	}

	private ReuploadKeyStatusDTO getReuploadKeyStatusDTO(ResourceData resource, ReuploadKeyStatus status) {
		return new ReuploadKeyStatusDTO().withReuploadKeyCallbackDto(
				new ReuploadKeyCallbackDTO().withResource(resource)).withReuploadKeyStatus(status).withUser(USER);
	}

}
