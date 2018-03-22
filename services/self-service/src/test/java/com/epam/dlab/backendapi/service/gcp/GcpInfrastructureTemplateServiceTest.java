package com.epam.dlab.backendapi.service.gcp;

import com.epam.dlab.backendapi.SelfServiceApplicationConfiguration;
import com.epam.dlab.backendapi.resources.dto.gcp.GcpDataprocConfiguration;
import com.epam.dlab.dto.base.computational.FullComputationalTemplate;
import com.epam.dlab.dto.imagemetadata.ComputationalMetadataDTO;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.lang.reflect.Field;
import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class GcpInfrastructureTemplateServiceTest {

	@Mock
	private SelfServiceApplicationConfiguration configuration;

	@InjectMocks
	private GcpInfrastructureTemplateService gcpInfrastructureTemplateService;

	@Test
	public void getCloudFullComputationalTemplate() throws NoSuchFieldException, IllegalAccessException {
		when(configuration.getDataprocAvailableMasterInstanceCount()).thenReturn(Collections.emptyList());
		when(configuration.getMinDataprocSlaveInstanceCount()).thenReturn(2);
		when(configuration.getMaxDataprocSlaveInstanceCount()).thenReturn(100);
		when(configuration.getMinDataprocPreemptibleCount()).thenReturn(10);

		GcpDataprocConfiguration expectedGcpDataprocConfiguration = GcpDataprocConfiguration.builder()
				.dataprocAvailableMasterInstanceCount(Collections.emptyList())
				.minDataprocSlaveInstanceCount(2)
				.maxDataprocSlaveInstanceCount(100)
				.minDataprocPreemptibleInstanceCount(10)
				.build();

		ComputationalMetadataDTO expectedComputationalMetadataDTO =
				new ComputationalMetadataDTO("someImageName");

		FullComputationalTemplate fullComputationalTemplate =
				gcpInfrastructureTemplateService.getCloudFullComputationalTemplate(expectedComputationalMetadataDTO);
		assertNotNull(fullComputationalTemplate);

		Field actualGcpDataprocConfiguration =
				fullComputationalTemplate.getClass().getDeclaredField("gcpDataprocConfiguration");
		actualGcpDataprocConfiguration.setAccessible(true);
		assertEquals(expectedGcpDataprocConfiguration, actualGcpDataprocConfiguration.get(fullComputationalTemplate));

		Field actualComputationalMetadataDTO = fullComputationalTemplate.getClass().getSuperclass()
				.getDeclaredField("computationalMetadataDTO");
		actualComputationalMetadataDTO.setAccessible(true);
		assertEquals(expectedComputationalMetadataDTO, actualComputationalMetadataDTO.get(fullComputationalTemplate));

		verify(configuration).getDataprocAvailableMasterInstanceCount();
		verify(configuration).getMinDataprocSlaveInstanceCount();
		verify(configuration).getMaxDataprocSlaveInstanceCount();
		verify(configuration).getMinDataprocPreemptibleCount();
		verifyNoMoreInteractions(configuration);
	}
}
