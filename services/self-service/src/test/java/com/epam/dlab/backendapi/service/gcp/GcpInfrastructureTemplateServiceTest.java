package com.epam.dlab.backendapi.service.gcp;

import com.epam.dlab.backendapi.SelfServiceApplicationConfiguration;
import com.epam.dlab.dto.imagemetadata.ComputationalMetadataDTO;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class GcpInfrastructureTemplateServiceTest {

	@Mock
	private SelfServiceApplicationConfiguration configuration;

	@InjectMocks
	private GcpInfrastructureTemplateService gcpInfrastructureTemplateService;

	@Test
	public void getCloudFullComputationalTemplate() {
		when(configuration.getDataprocAvailableMasterInstanceCount()).thenReturn(Collections.emptyList());
		when(configuration.getMinDataprocSlaveInstanceCount()).thenReturn(2);
		when(configuration.getMaxDataprocSlaveInstanceCount()).thenReturn(100);
		when(configuration.getMinDataprocPreemptibleCount()).thenReturn(10);

		assertNotNull(gcpInfrastructureTemplateService
				.getCloudFullComputationalTemplate(new ComputationalMetadataDTO()));

		verify(configuration).getDataprocAvailableMasterInstanceCount();
		verify(configuration).getMinDataprocSlaveInstanceCount();
		verify(configuration).getMaxDataprocSlaveInstanceCount();
		verify(configuration).getMinDataprocPreemptibleCount();
		verifyNoMoreInteractions(configuration);
	}
}
