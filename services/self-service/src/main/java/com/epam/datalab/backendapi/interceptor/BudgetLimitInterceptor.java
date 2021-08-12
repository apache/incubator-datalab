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

package com.epam.datalab.backendapi.interceptor;

import com.epam.datalab.backendapi.annotation.Project;
import com.epam.datalab.backendapi.dao.BillingDAO;
import com.epam.datalab.backendapi.service.BillingService;
import com.epam.datalab.exceptions.ResourceQuoteReachedException;
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
    private BillingService billingService;

    @Override
    public Object invoke(MethodInvocation mi) throws Throwable {
        if (projectQuoteReached(mi) || billingDAO.isBillingQuoteReached()) {
            final Method method = mi.getMethod();
            log.warn("Execution of method {} failed because of reaching resource limit quote", method.getName());
            throw new ResourceQuoteReachedException("Operation can not be finished. Resource quote is reached");
        } else {
            return mi.proceed();
        }
    }

    private Boolean projectQuoteReached(MethodInvocation mi) {

        final Parameter[] parameters = mi.getMethod().getParameters();
        return IntStream.range(0, parameters.length)
                .filter(i -> Objects.nonNull(parameters[i].getAnnotation(Project.class)))
                .mapToObj(i -> (String) mi.getArguments()[i])
                .findAny()
                .map(billingService::isProjectQuoteReached)
                .orElse(Boolean.FALSE);
    }
}
