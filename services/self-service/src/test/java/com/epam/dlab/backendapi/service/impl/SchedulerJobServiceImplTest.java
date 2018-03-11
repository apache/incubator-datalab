package com.epam.dlab.backendapi.service.impl;

import com.epam.dlab.UserInstanceStatus;
import com.epam.dlab.auth.SystemUserInfoServiceImpl;
import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.dao.SchedulerJobDAO;
import com.epam.dlab.backendapi.service.ExploratoryService;
import com.epam.dlab.dto.SchedulerJobDTO;
import com.epam.dlab.dto.UserInstanceDTO;
import com.epam.dlab.model.scheduler.SchedulerJobData;
import com.mongodb.client.result.UpdateResult;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class SchedulerJobServiceImplTest {

	private final String USER = "test";
	private final String EXPLORATORY_NAME = "explName";

	@Mock
	private SchedulerJobDAO schedulerJobDAO;
	@Mock
	private ExploratoryDAO exploratoryDAO;
	@Mock
	private ExploratoryService exploratoryService;
	@Mock
	private SystemUserInfoServiceImpl systemUserService;

	@InjectMocks
	private SchedulerJobServiceImpl schedulerJobService;

	@Test
	public void fetchSchedulerJobForUserAndExploratory() {
		when(exploratoryDAO.isExploratoryExist(anyString(), anyString())).thenReturn(true);

		SchedulerJobDTO expectedSchedulerJobDTO = new SchedulerJobDTO();
		when(schedulerJobDAO.fetchSingleSchedulerJobByUserAndExploratory(anyString(), anyString()))
				.thenReturn(Optional.of(expectedSchedulerJobDTO));

		SchedulerJobDTO actualSchedulerJobDto =
				schedulerJobService.fetchSchedulerJobForUserAndExploratory(USER, EXPLORATORY_NAME);
		assertNotNull(actualSchedulerJobDto);
		assertEquals(expectedSchedulerJobDTO, actualSchedulerJobDto);

		verify(exploratoryDAO).isExploratoryExist(USER, EXPLORATORY_NAME);
		verifyNoMoreInteractions(exploratoryDAO);

		verify(schedulerJobDAO).fetchSingleSchedulerJobByUserAndExploratory(USER, EXPLORATORY_NAME);
		verifyNoMoreInteractions(schedulerJobDAO);
	}

	@Test
	public void updateSchedulerDataForUserAndExploratory() {
		SchedulerJobDTO schedulerJobDTO = new SchedulerJobDTO();
		UserInstanceDTO userInstanceDTO = new UserInstanceDTO().withUser(USER).withExploratoryName(EXPLORATORY_NAME)
				.withStatus("running");
		when(exploratoryDAO.fetchExploratoryFields(anyString(), anyString())).thenReturn(userInstanceDTO);
		when(exploratoryDAO.updateSchedulerDataForUserAndExploratory(anyString(), anyString(),
				any(SchedulerJobDTO.class))).thenReturn(mock(UpdateResult.class));

		schedulerJobService.updateSchedulerDataForUserAndExploratory(USER, EXPLORATORY_NAME, schedulerJobDTO);

		verify(exploratoryDAO).fetchExploratoryFields(USER, EXPLORATORY_NAME);
		verify(exploratoryDAO).updateSchedulerDataForUserAndExploratory(USER, EXPLORATORY_NAME, schedulerJobDTO);
	}

	@Test
	public void executeStartExploratoryJob() {
		SchedulerJobDTO schedulerJobDTO = new SchedulerJobDTO();
		SchedulerJobData schedulerJobData = new SchedulerJobData(USER, EXPLORATORY_NAME, schedulerJobDTO);
		List<SchedulerJobData> jobDataList = Collections.singletonList(schedulerJobData);
		when(schedulerJobDAO.getSchedulerJobsToAchieveStatus(any(UserInstanceStatus.class), any(OffsetDateTime.class)))
				.thenReturn(jobDataList);
		UserInfo userInfo = new UserInfo(USER, "token");
		when(systemUserService.create(anyString())).thenReturn(userInfo);
		when(exploratoryService.start(any(UserInfo.class), anyString())).thenReturn("someUuid");

		schedulerJobService.executeStartExploratoryJob();

		verify(schedulerJobDAO).getSchedulerJobsToAchieveStatus(
				refEq(UserInstanceStatus.RUNNING), any(OffsetDateTime.class));
		verifyNoMoreInteractions(schedulerJobDAO);

		verify(systemUserService).create(USER);
		verifyNoMoreInteractions(systemUserService);

		verify(exploratoryService).start(userInfo, EXPLORATORY_NAME);
		verifyNoMoreInteractions(exploratoryService);
	}

	@Test
	public void executeStopExploratoryJob() {
		SchedulerJobDTO schedulerJobDTO = new SchedulerJobDTO();
		schedulerJobDTO.setStartTime(LocalTime.of(12, 15));
		schedulerJobDTO.setEndTime(LocalTime.of(18, 15));
		schedulerJobDTO.setTimeZoneOffset(ZoneOffset.of("+02:00"));
		SchedulerJobData schedulerJobData = new SchedulerJobData(USER, EXPLORATORY_NAME, schedulerJobDTO);
		List<SchedulerJobData> jobDataList = Collections.singletonList(schedulerJobData);
		when(schedulerJobDAO.getSchedulerJobsToAchieveStatus(any(UserInstanceStatus.class), any(OffsetDateTime.class)))
				.thenReturn(jobDataList);
		UserInfo userInfo = new UserInfo(USER, "token");
		when(systemUserService.create(anyString())).thenReturn(userInfo);
		when(exploratoryService.stop(any(UserInfo.class), anyString())).thenReturn("someUuid");

		schedulerJobService.executeStopExploratoryJob();

		verify(systemUserService).create(USER);
		verifyNoMoreInteractions(systemUserService);

		verify(exploratoryService).stop(userInfo, EXPLORATORY_NAME);
		verifyNoMoreInteractions(exploratoryService);
	}
}
