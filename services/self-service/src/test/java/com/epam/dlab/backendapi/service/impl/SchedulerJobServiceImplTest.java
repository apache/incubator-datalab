package com.epam.dlab.backendapi.service.impl;

import com.epam.dlab.UserInstanceStatus;
import com.epam.dlab.auth.SystemUserInfoServiceImpl;
import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.dao.SchedulerJobDAO;
import com.epam.dlab.dto.SchedulerJobDTO;
import com.epam.dlab.dto.UserInstanceDTO;
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

import java.time.DayOfWeek;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.Optional;

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
	private SystemUserInfoServiceImpl systemUserService;
	@Mock
	private ExploratoryServiceImpl exploratoryService;

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
	public void updateSchedulerDataForUserAndExploratory() {
		userInstance.withStatus("running");
		when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString())).thenReturn(userInstance);
		when(exploratoryDAO.updateSchedulerDataForUserAndExploratory(anyString(), anyString(),
				any(SchedulerJobDTO.class))).thenReturn(mock(UpdateResult.class));

		schedulerJobService.updateSchedulerDataForUserAndExploratory(USER, EXPLORATORY_NAME, schedulerJobDTO);

		verify(exploratoryDAO).fetchExploratoryFields(USER, EXPLORATORY_NAME);
		verify(exploratoryDAO).updateSchedulerDataForUserAndExploratory(USER, EXPLORATORY_NAME, schedulerJobDTO);
		verifyNoMoreInteractions(exploratoryDAO);
	}

	@Test
	public void updateSchedulerDataForUserAndExploratoryWhenMethodFetchExploratoryFieldsThrowsException() {
		doThrow(new ResourceNotFoundException("Exploratory for user with name not found"))
				.when(exploratoryDAO).fetchExploratoryFields(anyString(), anyString());
		try {
			schedulerJobService.updateSchedulerDataForUserAndExploratory(USER, EXPLORATORY_NAME, schedulerJobDTO);
		} catch (ResourceNotFoundException e) {
			assertEquals("Exploratory for user with name not found", e.getMessage());
		}
		verify(exploratoryDAO).fetchExploratoryFields(USER, EXPLORATORY_NAME);
		verifyNoMoreInteractions(exploratoryDAO);
	}

	@Test
	public void updateSchedulerDataForUserAndExploratoryWithInapproprietaryStatus() {
		userInstance.withStatus("terminated");
		when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString())).thenReturn(userInstance);
		try {
			schedulerJobService.updateSchedulerDataForUserAndExploratory(USER, EXPLORATORY_NAME, schedulerJobDTO);
		} catch (ResourceInappropriateStateException e) {
			assertEquals("Can not create/update scheduler for user instance with status: terminated",
					e.getMessage());
		}
		verify(exploratoryDAO).fetchExploratoryFields(USER, EXPLORATORY_NAME);
		verifyNoMoreInteractions(exploratoryDAO);
	}

	@Test
	public void updateSchedulerDataForUserAndExploratoryWithEnrichingSchedulerJob() {
		userInstance.withStatus("running");
		when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString())).thenReturn(userInstance);
		when(exploratoryDAO.updateSchedulerDataForUserAndExploratory(anyString(), anyString(),
				any(SchedulerJobDTO.class))).thenReturn(mock(UpdateResult.class));

		assertTrue(schedulerJobDTO.getDaysRepeat().isEmpty());

		schedulerJobService.updateSchedulerDataForUserAndExploratory(USER, EXPLORATORY_NAME, schedulerJobDTO);

		assertArrayEquals(DayOfWeek.values(), schedulerJobDTO.getDaysRepeat().toArray());

		verify(exploratoryDAO).fetchExploratoryFields(USER, EXPLORATORY_NAME);
		verify(exploratoryDAO).updateSchedulerDataForUserAndExploratory(USER, EXPLORATORY_NAME, schedulerJobDTO);
		verifyNoMoreInteractions(exploratoryDAO);
	}

	@Test
	public void executeStartExploratoryJob() {
		when(schedulerJobDAO.getSchedulerJobsToAchieveStatus(any(UserInstanceStatus.class), any(OffsetDateTime.class),
				anyBoolean()))
				.thenReturn(Collections.singletonList(new SchedulerJobData(USER, EXPLORATORY_NAME, COMPUTATIONAL_NAME,
						schedulerJobDTO)));
		when(systemUserService.create(anyString())).thenReturn(userInfo);
		when(exploratoryService.start(any(UserInfo.class), anyString())).thenReturn("someUuid");

		schedulerJobService.executeStartResourceJob(false);

		verify(schedulerJobDAO).getSchedulerJobsToAchieveStatus(
				refEq(UserInstanceStatus.RUNNING), any(OffsetDateTime.class), eq(false));
		verify(systemUserService).create(USER);
		verify(exploratoryService).start(userInfo, EXPLORATORY_NAME);
		verifyNoMoreInteractions(schedulerJobDAO, systemUserService, exploratoryService);
	}

	@Test
	public void executeStartExploratoryJobWhenSchedulerIsAbsent() {
		when(schedulerJobDAO.getSchedulerJobsToAchieveStatus(any(UserInstanceStatus.class), any(OffsetDateTime.class),
				anyBoolean())).thenReturn(Collections.emptyList());
		schedulerJobService.executeStartResourceJob(false);
		verify(schedulerJobDAO).getSchedulerJobsToAchieveStatus(
				refEq(UserInstanceStatus.RUNNING), any(OffsetDateTime.class), eq(false));
		verify(systemUserService, never()).create(any());
		verify(exploratoryService, never()).start(any(), any());
		verifyNoMoreInteractions(schedulerJobDAO);
	}

	@Test
	public void executeStopExploratoryJob() {
		SchedulerJobDTO schedulerJobDTO = new SchedulerJobDTO();
		schedulerJobDTO.setStartTime(LocalTime.of(12, 15));
		schedulerJobDTO.setEndTime(LocalTime.of(18, 15));
		schedulerJobDTO.setTimeZoneOffset(ZoneOffset.of("+02:00"));

		when(schedulerJobDAO.getSchedulerJobsToAchieveStatus(any(UserInstanceStatus.class), any(OffsetDateTime.class),
				anyBoolean())).thenReturn(Collections.singletonList(new SchedulerJobData(USER, EXPLORATORY_NAME,
				COMPUTATIONAL_NAME, schedulerJobDTO)));
		when(systemUserService.create(anyString())).thenReturn(userInfo);
		when(exploratoryService.stop(any(UserInfo.class), anyString())).thenReturn("someUuid");

		schedulerJobService.executeStopResourceJob(false);

		verify(schedulerJobDAO, times(3)).getSchedulerJobsToAchieveStatus(
				refEq(UserInstanceStatus.STOPPED), any(OffsetDateTime.class), eq(false));
		verify(systemUserService).create(USER);
		verify(exploratoryService).stop(userInfo, EXPLORATORY_NAME);
		verifyNoMoreInteractions(schedulerJobDAO);
	}

	@Test
	public void executeStopExploratoryJobWhenSchedulerIsAbsent() {
		when(schedulerJobDAO.getSchedulerJobsToAchieveStatus(any(UserInstanceStatus.class), any(OffsetDateTime.class),
				anyBoolean())).thenReturn(Collections.emptyList());
		schedulerJobService.executeStopResourceJob(false);

		verify(schedulerJobDAO, times(3)).getSchedulerJobsToAchieveStatus(
				refEq(UserInstanceStatus.STOPPED), any(OffsetDateTime.class), eq(false));
		verify(systemUserService, never()).create(USER);
		verify(exploratoryService, never()).stop(userInfo, EXPLORATORY_NAME);
		verifyNoMoreInteractions(schedulerJobDAO);
	}

	private UserInfo getUserInfo() {
		return new UserInfo(USER, "token");
	}

	private SchedulerJobDTO getSchedulerJobDTO() {
		return new SchedulerJobDTO();
	}

	private UserInstanceDTO getUserInstanceDTO() {
		return new UserInstanceDTO().withUser(USER).withExploratoryName(EXPLORATORY_NAME);
	}
}
