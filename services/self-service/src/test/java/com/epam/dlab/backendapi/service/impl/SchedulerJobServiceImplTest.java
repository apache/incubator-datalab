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
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.dao.SchedulerJobDAO;
import com.epam.dlab.dto.SchedulerJobDTO;
import com.epam.dlab.dto.UserInstanceDTO;
import com.epam.dlab.dto.UserInstanceStatus;
import com.epam.dlab.dto.base.DataEngineType;
import com.epam.dlab.dto.computational.UserComputationalResource;
import com.epam.dlab.exceptions.ResourceInappropriateStateException;
import com.epam.dlab.exceptions.ResourceNotFoundException;
import com.epam.dlab.model.scheduler.SchedulerJobData;
import com.mongodb.client.result.UpdateResult;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;

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
		when(exploratoryDAO.isExploratoryExist(anyString(), anyString())).thenReturn(true);
		when(schedulerJobDAO.fetchSingleSchedulerJobByUserAndExploratory(anyString(), anyString()))
				.thenReturn(Optional.of(schedulerJobDTO));

		SchedulerJobDTO actualSchedulerJobDto =
				schedulerJobService.fetchSchedulerJobForUserAndExploratory(USER, EXPLORATORY_NAME);
		assertNotNull(actualSchedulerJobDto);
		assertEquals(schedulerJobDTO, actualSchedulerJobDto);

		verify(exploratoryDAO).isExploratoryExist(USER, EXPLORATORY_NAME);
		verify(schedulerJobDAO).fetchSingleSchedulerJobByUserAndExploratory(USER, EXPLORATORY_NAME);
		verifyNoMoreInteractions(exploratoryDAO, schedulerJobDAO);
	}

	@Test
	public void fetchSchedulerJobForUserAndExploratoryWhenNotebookNotExist() {
		when(exploratoryDAO.isExploratoryExist(anyString(), anyString())).thenReturn(false);
		try {
			schedulerJobService.fetchSchedulerJobForUserAndExploratory(USER, EXPLORATORY_NAME);
		} catch (ResourceNotFoundException e) {
			assertEquals("Exploratory for user test with name explName not found", e.getMessage());
		}
		verify(exploratoryDAO).isExploratoryExist(USER, EXPLORATORY_NAME);
		verifyNoMoreInteractions(exploratoryDAO);
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
		verify(exploratoryDAO).isExploratoryExist(USER, EXPLORATORY_NAME);
		verify(schedulerJobDAO).fetchSingleSchedulerJobByUserAndExploratory(USER, EXPLORATORY_NAME);
		verifyNoMoreInteractions(exploratoryDAO, schedulerJobDAO);
	}

	@Test
	public void fetchSchedulerJobForComputationalResource() {
		when(computationalDAO.isComputationalExist(anyString(), anyString(), anyString())).thenReturn(true);
		when(schedulerJobDAO.fetchSingleSchedulerJobForCluster(anyString(), anyString(), anyString()))
				.thenReturn(Optional.of(schedulerJobDTO));

		SchedulerJobDTO actualSchedulerJobDto = schedulerJobService
				.fetchSchedulerJobForComputationalResource(USER, EXPLORATORY_NAME, COMPUTATIONAL_NAME);
		assertNotNull(actualSchedulerJobDto);
		assertEquals(schedulerJobDTO, actualSchedulerJobDto);

		verify(computationalDAO).isComputationalExist(USER, EXPLORATORY_NAME, COMPUTATIONAL_NAME);
		verify(schedulerJobDAO).fetchSingleSchedulerJobForCluster(USER, EXPLORATORY_NAME, COMPUTATIONAL_NAME);
		verifyNoMoreInteractions(computationalDAO, schedulerJobDAO);
	}

	@Test
	public void fetchSchedulerJobForComputationalResourceWhenClusterNotExist() {
		when(computationalDAO.isComputationalExist(anyString(), anyString(), anyString())).thenReturn(false);
		try {
			schedulerJobService.fetchSchedulerJobForComputationalResource(USER, EXPLORATORY_NAME, COMPUTATIONAL_NAME);
		} catch (ResourceNotFoundException e) {
			assertEquals("Computational resource compName affiliated with exploratory explName for user test " +
					"not found", e.getMessage());
		}
		verify(computationalDAO).isComputationalExist(USER, EXPLORATORY_NAME, COMPUTATIONAL_NAME);
		verifyNoMoreInteractions(computationalDAO);
	}

	@Test
	public void fetchEmptySchedulerJobForComputationalResource() {
		when(computationalDAO.isComputationalExist(anyString(), anyString(), anyString())).thenReturn(true);
		when(schedulerJobDAO.fetchSingleSchedulerJobForCluster(anyString(), anyString(), anyString()))
				.thenReturn(Optional.empty());
		try {
			schedulerJobService.fetchSchedulerJobForComputationalResource(USER, EXPLORATORY_NAME, COMPUTATIONAL_NAME);
		} catch (ResourceNotFoundException e) {
			assertEquals("Scheduler job data not found for user test with exploratory explName with " +
					"computational resource compName", e.getMessage());
		}
		verify(computationalDAO).isComputationalExist(USER, EXPLORATORY_NAME, COMPUTATIONAL_NAME);
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
	public void updateSchedulerDataForUserAndExploratoryWhenSchedulerIsNull() {
		userInstance.withStatus("running");
		when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString())).thenReturn(userInstance);
		when(exploratoryDAO.updateSchedulerDataForUserAndExploratory(anyString(), anyString(),
				any(SchedulerJobDTO.class))).thenReturn(mock(UpdateResult.class));

		final SchedulerJobDTO schedulerJobDTO = getSchedulerJobDTO();
		schedulerJobDTO.setStartDaysRepeat(Collections.emptyList());
		schedulerJobDTO.setStopDaysRepeat(Collections.emptyList());
		schedulerJobService.updateExploratorySchedulerData(USER, EXPLORATORY_NAME, schedulerJobDTO);

		verify(exploratoryDAO).fetchExploratoryFields(USER, EXPLORATORY_NAME);
		verify(exploratoryDAO).updateSchedulerDataForUserAndExploratory(USER, EXPLORATORY_NAME, null);
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
		verify(computationalDAO).updateSchedulerDataForComputationalResource(USER, EXPLORATORY_NAME,
				COMPUTATIONAL_NAME, null);
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
	public void executeStartExploratoryJobWithoutSparkClusters() {
		when(schedulerJobDAO.getSchedulerJobsToAchieveStatus(any(UserInstanceStatus.class), anyBoolean()))
				.thenReturn(Collections.singletonList(new SchedulerJobData(USER, EXPLORATORY_NAME, COMPUTATIONAL_NAME,
						schedulerJobDTO)));
		when(exploratoryService.start(any(UserInfo.class), anyString())).thenReturn("someUuid");
		when(schedulerJobDAO.fetchSingleSchedulerJobByUserAndExploratory(anyString(), anyString()))
				.thenReturn(Optional.of(schedulerJobDTO));
		when(systemUserService.create(anyString())).thenReturn(userInfo);

		schedulerJobService.executeStartResourceJob(false);

		verify(schedulerJobDAO).getSchedulerJobsToAchieveStatus(refEq(UserInstanceStatus.RUNNING), eq(false));
		verify(exploratoryService).start(userInfo, EXPLORATORY_NAME);
		verify(schedulerJobDAO).fetchSingleSchedulerJobByUserAndExploratory(USER, EXPLORATORY_NAME);
		verify(systemUserService).create(USER);
		verifyNoMoreInteractions(schedulerJobDAO, systemUserService, exploratoryService);
		verifyZeroInteractions(computationalService);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void executeStartExploratoryJobWithSparkClusters() {
		schedulerJobDTO.setSyncStartRequired(true);
		when(schedulerJobDAO.getSchedulerJobsToAchieveStatus(any(UserInstanceStatus.class), anyBoolean()))
				.thenReturn(Collections.singletonList(new SchedulerJobData(USER, EXPLORATORY_NAME, COMPUTATIONAL_NAME,
						schedulerJobDTO)));
		when(schedulerJobDAO.fetchSingleSchedulerJobByUserAndExploratory(anyString(), anyString()))
				.thenReturn(Optional.of(schedulerJobDTO));
		when(exploratoryService.start(any(UserInfo.class), anyString())).thenReturn("someUuid");
		when(computationalDAO.getComputationalResourcesWhereStatusIn(anyString(), any(List.class),
				anyString(), any(UserInstanceStatus.class))).thenReturn(Collections.singletonList(COMPUTATIONAL_NAME));

		LocalDate notebookBeginDate = schedulerJobDTO.getBeginDate();
		LocalDate notebookFinishDate = schedulerJobDTO.getFinishDate();
		LocalTime notebookStartTime = schedulerJobDTO.getStartTime();
		LocalTime notebookEndTime = schedulerJobDTO.getEndTime();
		List<DayOfWeek> notebookStartDaysRepeat = schedulerJobDTO.getStartDaysRepeat();
		List<DayOfWeek> notebookStopDaysRepeat = schedulerJobDTO.getStopDaysRepeat();
		LocalDateTime notebookTerminateDateTime = schedulerJobDTO.getTerminateDateTime();
		ZoneOffset notebookZoneOffset = schedulerJobDTO.getTimeZoneOffset();
		boolean notebookIsSyncStartRequired = schedulerJobDTO.isSyncStartRequired();

		SchedulerJobDTO clusterScheduler = new SchedulerJobDTO();
		clusterScheduler.setBeginDate(LocalDate.of(notebookBeginDate.getYear(), notebookBeginDate.getMonth(),
				notebookBeginDate.getDayOfMonth()));
		clusterScheduler.setFinishDate(LocalDate.of(notebookFinishDate.getYear() + 1, notebookFinishDate.getMonth(),
				notebookFinishDate.getDayOfMonth()));
		clusterScheduler.setStartTime(LocalTime.of(notebookStartTime.getHour(), notebookStartTime.getMinute()));
		clusterScheduler.setEndTime(LocalTime.of(notebookEndTime.getHour() + 1, notebookEndTime.getMinute()));
		clusterScheduler.setStartDaysRepeat(new ArrayList<>(notebookStartDaysRepeat));
		clusterScheduler.setStopDaysRepeat(new ArrayList<>(notebookStopDaysRepeat));
		clusterScheduler.setTerminateDateTime(LocalDateTime.of(notebookTerminateDateTime.getYear() + 1,
				notebookTerminateDateTime.getMonth(), notebookTerminateDateTime.getDayOfMonth(),
				notebookTerminateDateTime.getHour(), notebookTerminateDateTime.getMinute()));
		clusterScheduler.setTimeZoneOffset(notebookZoneOffset);
		clusterScheduler.setSyncStartRequired(notebookIsSyncStartRequired);

		when(schedulerJobDAO.fetchSingleSchedulerJobForCluster(anyString(), anyString(), anyString()))
				.thenReturn(Optional.of(clusterScheduler));
		when(systemUserService.create(anyString())).thenReturn(userInfo);

		doNothing().when(computationalService).startSparkCluster(any(UserInfo.class), anyString(), anyString());

		schedulerJobService.executeStartResourceJob(false);

		verify(schedulerJobDAO).getSchedulerJobsToAchieveStatus(refEq(UserInstanceStatus.RUNNING), eq(false));
		verify(schedulerJobDAO).fetchSingleSchedulerJobByUserAndExploratory(USER, EXPLORATORY_NAME);
		verify(computationalDAO).getComputationalResourcesWhereStatusIn(USER,
				Collections.singletonList(DataEngineType.SPARK_STANDALONE),
				EXPLORATORY_NAME, UserInstanceStatus.STOPPED);
		verify(schedulerJobDAO).fetchSingleSchedulerJobForCluster(USER, EXPLORATORY_NAME, COMPUTATIONAL_NAME);
		verify(systemUserService, times(2)).create(USER);
		verify(exploratoryService).start(userInfo, EXPLORATORY_NAME);
		verify(computationalService).startSparkCluster(userInfo, EXPLORATORY_NAME, COMPUTATIONAL_NAME);
		verifyNoMoreInteractions(schedulerJobDAO, computationalDAO, systemUserService, exploratoryService,
				computationalService);
	}

	@Test
	@SuppressWarnings("unchecked")
	public void executeStartExploratoryJobWithSparkClustersWhenNotebookAndClusterSchedulersAreNotEqual() {
		schedulerJobDTO.setSyncStartRequired(true);
		when(schedulerJobDAO.getSchedulerJobsToAchieveStatus(any(UserInstanceStatus.class), anyBoolean()))
				.thenReturn(Collections.singletonList(new SchedulerJobData(USER, EXPLORATORY_NAME, COMPUTATIONAL_NAME,
						schedulerJobDTO)));
		when(schedulerJobDAO.fetchSingleSchedulerJobByUserAndExploratory(anyString(), anyString()))
				.thenReturn(Optional.of(schedulerJobDTO));
		when(exploratoryService.start(any(UserInfo.class), anyString())).thenReturn("someUuid");
		when(computationalDAO.getComputationalResourcesWhereStatusIn(anyString(), any(List.class),
				anyString(), any(UserInstanceStatus.class))).thenReturn(Collections.singletonList(COMPUTATIONAL_NAME));

		LocalDate notebookBeginDate = schedulerJobDTO.getBeginDate();
		LocalDate notebookFinishDate = schedulerJobDTO.getFinishDate();
		LocalTime notebookStartTime = schedulerJobDTO.getStartTime();
		LocalTime notebookEndTime = schedulerJobDTO.getEndTime();
		List<DayOfWeek> notebookStartDaysRepeat = schedulerJobDTO.getStartDaysRepeat();
		List<DayOfWeek> notebookStopDaysRepeat = schedulerJobDTO.getStopDaysRepeat();
		LocalDateTime notebookTerminateDateTime = schedulerJobDTO.getTerminateDateTime();
		ZoneOffset notebookZoneOffset = schedulerJobDTO.getTimeZoneOffset();
		boolean notebookIsSyncStartRequired = schedulerJobDTO.isSyncStartRequired();

		SchedulerJobDTO clusterScheduler = new SchedulerJobDTO();
		clusterScheduler.setBeginDate(LocalDate.of(notebookBeginDate.getYear() + 1, notebookBeginDate.getMonth(),
				notebookBeginDate.getDayOfMonth()));
		clusterScheduler.setFinishDate(LocalDate.of(notebookFinishDate.getYear() + 1, notebookFinishDate.getMonth(),
				notebookFinishDate.getDayOfMonth()));
		clusterScheduler.setStartTime(LocalTime.of(notebookStartTime.getHour() + 1, notebookStartTime.getMinute()));
		clusterScheduler.setEndTime(LocalTime.of(notebookEndTime.getHour() + 1, notebookEndTime.getMinute()));
		clusterScheduler.setStartDaysRepeat(new ArrayList<>(notebookStartDaysRepeat));
		clusterScheduler.setStopDaysRepeat(new ArrayList<>(notebookStopDaysRepeat));
		clusterScheduler.setTerminateDateTime(LocalDateTime.of(notebookTerminateDateTime.getYear() + 1,
				notebookTerminateDateTime.getMonth(), notebookTerminateDateTime.getDayOfMonth(),
				notebookTerminateDateTime.getHour(), notebookTerminateDateTime.getMinute()));
		clusterScheduler.setTimeZoneOffset(notebookZoneOffset);
		clusterScheduler.setSyncStartRequired(notebookIsSyncStartRequired);

		when(schedulerJobDAO.fetchSingleSchedulerJobForCluster(anyString(), anyString(), anyString()))
				.thenReturn(Optional.of(clusterScheduler));
		when(systemUserService.create(anyString())).thenReturn(userInfo);

		schedulerJobService.executeStartResourceJob(false);

		verify(schedulerJobDAO).getSchedulerJobsToAchieveStatus(refEq(UserInstanceStatus.RUNNING), eq(false));
		verify(schedulerJobDAO).fetchSingleSchedulerJobByUserAndExploratory(USER, EXPLORATORY_NAME);
		verify(computationalDAO).getComputationalResourcesWhereStatusIn(USER,
				Collections.singletonList(DataEngineType.SPARK_STANDALONE),
				EXPLORATORY_NAME, UserInstanceStatus.STOPPED);
		verify(schedulerJobDAO).fetchSingleSchedulerJobForCluster(USER, EXPLORATORY_NAME, COMPUTATIONAL_NAME);
		verify(systemUserService).create(USER);
		verify(exploratoryService).start(userInfo, EXPLORATORY_NAME);
		verifyNoMoreInteractions(schedulerJobDAO, computationalDAO, systemUserService, exploratoryService);
		verifyZeroInteractions(computationalService);
	}

	@Test
	public void executeStartExploratoryJobWhenSchedulerIsAbsent() {
		when(schedulerJobDAO.getSchedulerJobsToAchieveStatus(any(UserInstanceStatus.class), anyBoolean()))
				.thenReturn(Collections.emptyList());
		schedulerJobService.executeStartResourceJob(false);
		verify(schedulerJobDAO).getSchedulerJobsToAchieveStatus(refEq(UserInstanceStatus.RUNNING), eq(false));
		verify(systemUserService, never()).create(any());
		verify(exploratoryService, never()).start(any(), any());
		verifyNoMoreInteractions(schedulerJobDAO);
	}

	@Test
	public void executeStartComputationalJob() {
		when(schedulerJobDAO.getSchedulerJobsToAchieveStatus(any(UserInstanceStatus.class), anyBoolean()))
				.thenReturn(Collections.singletonList(new SchedulerJobData(USER, EXPLORATORY_NAME, COMPUTATIONAL_NAME,
						schedulerJobDTO)));
		when(systemUserService.create(anyString())).thenReturn(userInfo);
		doNothing().when(computationalService).startSparkCluster(any(UserInfo.class), anyString(), anyString());

		schedulerJobService.executeStartResourceJob(true);

		verify(schedulerJobDAO).getSchedulerJobsToAchieveStatus(refEq(UserInstanceStatus.RUNNING), eq(true));
		verify(systemUserService).create(USER);
		verify(computationalService).startSparkCluster(userInfo, EXPLORATORY_NAME, COMPUTATIONAL_NAME);
		verifyNoMoreInteractions(schedulerJobDAO, systemUserService, computationalService);
		verifyZeroInteractions(exploratoryService);
	}

	@Test
	public void executeStartComputationalJobWhenSchedulerIsAbsent() {
		when(schedulerJobDAO.getSchedulerJobsToAchieveStatus(any(UserInstanceStatus.class), anyBoolean()))
				.thenReturn(Collections.emptyList());
		schedulerJobService.executeStartResourceJob(true);
		verify(schedulerJobDAO).getSchedulerJobsToAchieveStatus(refEq(UserInstanceStatus.RUNNING), eq(true));
		verify(systemUserService, never()).create(any());
		verify(computationalService, never()).startSparkCluster(any(), any(), any());
		verifyNoMoreInteractions(schedulerJobDAO);
	}

	@Test
	public void executeStopExploratoryJob() {
		schedulerJobDTO.setStartTime(LocalTime.now().minusHours(1));
		when(schedulerJobDAO.getSchedulerJobsToAchieveStatus(any(UserInstanceStatus.class), anyBoolean()))
				.thenReturn(Collections.singletonList(new SchedulerJobData(USER, EXPLORATORY_NAME,
						COMPUTATIONAL_NAME, schedulerJobDTO)));
		when(systemUserService.create(anyString())).thenReturn(userInfo);
		when(exploratoryService.stop(any(UserInfo.class), anyString())).thenReturn("someUuid");

		schedulerJobService.executeStopResourceJob(false);

		verify(schedulerJobDAO).getSchedulerJobsToAchieveStatus(refEq(UserInstanceStatus.STOPPED), eq(false));
		verify(systemUserService).create(USER);
		verify(exploratoryService).stop(userInfo, EXPLORATORY_NAME);
		verifyNoMoreInteractions(schedulerJobDAO, systemUserService, exploratoryService);
		verifyZeroInteractions(computationalService);
	}

	@Test
	public void executeStopExploratoryJobWhenSchedulerIsAbsent() {
		when(schedulerJobDAO.getSchedulerJobsToAchieveStatus(any(UserInstanceStatus.class), anyBoolean()))
				.thenReturn(Collections.emptyList());
		schedulerJobService.executeStopResourceJob(false);

		verify(schedulerJobDAO).getSchedulerJobsToAchieveStatus(refEq(UserInstanceStatus.STOPPED), eq(false));
		verify(systemUserService, never()).create(USER);
		verify(exploratoryService, never()).stop(any(), any());
		verifyNoMoreInteractions(schedulerJobDAO);
		verifyZeroInteractions(computationalService);
	}

	@Test
	public void executeStopComputationalJob() {
		schedulerJobDTO.setStartTime(LocalTime.now().minusHours(1));
		when(schedulerJobDAO.getSchedulerJobsToAchieveStatus(any(UserInstanceStatus.class), anyBoolean()))
				.thenReturn(Collections.singletonList(new SchedulerJobData(USER, EXPLORATORY_NAME,
						COMPUTATIONAL_NAME, schedulerJobDTO)));
		when(systemUserService.create(anyString())).thenReturn(userInfo);
		doNothing().when(computationalService).stopSparkCluster(any(UserInfo.class), anyString(), anyString());

		schedulerJobService.executeStopResourceJob(true);

		verify(schedulerJobDAO).getSchedulerJobsToAchieveStatus(refEq(UserInstanceStatus.STOPPED), eq(true));
		verify(systemUserService).create(USER);
		verify(computationalService).stopSparkCluster(userInfo, EXPLORATORY_NAME, COMPUTATIONAL_NAME);
		verifyNoMoreInteractions(schedulerJobDAO, systemUserService, computationalService);
		verifyZeroInteractions(exploratoryService);
	}

	@Test
	public void executeStopComputationalJobWhenSchedulerIsAbsent() {
		when(schedulerJobDAO.getSchedulerJobsToAchieveStatus(any(UserInstanceStatus.class), anyBoolean()))
				.thenReturn(Collections.emptyList());
		schedulerJobService.executeStopResourceJob(true);

		verify(schedulerJobDAO).getSchedulerJobsToAchieveStatus(refEq(UserInstanceStatus.STOPPED), eq(true));
		verify(systemUserService, never()).create(USER);
		verify(computationalService, never()).stopSparkCluster(any(), any(), any());
		verifyNoMoreInteractions(schedulerJobDAO);
		verifyZeroInteractions(exploratoryService);
	}

	@Test
	public void executeTerminateExploratoryJob() {
		when(schedulerJobDAO.getSchedulerJobsToAchieveStatus(any(UserInstanceStatus.class), anyBoolean()))
				.thenReturn(Collections.singletonList(new SchedulerJobData(USER, EXPLORATORY_NAME,
						COMPUTATIONAL_NAME, schedulerJobDTO)));
		when(systemUserService.create(anyString())).thenReturn(userInfo);
		when(exploratoryService.terminate(any(UserInfo.class), anyString())).thenReturn("someUuid");

		schedulerJobService.executeTerminateResourceJob(false);

		verify(schedulerJobDAO).getSchedulerJobsToAchieveStatus(refEq(UserInstanceStatus.TERMINATED), eq(false));
		verify(systemUserService).create(USER);
		verify(exploratoryService).terminate(userInfo, EXPLORATORY_NAME);
		verifyNoMoreInteractions(schedulerJobDAO, systemUserService, exploratoryService);
		verifyZeroInteractions(computationalService);
	}

	@Test
	public void executeTerminateExploratoryJobWhenSchedulerIsAbsent() {
		when(schedulerJobDAO.getSchedulerJobsToAchieveStatus(any(UserInstanceStatus.class), anyBoolean()))
				.thenReturn(Collections.emptyList());
		schedulerJobService.executeTerminateResourceJob(false);

		verify(schedulerJobDAO).getSchedulerJobsToAchieveStatus(refEq(UserInstanceStatus.TERMINATED), eq(false));
		verify(systemUserService, never()).create(USER);
		verify(exploratoryService, never()).terminate(any(), any());
		verifyNoMoreInteractions(schedulerJobDAO);
		verifyZeroInteractions(computationalService);
	}

	@Test
	public void executeTerminateComputationalJob() {
		when(schedulerJobDAO.getSchedulerJobsToAchieveStatus(any(UserInstanceStatus.class), anyBoolean()))
				.thenReturn(Collections.singletonList(new SchedulerJobData(USER, EXPLORATORY_NAME,
						COMPUTATIONAL_NAME, schedulerJobDTO)));
		when(systemUserService.create(anyString())).thenReturn(userInfo);
		doNothing().when(computationalService).terminateComputationalEnvironment(any(UserInfo.class), anyString(),
				anyString());

		schedulerJobService.executeTerminateResourceJob(true);

		verify(schedulerJobDAO).getSchedulerJobsToAchieveStatus(refEq(UserInstanceStatus.TERMINATED), eq(true));
		verify(systemUserService).create(USER);
		verify(computationalService).terminateComputationalEnvironment(userInfo, EXPLORATORY_NAME, COMPUTATIONAL_NAME);
		verifyNoMoreInteractions(schedulerJobDAO, systemUserService, computationalService);
		verifyZeroInteractions(exploratoryService);
	}

	@Test
	public void executeTerminateComputationalJobWhenSchedulerIsAbsent() {
		when(schedulerJobDAO.getSchedulerJobsToAchieveStatus(any(UserInstanceStatus.class), anyBoolean()))
				.thenReturn(Collections.emptyList());
		schedulerJobService.executeTerminateResourceJob(true);

		verify(schedulerJobDAO).getSchedulerJobsToAchieveStatus(refEq(UserInstanceStatus.TERMINATED), eq(true));
		verify(systemUserService, never()).create(USER);
		verify(computationalService, never()).terminateComputationalEnvironment(any(), any(), any());
		verifyNoMoreInteractions(schedulerJobDAO);
		verifyZeroInteractions(exploratoryService);
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
