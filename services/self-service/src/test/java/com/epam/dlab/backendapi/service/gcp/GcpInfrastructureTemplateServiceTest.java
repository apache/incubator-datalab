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

package com.epam.dlab.backendapi.service.gcp;

import com.epam.dlab.backendapi.conf.SelfServiceApplicationConfiguration;
import com.epam.dlab.backendapi.resources.dto.gcp.GcpDataprocConfiguration;
import com.epam.dlab.dto.base.computational.FullComputationalTemplate;
import com.epam.dlab.dto.imagemetadata.ComputationalMetadataDTO;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.lang.reflect.Field;

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
		when(configuration.getMinInstanceCount()).thenReturn(2);
		when(configuration.getMaxInstanceCount()).thenReturn(100);
		when(configuration.getMinDataprocPreemptibleCount()).thenReturn(10);

		GcpDataprocConfiguration expectedGcpDataprocConfiguration = GcpDataprocConfiguration.builder()
				.minInstanceCount(2)
				.maxInstanceCount(100)
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

		verify(configuration).getMinInstanceCount();
		verify(configuration).getMaxInstanceCount();
		verify(configuration).getMinDataprocPreemptibleCount();
		verifyNoMoreInteractions(configuration);
	}
}
