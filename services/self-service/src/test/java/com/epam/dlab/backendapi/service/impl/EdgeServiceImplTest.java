package com.epam.dlab.backendapi.service.impl;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.KeyDAO;
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.util.RequestBuilder;
import com.epam.dlab.dto.ResourceSysBaseDTO;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.client.RESTService;
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
public class EdgeServiceImplTest {

	private final String USER = "test";
	private final String TOKEN = "token";
	private final String UUID = "1234-56789765-4321";
	private final String STATUS_STOPPED = "stopped";
	private final String STATUS_RUNNING = "running";
	private UserInfo userInfo;

	@Mock
	private KeyDAO keyDAO;
	@Mock
	private RESTService provisioningService;
	@Mock
	private RequestBuilder requestBuilder;
	@Mock
	private RequestId requestId;

	@InjectMocks
	private EdgeServiceImpl edgeService;

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Before
	public void setUp() {
		userInfo = getUserInfo();
	}

	@Test
	public void start() {
		when(keyDAO.getEdgeStatus(anyString())).thenReturn(STATUS_STOPPED);
		ResourceSysBaseDTO rsbDto = new ResourceSysBaseDTO();
		when(requestBuilder.newEdgeAction(any(UserInfo.class))).thenReturn(rsbDto);
		String edgeStart = "infrastructure/edge/start";
		when(provisioningService.post(anyString(), anyString(), any(ResourceSysBaseDTO.class), any())).thenReturn
				(UUID);
		when(requestId.put(anyString(), anyString())).thenReturn(UUID);

		String uuid = edgeService.start(userInfo);
		assertNotNull(uuid);
		assertEquals(UUID, uuid);

		verify(keyDAO).getEdgeStatus(USER);

		verify(requestBuilder).newEdgeAction(userInfo);
		verifyNoMoreInteractions(requestBuilder);

		verify(provisioningService).post(edgeStart, TOKEN, rsbDto, String.class);
		verifyNoMoreInteractions(provisioningService);

		verify(requestId).put(USER, UUID);
		verifyNoMoreInteractions(requestId);
	}

	@Test
	public void startWithException() {
		when(keyDAO.getEdgeStatus(anyString())).thenReturn(STATUS_RUNNING);
		expectedException.expect(DlabException.class);
		expectedException.expectMessage("Could not start EDGE node because the status of instance is running");

		edgeService.start(userInfo);
	}

	@Test
	public void stop() {
		when(keyDAO.getEdgeStatus(anyString())).thenReturn(STATUS_RUNNING);
		ResourceSysBaseDTO rsbDto = new ResourceSysBaseDTO();
		when(requestBuilder.newEdgeAction(any(UserInfo.class))).thenReturn(rsbDto);
		String edgeStop = "infrastructure/edge/stop";
		when(provisioningService.post(anyString(), anyString(), any(ResourceSysBaseDTO.class), any())).thenReturn
				(UUID);
		when(requestId.put(anyString(), anyString())).thenReturn(UUID);

		String uuid = edgeService.stop(userInfo);
		assertNotNull(uuid);
		assertEquals(UUID, uuid);

		verify(keyDAO).getEdgeStatus(USER);

		verify(requestBuilder).newEdgeAction(userInfo);
		verifyNoMoreInteractions(requestBuilder);

		verify(provisioningService).post(edgeStop, TOKEN, rsbDto, String.class);
		verifyNoMoreInteractions(provisioningService);

		verify(requestId).put(USER, UUID);
		verifyNoMoreInteractions(requestId);
	}

	@Test
	public void stopWithException() {
		when(keyDAO.getEdgeStatus(anyString())).thenReturn(STATUS_STOPPED);
		expectedException.expect(DlabException.class);
		expectedException.expectMessage("Could not stop EDGE node because the status of instance is stopped");

		edgeService.stop(userInfo);
	}

	@Test
	public void terminate() {
		when(keyDAO.getEdgeStatus(anyString())).thenReturn(STATUS_RUNNING);
		ResourceSysBaseDTO rsbDto = new ResourceSysBaseDTO();
		when(requestBuilder.newEdgeAction(any(UserInfo.class))).thenReturn(rsbDto);
		String edgeTerminate = "infrastructure/edge/terminate";
		when(provisioningService.post(anyString(), anyString(), any(ResourceSysBaseDTO.class), any())).thenReturn
				(UUID);
		when(requestId.put(anyString(), anyString())).thenReturn(UUID);

		String uuid = edgeService.terminate(userInfo);
		assertNotNull(uuid);
		assertEquals(UUID, uuid);

		verify(keyDAO).getEdgeStatus(USER);

		verify(requestBuilder).newEdgeAction(userInfo);
		verifyNoMoreInteractions(requestBuilder);

		verify(provisioningService).post(edgeTerminate, TOKEN, rsbDto, String.class);
		verifyNoMoreInteractions(provisioningService);

		verify(requestId).put(USER, UUID);
		verifyNoMoreInteractions(requestId);
	}

	@Test
	public void terminateWithException() {
		when(keyDAO.getEdgeStatus(anyString())).thenReturn(anyString());
		expectedException.expect(DlabException.class);
		expectedException.expectMessage("Could not terminate EDGE node because the status of instance is null");

		edgeService.terminate(userInfo);
	}

	private UserInfo getUserInfo() {
		return new UserInfo(USER, TOKEN);
	}
}
