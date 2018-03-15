package com.epam.dlab.backendapi.service.impl;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.SelfServiceApplicationConfiguration;
import com.epam.dlab.backendapi.dao.SettingsDAO;
import com.epam.dlab.dto.base.computational.FullComputationalTemplate;
import com.epam.dlab.dto.imagemetadata.ComputationalMetadataDTO;
import com.epam.dlab.dto.imagemetadata.ExploratoryMetadataDTO;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.client.RESTService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class InfrastructureTemplateServiceBaseTest {

	@Mock
	private SelfServiceApplicationConfiguration configuration;
	@Mock
	private SettingsDAO settingsDAO;
	@Mock
	private RESTService provisioningService;

	@InjectMocks
	private InfrastructureTemplateServiceBaseChild infrastructureTemplateServiceBaseChild =
			new InfrastructureTemplateServiceBaseChild();

	@Test
	public void getExploratoryTemplates() {
		List<ExploratoryMetadataDTO> expectedEmdDtoList = Arrays.asList(
				new ExploratoryMetadataDTO("someImage1"), new ExploratoryMetadataDTO("someImage2")
		);
		when(provisioningService.get(anyString(), anyString(), any())).thenReturn(expectedEmdDtoList.toArray());

		UserInfo userInfo = new UserInfo("test", "token");
		List<ExploratoryMetadataDTO> actualEmdDtoList =
				infrastructureTemplateServiceBaseChild.getExploratoryTemplates(userInfo);
		assertNotNull(actualEmdDtoList);
		assertEquals(expectedEmdDtoList, actualEmdDtoList);

		verify(provisioningService).get("docker/exploratory", "token", ExploratoryMetadataDTO[].class);
		verifyNoMoreInteractions(provisioningService);
	}

	@Test
	public void getExploratoryTemplatesWithException() {
		doThrow(new DlabException("Could not load list of exploratory templates for user"))
				.when(provisioningService).get(anyString(), anyString(), any());

		UserInfo userInfo = new UserInfo("test", "token");
		try {
			infrastructureTemplateServiceBaseChild.getExploratoryTemplates(userInfo);
		} catch (DlabException e) {
			assertEquals("Could not load list of exploratory templates for user", e.getMessage());
		}
		verify(provisioningService).get("docker/exploratory", "token", ExploratoryMetadataDTO[].class);
		verifyNoMoreInteractions(provisioningService);
	}

	@Test
	public void getComputationalTemplates() throws NoSuchFieldException, IllegalAccessException {
		List<ComputationalMetadataDTO> expectedCmdDtoList = Arrays.asList(
				new ComputationalMetadataDTO("dataengine-service"),
				new ComputationalMetadataDTO("dataengine-service")
		);
		when(provisioningService.get(anyString(), anyString(), any())).thenReturn(expectedCmdDtoList.toArray());

		List<FullComputationalTemplate> expectedFullCmdDtoList = expectedCmdDtoList.stream()
				.map(e -> infrastructureTemplateServiceBaseChild.getCloudFullComputationalTemplate(e))
				.collect(Collectors.toList());

		UserInfo userInfo = new UserInfo("test", "token");
		List<FullComputationalTemplate> actualFullCmdDtoList =
				infrastructureTemplateServiceBaseChild.getComputationalTemplates(userInfo);
		assertNotNull(actualFullCmdDtoList);
		assertTrue(expectedFullCmdDtoList.size() == actualFullCmdDtoList.size());
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
			infrastructureTemplateServiceBaseChild.getComputationalTemplates(userInfo);
		} catch (DlabException e) {
			assertEquals("Could not load list of computational templates for user", e.getMessage());
		}
		verify(provisioningService).get("docker/computational", "token", ComputationalMetadataDTO[].class);
		verifyNoMoreInteractions(provisioningService);
	}

	@Test
	public void getComputationalTemplatesWithInapproprietaryImageName() {
		List<ComputationalMetadataDTO> expectedCmdDtoList = Arrays.asList(
				new ComputationalMetadataDTO("dataengine-service"),
				new ComputationalMetadataDTO("blablabla")
		);
		when(provisioningService.get(anyString(), anyString(), any())).thenReturn(expectedCmdDtoList.toArray());

		UserInfo userInfo = new UserInfo("test", "token");
		try {
			infrastructureTemplateServiceBaseChild.getComputationalTemplates(userInfo);
		} catch (IllegalArgumentException e) {
			assertEquals("Unknown data engine null", e.getMessage());
		}
		verify(provisioningService).get("docker/computational", "token", ComputationalMetadataDTO[].class);
		verifyNoMoreInteractions(provisioningService);
	}

	private boolean areFullComputationalTemplatesEqual(FullComputationalTemplate object1,
													   FullComputationalTemplate object2) throws NoSuchFieldException,
			IllegalAccessException {
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
