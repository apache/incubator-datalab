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
import com.epam.dlab.backendapi.dao.KeyDAO;
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.util.RequestBuilder;
import com.epam.dlab.dto.ResourceSysBaseDTO;
import com.epam.dlab.dto.UserInstanceStatus;
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
		doNothing().when(keyDAO).updateEdgeStatus(anyString(), anyString());
		ResourceSysBaseDTO rsbDto = new ResourceSysBaseDTO();
		when(requestBuilder.newEdgeAction(any(UserInfo.class))).thenReturn(rsbDto);
		String edgeStart = "infrastructure/edge/start";
		when(provisioningService.post(anyString(), anyString(), any(ResourceSysBaseDTO.class), any()))
				.thenReturn(UUID);
		when(requestId.put(anyString(), anyString())).thenReturn(UUID);

		String uuid = edgeService.start(userInfo);
		assertNotNull(uuid);
		assertEquals(UUID, uuid);

		verify(keyDAO).getEdgeStatus(USER);
		verify(keyDAO).updateEdgeStatus(USER, "starting");
		verify(requestBuilder).newEdgeAction(userInfo);
		verify(provisioningService).post(edgeStart, TOKEN, rsbDto, String.class);
		verify(requestId).put(USER, UUID);
		verifyNoMoreInteractions(keyDAO, requestBuilder, provisioningService, requestId);
	}

	@Test
	public void startWithInappropriateEdgeStatus() {
		when(keyDAO.getEdgeStatus(anyString())).thenReturn(STATUS_RUNNING);
		expectedException.expect(DlabException.class);
		expectedException.expectMessage("Could not start EDGE node because the status of instance is running");

		edgeService.start(userInfo);
	}

	@Test
	public void startWhenMethodNewEdgeActionThrowsException() {
		when(keyDAO.getEdgeStatus(anyString())).thenReturn(STATUS_STOPPED);
		doNothing().when(keyDAO).updateEdgeStatus(anyString(), anyString());

		doThrow(new DlabException("Cannot create instance of resource class "))
				.when(requestBuilder).newEdgeAction(any(UserInfo.class));
		try {
			edgeService.start(userInfo);
		} catch (DlabException e) {
			assertEquals("Could not start EDGE node: Could not infrastructure/edge/start EDGE node : " +
					"Cannot create instance of resource class ", e.getMessage());
		}
		verify(keyDAO).getEdgeStatus(USER);
		verify(keyDAO).updateEdgeStatus(USER, "starting");
		verify(keyDAO).updateEdgeStatus(USER, "failed");
		verify(requestBuilder).newEdgeAction(userInfo);
		verifyNoMoreInteractions(keyDAO, requestBuilder);
	}

	@Test
	public void stop() {
		when(keyDAO.getEdgeStatus(anyString())).thenReturn(STATUS_RUNNING);
		doNothing().when(keyDAO).updateEdgeStatus(anyString(), anyString());
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
		verify(keyDAO).updateEdgeStatus(USER, "stopping");
		verify(requestBuilder).newEdgeAction(userInfo);
		verify(provisioningService).post(edgeStop, TOKEN, rsbDto, String.class);
		verify(requestId).put(USER, UUID);
		verifyNoMoreInteractions(keyDAO, requestBuilder, provisioningService, requestId);
	}

	@Test
	public void stopWithInappropriateEdgeStatus() {
		when(keyDAO.getEdgeStatus(anyString())).thenReturn(STATUS_STOPPED);
		expectedException.expect(DlabException.class);
		expectedException.expectMessage("Could not stop EDGE node because the status of instance is stopped");

		edgeService.stop(userInfo);
	}

	@Test
	public void stopWhenMethodNewEdgeActionThrowsException() {
		when(keyDAO.getEdgeStatus(anyString())).thenReturn(STATUS_RUNNING);
		doNothing().when(keyDAO).updateEdgeStatus(anyString(), anyString());

		doThrow(new DlabException("Cannot create instance of resource class "))
				.when(requestBuilder).newEdgeAction(any(UserInfo.class));
		try {
			edgeService.stop(userInfo);
		} catch (DlabException e) {
			assertEquals("Could not stop EDGE node: Could not infrastructure/edge/stop EDGE node : " +
					"Cannot create instance of resource class ", e.getMessage());
		}
		verify(keyDAO).getEdgeStatus(USER);
		verify(keyDAO).updateEdgeStatus(USER, "stopping");
		verify(keyDAO).updateEdgeStatus(USER, "failed");
		verify(requestBuilder).newEdgeAction(userInfo);
		verifyNoMoreInteractions(keyDAO, requestBuilder);
	}

	@Test
	public void terminate() {
		when(keyDAO.getEdgeStatus(anyString())).thenReturn(STATUS_RUNNING);
		doNothing().when(keyDAO).updateEdgeStatus(anyString(), anyString());
		ResourceSysBaseDTO rsbDto = new ResourceSysBaseDTO();
		when(requestBuilder.newEdgeAction(any(UserInfo.class))).thenReturn(rsbDto);
		String edgeTerminate = "infrastructure/edge/terminate";
		when(provisioningService.post(anyString(), anyString(), any(ResourceSysBaseDTO.class), any()))
				.thenReturn(UUID);
		when(requestId.put(anyString(), anyString())).thenReturn(UUID);

		String uuid = edgeService.terminate(userInfo);
		assertNotNull(uuid);
		assertEquals(UUID, uuid);

		verify(keyDAO).getEdgeStatus(USER);
		verify(keyDAO).updateEdgeStatus(USER, "terminating");
		verify(requestBuilder).newEdgeAction(userInfo);
		verify(provisioningService).post(edgeTerminate, TOKEN, rsbDto, String.class);
		verify(requestId).put(USER, UUID);
		verifyNoMoreInteractions(keyDAO, requestBuilder, provisioningService, requestId);
	}

	@Test
	public void terminateWithInappropriateEdgeStatus() {
		when(keyDAO.getEdgeStatus(anyString())).thenReturn(anyString());
		expectedException.expect(DlabException.class);
		expectedException.expectMessage("Could not terminate EDGE node because the status of instance is null");

		edgeService.terminate(userInfo);
	}

	@Test
	public void terminateWhenMethodNewEdgeActionThrowsException() {
		when(keyDAO.getEdgeStatus(anyString())).thenReturn(STATUS_RUNNING);
		doNothing().when(keyDAO).updateEdgeStatus(anyString(), anyString());

		doThrow(new DlabException("Cannot create instance of resource class "))
				.when(requestBuilder).newEdgeAction(any(UserInfo.class));
		try {
			edgeService.terminate(userInfo);
		} catch (DlabException e) {
			assertEquals("Could not terminate EDGE node: Could not infrastructure/edge/terminate EDGE node : " +
					"Cannot create instance of resource class ", e.getMessage());
		}
		verify(keyDAO).getEdgeStatus(USER);
		verify(keyDAO).updateEdgeStatus(USER, "terminating");
		verify(keyDAO).updateEdgeStatus(USER, "failed");
		verify(requestBuilder).newEdgeAction(userInfo);
		verifyNoMoreInteractions(keyDAO, requestBuilder);
	}

	@Test
	public void updateReuploadKeyFlag() {
		doNothing().when(keyDAO).updateEdgeReuploadKey(anyString(), anyBoolean(), anyVararg());
		edgeService.updateReuploadKeyFlag(USER, true, UserInstanceStatus.RUNNING);

		verify(keyDAO).updateEdgeReuploadKey(USER, true, UserInstanceStatus.RUNNING);
		verifyNoMoreInteractions(keyDAO);
	}

	private UserInfo getUserInfo() {
		return new UserInfo(USER, TOKEN);
	}
}
