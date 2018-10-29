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
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.resources.dto.ExploratoryImageCreateFormDTO;
import com.epam.dlab.backendapi.resources.dto.ImageInfoRecord;
import com.epam.dlab.backendapi.service.ImageExploratoryService;
import com.epam.dlab.dto.exploratory.ImageStatus;
import com.epam.dlab.exceptions.ResourceAlreadyExistException;
import com.epam.dlab.exceptions.ResourceNotFoundException;
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

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.*;

public class ImageExploratoryResourceTest extends TestBase {

	private ImageExploratoryService imageExploratoryService = mock(ImageExploratoryService.class);
	private RequestId requestId = mock(RequestId.class);

	@Rule
	public final ResourceTestRule resources =
			getResourceTestRuleInstance(new ImageExploratoryResource(imageExploratoryService, requestId));

	@Before
	public void setup() throws AuthenticationException {
		authSetup();
	}

	@Test
	public void createImage() {
		when(imageExploratoryService.createImage(any(UserInfo.class), anyString(), anyString(), anyString()))
				.thenReturn("someUuid");
		when(requestId.put(anyString(), anyString())).thenReturn("someUuid");
		final Response response = resources.getJerseyTest()
				.target("/infrastructure_provision/exploratory_environment/image")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.post(Entity.json(getExploratoryImageCreateFormDTO()));

		assertEquals(HttpStatus.SC_ACCEPTED, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(imageExploratoryService).createImage(getUserInfo(), "someNotebookName",
				"someImageName", "someDescription");
		verify(requestId).put(USER.toLowerCase(), "someUuid");
		verifyNoMoreInteractions(imageExploratoryService, requestId);
	}

	@Test
	public void createImageWithFailedAuth() throws AuthenticationException {
		authFailSetup();
		when(imageExploratoryService.createImage(any(UserInfo.class), anyString(), anyString(), anyString()))
				.thenReturn("someUuid");
		when(requestId.put(anyString(), anyString())).thenReturn("someUuid");
		final Response response = resources.getJerseyTest()
				.target("/infrastructure_provision/exploratory_environment/image")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.post(Entity.json(getExploratoryImageCreateFormDTO()));

		assertEquals(HttpStatus.SC_ACCEPTED, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(imageExploratoryService).createImage(getUserInfo(), "someNotebookName",
				"someImageName", "someDescription");
		verify(requestId).put(USER.toLowerCase(), "someUuid");
		verifyNoMoreInteractions(imageExploratoryService, requestId);
	}

	@Test
	public void createImageWithException() {
		doThrow(new ResourceAlreadyExistException("Image with name is already exist"))
				.when(imageExploratoryService).createImage(any(UserInfo.class), anyString(), anyString(), anyString());
		final Response response = resources.getJerseyTest()
				.target("/infrastructure_provision/exploratory_environment/image")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.post(Entity.json(getExploratoryImageCreateFormDTO()));

		assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(imageExploratoryService).createImage(getUserInfo(), "someNotebookName",
				"someImageName", "someDescription");
		verifyNoMoreInteractions(imageExploratoryService);
		verifyZeroInteractions(requestId);
	}

	@Test
	public void getImages() {
		when(imageExploratoryService.getNotFailedImages(anyString(), anyString()))
				.thenReturn(getImageList());
		final Response response = resources.getJerseyTest()
				.target("/infrastructure_provision/exploratory_environment/image")
				.queryParam("docker_image", "someDockerImage")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.get();

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertEquals(getImageList(), response.readEntity(new GenericType<List<ImageInfoRecord>>() {
		}));
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(imageExploratoryService).getNotFailedImages(USER.toLowerCase(), "someDockerImage");
		verifyNoMoreInteractions(imageExploratoryService);
	}

	@Test
	public void getImagesWithFailedAuth() throws AuthenticationException {
		authFailSetup();
		when(imageExploratoryService.getNotFailedImages(anyString(), anyString()))
				.thenReturn(getImageList());
		final Response response = resources.getJerseyTest()
				.target("/infrastructure_provision/exploratory_environment/image")
				.queryParam("docker_image", "someDockerImage")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.get();

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertEquals(getImageList(), response.readEntity(new GenericType<List<ImageInfoRecord>>() {
		}));
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(imageExploratoryService).getNotFailedImages(USER.toLowerCase(), "someDockerImage");
		verifyNoMoreInteractions(imageExploratoryService);
	}

	@Test
	public void getImage() {
		when(imageExploratoryService.getImage(anyString(), anyString()))
				.thenReturn(getImageList().get(0));
		final Response response = resources.getJerseyTest()
				.target("/infrastructure_provision/exploratory_environment/image/someName")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.get();

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertEquals(getImageList().get(0), response.readEntity(ImageInfoRecord.class));
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(imageExploratoryService).getImage(USER.toLowerCase(), "someName");
		verifyNoMoreInteractions(imageExploratoryService);
	}

	@Test
	public void getImageWithFailedAuth() throws AuthenticationException {
		authFailSetup();
		when(imageExploratoryService.getImage(anyString(), anyString()))
				.thenReturn(getImageList().get(0));
		final Response response = resources.getJerseyTest()
				.target("/infrastructure_provision/exploratory_environment/image/someName")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.get();

		assertEquals(HttpStatus.SC_OK, response.getStatus());
		assertEquals(getImageList().get(0), response.readEntity(ImageInfoRecord.class));
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(imageExploratoryService).getImage(USER.toLowerCase(), "someName");
		verifyNoMoreInteractions(imageExploratoryService);
	}

	@Test
	public void getImageWithException() {
		doThrow(new ResourceNotFoundException("Image with name was not found for user"))
				.when(imageExploratoryService).getImage(anyString(), anyString());
		final Response response = resources.getJerseyTest()
				.target("/infrastructure_provision/exploratory_environment/image/someName")
				.request()
				.header("Authorization", "Bearer " + TOKEN)
				.get();

		assertEquals(HttpStatus.SC_NOT_FOUND, response.getStatus());
		assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

		verify(imageExploratoryService).getImage(USER.toLowerCase(), "someName");
		verifyNoMoreInteractions(imageExploratoryService);
	}

	private ExploratoryImageCreateFormDTO getExploratoryImageCreateFormDTO() {
		ExploratoryImageCreateFormDTO eicfDto = new ExploratoryImageCreateFormDTO("someImageName", "someDescription");
		eicfDto.setNotebookName("someNotebookName");
		return eicfDto;
	}

	private List<ImageInfoRecord> getImageList() {
		ImageInfoRecord imageInfoRecord = new ImageInfoRecord("someName", "someDescription", "someApp",
				"someFullName", ImageStatus.CREATED);
		return Collections.singletonList(imageInfoRecord);
	}
}
