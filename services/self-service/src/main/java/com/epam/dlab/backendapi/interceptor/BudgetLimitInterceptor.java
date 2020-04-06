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

package com.epam.dlab.backendapi.interceptor;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.annotation.Project;
import com.epam.dlab.backendapi.annotation.User;
import com.epam.dlab.backendapi.dao.BillingDAO;
import com.epam.dlab.backendapi.service.SecurityService;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.exceptions.ResourceQuoteReachedException;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Objects;
import java.util.stream.IntStream;

@Slf4j
public class BudgetLimitInterceptor implements MethodInterceptor {
	@Inject
	private BillingDAO billingDAO;
	@Inject
	private SecurityService securityService;

	@Override
	public Object invoke(MethodInvocation mi) throws Throwable {
		if (projectQuoteReached(mi) || billingDAO.isBillingQuoteReached(securityService.getServiceAccountInfo("admin"))) {
			final Method method = mi.getMethod();
			log.warn("Execution of method {} failed because of reaching resource limit quote", method.getName());
			throw new ResourceQuoteReachedException("Operation can not be finished. Resource quote is reached");
		} else {
			return mi.proceed();
		}
	}

	private Boolean projectQuoteReached(MethodInvocation mi) {

		final Parameter[] parameters = mi.getMethod().getParameters();
		String project = IntStream.range(0, parameters.length)
				.filter(i -> Objects.nonNull(parameters[i].getAnnotation(Project.class)))
				.mapToObj(i -> (String) mi.getArguments()[i])
				.findAny()
				.orElseThrow(() -> new DlabException("Project parameter wanted!"));
		UserInfo userInfo = IntStream.range(0, parameters.length)
				.filter(i -> Objects.nonNull(parameters[i].getAnnotation(User.class)))
				.mapToObj(i -> (UserInfo) mi.getArguments()[i])
				.findAny()
				.orElseThrow(() -> new DlabException("UserInfo parameter wanted!"));

		return billingDAO.isProjectQuoteReached(project, userInfo);
	}
}
