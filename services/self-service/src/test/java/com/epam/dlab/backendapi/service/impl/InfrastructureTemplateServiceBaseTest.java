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

package com.epam.dlab.backendapi.service.impl;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.ProjectDAO;
import com.epam.dlab.backendapi.dao.SettingsDAO;
import com.epam.dlab.backendapi.domain.ProjectDTO;
import com.epam.dlab.dto.base.computational.FullComputationalTemplate;
import com.epam.dlab.dto.imagemetadata.ComputationalMetadataDTO;
import com.epam.dlab.dto.imagemetadata.ComputationalResourceShapeDto;
import com.epam.dlab.dto.imagemetadata.ExploratoryMetadataDTO;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.client.RESTService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.lang.reflect.Field;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class InfrastructureTemplateServiceBaseTest {

	@Mock
	private SettingsDAO settingsDAO;
	@Mock
	private RESTService provisioningService;
	@Mock
	private ProjectDAO projectDAO;

	@InjectMocks
	private InfrastructureTemplateServiceBaseChild infrastructureTemplateServiceBaseChild =
			new InfrastructureTemplateServiceBaseChild();

	@Test
	public void getExploratoryTemplates() {
		ExploratoryMetadataDTO emDto1 = new ExploratoryMetadataDTO("someImage1");
		HashMap<String, List<ComputationalResourceShapeDto>> shapes1 = new HashMap<>();
		shapes1.put("Memory optimized", Arrays.asList(
				new ComputationalResourceShapeDto("Standard_E4s_v3", "someSize", "someDescription",
						"someRam", 2),
				new ComputationalResourceShapeDto("Standard_E32s_v3", "someSize2", "someDescription2",
						"someRam2", 5)));
		emDto1.setExploratoryEnvironmentShapes(shapes1);

		ExploratoryMetadataDTO emDto2 = new ExploratoryMetadataDTO("someImage2");
		HashMap<String, List<ComputationalResourceShapeDto>> shapes2 = new HashMap<>();
		shapes2.put("Compute optimized", Arrays.asList(
				new ComputationalResourceShapeDto("Standard_F2s", "someSize", "someDescription",
						"someRam", 3),
				new ComputationalResourceShapeDto("Standard_F16s", "someSize2", "someDescription2",
						"someRam2", 6)));
		emDto2.setExploratoryEnvironmentShapes(shapes2);
		List<ExploratoryMetadataDTO> expectedEmdDtoList = Arrays.asList(emDto1, emDto2);
		when(projectDAO.get(anyString())).thenReturn(Optional.of(new ProjectDTO("project", Collections.emptySet(),
				Collections.singleton("project"), null, null, null)));
		when(provisioningService.get(anyString(), anyString(), any())).thenReturn(expectedEmdDtoList.toArray());
		when(settingsDAO.getConfOsFamily()).thenReturn("someConfOsFamily");

		UserInfo userInfo = new UserInfo("test", "token");
		List<ExploratoryMetadataDTO> actualEmdDtoList =
				infrastructureTemplateServiceBaseChild.getExploratoryTemplates(userInfo, "project");
		assertNotNull(actualEmdDtoList);
		assertEquals(expectedEmdDtoList, actualEmdDtoList);

		verify(provisioningService).get("docker/exploratory", "token", ExploratoryMetadataDTO[].class);
		verify(settingsDAO, times(2)).getConfOsFamily();
		verifyNoMoreInteractions(provisioningService, settingsDAO);
	}

	@Test
	public void getExploratoryTemplatesWithException() {
		doThrow(new DlabException("Could not load list of exploratory templates for user"))
				.when(provisioningService).get(anyString(), anyString(), any());

		UserInfo userInfo = new UserInfo("test", "token");
		try {
			infrastructureTemplateServiceBaseChild.getExploratoryTemplates(userInfo, "project");
		} catch (DlabException e) {
			assertEquals("Could not load list of exploratory templates for user", e.getMessage());
		}
		verify(provisioningService).get("docker/exploratory", "token", ExploratoryMetadataDTO[].class);
		verifyNoMoreInteractions(provisioningService);
	}

	@Test
	public void getComputationalTemplates() throws NoSuchFieldException, IllegalAccessException {

		final ComputationalMetadataDTO computationalMetadataDTO = new ComputationalMetadataDTO("dataengine-service");
		computationalMetadataDTO.setComputationResourceShapes(Collections.emptyMap());
		List<ComputationalMetadataDTO> expectedCmdDtoList = Collections.singletonList(
				computationalMetadataDTO
		);
		when(projectDAO.get(anyString())).thenReturn(Optional.of(new ProjectDTO("project", Collections.emptySet(),
				Collections.singleton("project"), null, null, null)));
		when(provisioningService.get(anyString(), anyString(), any())).thenReturn(expectedCmdDtoList.toArray(new ComputationalMetadataDTO[]{}));

		List<FullComputationalTemplate> expectedFullCmdDtoList = expectedCmdDtoList.stream()
				.map(e -> infrastructureTemplateServiceBaseChild.getCloudFullComputationalTemplate(e))
				.collect(Collectors.toList());

		UserInfo userInfo = new UserInfo("test", "token");
		List<FullComputationalTemplate> actualFullCmdDtoList =
				infrastructureTemplateServiceBaseChild.getComputationalTemplates(userInfo, "project");
		assertNotNull(actualFullCmdDtoList);
		assertEquals(expectedFullCmdDtoList.size(), actualFullCmdDtoList.size());
		for (int i = 0; i < expectedFullCmdDtoList.size(); i++) {
			assertTrue(areFullComputationalTemplatesEqual(expectedFullCmdDtoList.get(i), actualFullCmdDtoList.get(i)));
		}

		verify(provisioningService).get("docker/computational", "token", ComputationalMetadataDTO[].class);
		verifyNoMoreInteractions(provisioningService);
	}

	@Test
	public void getComputationalTemplatesWhenMethodThrowsException() {
		doThrow(new DlabException("Could not load list of computational templates for user"))
				.when(provisioningService).get(anyString(), anyString(), any());

		UserInfo userInfo = new UserInfo("test", "token");
		try {
			infrastructureTemplateServiceBaseChild.getComputationalTemplates(userInfo, "project");
		} catch (DlabException e) {
			assertEquals("Could not load list of computational templates for user", e.getMessage());
		}
		verify(provisioningService).get("docker/computational", "token", ComputationalMetadataDTO[].class);
		verifyNoMoreInteractions(provisioningService);
	}

	@Test
	public void getComputationalTemplatesWithInapproprietaryImageName() {
		final ComputationalMetadataDTO computationalMetadataDTO = new ComputationalMetadataDTO("dataengine-service");
		computationalMetadataDTO.setComputationResourceShapes(Collections.emptyMap());
		List<ComputationalMetadataDTO> expectedCmdDtoList = Collections.singletonList(computationalMetadataDTO);
		when(provisioningService.get(anyString(), anyString(), any())).thenReturn(expectedCmdDtoList.toArray(new ComputationalMetadataDTO[]{}));
		when(projectDAO.get(anyString())).thenReturn(Optional.of(new ProjectDTO("project", Collections.emptySet(),
				Collections.singleton("project"), null, null,null)));

		UserInfo userInfo = new UserInfo("test", "token");
		try {
			infrastructureTemplateServiceBaseChild.getComputationalTemplates(userInfo, "project");
		} catch (IllegalArgumentException e) {
			assertEquals("Unknown data engine null", e.getMessage());
		}
		verify(provisioningService).get("docker/computational", "token", ComputationalMetadataDTO[].class);
		verifyNoMoreInteractions(provisioningService);
	}

	private boolean areFullComputationalTemplatesEqual(FullComputationalTemplate object1,
													   FullComputationalTemplate object2) throws NoSuchFieldException,
			IllegalAccessException {
		String project = "";//TODO CHANGEIT
		Field computationalMetadataDTO1 = object1.getClass().getDeclaredField("computationalMetadataDTO");
		computationalMetadataDTO1.setAccessible(true);
		Field computationalMetadataDTO2 = object2.getClass().getDeclaredField("computationalMetadataDTO");
		computationalMetadataDTO2.setAccessible(true);
		return computationalMetadataDTO1.get(object1).equals(computationalMetadataDTO2.get(object2));
	}

	private class InfrastructureTemplateServiceBaseChild extends InfrastructureTemplateServiceBase {
		@Override
		protected FullComputationalTemplate getCloudFullComputationalTemplate(ComputationalMetadataDTO metadataDTO) {
			return new FullComputationalTemplate(metadataDTO);
		}
	}
}
