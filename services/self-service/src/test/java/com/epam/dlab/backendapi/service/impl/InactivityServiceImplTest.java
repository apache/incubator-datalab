package com.epam.dlab.backendapi.service.impl;

import com.epam.dlab.auth.SystemUserInfoService;
import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.SelfServiceApplicationConfiguration;
import com.epam.dlab.backendapi.dao.ComputationalDAO;
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.service.ComputationalService;
import com.epam.dlab.backendapi.service.ExploratoryService;
import com.epam.dlab.backendapi.util.RequestBuilder;
import com.epam.dlab.dto.SchedulerJobDTO;
import com.epam.dlab.dto.UserInstanceDTO;
import com.epam.dlab.dto.UserInstanceStatus;
import com.epam.dlab.dto.computational.CheckInactivityStatus;
import com.epam.dlab.dto.computational.CheckInactivityStatusDTO;
import com.epam.dlab.dto.computational.ComputationalStatusDTO;
import com.epam.dlab.dto.computational.UserComputationalResource;
import com.epam.dlab.dto.status.EnvResource;
import com.epam.dlab.rest.client.RESTService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import static com.epam.dlab.dto.UserInstanceStatus.RUNNING;
import static java.util.Collections.singletonList;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyList;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class InactivityServiceImplTest {

	private static final long MAX_INACTIVITY = 10L;
	private static final String DOCKER_DLAB_DATAENGINE = "docker.dlab-dataengine";
	private static final String DOCKER_DLAB_DATAENGINE_SERVICE = "docker.dlab-dataengine-service";
	private final String USER = "test";
	private final String TOKEN = "token";
	private final String EXPLORATORY_NAME = "expName";
	private final String COMP_NAME = "compName";
	private final LocalDateTime LAST_ACTIVITY = LocalDateTime.now().minusMinutes(MAX_INACTIVITY);

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
	@Mock
	private ExploratoryService exploratoryService;
	@Mock
	private ComputationalService computationalService;
	@Mock
	private SystemUserInfoService systemUserInfoService;
	@InjectMocks
	private InactivityServiceImpl inactivityService;

	@Test
	@SuppressWarnings("unchecked")
	public void stopClustersByConditionForDataengines() {
		CheckInactivityStatusDTO dto = new CheckInactivityStatusDTO()
				.withStatus(CheckInactivityStatus.COMPLETED)
				.withResources(singletonList(new EnvResource().withName(COMP_NAME).withId("someId")));
		final List<UserComputationalResource> computationalResources =
				singletonList(getUserComputationalResource(RUNNING, DOCKER_DLAB_DATAENGINE));
		final UserInstanceDTO exploratory = getUserInstanceDTO(computationalResources);

		when(exploratoryDAO.getInstancesByComputationalIdsAndStatus(anyList(), any(UserInstanceStatus.class)))
				.thenReturn(singletonList(exploratory));
		when(systemUserInfoService.create(anyString())).thenReturn(getUserInfo());

		inactivityService.stopClustersByInactivity(dto.getResources().stream().map(EnvResource::getId).collect(Collectors.toList()));

		verify(exploratoryDAO).getInstancesByComputationalIdsAndStatus(singletonList("someId"), RUNNING);
		verify(systemUserInfoService).create(USER);
		verify(computationalService).stopSparkCluster(refEq(getUserInfo()), eq(EXPLORATORY_NAME), eq(COMP_NAME));
		verify(exploratoryDAO).getInstancesByComputationalIdsAndStatus(singletonList("someId"), RUNNING);
		verifyNoMoreInteractions(exploratoryService, configuration, systemUserInfoService, computationalDAO,
				exploratoryDAO, computationalService, requestBuilder, provisioningService, requestId);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void stopClustersByConditionForDataengineServices() {
		CheckInactivityStatusDTO dto = new CheckInactivityStatusDTO()
				.withStatus(CheckInactivityStatus.COMPLETED)
				.withResources(singletonList(new EnvResource().withName(COMP_NAME).withId("someId")));
		final UserComputationalResource ucResource = getUserComputationalResource(RUNNING,
				DOCKER_DLAB_DATAENGINE_SERVICE);
		final UserInstanceDTO exploratory = getUserInstanceDTO(singletonList(ucResource));
		;
		when(exploratoryDAO.getInstancesByComputationalIdsAndStatus(anyList(), any(UserInstanceStatus.class))).thenReturn(singletonList(
				exploratory.withResources(singletonList(ucResource))));
		when(systemUserInfoService.create(anyString())).thenReturn(getUserInfo());

		inactivityService.stopClustersByInactivity(dto.getResources().stream().map(EnvResource::getId).collect(Collectors.toList()));

		verify(exploratoryDAO).getInstancesByComputationalIdsAndStatus(singletonList("someId"), RUNNING);
		verify(systemUserInfoService).create(USER);
		verify(computationalService).terminateComputational(refEq(getUserInfo()), eq(EXPLORATORY_NAME), eq(COMP_NAME));
		verifyNoMoreInteractions(exploratoryService, configuration, systemUserInfoService, computationalDAO,
				exploratoryDAO, requestBuilder, provisioningService, requestId, computationalService);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void updateLastActivityForClusters() {
		CheckInactivityStatusDTO dto = new CheckInactivityStatusDTO()
				.withStatus(CheckInactivityStatus.COMPLETED)
				.withResources(singletonList(new EnvResource().withName(COMP_NAME).withId("someId").withLastActivity(LAST_ACTIVITY)));
		doNothing().when(computationalDAO).updateLastActivityDateForInstanceId(anyString(), any(LocalDateTime.class));

		inactivityService.updateLastActivityForClusters(dto.getResources());

		verify(computationalDAO).updateLastActivityDateForInstanceId("someId", LAST_ACTIVITY);
		verifyNoMoreInteractions(exploratoryService, computationalDAO);
	}


	private UserComputationalResource getUserComputationalResource(UserInstanceStatus status, String imageName) {
		UserComputationalResource ucResource = new UserComputationalResource();
		ucResource.setComputationalName(COMP_NAME);
		ucResource.setImageName("dataengine");
		ucResource.setImageName(imageName);
		ucResource.setStatus(status.toString());
		ucResource.setLastActivity(LAST_ACTIVITY);
		final SchedulerJobDTO schedulerData = new SchedulerJobDTO();
		schedulerData.setCheckInactivityRequired(true);
		schedulerData.setMaxInactivity(MAX_INACTIVITY);
		ucResource.setSchedulerData(schedulerData);
		return ucResource;
	}

	private ComputationalStatusDTO getComputationalStatusDTOWithStatus(String status) {
		return new ComputationalStatusDTO()
				.withUser(USER)
				.withExploratoryName(EXPLORATORY_NAME)
				.withComputationalName(COMP_NAME)
				.withStatus(UserInstanceStatus.of(status));
	}

	private UserInfo getUserInfo() {
		return new UserInfo(USER, TOKEN);
	}

	private UserInstanceDTO getUserInstanceDTO(List<UserComputationalResource> computationalResources) {
		final UserInstanceDTO exploratory = new UserInstanceDTO();
		exploratory.setExploratoryId("someId");
		exploratory.setExploratoryName(EXPLORATORY_NAME);
		exploratory.setUser(USER);
		exploratory.setResources(computationalResources);
		return exploratory;
	}
}