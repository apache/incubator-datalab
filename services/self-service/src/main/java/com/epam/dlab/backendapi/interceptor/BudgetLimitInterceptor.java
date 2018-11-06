/*
 *
 *  * Copyright (c) 2018, EPAM SYSTEMS INC
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.epam.dlab.backendapi.interceptor;

import com.epam.dlab.backendapi.SelfServiceApplicationConfiguration;
import com.epam.dlab.backendapi.dao.BillingDAO;
import com.epam.dlab.exceptions.ResourceQuoteReachedException;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

@Slf4j
public class BudgetLimitInterceptor implements MethodInterceptor {
	@Inject
	private SelfServiceApplicationConfiguration configuration;
	@Inject
	private BillingDAO billingDAO;

	@Override
	public Object invoke(MethodInvocation methodInvocation) throws Throwable {
		if (billingDAO.getTotalCost() >= configuration.getAllowedBudgetUSD()) {
			log.warn("Execution of method {} failed because of reaching resource limit quote",
					methodInvocation.getMethod().getName());
			throw new ResourceQuoteReachedException("Operation can not be finished. Resource quote is reached");
		}
		return methodInvocation.proceed();
	}
}
