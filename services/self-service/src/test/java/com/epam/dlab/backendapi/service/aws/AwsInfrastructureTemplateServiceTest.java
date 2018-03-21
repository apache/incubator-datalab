package com.epam.dlab.backendapi.service.aws;

import com.epam.dlab.backendapi.SelfServiceApplicationConfiguration;
import com.epam.dlab.dto.imagemetadata.ComputationalMetadataDTO;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AwsInfrastructureTemplateServiceTest {

	@Mock
	private SelfServiceApplicationConfiguration configuration;

	@InjectMocks
	private AwsInfrastructureTemplateService awsInfrastructureTemplateService;

	@Test
	public void getCloudFullComputationalTemplate() {
		when(configuration.getMinEmrInstanceCount()).thenReturn(2);
		when(configuration.getMaxEmrInstanceCount()).thenReturn(1000);
		when(configuration.getMaxEmrSpotInstanceBidPct()).thenReturn(95);
		when(configuration.getMinEmrSpotInstanceBidPct()).thenReturn(10);

		assertNotNull(awsInfrastructureTemplateService.getCloudFullComputationalTemplate(new ComputationalMetadataDTO
				()));

		verify(configuration).getMinEmrInstanceCount();
		verify(configuration).getMaxEmrInstanceCount();
		verify(configuration).getMaxEmrSpotInstanceBidPct();
		verify(configuration).getMinEmrSpotInstanceBidPct();
		verifyNoMoreInteractions(configuration);
	}
}
