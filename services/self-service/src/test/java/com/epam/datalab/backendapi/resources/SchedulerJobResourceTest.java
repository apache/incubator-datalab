/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.epam.datalab.backendapi.resources;

import com.epam.datalab.auth.UserInfo;
import com.epam.datalab.backendapi.service.SchedulerJobService;
import com.epam.datalab.dto.SchedulerJobDTO;
import com.epam.datalab.exceptions.ResourceInappropriateStateException;
import com.epam.datalab.exceptions.ResourceNotFoundException;
import com.epam.datalab.model.scheduler.SchedulerJobData;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

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
                .updateExploratorySchedulerData(any(UserInfo.class), anyString(), anyString(), any(SchedulerJobDTO.class));
        final Response response = resources.getJerseyTest()
                .target("/infrastructure_provision/exploratory_environment/scheduler/projectName/explName")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .post(Entity.json(getSchedulerJobDTO()));

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertNull(response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(schedulerJobService).updateExploratorySchedulerData(getUserInfo(), "projectName",
                "explName", getSchedulerJobDTO());
        verifyNoMoreInteractions(schedulerJobService);
    }

    @Test
    public void updateExploratorySchedulerWithFailedAuth() throws AuthenticationException {
        authFailSetup();
        doNothing().when(schedulerJobService)
                .updateExploratorySchedulerData(any(UserInfo.class), anyString(), anyString(), any(SchedulerJobDTO.class));
        final Response response = resources.getJerseyTest()
                .target("/infrastructure_provision/exploratory_environment/scheduler/projectName/explName")
                .request()
                .header("Authorization", String.join(" ", "Bearer", TOKEN))
                .post(Entity.json(getSchedulerJobDTO()));

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertNull(response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(schedulerJobService).updateExploratorySchedulerData(getUserInfo(), "projectName",
                "explName", getSchedulerJobDTO());
        verifyNoMoreInteractions(schedulerJobService);
    }

    @Test
    public void updateExploratorySchedulerWithException() {
        doThrow(new ResourceInappropriateStateException("Can't create/update scheduler for user instance with status"))
                .when(schedulerJobService).updateExploratorySchedulerData(any(UserInfo.class), anyString(), anyString(),
                any(SchedulerJobDTO.class));
        final Response response = resources.getJerseyTest()
                .target("/infrastructure_provision/exploratory_environment/scheduler/projectName/explName")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .post(Entity.json(getSchedulerJobDTO()));

        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(schedulerJobService).updateExploratorySchedulerData(getUserInfo(), "projectName",
                "explName", getSchedulerJobDTO());
        verifyNoMoreInteractions(schedulerJobService);
    }

    @Test
    public void upsertComputationalScheduler() {
        doNothing().when(schedulerJobService)
                .updateComputationalSchedulerData(any(UserInfo.class), anyString(), anyString(), anyString(), any(SchedulerJobDTO.class));
        final Response response = resources.getJerseyTest()
                .target("/infrastructure_provision/exploratory_environment/scheduler/projectName/explName/compName")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .post(Entity.json(getSchedulerJobDTO()));

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertNull(response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(schedulerJobService).updateComputationalSchedulerData(getUserInfo(), "projectName",
                "explName", "compName", getSchedulerJobDTO());
        verifyNoMoreInteractions(schedulerJobService);
    }

    @Test
    public void upsertComputationalSchedulerWithFailedAuth() throws AuthenticationException {
        authFailSetup();
        doNothing().when(schedulerJobService)
                .updateComputationalSchedulerData(any(UserInfo.class), anyString(), anyString(), anyString(), any(SchedulerJobDTO.class));
        final Response response = resources.getJerseyTest()
                .target("/infrastructure_provision/exploratory_environment/scheduler/projectName/explName/compName")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .post(Entity.json(getSchedulerJobDTO()));

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertNull(response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(schedulerJobService).updateComputationalSchedulerData(getUserInfo(), "projectName",
                "explName", "compName", getSchedulerJobDTO());
        verifyNoMoreInteractions(schedulerJobService);
    }

    @Test
    public void upsertComputationalSchedulerWithException() {
        doThrow(new ResourceInappropriateStateException("Can't create/update scheduler for user instance with status"))
                .when(schedulerJobService).updateComputationalSchedulerData(any(UserInfo.class), anyString(), anyString(),
                anyString(), any(SchedulerJobDTO.class));
        final Response response = resources.getJerseyTest()
                .target("/infrastructure_provision/exploratory_environment/scheduler/projectName/explName/compName")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .post(Entity.json(getSchedulerJobDTO()));

        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(schedulerJobService).updateComputationalSchedulerData(getUserInfo(), "projectName",
                "explName", "compName", getSchedulerJobDTO());
        verifyNoMoreInteractions(schedulerJobService);
    }

    @Test
    public void fetchSchedulerJobForUserAndExploratory() {
        when(schedulerJobService.fetchSchedulerJobForUserAndExploratory(anyString(), anyString(), anyString()))
                .thenReturn(getSchedulerJobDTO());
        final Response response = resources.getJerseyTest()
                .target("/infrastructure_provision/exploratory_environment/scheduler/projectName/explName")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .get();

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertEquals(getSchedulerJobDTO(), response.readEntity(SchedulerJobDTO.class));
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(schedulerJobService).fetchSchedulerJobForUserAndExploratory(USER.toLowerCase(), "projectName", "explName");
        verifyNoMoreInteractions(schedulerJobService);
    }

    @Test
    public void fetchSchedulerJobForUserAndExploratoryWithFailedAuth() throws AuthenticationException {
        authFailSetup();
        when(schedulerJobService.fetchSchedulerJobForUserAndExploratory(anyString(), anyString(), anyString()))
                .thenReturn(getSchedulerJobDTO());
        final Response response = resources.getJerseyTest()
                .target("/infrastructure_provision/exploratory_environment/scheduler/projectName/explName")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .get();

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertEquals(getSchedulerJobDTO(), response.readEntity(SchedulerJobDTO.class));
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(schedulerJobService).fetchSchedulerJobForUserAndExploratory(USER.toLowerCase(), "projectName", "explName");
        verifyNoMoreInteractions(schedulerJobService);
    }


    @Test
    public void fetchSchedulerJobForUserAndExploratoryWithException() {
        doThrow(new ResourceNotFoundException("Scheduler job data not found for user with exploratory"))
                .when(schedulerJobService).fetchSchedulerJobForUserAndExploratory(anyString(), anyString(), anyString());
        final Response response = resources.getJerseyTest()
                .target("/infrastructure_provision/exploratory_environment/scheduler/projectName/explName")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .get();

        assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(schedulerJobService).fetchSchedulerJobForUserAndExploratory(USER.toLowerCase(), "projectName", "explName");
        verifyNoMoreInteractions(schedulerJobService);
    }

    @Test
    public void fetchSchedulerJobForComputationalResource() {
        when(schedulerJobService.fetchSchedulerJobForComputationalResource(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(getSchedulerJobDTO());
        final Response response = resources.getJerseyTest()
                .target("/infrastructure_provision/exploratory_environment/scheduler/projectName/explName/compName")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .get();

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertEquals(getSchedulerJobDTO(), response.readEntity(SchedulerJobDTO.class));
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(schedulerJobService).fetchSchedulerJobForComputationalResource(USER.toLowerCase(), "projectName",
                "explName", "compName");
        verifyNoMoreInteractions(schedulerJobService);
    }

    @Test
    public void fetchSchedulerJobForComputationalResourceWithFailedAuth() throws AuthenticationException {
        authFailSetup();
        when(schedulerJobService.fetchSchedulerJobForComputationalResource(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(getSchedulerJobDTO());
        final Response response = resources.getJerseyTest()
                .target("/infrastructure_provision/exploratory_environment/scheduler/projectName/explName/compName")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .get();

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertEquals(getSchedulerJobDTO(), response.readEntity(SchedulerJobDTO.class));
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(schedulerJobService).fetchSchedulerJobForComputationalResource(USER.toLowerCase(), "projectName",
                "explName", "compName");
        verifyNoMoreInteractions(schedulerJobService);
    }

    @Test
    public void fetchSchedulerJobForComputationalResourceWithException() {
        doThrow(new ResourceNotFoundException("Scheduler job data not found for user with exploratory with " +
                "computational resource")).when(schedulerJobService)
                .fetchSchedulerJobForComputationalResource(anyString(), anyString(), anyString(), anyString());
        final Response response = resources.getJerseyTest()
                .target("/infrastructure_provision/exploratory_environment/scheduler/projectName/explName/compName")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .get();

        assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(schedulerJobService).fetchSchedulerJobForComputationalResource(USER.toLowerCase(), "projectName",
                "explName", "compName");
        verifyNoMoreInteractions(schedulerJobService);
    }

    @Test
    public void testGetActiveSchedulers() {
        when(schedulerJobService.getActiveSchedulers(anyString(), anyLong()))
                .thenReturn(Collections.singletonList(new SchedulerJobData(USER, "exploratoryName", null,
                        "project", getSchedulerJobDTO())));
        final long minuteOffset = 10L;
        final Response response = resources.getJerseyTest()
                .target("/infrastructure_provision/exploratory_environment/scheduler/active")
                .queryParam("minuteOffset", minuteOffset)
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .get();
        final List<SchedulerJobData> activeSchedulers = response.readEntity(new GenericType<List<SchedulerJobData>>() {
        });

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertEquals(1, activeSchedulers.size());
        assertEquals(Collections.singletonList(new SchedulerJobData(USER, "exploratoryName", null,
                "project", getSchedulerJobDTO())), activeSchedulers);
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(schedulerJobService).getActiveSchedulers(USER.toLowerCase(), minuteOffset);
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
