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

import com.epam.dlab.auth.SystemUserInfoService;
import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.ComputationalDAO;
import com.epam.dlab.backendapi.dao.EnvDAO;
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.dao.SchedulerJobDAO;
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.service.ComputationalService;
import com.epam.dlab.backendapi.service.ExploratoryService;
import com.epam.dlab.dto.SchedulerJobDTO;
import com.epam.dlab.dto.UserInstanceDTO;
import com.epam.dlab.dto.UserInstanceStatus;
import com.epam.dlab.dto.aws.computational.AwsComputationalResource;
import com.epam.dlab.dto.base.DataEngineType;
import com.epam.dlab.dto.computational.UserComputationalResource;
import com.epam.dlab.dto.status.EnvResource;
import com.epam.dlab.exceptions.ResourceInappropriateStateException;
import com.epam.dlab.exceptions.ResourceNotFoundException;
import com.epam.dlab.model.scheduler.SchedulerJobData;
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
import java.util.stream.Collectors;

import static com.epam.dlab.dto.UserInstanceStatus.*;
import static java.util.Collections.singletonList;
import static org.junit.Assert.*;
import static org.mockito.Matchers.anyVararg;
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
	private SystemUserInfoService systemUserService;
	@Mock
	private ExploratoryService exploratoryService;
	@Mock
	private ComputationalService computationalService;
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
		schedulerJobDTO = getSchedulerJobDTO(LocalDate.now(), LocalDate.now().plusDays(1),
				Arrays.asList(DayOfWeek.values()), Arrays.asList(DayOfWeek.values()), false,
				LocalDateTime.of(LocalDate.now(), LocalTime.now().truncatedTo(ChronoUnit.MINUTES)));
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
				anyString(), anyVararg())).thenReturn(singletonList(COMPUTATIONAL_NAME));
		when(computationalDAO.updateSchedulerDataForComputationalResource(anyString(), anyString(), anyString(),
				any(SchedulerJobDTO.class))).thenReturn(mock(UpdateResult.class));

		schedulerJobService.updateExploratorySchedulerData(USER, EXPLORATORY_NAME, schedulerJobDTO);

		verify(exploratoryDAO).fetchExploratoryFields(USER, EXPLORATORY_NAME);
		verify(exploratoryDAO).updateSchedulerDataForUserAndExploratory(USER, EXPLORATORY_NAME, schedulerJobDTO);
		verify(computationalDAO).getComputationalResourcesWhereStatusIn(USER,
				singletonList(DataEngineType.SPARK_STANDALONE),
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
				singletonList(DataEngineType.SPARK_STANDALONE),
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

		final SchedulerJobDTO schedulerJobDTO = getSchedulerJobDTO(LocalDate.now(), LocalDate.now().plusDays(1),
				Arrays.asList(DayOfWeek.values()), Arrays.asList(DayOfWeek.values()), false,
				LocalDateTime.of(LocalDate.now(), LocalTime.now().truncatedTo(ChronoUnit.MINUTES)));
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
		when(envDAO.findRunningResourcesForCheckInactivity()).thenReturn(singletonList(resource));
		when(provisioningService.post(anyString(), anyString(), anyListOf(EnvResource.class), any()))
				.thenReturn("someUuid");
		when(requestId.put(anyString(), anyString())).thenReturn("someUuid");

		schedulerJobService.updateRunningResourcesLastActivity(userInfo);

		verify(envDAO).findRunningResourcesForCheckInactivity();
		verify(provisioningService).post("/infrastructure/check_inactivity", "token",
				singletonList(resource), String.class);
		verify(requestId).put(USER, "someUuid");
		verifyNoMoreInteractions(envDAO, provisioningService, requestId);
	}

	@Test
	public void executeCheckClusterInactivityJobWithoutRunningClusters() {
		when(envDAO.findRunningResourcesForCheckInactivity()).thenReturn(Collections.emptyList());

		schedulerJobService.updateRunningResourcesLastActivity(userInfo);

		verify(envDAO).findRunningResourcesForCheckInactivity();
		verifyNoMoreInteractions(envDAO);
		verifyZeroInteractions(provisioningService, requestId);
	}

	@Test
	public void testStartComputationalByScheduler() {
		when(schedulerJobDAO.getComputationalSchedulerDataWithOneOfStatus(any(UserInstanceStatus.class),
				any(DataEngineType.class), anyVararg())).thenReturn(singletonList(getSchedulerJobData(LocalDate.now(),
				LocalDate.now().plusDays(1), Arrays.asList(DayOfWeek.values()), Arrays.asList(DayOfWeek.values()),
				LocalDateTime.of(LocalDate.now(),
						LocalTime.now().truncatedTo(ChronoUnit.MINUTES)), false)));
		when(systemUserService.create(anyString())).thenReturn(getUserInfo());

		schedulerJobService.startComputationalByScheduler();

		verify(systemUserService).create(USER);
		verify(schedulerJobDAO)
				.getComputationalSchedulerDataWithOneOfStatus(RUNNING, DataEngineType.SPARK_STANDALONE, STOPPED);
		verify(computationalService).startSparkCluster(refEq(getUserInfo()), eq(EXPLORATORY_NAME),
				eq(COMPUTATIONAL_NAME));
		verifyNoMoreInteractions(systemUserService, schedulerJobDAO, computationalService);
	}

	@Test
	public void testStartComputationalBySchedulerWhenSchedulerIsNotConfigured() {
		when(schedulerJobDAO.getComputationalSchedulerDataWithOneOfStatus(any(UserInstanceStatus.class),
				any(DataEngineType.class), anyVararg())).thenReturn(Collections.emptyList());

		schedulerJobService.startComputationalByScheduler();

		verify(schedulerJobDAO).getComputationalSchedulerDataWithOneOfStatus(RUNNING, DataEngineType.SPARK_STANDALONE,
				STOPPED);
		verifyNoMoreInteractions(schedulerJobDAO);
		verifyZeroInteractions(systemUserService, computationalService);
	}

	@Test
	public void testStartComputationalBySchedulerWhenSchedulerFinishDateBeforeNow() {
		final LocalDate beginDate = LocalDate.now().plusDays(1);
		final LocalDate endDate = LocalDate.now();
		final List<DayOfWeek> startDays = Arrays.asList(DayOfWeek.values());
		final List<DayOfWeek> stopDays = Arrays.asList(DayOfWeek.values());
		final LocalDateTime terminateDateTime = LocalDateTime.of(LocalDate.now(),
				LocalTime.now().truncatedTo(ChronoUnit.MINUTES));
		final SchedulerJobData schedulerJobData = getSchedulerJobData(beginDate, endDate, startDays, stopDays,
				terminateDateTime, false);
		when(schedulerJobDAO.getComputationalSchedulerDataWithOneOfStatus(any(UserInstanceStatus.class),
				any(DataEngineType.class), anyVararg())).thenReturn(singletonList(schedulerJobData));
		when(systemUserService.create(anyString())).thenReturn(getUserInfo());

		schedulerJobService.startComputationalByScheduler();

		verify(schedulerJobDAO)
				.getComputationalSchedulerDataWithOneOfStatus(RUNNING, DataEngineType.SPARK_STANDALONE, STOPPED);
		verifyNoMoreInteractions(systemUserService, schedulerJobDAO, computationalService);
	}

	@Test
	public void testStartComputationalBySchedulerWhenSchedulerStartDateAfterNow() {
		final LocalDate beginDate = LocalDate.now().plusDays(1);
		final LocalDate finishDate = LocalDate.now();
		final List<DayOfWeek> startDays = Arrays.asList(DayOfWeek.values());
		final List<DayOfWeek> stopDays = Arrays.asList(DayOfWeek.values());
		final LocalDateTime terminateDateTime = LocalDateTime.of(LocalDate.now(),
				LocalTime.now().truncatedTo(ChronoUnit.MINUTES));
		final SchedulerJobData schedulerJobData = getSchedulerJobData(
				beginDate, finishDate, startDays, stopDays, terminateDateTime, false);
		when(schedulerJobDAO.getComputationalSchedulerDataWithOneOfStatus(any(UserInstanceStatus.class),
				any(DataEngineType.class), anyVararg())).thenReturn(singletonList(schedulerJobData));
		when(systemUserService.create(anyString())).thenReturn(getUserInfo());

		schedulerJobService.startComputationalByScheduler();

		verify(schedulerJobDAO)
				.getComputationalSchedulerDataWithOneOfStatus(RUNNING, DataEngineType.SPARK_STANDALONE, STOPPED);
		verifyNoMoreInteractions(systemUserService, schedulerJobDAO, computationalService);
	}

	@Test
	public void testStartComputationalBySchedulerWhenStartDayIsNotCurrentDay() {
		final List<DayOfWeek> stopDays = Arrays.stream(DayOfWeek.values()).collect(Collectors.toList());
		final List<DayOfWeek> startDays = Arrays.stream(DayOfWeek.values()).collect(Collectors.toList());
		startDays.remove(LocalDate.now().getDayOfWeek());
		final LocalDate beginDate = LocalDate.now();
		final SchedulerJobData schedulerJobData = getSchedulerJobData(
				beginDate, LocalDate.now().minusDays(1), startDays, stopDays,
				LocalDateTime.of(LocalDate.now(),
						LocalTime.now().truncatedTo(ChronoUnit.MINUTES)), false
		);
		when(schedulerJobDAO.getComputationalSchedulerDataWithOneOfStatus(any(UserInstanceStatus.class),
				any(DataEngineType.class), anyVararg())).thenReturn(singletonList(schedulerJobData));
		when(systemUserService.create(anyString())).thenReturn(getUserInfo());

		schedulerJobService.startComputationalByScheduler();

		verify(schedulerJobDAO)
				.getComputationalSchedulerDataWithOneOfStatus(RUNNING, DataEngineType.SPARK_STANDALONE, STOPPED);
		verifyNoMoreInteractions(systemUserService, schedulerJobDAO, computationalService);
	}


	@Test
	public void testStopComputationalByScheduler() {
		when(schedulerJobDAO.getComputationalSchedulerDataWithOneOfStatus(any(UserInstanceStatus.class),
				any(DataEngineType.class), anyVararg())).thenReturn(singletonList(getSchedulerJobData(LocalDate.now(),
				LocalDate.now().plusDays(1), Arrays.asList(DayOfWeek.values()), Arrays.asList(DayOfWeek.values()),
				LocalDateTime.of(LocalDate.now(),
						LocalTime.now().truncatedTo(ChronoUnit.MINUTES)), false)));
		when(systemUserService.create(anyString())).thenReturn(getUserInfo());

		schedulerJobService.stopComputationalByScheduler();

		verify(systemUserService).create(USER);
		verify(schedulerJobDAO)
				.getComputationalSchedulerDataWithOneOfStatus(RUNNING, DataEngineType.SPARK_STANDALONE, RUNNING);
		verify(computationalService).stopSparkCluster(refEq(getUserInfo()), eq(EXPLORATORY_NAME),
				eq(COMPUTATIONAL_NAME));
		verifyNoMoreInteractions(systemUserService, schedulerJobDAO, computationalService);
	}

	@Test
	public void testStopComputationalBySchedulerWhenSchedulerIsNotConfigured() {
		when(schedulerJobDAO.getComputationalSchedulerDataWithOneOfStatus(any(UserInstanceStatus.class),
				any(DataEngineType.class), anyVararg())).thenReturn(Collections.emptyList());

		schedulerJobService.stopComputationalByScheduler();

		verify(schedulerJobDAO)
				.getComputationalSchedulerDataWithOneOfStatus(RUNNING, DataEngineType.SPARK_STANDALONE, RUNNING);
		verifyNoMoreInteractions(schedulerJobDAO);
		verifyZeroInteractions(systemUserService, computationalService);
	}

	@Test
	public void testStopComputationalBySchedulerWhenSchedulerFinishDateBeforeNow() {
		final LocalDate beginDate = LocalDate.now().plusDays(1);
		final LocalDate endDate = LocalDate.now();
		final List<DayOfWeek> startDays = Arrays.asList(DayOfWeek.values());
		final List<DayOfWeek> stopDays = Arrays.asList(DayOfWeek.values());
		final LocalDateTime terminateDateTime = LocalDateTime.of(LocalDate.now(),
				LocalTime.now().truncatedTo(ChronoUnit.MINUTES));
		final SchedulerJobData schedulerJobData = getSchedulerJobData(beginDate, endDate, startDays, stopDays,
				terminateDateTime, false);
		when(schedulerJobDAO.getComputationalSchedulerDataWithOneOfStatus(any(UserInstanceStatus.class),
				any(DataEngineType.class), anyVararg())).thenReturn(singletonList(schedulerJobData));
		when(systemUserService.create(anyString())).thenReturn(getUserInfo());

		schedulerJobService.stopComputationalByScheduler();

		verify(schedulerJobDAO)
				.getComputationalSchedulerDataWithOneOfStatus(RUNNING, DataEngineType.SPARK_STANDALONE, RUNNING);
		verifyNoMoreInteractions(systemUserService, schedulerJobDAO, computationalService);
	}

	@Test
	public void testStopComputationalBySchedulerWhenSchedulerStartDateAfterNow() {
		final LocalDate beginDate = LocalDate.now().plusDays(1);
		final LocalDate finishDate = LocalDate.now();
		final List<DayOfWeek> startDays = Arrays.asList(DayOfWeek.values());
		final List<DayOfWeek> stopDays = Arrays.asList(DayOfWeek.values());
		final LocalDateTime terminateDateTime = LocalDateTime.of(LocalDate.now(),
				LocalTime.now().truncatedTo(ChronoUnit.MINUTES));
		final SchedulerJobData schedulerJobData = getSchedulerJobData(
				beginDate, finishDate, startDays, stopDays, terminateDateTime, false);
		when(schedulerJobDAO.getComputationalSchedulerDataWithOneOfStatus(any(UserInstanceStatus.class),
				any(DataEngineType.class), anyVararg())).thenReturn(singletonList(schedulerJobData));
		when(systemUserService.create(anyString())).thenReturn(getUserInfo());

		schedulerJobService.stopComputationalByScheduler();

		verify(schedulerJobDAO)
				.getComputationalSchedulerDataWithOneOfStatus(RUNNING, DataEngineType.SPARK_STANDALONE, RUNNING);
		verifyNoMoreInteractions(systemUserService, schedulerJobDAO, computationalService);
	}

	@Test
	public void testStopComputationalBySchedulerWhenStopDayIsNotCurrentDay() {
		final List<DayOfWeek> stopDays = Arrays.stream(DayOfWeek.values()).collect(Collectors.toList());
		stopDays.remove(LocalDate.now().getDayOfWeek());
		final SchedulerJobData schedulerJobData = getSchedulerJobData(
				LocalDate.now(), LocalDate.now().minusDays(1), Arrays.asList(DayOfWeek.values()), stopDays,
				LocalDateTime.of(LocalDate.now(),
						LocalTime.now().truncatedTo(ChronoUnit.MINUTES)), false
		);
		when(schedulerJobDAO.getComputationalSchedulerDataWithOneOfStatus(any(UserInstanceStatus.class),
				any(DataEngineType.class), anyVararg())).thenReturn(singletonList(schedulerJobData));
		when(systemUserService.create(anyString())).thenReturn(getUserInfo());

		schedulerJobService.stopComputationalByScheduler();

		verify(schedulerJobDAO)
				.getComputationalSchedulerDataWithOneOfStatus(RUNNING, DataEngineType.SPARK_STANDALONE, RUNNING);
		verifyNoMoreInteractions(systemUserService, schedulerJobDAO, computationalService);
	}


	@Test
	public void testStopExploratoryByScheduler() {
		when(schedulerJobDAO.getExploratorySchedulerDataWithStatus(any(UserInstanceStatus.class))).thenReturn(singletonList(getSchedulerJobData(LocalDate.now(), LocalDate.now().plusDays(1), Arrays.asList(DayOfWeek.values()), Arrays.asList(DayOfWeek.values()), LocalDateTime.of(LocalDate.now(),
				LocalTime.now().truncatedTo(ChronoUnit.MINUTES)), false)));
		when(systemUserService.create(anyString())).thenReturn(getUserInfo());

		schedulerJobService.stopExploratoryByScheduler();

		verify(systemUserService).create(USER);
		verify(schedulerJobDAO).getExploratorySchedulerDataWithStatus(RUNNING);
		verify(exploratoryService).stop(refEq(getUserInfo()), eq(EXPLORATORY_NAME));
		verifyNoMoreInteractions(systemUserService, schedulerJobDAO, exploratoryService);
	}

	@Test
	public void testStopExploratoryBySchedulerWhenSchedulerIsNotConfigured() {
		when(schedulerJobDAO.getExploratorySchedulerDataWithStatus(any(UserInstanceStatus.class))).thenReturn(Collections.emptyList());

		schedulerJobService.stopExploratoryByScheduler();

		verify(schedulerJobDAO).getExploratorySchedulerDataWithStatus(RUNNING);
		verifyNoMoreInteractions(schedulerJobDAO);
		verifyZeroInteractions(systemUserService, exploratoryService);
	}

	@Test
	public void testStopExploratoryBySchedulerWhenSchedulerFinishDateBeforeNow() {
		final LocalDate finishDate = LocalDate.now().minusDays(1);
		final List<DayOfWeek> stopDays = Arrays.asList(DayOfWeek.values());
		final SchedulerJobData schedulerJobData = getSchedulerJobData(LocalDate.now(), finishDate,
				Arrays.asList(DayOfWeek.values()), stopDays, LocalDateTime.of(LocalDate.now(),
						LocalTime.now().truncatedTo(ChronoUnit.MINUTES)), false
		);
		when(schedulerJobDAO.getExploratorySchedulerDataWithStatus(any(UserInstanceStatus.class))).thenReturn(singletonList(schedulerJobData));
		when(systemUserService.create(anyString())).thenReturn(getUserInfo());

		schedulerJobService.stopExploratoryByScheduler();

		verify(schedulerJobDAO).getExploratorySchedulerDataWithStatus(RUNNING);
		verifyNoMoreInteractions(systemUserService, schedulerJobDAO, exploratoryService);
	}

	@Test
	public void testStopExploratoryBySchedulerWhenSchedulerStartDateAfterNow() {
		final LocalDate now = LocalDate.now();
		final LocalDate finishDate = now;
		final List<DayOfWeek> stopDays = Arrays.asList(DayOfWeek.values());
		final List<DayOfWeek> startDays = Arrays.asList(DayOfWeek.values());
		final LocalDate beginDate = now.plusDays(1);
		final LocalDateTime terminateDateTime = LocalDateTime.of(now, LocalTime.now().truncatedTo(ChronoUnit.MINUTES));
		final SchedulerJobData schedulerJobData = getSchedulerJobData(beginDate, finishDate, startDays, stopDays,
				terminateDateTime, false
		);
		when(schedulerJobDAO.getExploratorySchedulerDataWithStatus(any(UserInstanceStatus.class))).thenReturn(singletonList(schedulerJobData));
		when(systemUserService.create(anyString())).thenReturn(getUserInfo());

		schedulerJobService.stopExploratoryByScheduler();

		verify(schedulerJobDAO).getExploratorySchedulerDataWithStatus(RUNNING);
		verifyNoMoreInteractions(systemUserService, schedulerJobDAO, exploratoryService);
	}

	@Test
	public void testStopExploratoryBySchedulerWhenStopDayIsNotCurrentDay() {
		final List<DayOfWeek> stopDays = Arrays.stream((DayOfWeek.values())).collect(Collectors.toList());
		stopDays.remove(LocalDate.now().getDayOfWeek());
		final SchedulerJobData schedulerJobData = getSchedulerJobData(
				LocalDate.now(), LocalDate.now().minusDays(1), Arrays.asList(DayOfWeek.values()), stopDays,
				LocalDateTime.of(LocalDate.now(),
						LocalTime.now().truncatedTo(ChronoUnit.MINUTES)), false
		);
		when(schedulerJobDAO.getExploratorySchedulerDataWithStatus(any(UserInstanceStatus.class))).thenReturn(singletonList(schedulerJobData));
		when(systemUserService.create(anyString())).thenReturn(getUserInfo());

		schedulerJobService.stopExploratoryByScheduler();

		verify(schedulerJobDAO).getExploratorySchedulerDataWithStatus(RUNNING);
		verifyNoMoreInteractions(systemUserService, schedulerJobDAO, exploratoryService);
	}


	@Test
	public void testStartExploratoryByScheduler() {
		final LocalDate finishDate = LocalDate.now().plusDays(1);
		final List<DayOfWeek> stopDays = Arrays.asList(DayOfWeek.values());
		when(schedulerJobDAO.getExploratorySchedulerDataWithStatus(any(UserInstanceStatus.class)))
				.thenReturn(singletonList(getSchedulerJobData(LocalDate.now(), finishDate,
						Arrays.asList(DayOfWeek.values()), stopDays, LocalDateTime.of(LocalDate.now(),
								LocalTime.now().truncatedTo(ChronoUnit.MINUTES)), false
				)));
		when(systemUserService.create(anyString())).thenReturn(getUserInfo());

		schedulerJobService.startExploratoryByScheduler();

		verify(systemUserService).create(USER);
		verify(schedulerJobDAO).getExploratorySchedulerDataWithStatus(STOPPED);
		verify(exploratoryService).start(refEq(getUserInfo()), eq(EXPLORATORY_NAME));
		verifyNoMoreInteractions(systemUserService, schedulerJobDAO, exploratoryService);
		verifyZeroInteractions(computationalService, computationalDAO);
	}

	@Test
	public void testStartExploratoryBySchedulerWithSyncComputationalStart() {
		final LocalDate finishDate = LocalDate.now().plusDays(1);
		final List<DayOfWeek> stopDays = Arrays.asList(DayOfWeek.values());
		when(schedulerJobDAO.getExploratorySchedulerDataWithStatus(any(UserInstanceStatus.class)))
				.thenReturn(singletonList(getSchedulerJobData(LocalDate.now(), finishDate,
						Arrays.asList(DayOfWeek.values()), stopDays, LocalDateTime.of(LocalDate.now(),
								LocalTime.now().truncatedTo(ChronoUnit.MINUTES)), true
				)));
		when(systemUserService.create(anyString())).thenReturn(getUserInfo());
		when(computationalDAO.findComputationalResourcesWithStatus(anyString(), anyString(),
				any(UserInstanceStatus.class))).thenReturn(singletonList(getComputationalResource(COMPUTATIONAL_NAME,
				DataEngineType.SPARK_STANDALONE, true)));

		schedulerJobService.startExploratoryByScheduler();

		verify(systemUserService, times(2)).create(USER);
		verify(schedulerJobDAO).getExploratorySchedulerDataWithStatus(STOPPED);
		verify(exploratoryService).start(refEq(getUserInfo()), eq(EXPLORATORY_NAME));
		verify(computationalDAO).findComputationalResourcesWithStatus(USER, EXPLORATORY_NAME, STOPPED);
		verify(computationalService).startSparkCluster(refEq(getUserInfo()), eq(EXPLORATORY_NAME),
				eq(COMPUTATIONAL_NAME));
		verifyNoMoreInteractions(systemUserService, schedulerJobDAO, exploratoryService, computationalService,
				computationalDAO);
	}

	@Test
	public void testStartExploratoryBySchedulerWithSyncComputationalStartDataEngine() {
		final LocalDate finishDate = LocalDate.now().plusDays(1);
		final List<DayOfWeek> stopDays = Arrays.asList(DayOfWeek.values());
		when(schedulerJobDAO.getExploratorySchedulerDataWithStatus(any(UserInstanceStatus.class)))
				.thenReturn(singletonList(getSchedulerJobData(LocalDate.now(), finishDate,
						Arrays.asList(DayOfWeek.values()), stopDays, LocalDateTime.of(LocalDate.now(),
								LocalTime.now().truncatedTo(ChronoUnit.MINUTES)), true
				)));
		when(systemUserService.create(anyString())).thenReturn(getUserInfo());
		when(computationalDAO.findComputationalResourcesWithStatus(anyString(), anyString(),
				any(UserInstanceStatus.class))).thenReturn(singletonList(getComputationalResource(COMPUTATIONAL_NAME,
				DataEngineType.CLOUD_SERVICE, true)));

		schedulerJobService.startExploratoryByScheduler();

		verify(systemUserService).create(USER);
		verify(schedulerJobDAO).getExploratorySchedulerDataWithStatus(STOPPED);
		verify(exploratoryService).start(refEq(getUserInfo()), eq(EXPLORATORY_NAME));
		verify(computationalDAO).findComputationalResourcesWithStatus(USER, EXPLORATORY_NAME, STOPPED);
		verifyNoMoreInteractions(systemUserService, schedulerJobDAO, exploratoryService, computationalDAO);
		verifyZeroInteractions(computationalService);
	}

	@Test
	public void testStartExploratoryBySchedulerWithSyncComputationalStartOnExploratoryButNotOnComputational() {
		final LocalDate finishDate = LocalDate.now().plusDays(1);
		final List<DayOfWeek> stopDays = Arrays.asList(DayOfWeek.values());
		when(schedulerJobDAO.getExploratorySchedulerDataWithStatus(any(UserInstanceStatus.class)))
				.thenReturn(singletonList(getSchedulerJobData(LocalDate.now(), finishDate,
						Arrays.asList(DayOfWeek.values()), stopDays, LocalDateTime.of(LocalDate.now(),
								LocalTime.now().truncatedTo(ChronoUnit.MINUTES)), true
				)));
		when(systemUserService.create(anyString())).thenReturn(getUserInfo());
		when(computationalDAO.findComputationalResourcesWithStatus(anyString(), anyString(),
				any(UserInstanceStatus.class))).thenReturn(singletonList(getComputationalResource(COMPUTATIONAL_NAME,
				DataEngineType.SPARK_STANDALONE, false)));

		schedulerJobService.startExploratoryByScheduler();

		verify(systemUserService).create(USER);
		verify(schedulerJobDAO).getExploratorySchedulerDataWithStatus(STOPPED);
		verify(exploratoryService).start(refEq(getUserInfo()), eq(EXPLORATORY_NAME));
		verify(computationalDAO).findComputationalResourcesWithStatus(USER, EXPLORATORY_NAME, STOPPED);
		verifyNoMoreInteractions(systemUserService, schedulerJobDAO, exploratoryService, computationalDAO);
		verifyZeroInteractions(computationalService);
	}

	@Test
	public void testStartExploratoryBySchedulerWhenSchedulerIsNotConfigured() {
		when(schedulerJobDAO.getExploratorySchedulerDataWithStatus(any(UserInstanceStatus.class))).thenReturn(Collections.emptyList());

		schedulerJobService.startExploratoryByScheduler();

		verify(schedulerJobDAO).getExploratorySchedulerDataWithStatus(STOPPED);
		verifyNoMoreInteractions(schedulerJobDAO);
		verifyZeroInteractions(systemUserService, exploratoryService, computationalService, computationalDAO);
	}

	@Test
	public void testStartExploratoryBySchedulerWhenSchedulerFinishDateBeforeNow() {
		final LocalDate finishDate = LocalDate.now().minusDays(1);
		final List<DayOfWeek> stopDays = Arrays.asList(DayOfWeek.values());
		final SchedulerJobData schedulerJobData = getSchedulerJobData(LocalDate.now(), finishDate,
				Arrays.asList(DayOfWeek.values()), stopDays, LocalDateTime.of(LocalDate.now(),
						LocalTime.now().truncatedTo(ChronoUnit.MINUTES)), false
		);
		when(schedulerJobDAO.getExploratorySchedulerDataWithStatus(any(UserInstanceStatus.class))).thenReturn(singletonList(schedulerJobData));
		when(systemUserService.create(anyString())).thenReturn(getUserInfo());

		schedulerJobService.startExploratoryByScheduler();

		verify(schedulerJobDAO).getExploratorySchedulerDataWithStatus(STOPPED);
		verifyZeroInteractions(systemUserService, exploratoryService, computationalService, computationalDAO);
	}

	@Test
	public void testStartExploratoryBySchedulerWhenSchedulerStartDateAfterNow() {
		final LocalDate finishDate = LocalDate.now();
		final List<DayOfWeek> stopDays = Arrays.asList(DayOfWeek.values());
		final LocalDate beginDate = LocalDate.now().plusDays(1);
		final List<DayOfWeek> startDays = Arrays.asList(DayOfWeek.values());
		final LocalDateTime terminateDateTime = LocalDateTime.of(LocalDate.now(),
				LocalTime.now().truncatedTo(ChronoUnit.MINUTES));
		final SchedulerJobData schedulerJobData = getSchedulerJobData(beginDate, finishDate, startDays, stopDays,
				terminateDateTime, false);
		when(schedulerJobDAO.getExploratorySchedulerDataWithStatus(any(UserInstanceStatus.class))).thenReturn(singletonList(schedulerJobData));
		when(systemUserService.create(anyString())).thenReturn(getUserInfo());

		schedulerJobService.startExploratoryByScheduler();

		verify(schedulerJobDAO).getExploratorySchedulerDataWithStatus(STOPPED);
		verifyZeroInteractions(systemUserService, exploratoryService, computationalService, computationalDAO);
	}

	@Test
	public void testStartExploratoryBySchedulerWhenStartDayIsNotCurrentDay() {
		final List<DayOfWeek> stopDays = Arrays.stream((DayOfWeek.values())).collect(Collectors.toList());
		final List<DayOfWeek> startDays = Arrays.stream(DayOfWeek.values()).collect(Collectors.toList());
		startDays.remove(LocalDate.now().getDayOfWeek());
		final SchedulerJobData schedulerJobData = getSchedulerJobData(
				LocalDate.now(), LocalDate.now().minusDays(1), startDays, stopDays, LocalDateTime.of(LocalDate.now(),
						LocalTime.now().truncatedTo(ChronoUnit.MINUTES)), false);
		when(schedulerJobDAO.getExploratorySchedulerDataWithStatus(any(UserInstanceStatus.class))).thenReturn(singletonList(schedulerJobData));
		when(systemUserService.create(anyString())).thenReturn(getUserInfo());

		schedulerJobService.startExploratoryByScheduler();

		verify(schedulerJobDAO).getExploratorySchedulerDataWithStatus(STOPPED);
		verifyZeroInteractions(systemUserService, exploratoryService, computationalService, computationalDAO);
	}


	@Test
	public void testTerminateComputationalByScheduler() {
		final List<DayOfWeek> stopDays = Arrays.asList(DayOfWeek.values());
		final List<DayOfWeek> startDays = Arrays.asList(DayOfWeek.values());
		final LocalDateTime terminateDateTime = LocalDateTime.of(LocalDate.now(),
				LocalTime.now().truncatedTo(ChronoUnit.MINUTES));
		final LocalDate finishDate = LocalDate.now().plusDays(1);
		final SchedulerJobData schedulerJobData = getSchedulerJobData(LocalDate.now(), finishDate, startDays, stopDays
				, terminateDateTime, false
		);
		when(schedulerJobDAO.getComputationalSchedulerDataWithOneOfStatus(any(UserInstanceStatus.class),
				any(DataEngineType.class), anyVararg())).thenReturn(singletonList(schedulerJobData));
		when(systemUserService.create(anyString())).thenReturn(getUserInfo());

		schedulerJobService.terminateComputationalByScheduler();

		verify(systemUserService).create(USER);
		verify(schedulerJobDAO)
				.getComputationalSchedulerDataWithOneOfStatus(RUNNING, DataEngineType.SPARK_STANDALONE, STOPPED,
						RUNNING);
		verify(computationalService).terminateComputational(refEq(getUserInfo()), eq(EXPLORATORY_NAME),
				eq(COMPUTATIONAL_NAME));
		verifyNoMoreInteractions(systemUserService, schedulerJobDAO, computationalService);
	}

	@Test
	public void testTerminateComputationalBySchedulerWhenSchedulerIsNotConfigured() {
		when(schedulerJobDAO.getComputationalSchedulerDataWithOneOfStatus(any(UserInstanceStatus.class),
				any(DataEngineType.class), anyVararg())).thenReturn(Collections.emptyList());

		schedulerJobService.terminateComputationalByScheduler();

		verify(schedulerJobDAO).getComputationalSchedulerDataWithOneOfStatus(RUNNING, DataEngineType.SPARK_STANDALONE,
				STOPPED, RUNNING);
		verifyNoMoreInteractions(schedulerJobDAO);
		verifyZeroInteractions(systemUserService, computationalService);
	}

	@Test
	public void testTerminateComputationalBySchedulerWhenSchedulerFinishDateBeforeNow() {
		final SchedulerJobData schedulerJobData = getSchedulerJobData(
				LocalDate.now(), LocalDate.now().minusDays(1), Arrays.asList(DayOfWeek.values()),
				Arrays.asList(DayOfWeek.values()), LocalDateTime.of(LocalDate.now(),
						LocalTime.now().truncatedTo(ChronoUnit.MINUTES)), false
		);
		when(schedulerJobDAO.getComputationalSchedulerDataWithOneOfStatus(any(UserInstanceStatus.class),
				any(DataEngineType.class), anyVararg())).thenReturn(singletonList(schedulerJobData));
		when(systemUserService.create(anyString())).thenReturn(getUserInfo());

		schedulerJobService.terminateComputationalByScheduler();

		verify(schedulerJobDAO).getComputationalSchedulerDataWithOneOfStatus(RUNNING, DataEngineType.SPARK_STANDALONE,
				STOPPED, RUNNING);
		verifyNoMoreInteractions(systemUserService, schedulerJobDAO, computationalService);
	}

	@Test
	public void testTerminateComputationalBySchedulerWhenSchedulerStartDateAfterNow() {
		final LocalDate beginDate = LocalDate.now().plusDays(1);
		final LocalDate finishDate = LocalDate.now();
		final List<DayOfWeek> startDays = Arrays.asList(DayOfWeek.values());
		final List<DayOfWeek> stopDays = Arrays.asList(DayOfWeek.values());
		final LocalDateTime terminateDateTime = LocalDateTime.of(LocalDate.now(),
				LocalTime.now().truncatedTo(ChronoUnit.MINUTES));
		final SchedulerJobData schedulerJobData = getSchedulerJobData(beginDate, finishDate, startDays, stopDays,
				terminateDateTime, false);
		when(schedulerJobDAO.getComputationalSchedulerDataWithOneOfStatus(any(UserInstanceStatus.class),
				any(DataEngineType.class), anyVararg())).thenReturn(singletonList(schedulerJobData));
		when(systemUserService.create(anyString())).thenReturn(getUserInfo());

		schedulerJobService.terminateComputationalByScheduler();

		verify(schedulerJobDAO).getComputationalSchedulerDataWithOneOfStatus(RUNNING, DataEngineType.SPARK_STANDALONE,
				STOPPED, RUNNING);
		verifyNoMoreInteractions(systemUserService, schedulerJobDAO, computationalService);
	}

	@Test
	public void testTerminateComputationalBySchedulerWhenTerminateDateNotCurrent() {
		final List<DayOfWeek> stopDays = Arrays.stream(DayOfWeek.values()).collect(Collectors.toList());
		stopDays.remove(LocalDate.now().getDayOfWeek());
		final LocalDateTime terminateDateTime = LocalDateTime.of(LocalDate.now(),
				LocalTime.now().truncatedTo(ChronoUnit.MINUTES)).plusDays(1);
		final SchedulerJobData schedulerJobData = getSchedulerJobData(
				LocalDate.now(), LocalDate.now().minusDays(1), Arrays.asList(DayOfWeek.values()), stopDays,
				terminateDateTime, false
		);
		when(schedulerJobDAO.getComputationalSchedulerDataWithOneOfStatus(any(UserInstanceStatus.class),
				any(DataEngineType.class), anyVararg())).thenReturn(singletonList(schedulerJobData));
		when(systemUserService.create(anyString())).thenReturn(getUserInfo());

		schedulerJobService.terminateComputationalByScheduler();

		verify(schedulerJobDAO)
				.getComputationalSchedulerDataWithOneOfStatus(RUNNING, DataEngineType.SPARK_STANDALONE, STOPPED,
						RUNNING);
		verifyNoMoreInteractions(systemUserService, schedulerJobDAO, computationalService);
	}

	@Test
	public void testTerminateExploratoryByScheduler() {
		final List<DayOfWeek> stopDays = Arrays.asList(DayOfWeek.values());
		final List<DayOfWeek> startDays = Arrays.asList(DayOfWeek.values());
		final LocalDateTime terminateDateTime = LocalDateTime.of(LocalDate.now(),
				LocalTime.now().truncatedTo(ChronoUnit.MINUTES));
		final LocalDate finishDate = LocalDate.now().plusDays(1);
		final SchedulerJobData schedulerJobData = getSchedulerJobData(LocalDate.now(), finishDate, startDays, stopDays
				, terminateDateTime, false
		);
		when(schedulerJobDAO.getExploratorySchedulerDataWithOneOfStatus(anyVararg())).thenReturn(singletonList(schedulerJobData));
		when(systemUserService.create(anyString())).thenReturn(getUserInfo());

		schedulerJobService.terminateExploratoryByScheduler();

		verify(systemUserService).create(USER);
		verify(schedulerJobDAO).getExploratorySchedulerDataWithOneOfStatus(RUNNING, STOPPED);
		verify(exploratoryService).terminate(refEq(getUserInfo()), eq(EXPLORATORY_NAME));
		verifyNoMoreInteractions(systemUserService, schedulerJobDAO, computationalService, exploratoryService);
	}

	@Test
	public void testTerminateExploratoryBySchedulerWhenSchedulerIsNotConfigured() {
		when(schedulerJobDAO.getComputationalSchedulerDataWithOneOfStatus(any(UserInstanceStatus.class),
				any(DataEngineType.class), anyVararg())).thenReturn(Collections.emptyList());

		schedulerJobService.terminateExploratoryByScheduler();

		verify(schedulerJobDAO).getExploratorySchedulerDataWithOneOfStatus(RUNNING, STOPPED);
		verifyNoMoreInteractions(schedulerJobDAO);
		verifyZeroInteractions(systemUserService, exploratoryService, computationalService);
	}

	@Test
	public void testTerminateExploratoryBySchedulerWhenSchedulerFinishDateBeforeNow() {
		final SchedulerJobData schedulerJobData = getSchedulerJobData(
				LocalDate.now(), LocalDate.now().minusDays(1), Arrays.asList(DayOfWeek.values()),
				Arrays.asList(DayOfWeek.values()), LocalDateTime.of(LocalDate.now(),
						LocalTime.now().truncatedTo(ChronoUnit.MINUTES)), false
		);
		when(schedulerJobDAO.getExploratorySchedulerDataWithOneOfStatus(anyVararg())).thenReturn(singletonList(schedulerJobData));
		when(systemUserService.create(anyString())).thenReturn(getUserInfo());

		schedulerJobService.terminateExploratoryByScheduler();

		verify(schedulerJobDAO).getExploratorySchedulerDataWithOneOfStatus(RUNNING, STOPPED);
		verifyNoMoreInteractions(systemUserService, schedulerJobDAO, exploratoryService, computationalService);
	}

	@Test
	public void testTerminateExploratoryBySchedulerWhenSchedulerStartDateAfterNow() {
		final LocalDate beginDate = LocalDate.now().plusDays(1);
		final LocalDate finishDate = LocalDate.now();
		final List<DayOfWeek> startDays = Arrays.asList(DayOfWeek.values());
		final List<DayOfWeek> stopDays = Arrays.asList(DayOfWeek.values());
		final LocalDateTime terminateDateTime = LocalDateTime.of(LocalDate.now(),
				LocalTime.now().truncatedTo(ChronoUnit.MINUTES));
		final SchedulerJobData schedulerJobData = getSchedulerJobData(
				beginDate, finishDate, startDays, stopDays, terminateDateTime, false);
		when(schedulerJobDAO.getExploratorySchedulerDataWithOneOfStatus(anyVararg())).thenReturn(singletonList(schedulerJobData));
		when(systemUserService.create(anyString())).thenReturn(getUserInfo());

		schedulerJobService.terminateExploratoryByScheduler();

		verify(schedulerJobDAO).getExploratorySchedulerDataWithOneOfStatus(RUNNING, STOPPED);
		verifyNoMoreInteractions(systemUserService, schedulerJobDAO, exploratoryService, computationalService);
	}

	@Test
	public void testTerminateExploratoryBySchedulerWhenTerminateDateNotCurrent() {
		final List<DayOfWeek> stopDays = Arrays.stream(DayOfWeek.values()).collect(Collectors.toList());
		stopDays.remove(LocalDate.now().getDayOfWeek());
		final LocalDateTime terminateDateTime = LocalDateTime.of(LocalDate.now(),
				LocalTime.now().truncatedTo(ChronoUnit.MINUTES)).plusDays(1);
		final SchedulerJobData schedulerJobData = getSchedulerJobData(
				LocalDate.now(), LocalDate.now().minusDays(1), Arrays.asList(DayOfWeek.values()), stopDays,
				terminateDateTime, false
		);
		when(schedulerJobDAO.getExploratorySchedulerDataWithOneOfStatus(anyVararg())).thenReturn(singletonList(schedulerJobData));
		when(systemUserService.create(anyString())).thenReturn(getUserInfo());

		schedulerJobService.terminateExploratoryByScheduler();

		verify(schedulerJobDAO).getExploratorySchedulerDataWithOneOfStatus(RUNNING, STOPPED);
		verifyNoMoreInteractions(systemUserService, schedulerJobDAO, computationalService, exploratoryService);
	}

	private SchedulerJobData getSchedulerJobData(LocalDate beginDate, LocalDate schedulerFinishDate,
												 List<DayOfWeek> startDays, List<DayOfWeek> stopDays,
												 LocalDateTime terminateDateTime, boolean syncStartRequired) {
		return new SchedulerJobData(USER, EXPLORATORY_NAME, COMPUTATIONAL_NAME, getSchedulerJobDTO(beginDate,
				schedulerFinishDate, startDays, stopDays, syncStartRequired, terminateDateTime));
	}

	private UserInfo getUserInfo() {
		return new UserInfo(USER, "token");
	}

	private SchedulerJobDTO getSchedulerJobDTO(LocalDate beginDate, LocalDate finishDate, List<DayOfWeek> startDays,
											   List<DayOfWeek> stopDays, boolean syncStartRequired,
											   LocalDateTime terminateDateTime) {
		SchedulerJobDTO schedulerJobDTO = new SchedulerJobDTO();
		schedulerJobDTO.setTimeZoneOffset(OffsetDateTime.now(ZoneId.systemDefault()).getOffset());
		schedulerJobDTO.setBeginDate(beginDate);
		schedulerJobDTO.setFinishDate(finishDate);
		schedulerJobDTO.setStartTime(LocalTime.now().truncatedTo(ChronoUnit.MINUTES));
		schedulerJobDTO.setEndTime(LocalTime.now().truncatedTo(ChronoUnit.MINUTES));
		schedulerJobDTO.setTerminateDateTime(terminateDateTime);
		schedulerJobDTO.setStartDaysRepeat(startDays);
		schedulerJobDTO.setStopDaysRepeat(stopDays);
		schedulerJobDTO.setSyncStartRequired(syncStartRequired);
		return schedulerJobDTO;
	}

	private UserInstanceDTO getUserInstanceDTO() {
		UserComputationalResource computationalResource = new UserComputationalResource();
		computationalResource.setStatus("running");
		return new UserInstanceDTO().withUser(USER).withExploratoryName(EXPLORATORY_NAME)
				.withResources(singletonList(computationalResource));
	}

	private AwsComputationalResource getComputationalResource(String computationalName,
															  DataEngineType dataEngineType,
															  boolean syncStartRequired) {
		final SchedulerJobDTO schedulerJobData = new SchedulerJobDTO();
		schedulerJobData.setSyncStartRequired(syncStartRequired);
		return AwsComputationalResource.builder()
				.computationalName(computationalName)
				.imageName(DataEngineType.getDockerImageName(dataEngineType))
				.schedulerJobData(schedulerJobData)
				.build();
	}
}
