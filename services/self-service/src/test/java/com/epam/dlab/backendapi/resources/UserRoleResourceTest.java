package com.epam.dlab.backendapi.resources;

import com.epam.dlab.backendapi.resources.dto.UserRoleDto;
import com.epam.dlab.backendapi.service.UserRoleService;
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
import java.util.Collections;
import java.util.List;

import static java.util.Collections.singleton;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

public class UserRoleResourceTest extends TestBase {


	private static final String USER = "user";
	private static final String ROLE_ID = "id";

	private UserRoleService rolesService = mock(UserRoleService.class);

	@Before
	public void setup() throws AuthenticationException {
		authSetup();
	}

	@Rule
	public final ResourceTestRule resources =
			getResourceTestRuleInstance(new UserRoleResource(rolesService));


	@Test
	public void getRoles() {
		when(rolesService.getUserRoles()).thenReturn(Collections.singletonList(getUserRole()));

		final Response response = resources.getJerseyTest()
				.target("/role")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.get();

		final List<UserRoleDto> actualRoles = response.readEntity(new GenericType<List<UserRoleDto>>() {
		});

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertEquals(ROLE_ID, actualRoles.get(0).getId());
		assertEquals(singleton(USER), actualRoles.get(0).getUsers());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(rolesService).getUserRoles();
		verifyNoMoreInteractions(rolesService);
	}

	@Test
	public void createRole() {

		final Response response = resources.getJerseyTest()
				.target("/role")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.post(Entity.json(getUserRole()));

		assertEquals(HttpStatus.SC_OK, response.getStatus());

		verify(rolesService).createRole(refEq(getUserRole()));
		verifyNoMoreInteractions(rolesService);
	}

	private UserRoleDto getUserRole() {
		final UserRoleDto userRoleDto = new UserRoleDto();
		userRoleDto.setId(ROLE_ID);
		userRoleDto.setUsers(singleton(USER));
		return userRoleDto;
	}

}