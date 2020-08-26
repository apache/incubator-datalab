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

import com.epam.dlab.backendapi.conf.SelfServiceApplicationConfiguration;
import com.epam.dlab.backendapi.dao.BillingDAO;
import com.epam.dlab.backendapi.dao.ProjectDAO;
import com.epam.dlab.backendapi.domain.BudgetDTO;
import com.epam.dlab.backendapi.domain.ProjectDTO;
import com.epam.dlab.backendapi.domain.ProjectEndpointDTO;
import com.epam.dlab.backendapi.resources.TestBase;
import com.epam.dlab.backendapi.service.ProjectService;
import com.epam.dlab.dto.UserInstanceStatus;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BillingServiceImplTest extends TestBase {

	private static final String PROJECT = "project";

	@Mock
	private SelfServiceApplicationConfiguration configuration;
	@Mock
	private ProjectDAO projectDAO;
	@Mock
	private ProjectService projectService;
	@Mock
	private BillingDAO billingDAO;

	@InjectMocks
	private BillingServiceImpl billingService;

	@Test
	public void getBillingProjectQuoteUsed() {
		when(projectDAO.getAllowedBudget(anyString())).thenReturn(Optional.of(10));
		when(projectService.get(anyString())).thenReturn(getProjectDTO(Boolean.FALSE));
		when(billingDAO.getOverallProjectCost(anyString())).thenReturn(5d);

		final int billingProjectQuoteUsed = billingService.getBillingProjectQuoteUsed(PROJECT);

		assertEquals("quotes should be equal", 50, billingProjectQuoteUsed);
		verify(projectDAO).getAllowedBudget(PROJECT);
		verify(projectService).get(PROJECT);
		verify(billingDAO).getOverallProjectCost(PROJECT);
		verifyNoMoreInteractions(projectDAO, projectService, billingDAO);
	}

	@Test
	public void getBillingProjectQuoteNullUsed() {
		when(projectDAO.getAllowedBudget(anyString())).thenReturn(Optional.of(10));
		when(projectService.get(anyString())).thenReturn(getProjectQuoteNullDTO());
		when(billingDAO.getOverallProjectCost(anyString())).thenReturn(5d);

		final int billingProjectQuoteUsed = billingService.getBillingProjectQuoteUsed(PROJECT);

		assertEquals("quotes should be equal", 50, billingProjectQuoteUsed);
		verify(projectDAO).getAllowedBudget(PROJECT);
		verify(projectService).get(PROJECT);
		verify(billingDAO).getOverallProjectCost(PROJECT);
		verifyNoMoreInteractions(projectDAO, projectService, billingDAO);
	}

	@Test
	public void getBillingProjectMonthlyQuoteUsed() {
		when(projectDAO.getAllowedBudget(anyString())).thenReturn(Optional.of(10));
		when(projectService.get(anyString())).thenReturn(getProjectDTO(Boolean.TRUE));
		when(billingDAO.getMonthlyProjectCost(anyString(), any(LocalDate.class))).thenReturn(5d);

		final int billingProjectQuoteUsed = billingService.getBillingProjectQuoteUsed(PROJECT);

		assertEquals("quotes should be equal", 50, billingProjectQuoteUsed);
		verify(projectDAO).getAllowedBudget(PROJECT);
		verify(projectService).get(PROJECT);
		verify(billingDAO).getMonthlyProjectCost(PROJECT, LocalDate.now());
		verifyNoMoreInteractions(projectDAO, projectService, billingDAO);
	}

	private ProjectDTO getProjectDTO(boolean isMonthlyBudget) {
		return ProjectDTO.builder()
				.name(PROJECT)
				.budget(new BudgetDTO(10, isMonthlyBudget))
				.endpoints(Collections.singletonList(new ProjectEndpointDTO(ENDPOINT_NAME, UserInstanceStatus.RUNNING, null)))
				.build();
	}

	private ProjectDTO getProjectQuoteNullDTO() {
		return ProjectDTO.builder()
				.name(PROJECT)
				.endpoints(Collections.singletonList(new ProjectEndpointDTO(ENDPOINT_NAME, UserInstanceStatus.RUNNING, null)))
				.build();
	}
}