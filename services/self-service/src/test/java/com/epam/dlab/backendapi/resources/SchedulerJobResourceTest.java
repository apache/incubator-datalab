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

package com.epam.dlab.backendapi.resources;

import com.epam.dlab.backendapi.service.SchedulerJobService;
import com.epam.dlab.dto.SchedulerJobDTO;
import com.epam.dlab.exceptions.ResourceInappropriateStateException;
import com.epam.dlab.exceptions.ResourceNotFoundException;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class SchedulerJobResourceTest extends TestBase {

	private SchedulerJobService schedulerJobService = mock(SchedulerJobService.class);

	@Rule
	public final ResourceTestRule resources =
			getResourceTestRuleInstance(new SchedulerJobResource(schedulerJobService));

	@Before
	public void setup() throws AuthenticationException {
		authSetup();
	}

	@Test
	public void updateExploratoryScheduler() {
		doNothing().when(schedulerJobService)
				.updateExploratorySchedulerData(anyString(), anyString(), any(SchedulerJobDTO.class));
		final Response response = resources.getJerseyTest()
				.target("/infrastructure_provision/exploratory_environment/scheduler/explName")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.post(Entity.json(getSchedulerJobDTO()));

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertNull(response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(schedulerJobService).updateExploratorySchedulerData(USER.toLowerCase(),
				"explName", getSchedulerJobDTO());
		verifyNoMoreInteractions(schedulerJobService);
	}

	@Test
	public void updateExploratorySchedulerWithFailedAuth() throws AuthenticationException {
		authFailSetup();
		doNothing().when(schedulerJobService)
				.updateExploratorySchedulerData(anyString(), anyString(), any(SchedulerJobDTO.class));
		final Response response = resources.getJerseyTest()
				.target("/infrastructure_provision/exploratory_environment/scheduler/explName")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.post(Entity.json(getSchedulerJobDTO()));

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertNull(response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(schedulerJobService).updateExploratorySchedulerData(USER.toLowerCase(),
				"explName", getSchedulerJobDTO());
		verifyNoMoreInteractions(schedulerJobService);
	}

	@Test
	public void updateExploratorySchedulerWithException() {
		doThrow(new ResourceInappropriateStateException("Can't create/update scheduler for user instance with status"))
				.when(schedulerJobService).updateExploratorySchedulerData(anyString(), anyString(),
				any(SchedulerJobDTO.class));
		final Response response = resources.getJerseyTest()
				.target("/infrastructure_provision/exploratory_environment/scheduler/explName")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.post(Entity.json(getSchedulerJobDTO()));

		assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(schedulerJobService).updateExploratorySchedulerData(USER.toLowerCase(), "explName",
				getSchedulerJobDTO());
		verifyNoMoreInteractions(schedulerJobService);
	}

	@Test
	public void upsertComputationalScheduler() {
		doNothing().when(schedulerJobService)
				.updateComputationalSchedulerData(anyString(), anyString(), anyString(), any(SchedulerJobDTO.class));
		final Response response = resources.getJerseyTest()
				.target("/infrastructure_provision/exploratory_environment/scheduler/explName/compName")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.post(Entity.json(getSchedulerJobDTO()));

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertNull(response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(schedulerJobService).updateComputationalSchedulerData(USER.toLowerCase(), "explName",
				"compName", getSchedulerJobDTO());
		verifyNoMoreInteractions(schedulerJobService);
	}

	@Test
	public void upsertComputationalSchedulerWithFailedAuth() throws AuthenticationException {
		authFailSetup();
		doNothing().when(schedulerJobService)
				.updateComputationalSchedulerData(anyString(), anyString(), anyString(), any(SchedulerJobDTO.class));
		final Response response = resources.getJerseyTest()
				.target("/infrastructure_provision/exploratory_environment/scheduler/explName/compName")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.post(Entity.json(getSchedulerJobDTO()));

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertNull(response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(schedulerJobService).updateComputationalSchedulerData(USER.toLowerCase(), "explName",
				"compName", getSchedulerJobDTO());
		verifyNoMoreInteractions(schedulerJobService);
	}

	@Test
	public void upsertComputationalSchedulerWithException() {
		doThrow(new ResourceInappropriateStateException("Can't create/update scheduler for user instance with status"))
				.when(schedulerJobService).updateComputationalSchedulerData(anyString(), anyString(), anyString(),
				any(SchedulerJobDTO.class));
		final Response response = resources.getJerseyTest()
				.target("/infrastructure_provision/exploratory_environment/scheduler/explName/compName")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.post(Entity.json(getSchedulerJobDTO()));

		assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(schedulerJobService).updateComputationalSchedulerData(USER.toLowerCase(), "explName",
				"compName", getSchedulerJobDTO());
		verifyNoMoreInteractions(schedulerJobService);
	}

	@Test
	public void fetchSchedulerJobForUserAndExploratory() {
		when(schedulerJobService.fetchSchedulerJobForUserAndExploratory(anyString(), anyString()))
				.thenReturn(getSchedulerJobDTO());
		final Response response = resources.getJerseyTest()
				.target("/infrastructure_provision/exploratory_environment/scheduler/explName")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.get();

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertEquals(getSchedulerJobDTO(), response.readEntity(SchedulerJobDTO.class));
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(schedulerJobService).fetchSchedulerJobForUserAndExploratory(USER.toLowerCase(), "explName");
		verifyNoMoreInteractions(schedulerJobService);
	}

	@Test
	public void fetchSchedulerJobForUserAndExploratoryWithFailedAuth() throws AuthenticationException {
		authFailSetup();
		when(schedulerJobService.fetchSchedulerJobForUserAndExploratory(anyString(), anyString()))
				.thenReturn(getSchedulerJobDTO());
		final Response response = resources.getJerseyTest()
				.target("/infrastructure_provision/exploratory_environment/scheduler/explName")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.get();

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertEquals(getSchedulerJobDTO(), response.readEntity(SchedulerJobDTO.class));
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(schedulerJobService).fetchSchedulerJobForUserAndExploratory(USER.toLowerCase(), "explName");
		verifyNoMoreInteractions(schedulerJobService);
	}


	@Test
	public void fetchSchedulerJobForUserAndExploratoryWithException() {
		doThrow(new ResourceNotFoundException("Scheduler job data not found for user with exploratory"))
				.when(schedulerJobService).fetchSchedulerJobForUserAndExploratory(anyString(), anyString());
		final Response response = resources.getJerseyTest()
				.target("/infrastructure_provision/exploratory_environment/scheduler/explName")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.get();

		assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(schedulerJobService).fetchSchedulerJobForUserAndExploratory(USER.toLowerCase(), "explName");
		verifyNoMoreInteractions(schedulerJobService);
	}

	@Test
	public void fetchSchedulerJobForComputationalResource() {
		when(schedulerJobService.fetchSchedulerJobForComputationalResource(anyString(), anyString(), anyString()))
				.thenReturn(getSchedulerJobDTO());
		final Response response = resources.getJerseyTest()
				.target("/infrastructure_provision/exploratory_environment/scheduler/explName/compName")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.get();

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertEquals(getSchedulerJobDTO(), response.readEntity(SchedulerJobDTO.class));
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(schedulerJobService).fetchSchedulerJobForComputationalResource(USER.toLowerCase(),
				"explName", "compName");
		verifyNoMoreInteractions(schedulerJobService);
	}

	@Test
	public void fetchSchedulerJobForComputationalResourceWithFailedAuth() throws AuthenticationException {
		authFailSetup();
		when(schedulerJobService.fetchSchedulerJobForComputationalResource(anyString(), anyString(), anyString()))
				.thenReturn(getSchedulerJobDTO());
		final Response response = resources.getJerseyTest()
				.target("/infrastructure_provision/exploratory_environment/scheduler/explName/compName")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.get();

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertEquals(getSchedulerJobDTO(), response.readEntity(SchedulerJobDTO.class));
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(schedulerJobService).fetchSchedulerJobForComputationalResource(USER.toLowerCase(),
				"explName", "compName");
		verifyNoMoreInteractions(schedulerJobService);
	}

	@Test
	public void fetchSchedulerJobForComputationalResourceWithException() {
		doThrow(new ResourceNotFoundException("Scheduler job data not found for user with exploratory with " +
				"computational resource")).when(schedulerJobService)
				.fetchSchedulerJobForComputationalResource(anyString(), anyString(), anyString());
		final Response response = resources.getJerseyTest()
				.target("/infrastructure_provision/exploratory_environment/scheduler/explName/compName")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.get();

		assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(schedulerJobService).fetchSchedulerJobForComputationalResource(USER.toLowerCase(),
				"explName", "compName");
		verifyNoMoreInteractions(schedulerJobService);
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
}
