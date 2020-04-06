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

package com.epam.dlab.backendapi.schedulers;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.BillingDAO;
import com.epam.dlab.backendapi.service.EnvironmentService;
import com.epam.dlab.backendapi.service.SecurityService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.quartz.JobExecutionContext;

import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CheckApplicationQuoteSchedulerTest {

	@Mock
	private BillingDAO billingDAO;
	@Mock
	private EnvironmentService environmentService;
	@Mock
	private JobExecutionContext jobExecutionContext;
	@Mock
	private SecurityService securityService;
	@InjectMocks
	private CheckApplicationQuoteScheduler checkApplicationQuoteScheduler;

	@Test
	public void testWhenQuoteNotReached() {
		when(billingDAO.isBillingQuoteReached(any(UserInfo.class))).thenReturn(false);
		when(securityService.getServiceAccountInfo(anyString())).thenReturn(getUserInfo());

		checkApplicationQuoteScheduler.execute(jobExecutionContext);

		verify(securityService).getServiceAccountInfo("admin");
		verify(billingDAO).isBillingQuoteReached(getUserInfo());
		verifyNoMoreInteractions(billingDAO);
		verifyZeroInteractions(environmentService);
	}

	@Test
	public void testWhenQuoteReached() {
		when(billingDAO.isBillingQuoteReached(any(UserInfo.class))).thenReturn(true);
		when(securityService.getServiceAccountInfo(anyString())).thenReturn(getUserInfo());

		checkApplicationQuoteScheduler.execute(jobExecutionContext);

		verify(securityService).getServiceAccountInfo("admin");
		verify(billingDAO).isBillingQuoteReached(getUserInfo());
		verify(environmentService).stopAll();
		verifyNoMoreInteractions(billingDAO, environmentService);
	}

	private UserInfo getUserInfo() {
		return new UserInfo("admin", null);
	}

}