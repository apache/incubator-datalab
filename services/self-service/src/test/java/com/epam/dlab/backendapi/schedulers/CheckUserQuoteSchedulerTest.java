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
import com.epam.dlab.backendapi.resources.dto.UserDTO;
import com.epam.dlab.backendapi.service.EnvironmentService;
import com.epam.dlab.backendapi.service.SecurityService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.quartz.JobExecutionContext;

import java.util.Collections;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class CheckUserQuoteSchedulerTest {

	private static final String USER = "test";
	@Mock
	private BillingDAO billingDAO;
	@Mock
	private EnvironmentService environmentService;
	@Mock
	private JobExecutionContext jobExecutionContext;
	@Mock
	private SecurityService securityService;
	@InjectMocks
	private CheckUserQuoteScheduler checkUserQuoteScheduler;

	@Test
	public void testWhenUserQuoteReached() {
		when(billingDAO.isUserQuoteReached(anyString(), any(UserInfo.class))).thenReturn(true);
		when(environmentService.getUsers()).thenReturn(Collections.singletonList(new UserDTO(USER, 1, UserDTO.Status.ACTIVE)));
		when(securityService.getServiceAccountInfo(anyString())).thenReturn(getUserInfo());

		checkUserQuoteScheduler.execute(jobExecutionContext);

		verify(environmentService).getUsers();
		verify(securityService).getServiceAccountInfo("admin");
		verify(billingDAO).isUserQuoteReached(USER, getUserInfo());
		verify(environmentService).stopEnvironmentWithServiceAccount(USER);
		verifyNoMoreInteractions(environmentService, billingDAO);
		verifyZeroInteractions(jobExecutionContext);
	}

	@Test
	public void testWhenUserQuoteNotReached() {
		when(billingDAO.isUserQuoteReached(anyString(), any(UserInfo.class))).thenReturn(false);
		when(environmentService.getUsers()).thenReturn(Collections.singletonList(new UserDTO(USER, 1, UserDTO.Status.ACTIVE)));
		when(securityService.getServiceAccountInfo(anyString())).thenReturn(getUserInfo());

		checkUserQuoteScheduler.execute(jobExecutionContext);

		verify(environmentService).getUsers();
		verify(securityService).getServiceAccountInfo("admin");
		verify(billingDAO).isUserQuoteReached(USER, getUserInfo());
		verify(environmentService, never()).stopEnvironmentWithServiceAccount(anyString());
		verifyNoMoreInteractions(environmentService, billingDAO);
		verifyZeroInteractions(jobExecutionContext);
	}

	private UserInfo getUserInfo() {
		return new UserInfo("admin", null);
	}
}