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

import com.epam.dlab.auth.SystemUserInfoServiceImpl;
import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.ComputationalDAO;
import com.epam.dlab.backendapi.dao.EnvDAO;
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.dao.SchedulerJobDAO;
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.dto.SchedulerJobDTO;
import com.epam.dlab.dto.UserInstanceDTO;
import com.epam.dlab.dto.base.DataEngineType;
import com.epam.dlab.dto.computational.UserComputationalResource;
import com.epam.dlab.dto.status.EnvResource;
import com.epam.dlab.exceptions.ResourceInappropriateStateException;
import com.epam.dlab.exceptions.ResourceNotFoundException;
import com.epam.dlab.rest.client.RESTService;
import com.mongodb.client.result.UpdateResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.epam.dlab.dto.UserInstanceStatus.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SchedulerJobServiceImplTest {

	private final String USER = "test";
	private final String EXPLORATORY_NAME = "explName";
	private final String COMPUTATIONAL_NAME = "compName";

	private UserInfo userInfo;
	private SchedulerJobDTO schedulerJobDTO;
	private UserInstanceDTO userInstance;

	@Mock
	private SchedulerJobDAO schedulerJobDAO;
	@Mock
	private ExploratoryDAO exploratoryDAO;
	@Mock
	private ComputationalDAO computationalDAO;
	@Mock
	private SystemUserInfoServiceImpl systemUserService;
	@Mock
	private ExploratoryServiceImpl exploratoryService;
	@Mock
	private ComputationalServiceImpl computationalService;
	@Mock
	private EnvDAO envDAO;
	@Mock
	private RESTService provisioningService;
	@Mock
	private RequestId requestId;

	@InjectMocks
	private SchedulerJobServiceImpl schedulerJobService;


	@Before
	public void setUp() {
		userInfo = getUserInfo();
		schedulerJobDTO = getSchedulerJobDTO();
		userInstance = getUserInstanceDTO();
	}

	@Test
	public void fetchSchedulerJobForUserAndExploratory() {
		when(schedulerJobDAO.fetchSingleSchedulerJobByUserAndExploratory(anyString(), anyString()))
				.thenReturn(Optional.of(schedulerJobDTO));

		SchedulerJobDTO actualSchedulerJobDto =
				schedulerJobService.fetchSchedulerJobForUserAndExploratory(USER, EXPLORATORY_NAME);
		assertNotNull(actualSchedulerJobDto);
		assertEquals(schedulerJobDTO, actualSchedulerJobDto);

		verify(schedulerJobDAO).fetchSingleSchedulerJobByUserAndExploratory(USER, EXPLORATORY_NAME);
		verifyNoMoreInteractions(exploratoryDAO, schedulerJobDAO);
	}

	@Test
	public void fetchSchedulerJobForUserAndExploratoryWhenNotebookNotExist() {
		when(schedulerJobDAO.fetchSingleSchedulerJobByUserAndExploratory(anyString(), anyString())).thenReturn(Optional.empty());
		try {
			schedulerJobService.fetchSchedulerJobForUserAndExploratory(USER, EXPLORATORY_NAME);
		} catch (ResourceNotFoundException e) {
			assertEquals("Scheduler job data not found for user test with exploratory explName", e.getMessage());
		}
		verify(schedulerJobDAO).fetchSingleSchedulerJobByUserAndExploratory(USER, EXPLORATORY_NAME);
		verifyNoMoreInteractions(schedulerJobDAO);
	}

	@Test
	public void fetchEmptySchedulerJobForUserAndExploratory() {
		when(exploratoryDAO.isExploratoryExist(anyString(), anyString())).thenReturn(true);
		when(schedulerJobDAO.fetchSingleSchedulerJobByUserAndExploratory(anyString(), anyString()))
				.thenReturn(Optional.empty());
		try {
			schedulerJobService.fetchSchedulerJobForUserAndExploratory(USER, EXPLORATORY_NAME);
		} catch (ResourceNotFoundException e) {
			assertEquals("Scheduler job data not found for user test with exploratory explName", e.getMessage());
		}
		verify(schedulerJobDAO).fetchSingleSchedulerJobByUserAndExploratory(USER, EXPLORATORY_NAME);
		verifyNoMoreInteractions(exploratoryDAO, schedulerJobDAO);
	}

	@Test
	public void fetchSchedulerJobForComputationalResource() {
		when(schedulerJobDAO.fetchSingleSchedulerJobForCluster(anyString(), anyString(), anyString()))
				.thenReturn(Optional.of(schedulerJobDTO));

		SchedulerJobDTO actualSchedulerJobDto = schedulerJobService
				.fetchSchedulerJobForComputationalResource(USER, EXPLORATORY_NAME, COMPUTATIONAL_NAME);
		assertNotNull(actualSchedulerJobDto);
		assertEquals(schedulerJobDTO, actualSchedulerJobDto);

		verify(schedulerJobDAO).fetchSingleSchedulerJobForCluster(USER, EXPLORATORY_NAME, COMPUTATIONAL_NAME);
		verifyNoMoreInteractions(computationalDAO, schedulerJobDAO);
	}

	@Test
	public void fetchEmptySchedulerJobForComputationalResource() {
		when(schedulerJobDAO.fetchSingleSchedulerJobForCluster(anyString(), anyString(), anyString()))
				.thenReturn(Optional.empty());
		try {
			schedulerJobService.fetchSchedulerJobForComputationalResource(USER, EXPLORATORY_NAME, COMPUTATIONAL_NAME);
		} catch (ResourceNotFoundException e) {
			assertEquals("Scheduler job data not found for user test with exploratory explName with " +
					"computational resource compName", e.getMessage());
		}
		verify(schedulerJobDAO).fetchSingleSchedulerJobForCluster(USER, EXPLORATORY_NAME, COMPUTATIONAL_NAME);
		verifyNoMoreInteractions(computationalDAO, schedulerJobDAO);
	}

	@Test
	public void updateSchedulerDataForUserAndExploratory() {
		userInstance.withStatus("running");
		when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString())).thenReturn(userInstance);
		when(exploratoryDAO.updateSchedulerDataForUserAndExploratory(anyString(), anyString(),
				any(SchedulerJobDTO.class))).thenReturn(mock(UpdateResult.class));

		schedulerJobService.updateExploratorySchedulerData(USER, EXPLORATORY_NAME, schedulerJobDTO);

		verify(exploratoryDAO).fetchExploratoryFields(USER, EXPLORATORY_NAME);
		verify(exploratoryDAO).updateSchedulerDataForUserAndExploratory(USER, EXPLORATORY_NAME, schedulerJobDTO);
		verify(computationalDAO).updateSchedulerSyncFlag(USER, EXPLORATORY_NAME, false);
		verifyNoMoreInteractions(exploratoryDAO);
		verifyZeroInteractions(computationalDAO);
	}

	@Test
	public void updateSchedulerDataForUserAndExploratoryWhenMethodFetchExploratoryFieldsThrowsException() {
		doThrow(new ResourceNotFoundException("Exploratory for user with name not found"))
				.when(exploratoryDAO).fetchExploratoryFields(anyString(), anyString());
		try {
			schedulerJobService.updateExploratorySchedulerData(USER, EXPLORATORY_NAME, schedulerJobDTO);
		} catch (ResourceNotFoundException e) {
			assertEquals("Exploratory for user with name not found", e.getMessage());
		}
		verify(exploratoryDAO).fetchExploratoryFields(USER, EXPLORATORY_NAME);
		verifyNoMoreInteractions(exploratoryDAO);
		verifyZeroInteractions(computationalDAO);
	}

	@Test
	public void updateSchedulerDataForUserAndExploratoryWithInapproprietaryStatus() {
		userInstance.withStatus("terminated");
		when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString())).thenReturn(userInstance);
		try {
			schedulerJobService.updateExploratorySchedulerData(USER, EXPLORATORY_NAME, schedulerJobDTO);
		} catch (ResourceInappropriateStateException e) {
			assertEquals("Can not create/update scheduler for user instance with status: terminated",
					e.getMessage());
		}
		verify(exploratoryDAO).fetchExploratoryFields(USER, EXPLORATORY_NAME);
		verifyNoMoreInteractions(exploratoryDAO);
		verifyZeroInteractions(computationalDAO);
	}

	@Test
	public void updateSchedulerDataForUserAndExploratoryWithEnrichingSchedulerJob() {
		schedulerJobDTO.setBeginDate(null);
		schedulerJobDTO.setTimeZoneOffset(null);
		userInstance.withStatus("running");
		when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString())).thenReturn(userInstance);
		when(exploratoryDAO.updateSchedulerDataForUserAndExploratory(anyString(), anyString(),
				any(SchedulerJobDTO.class))).thenReturn(mock(UpdateResult.class));

		assertNull(schedulerJobDTO.getBeginDate());
		assertNull(schedulerJobDTO.getTimeZoneOffset());

		schedulerJobService.updateExploratorySchedulerData(USER, EXPLORATORY_NAME, schedulerJobDTO);

		assertEquals(LocalDate.now(), schedulerJobDTO.getBeginDate());
		assertEquals(OffsetDateTime.now(ZoneId.systemDefault()).getOffset(), schedulerJobDTO.getTimeZoneOffset());

		verify(exploratoryDAO).fetchExploratoryFields(USER, EXPLORATORY_NAME);
		verify(exploratoryDAO).updateSchedulerDataForUserAndExploratory(USER, EXPLORATORY_NAME, schedulerJobDTO);
		verify(computationalDAO).updateSchedulerSyncFlag(USER, EXPLORATORY_NAME, false);
		verifyNoMoreInteractions(exploratoryDAO);
		verifyZeroInteractions(computationalDAO);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void updateSchedulerDataForUserAndExploratoryWithSyncStartRequiredParam() {
		userInstance.withStatus("running");
		schedulerJobDTO.setSyncStartRequired(true);
		when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString())).thenReturn(userInstance);
		when(exploratoryDAO.updateSchedulerDataForUserAndExploratory(anyString(), anyString(),
				any(SchedulerJobDTO.class))).thenReturn(mock(UpdateResult.class));
		when(computationalDAO.getComputationalResourcesWhereStatusIn(anyString(), any(List.class),
				anyString(), anyVararg())).thenReturn(Collections.singletonList(COMPUTATIONAL_NAME));
		when(computationalDAO.updateSchedulerDataForComputationalResource(anyString(), anyString(), anyString(),
				any(SchedulerJobDTO.class))).thenReturn(mock(UpdateResult.class));

		schedulerJobService.updateExploratorySchedulerData(USER, EXPLORATORY_NAME, schedulerJobDTO);

		verify(exploratoryDAO).fetchExploratoryFields(USER, EXPLORATORY_NAME);
		verify(exploratoryDAO).updateSchedulerDataForUserAndExploratory(USER, EXPLORATORY_NAME, schedulerJobDTO);
		verify(computationalDAO).getComputationalResourcesWhereStatusIn(USER,
				Collections.singletonList(DataEngineType.SPARK_STANDALONE),
				EXPLORATORY_NAME, STARTING, RUNNING, STOPPING, STOPPED);
		schedulerJobDTO.setEndTime(null);
		schedulerJobDTO.setStopDaysRepeat(Collections.emptyList());
		verify(computationalDAO).updateSchedulerDataForComputationalResource(USER, EXPLORATORY_NAME,
				COMPUTATIONAL_NAME, schedulerJobDTO);
		verifyNoMoreInteractions(exploratoryDAO, computationalDAO);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void updateSchedulerDataForUserAndExploratoryWithSyncStartRequiredParamButAbsenceClusters() {
		userInstance.withStatus("running");
		schedulerJobDTO.setSyncStartRequired(true);
		when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString())).thenReturn(userInstance);
		when(exploratoryDAO.updateSchedulerDataForUserAndExploratory(anyString(), anyString(),
				any(SchedulerJobDTO.class))).thenReturn(mock(UpdateResult.class));
		when(computationalDAO.getComputationalResourcesWhereStatusIn(anyString(), any(List.class),
				anyString(), anyVararg())).thenReturn(Collections.emptyList());

		schedulerJobService.updateExploratorySchedulerData(USER, EXPLORATORY_NAME, schedulerJobDTO);

		verify(exploratoryDAO).fetchExploratoryFields(USER, EXPLORATORY_NAME);
		verify(exploratoryDAO).updateSchedulerDataForUserAndExploratory(USER, EXPLORATORY_NAME, schedulerJobDTO);
		verify(computationalDAO).getComputationalResourcesWhereStatusIn(USER,
				Collections.singletonList(DataEngineType.SPARK_STANDALONE),
				EXPLORATORY_NAME, STARTING, RUNNING, STOPPING, STOPPED);
		verifyNoMoreInteractions(exploratoryDAO, computationalDAO);
	}


	@Test
	public void updateSchedulerDataForComputationalResource() {
		userInstance.withStatus("running");
		when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString())).thenReturn(userInstance);
		when(computationalDAO.fetchComputationalFields(anyString(), anyString(), anyString()))
				.thenReturn(userInstance.getResources().get(0));
		when(computationalDAO.updateSchedulerDataForComputationalResource(anyString(), anyString(), anyString(),
				any(SchedulerJobDTO.class))).thenReturn(mock(UpdateResult.class));

		schedulerJobService.updateComputationalSchedulerData(USER, EXPLORATORY_NAME, COMPUTATIONAL_NAME,
				schedulerJobDTO);

		verify(exploratoryDAO).fetchExploratoryFields(USER, EXPLORATORY_NAME);
		verify(computationalDAO).fetchComputationalFields(USER, EXPLORATORY_NAME, COMPUTATIONAL_NAME);
		verify(computationalDAO).updateSchedulerDataForComputationalResource(USER, EXPLORATORY_NAME,
				COMPUTATIONAL_NAME, schedulerJobDTO);
		verifyNoMoreInteractions(exploratoryDAO, computationalDAO);
	}

	@Test
	public void updateSchedulerDataForComputationalResourceWhenSchedulerIsNull() {
		userInstance.withStatus("running");
		when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString())).thenReturn(userInstance);
		when(computationalDAO.fetchComputationalFields(anyString(), anyString(), anyString()))
				.thenReturn(userInstance.getResources().get(0));
		when(computationalDAO.updateSchedulerDataForComputationalResource(anyString(), anyString(), anyString(),
				any(SchedulerJobDTO.class))).thenReturn(mock(UpdateResult.class));

		final SchedulerJobDTO schedulerJobDTO = getSchedulerJobDTO();
		schedulerJobDTO.setStartDaysRepeat(null);
		schedulerJobDTO.setStopDaysRepeat(null);
		schedulerJobService.updateComputationalSchedulerData(USER, EXPLORATORY_NAME, COMPUTATIONAL_NAME,
				schedulerJobDTO);

		verify(exploratoryDAO).fetchExploratoryFields(USER, EXPLORATORY_NAME);
		verify(computationalDAO).fetchComputationalFields(USER, EXPLORATORY_NAME, COMPUTATIONAL_NAME);
		verify(computationalDAO).updateSchedulerDataForComputationalResource(eq(USER), eq(EXPLORATORY_NAME),
				eq(COMPUTATIONAL_NAME), refEq(schedulerJobDTO));
		verifyNoMoreInteractions(exploratoryDAO, computationalDAO);
	}

	@Test
	public void updateSchedulerDataForComputationalResourceWhenMethodFetchComputationalFieldsThrowsException() {
		userInstance.withStatus("running");
		when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString())).thenReturn(userInstance);
		doThrow(new ResourceNotFoundException("Computational resource for user with name not found"))
				.when(computationalDAO).fetchComputationalFields(anyString(), anyString(), anyString());
		try {
			schedulerJobService.updateComputationalSchedulerData(USER, EXPLORATORY_NAME,
					COMPUTATIONAL_NAME, schedulerJobDTO);
		} catch (ResourceNotFoundException e) {
			assertEquals("Computational resource for user with name not found", e.getMessage());
		}

		verify(exploratoryDAO).fetchExploratoryFields(USER, EXPLORATORY_NAME);
		verify(computationalDAO).fetchComputationalFields(USER, EXPLORATORY_NAME, COMPUTATIONAL_NAME);
		verifyNoMoreInteractions(exploratoryDAO, computationalDAO);
	}

	@Test
	public void updateSchedulerDataForComputationalResourceWithInapproprietaryClusterStatus() {
		userInstance.setStatus("running");
		userInstance.getResources().get(0).setStatus("terminated");
		when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString())).thenReturn(userInstance);
		when(computationalDAO.fetchComputationalFields(anyString(), anyString(), anyString()))
				.thenReturn(userInstance.getResources().get(0));
		try {
			schedulerJobService.updateComputationalSchedulerData(USER, EXPLORATORY_NAME,
					COMPUTATIONAL_NAME, schedulerJobDTO);
		} catch (ResourceInappropriateStateException e) {
			assertEquals("Can not create/update scheduler for user instance with status: terminated",
					e.getMessage());
		}
		verify(exploratoryDAO).fetchExploratoryFields(USER, EXPLORATORY_NAME);
		verify(computationalDAO).fetchComputationalFields(USER, EXPLORATORY_NAME, COMPUTATIONAL_NAME);
		verifyNoMoreInteractions(exploratoryDAO, computationalDAO);
	}

	@Test
	public void updateSchedulerDataForComputationalResourceWithEnrichingSchedulerJob() {
		schedulerJobDTO.setBeginDate(null);
		schedulerJobDTO.setTimeZoneOffset(null);
		userInstance.withStatus("running");
		when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString())).thenReturn(userInstance);
		when(computationalDAO.fetchComputationalFields(anyString(), anyString(), anyString()))
				.thenReturn(userInstance.getResources().get(0));
		when(computationalDAO.updateSchedulerDataForComputationalResource(anyString(), anyString(), anyString(),
				any(SchedulerJobDTO.class))).thenReturn(mock(UpdateResult.class));

		assertNull(schedulerJobDTO.getBeginDate());
		assertNull(schedulerJobDTO.getTimeZoneOffset());

		schedulerJobService.updateComputationalSchedulerData(USER, EXPLORATORY_NAME, COMPUTATIONAL_NAME,
				schedulerJobDTO);

		assertEquals(LocalDate.now(), schedulerJobDTO.getBeginDate());
		assertEquals(OffsetDateTime.now(ZoneId.systemDefault()).getOffset(), schedulerJobDTO.getTimeZoneOffset());

		verify(exploratoryDAO).fetchExploratoryFields(USER, EXPLORATORY_NAME);
		verify(computationalDAO).fetchComputationalFields(USER, EXPLORATORY_NAME, COMPUTATIONAL_NAME);
		verify(computationalDAO).updateSchedulerDataForComputationalResource(USER, EXPLORATORY_NAME,
				COMPUTATIONAL_NAME, schedulerJobDTO);
		verifyNoMoreInteractions(exploratoryDAO, computationalDAO);
	}

	@Test
	public void executeCheckClusterInactivityJob() {
		EnvResource resource = new EnvResource();
		when(envDAO.findRunningClustersForCheckInactivity()).thenReturn(Collections.singletonList(resource));
		when(provisioningService.post(anyString(), anyString(), anyListOf(EnvResource.class), any()))
				.thenReturn("someUuid");
		when(requestId.put(anyString(), anyString())).thenReturn("someUuid");

		String expected = "someUuid";
		String actual = schedulerJobService.executeCheckClusterInactivityJob(userInfo);
		assertEquals(expected, actual);

		verify(envDAO).findRunningClustersForCheckInactivity();
		verify(provisioningService).post("/infrastructure/check_inactivity", "token",
				Collections.singletonList(resource), String.class);
		verify(requestId).put(USER, "someUuid");
		verifyNoMoreInteractions(envDAO, provisioningService, requestId);
	}

	@Test
	public void executeCheckClusterInactivityJobWithoutRunningClusters() {
		when(envDAO.findRunningClustersForCheckInactivity()).thenReturn(Collections.emptyList());

		String expected = "";
		String actual = schedulerJobService.executeCheckClusterInactivityJob(userInfo);
		assertEquals(expected, actual);

		verify(envDAO).findRunningClustersForCheckInactivity();
		verifyNoMoreInteractions(envDAO);
		verifyZeroInteractions(provisioningService, requestId);
	}

	private UserInfo getUserInfo() {
		return new UserInfo(USER, "token");
	}

	private SchedulerJobDTO getSchedulerJobDTO() {
		SchedulerJobDTO schedulerJobDTO = new SchedulerJobDTO();
		schedulerJobDTO.setTimeZoneOffset(OffsetDateTime.now(ZoneId.systemDefault()).getOffset());
		schedulerJobDTO.setBeginDate(LocalDate.now());
		schedulerJobDTO.setFinishDate(LocalDate.now().plusDays(1));
		schedulerJobDTO.setStartTime(LocalTime.now().truncatedTo(ChronoUnit.MINUTES));
		schedulerJobDTO.setEndTime(LocalTime.now().truncatedTo(ChronoUnit.MINUTES));
		schedulerJobDTO.setTerminateDateTime(
				LocalDateTime.of(LocalDate.now(), LocalTime.now().truncatedTo(ChronoUnit.MINUTES)));
		schedulerJobDTO.setStartDaysRepeat(Arrays.asList(DayOfWeek.values()));
		schedulerJobDTO.setStopDaysRepeat(Arrays.asList(DayOfWeek.values()));
		schedulerJobDTO.setSyncStartRequired(false);
		return schedulerJobDTO;
	}

	private UserInstanceDTO getUserInstanceDTO() {
		UserComputationalResource computationalResource = new UserComputationalResource();
		computationalResource.setStatus("running");
		return new UserInstanceDTO().withUser(USER).withExploratoryName(EXPLORATORY_NAME)
				.withResources(Collections.singletonList(computationalResource));
	}
}
