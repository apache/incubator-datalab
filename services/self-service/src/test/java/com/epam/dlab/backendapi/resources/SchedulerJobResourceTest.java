package com.epam.dlab.backendapi.resources;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.service.SchedulerJobService;
import com.epam.dlab.dto.SchedulerJobDTO;
import com.epam.dlab.exceptions.ResourceInappropriateStateException;
import com.epam.dlab.exceptions.ResourceNotFoundException;
import com.epam.dlab.rest.mappers.ResourceNotFoundExceptionMapper;
import io.dropwizard.auth.*;
import io.dropwizard.auth.oauth.OAuthCredentialAuthFilter;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.apache.http.HttpStatus;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class SchedulerJobResourceTest {

	private final String TOKEN = "TOKEN";
	private final String USER = "testUser";
	private final UserInfo userInfo = getUserInfo();
	@SuppressWarnings("unchecked")
	private Authenticator<String, UserInfo> authenticator = mock(Authenticator.class);
	@SuppressWarnings("unchecked")
	private Authorizer<UserInfo> authorizer = mock(Authorizer.class);
	private SchedulerJobService schedulerJobService = mock(SchedulerJobService.class);

	@Rule
	public final ResourceTestRule resources = ResourceTestRule.builder()
			.setTestContainerFactory(new GrizzlyWebTestContainerFactory())
			.addProvider(new AuthDynamicFeature(new OAuthCredentialAuthFilter.Builder<UserInfo>()
					.setAuthenticator(authenticator)
					.setAuthorizer(authorizer)
					.setRealm("SUPER SECRET STUFF")
					.setPrefix("Bearer")
					.buildAuthFilter()))
			.addProvider(new ResourceNotFoundExceptionMapper())
			.addProvider(RolesAllowedDynamicFeature.class)
			.addProvider(new AuthValueFactoryProvider.Binder<>(UserInfo.class))
			.addResource(new SchedulerJobResource(schedulerJobService))
			.build();

	@Test
	public void updateExploratoryScheduler() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
		doNothing().when(schedulerJobService)
				.updateExploratorySchedulerData(anyString(), anyString(), any(SchedulerJobDTO.class));
		final Response response = resources.getJerseyTest()
				.target("/infrastructure_provision/exploratory_environment/scheduler/explName")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.post(Entity.json(getSchedulerJobDTO()));

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertNull(response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(schedulerJobService).updateExploratorySchedulerData(USER.toLowerCase(), "explName", getSchedulerJobDTO
				());
		verifyNoMoreInteractions(schedulerJobService);
	}

	@Test
	public void updateExploratorySchedulerWithException() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
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

		verify(schedulerJobService).updateExploratorySchedulerData(USER.toLowerCase(), "explName", getSchedulerJobDTO
				());
		verifyNoMoreInteractions(schedulerJobService);
	}

	@Test
	public void upsertComputationalScheduler() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
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
	public void upsertComputationalSchedulerWithException() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
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
	public void fetchSchedulerJobForUserAndExploratory() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
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
	public void fetchSchedulerJobForUserAndExploratoryWithException() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
		doThrow(new ResourceNotFoundException("Scheduler job data not found for user with exploratory"))
				.when(schedulerJobService).fetchSchedulerJobForUserAndExploratory(anyString(), anyString());
		final Response response = resources.getJerseyTest()
				.target("/infrastructure_provision/exploratory_environment/scheduler/explName")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.get();

		assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatus());
		assertEquals(MediaType.TEXT_PLAIN, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(schedulerJobService).fetchSchedulerJobForUserAndExploratory(USER.toLowerCase(), "explName");
		verifyNoMoreInteractions(schedulerJobService);
	}

	@Test
	public void fetchSchedulerJobForComputationalResource() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
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
	public void fetchSchedulerJobForComputationalResourceWithException() throws AuthenticationException {
		when(authenticator.authenticate(TOKEN)).thenReturn(Optional.of(userInfo));
		when(authorizer.authorize(any(), any())).thenReturn(true);
		doThrow(new ResourceNotFoundException("Scheduler job data not found for user with exploratory with " +
				"computational resource")).when(schedulerJobService)
				.fetchSchedulerJobForComputationalResource(anyString(), anyString(), anyString());
		final Response response = resources.getJerseyTest()
				.target("/infrastructure_provision/exploratory_environment/scheduler/explName/compName")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.get();

		assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatus());
		assertEquals(MediaType.TEXT_PLAIN, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(schedulerJobService).fetchSchedulerJobForComputationalResource(USER.toLowerCase(),
				"explName", "compName");
		verifyNoMoreInteractions(schedulerJobService);
	}

	private UserInfo getUserInfo() {
		return new UserInfo(USER, TOKEN);
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
		schedulerJobDTO.setDaysRepeat(Arrays.asList(DayOfWeek.values()));
		schedulerJobDTO.setSyncStartRequired(false);
		return schedulerJobDTO;
	}
}
