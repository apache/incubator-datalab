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

import com.epam.dlab.backendapi.SelfServiceApplicationConfiguration;
import com.epam.dlab.backendapi.dao.EnvDAO;
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.dao.KeyDAO;
import com.epam.dlab.backendapi.resources.dto.HealthStatusEnum;
import com.epam.dlab.backendapi.resources.dto.HealthStatusPageDTO;
import com.epam.dlab.backendapi.resources.dto.InfrastructureInfo;
import com.epam.dlab.dto.base.edge.EdgeInfo;
import com.epam.dlab.exceptions.DlabException;
import org.bson.Document;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.lang.reflect.Field;
import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class InfrastructureInfoServiceBaseTest {

	private final String USER = "test";

	@Mock
	private EnvDAO envDAO;
	@Mock
	private ExploratoryDAO expDAO;
	@Mock
	private KeyDAO keyDAO;
	@Mock
	private SelfServiceApplicationConfiguration configuration;

	@InjectMocks
	private InfrastructureInfoServiceBase infrastructureInfoServiceBase = spy(InfrastructureInfoServiceBase.class);

	@Test
	public void getUserResources() throws NoSuchFieldException, IllegalAccessException {
		Iterable<Document> documents = Collections.singletonList(new Document());
		when(expDAO.findExploratory(anyString())).thenReturn(documents);

		EdgeInfo edgeInfo = new EdgeInfo();
		edgeInfo.setInstanceId("someId");
		edgeInfo.setEdgeStatus("someStatus");
		when(keyDAO.getEdgeInfo(anyString())).thenReturn(edgeInfo);

		InfrastructureInfo expectedInfrastructureInfo = new InfrastructureInfo(Collections.emptyMap(), documents);
		InfrastructureInfo actualInfrastructureInfo = infrastructureInfoServiceBase.getUserResources(USER);
		assertNotNull(actualInfrastructureInfo);
		assertTrue(areInfrastructureInfoObjectsEqual(actualInfrastructureInfo, expectedInfrastructureInfo));

		verify(expDAO).findExploratory(USER);
		verify(keyDAO).getEdgeInfo(USER);
		verifyNoMoreInteractions(expDAO, keyDAO);
	}

	@Test
	public void getUserResourcesWhenMethodGetEdgeInfoThrowsException() {
		Iterable<Document> documents = Collections.singletonList(new Document());
		when(expDAO.findExploratory(anyString())).thenReturn(documents);

		EdgeInfo edgeInfo = new EdgeInfo();
		edgeInfo.setInstanceId("someId");
		edgeInfo.setEdgeStatus("someStatus");
		doThrow(new DlabException("Edge info not found")).when(keyDAO).getEdgeInfo(anyString());

		try {
			infrastructureInfoServiceBase.getUserResources(USER);
		} catch (DlabException e) {
			assertEquals("Could not load list of provisioned resources for user: ", e.getMessage());
		}
		verify(expDAO).findExploratory(USER);
		verify(keyDAO).getEdgeInfo(USER);
		verifyNoMoreInteractions(expDAO, keyDAO);
	}

	@Test
	public void getHeathStatus() {
		when(envDAO.getHealthStatusPageDTO(anyString(), anyBoolean())).thenReturn(new HealthStatusPageDTO()
				.withStatus(HealthStatusEnum.OK));
		when(configuration.isBillingSchedulerEnabled()).thenReturn(false);

		HealthStatusPageDTO actualHealthStatusPageDTO =
				infrastructureInfoServiceBase.getHeathStatus(USER, false, true);
		assertNotNull(actualHealthStatusPageDTO);
		assertEquals(HealthStatusEnum.OK.toString(), actualHealthStatusPageDTO.getStatus());
		assertFalse(actualHealthStatusPageDTO.isBillingEnabled());
		assertTrue(actualHealthStatusPageDTO.isAdmin());

		verify(envDAO).getHealthStatusPageDTO(USER, false);
		verify(configuration).isBillingSchedulerEnabled();
		verifyNoMoreInteractions(envDAO, configuration);
	}

	@Test
	public void getHeathStatusWhenMethodGetHealthStatusPageDTOThrowsException() {
		doThrow(new DlabException("Cannot fetch health status!"))
				.when(envDAO).getHealthStatusPageDTO(anyString(), anyBoolean());
		try {
			infrastructureInfoServiceBase.getHeathStatus(USER, false, false);
		} catch (DlabException e) {
			assertEquals("Cannot fetch health status!", e.getMessage());
		}
		verify(envDAO).getHealthStatusPageDTO(USER, false);
		verifyNoMoreInteractions(envDAO);
	}

	private boolean areInfrastructureInfoObjectsEqual(InfrastructureInfo object1, InfrastructureInfo object2) throws
			NoSuchFieldException, IllegalAccessException {
		Field shared1 = object1.getClass().getDeclaredField("shared");
		shared1.setAccessible(true);
		Field shared2 = object2.getClass().getDeclaredField("shared");
		shared2.setAccessible(true);
		Field exploratory1 = object1.getClass().getDeclaredField("exploratory");
		exploratory1.setAccessible(true);
		Field exploratory2 = object2.getClass().getDeclaredField("exploratory");
		exploratory2.setAccessible(true);
		return shared1.get(object1).equals(shared2.get(object2))
				&& exploratory1.get(object1).equals(exploratory2.get(object2));
	}
}
