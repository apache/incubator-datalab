package com.epam.dlab.backendapi.service.impl;

import com.epam.dlab.backendapi.SelfServiceApplicationConfiguration;
import com.epam.dlab.backendapi.dao.EnvStatusDAO;
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.dao.KeyDAO;
import com.epam.dlab.backendapi.resources.dto.HealthStatusEnum;
import com.epam.dlab.backendapi.resources.dto.HealthStatusPageDTO;
import com.epam.dlab.backendapi.resources.dto.InfrastructureInfo;
import com.epam.dlab.dto.base.edge.EdgeInfo;
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
	private EnvStatusDAO envDAO;
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
		Document document = new Document();
		Iterable<Document> documents = Collections.singletonList(document);
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
		verifyNoMoreInteractions(expDAO);

		verify(keyDAO).getEdgeInfo(USER);
		verifyNoMoreInteractions(keyDAO);
	}

	@Test
	public void getHeathStatus() {
		HealthStatusPageDTO expectedHealthStatusPageDTO = new HealthStatusPageDTO().withStatus(HealthStatusEnum.OK);
		when(envDAO.getHealthStatusPageDTO(anyString(), anyBoolean())).thenReturn(expectedHealthStatusPageDTO);
		when(configuration.isBillingSchedulerEnabled()).thenReturn(false);
		expectedHealthStatusPageDTO.withBillingEnabled(false).withBackupAllowed(false);

		HealthStatusPageDTO actualHealthStatusPageDTO =
				infrastructureInfoServiceBase.getHeathStatus(USER, false, false);
		assertNotNull(actualHealthStatusPageDTO);
		assertEquals(expectedHealthStatusPageDTO, actualHealthStatusPageDTO);

		verify(envDAO).getHealthStatusPageDTO(USER, false);
		verifyNoMoreInteractions(envDAO);

		verify(configuration).isBillingSchedulerEnabled();
		verifyNoMoreInteractions(configuration);
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
