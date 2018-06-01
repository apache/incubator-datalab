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

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.service.InfrastructureTemplateService;
import com.epam.dlab.dto.base.computational.FullComputationalTemplate;
import com.epam.dlab.dto.imagemetadata.ComputationalMetadataDTO;
import com.epam.dlab.dto.imagemetadata.ExploratoryMetadataDTO;
import com.epam.dlab.exceptions.DlabException;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Collections;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

public class InfrastructureTemplateResourceTest extends TestBase {

	private InfrastructureTemplateService infrastructureTemplateService = mock(InfrastructureTemplateService.class);

	@Rule
	public final ResourceTestRule resources =
			getResourceTestRuleInstance(new InfrastructureTemplateResource(infrastructureTemplateService));

	@Before
	public void setup() throws AuthenticationException {
		authSetup();
	}

	@Test
	public void getComputationalTemplates() {
		FullComputationalTemplate fullComputationalTemplate =
				new FullComputationalTemplate(new ComputationalMetadataDTO());
		when(infrastructureTemplateService.getComputationalTemplates(any(UserInfo.class)))
				.thenReturn(Collections.singletonList(fullComputationalTemplate));
		final Response response = resources.getJerseyTest()
				.target("/infrastructure_templates/computational_templates")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.get();

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(infrastructureTemplateService).getComputationalTemplates(getUserInfo());
		verifyNoMoreInteractions(infrastructureTemplateService);
	}

	@Test
	public void getComputationalTemplatesWithFailedAuth() throws AuthenticationException {
		authFailSetup();
		FullComputationalTemplate fullComputationalTemplate =
				new FullComputationalTemplate(new ComputationalMetadataDTO());
		when(infrastructureTemplateService.getComputationalTemplates(any(UserInfo.class)))
				.thenReturn(Collections.singletonList(fullComputationalTemplate));
		final Response response = resources.getJerseyTest()
				.target("/infrastructure_templates/computational_templates")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.get();

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(infrastructureTemplateService).getComputationalTemplates(getUserInfo());
		verifyNoMoreInteractions(infrastructureTemplateService);
	}

	@Test
	public void getComputationalTemplatesWithException() {
		doThrow(new DlabException("Could not load list of computational templates for user"))
				.when(infrastructureTemplateService).getComputationalTemplates(any(UserInfo.class));
		final Response response = resources.getJerseyTest()
				.target("/infrastructure_templates/computational_templates")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.get();

		assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(infrastructureTemplateService).getComputationalTemplates(getUserInfo());
		verifyNoMoreInteractions(infrastructureTemplateService);
	}

	@Test
	public void getExploratoryTemplates() {
		ExploratoryMetadataDTO exploratoryMetadataDTO =
				new ExploratoryMetadataDTO("someImageName");
		when(infrastructureTemplateService.getExploratoryTemplates(any(UserInfo.class)))
				.thenReturn(Collections.singletonList(exploratoryMetadataDTO));
		final Response response = resources.getJerseyTest()
				.target("/infrastructure_templates/exploratory_templates")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.get();

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertEquals(Collections.singletonList(exploratoryMetadataDTO),
				response.readEntity(new GenericType<List<ExploratoryMetadataDTO>>() {
				}));
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(infrastructureTemplateService).getExploratoryTemplates(getUserInfo());
		verifyNoMoreInteractions(infrastructureTemplateService);
	}

	@Test
	public void getExploratoryTemplatesWithFailedAuth() throws AuthenticationException {
		authFailSetup();
		ExploratoryMetadataDTO exploratoryMetadataDTO =
				new ExploratoryMetadataDTO("someImageName");
		when(infrastructureTemplateService.getExploratoryTemplates(any(UserInfo.class)))
				.thenReturn(Collections.singletonList(exploratoryMetadataDTO));
		final Response response = resources.getJerseyTest()
				.target("/infrastructure_templates/exploratory_templates")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.get();

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertEquals(Collections.singletonList(exploratoryMetadataDTO),
				response.readEntity(new GenericType<List<ExploratoryMetadataDTO>>() {
				}));
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(infrastructureTemplateService).getExploratoryTemplates(getUserInfo());
		verifyNoMoreInteractions(infrastructureTemplateService);
	}


	@Test
	public void getExploratoryTemplatesWithException() {
		doThrow(new DlabException("Could not load list of exploratory templates for user"))
				.when(infrastructureTemplateService).getExploratoryTemplates(any(UserInfo.class));
		final Response response = resources.getJerseyTest()
				.target("/infrastructure_templates/exploratory_templates")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.get();

		assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(infrastructureTemplateService).getExploratoryTemplates(getUserInfo());
		verifyNoMoreInteractions(infrastructureTemplateService);
	}
}
