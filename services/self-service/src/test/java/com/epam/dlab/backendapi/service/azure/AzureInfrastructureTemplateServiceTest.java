package com.epam.dlab.backendapi.service.azure;

import com.epam.dlab.dto.imagemetadata.ComputationalMetadataDTO;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class AzureInfrastructureTemplateServiceTest {

	@InjectMocks
	private AzureInfrastructureTemplateService azureInfrastructureTemplateService;

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Test
	public void getCloudFullComputationalTemplate() {
		expectedException.expect(UnsupportedOperationException.class);
		expectedException.expectMessage("Operation is not supported currently");
		azureInfrastructureTemplateService.getCloudFullComputationalTemplate(new ComputationalMetadataDTO());
	}
}
