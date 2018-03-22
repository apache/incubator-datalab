package com.epam.dlab.backendapi.service.azure;

import com.epam.dlab.dto.imagemetadata.ComputationalMetadataDTO;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class AzureInfrastructureTemplateServiceTest {

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Test
	public void getCloudFullComputationalTemplate() {
		expectedException.expect(UnsupportedOperationException.class);
		expectedException.expectMessage("Operation is not supported currently");
		new AzureInfrastructureTemplateService().getCloudFullComputationalTemplate(new ComputationalMetadataDTO());
	}
}
