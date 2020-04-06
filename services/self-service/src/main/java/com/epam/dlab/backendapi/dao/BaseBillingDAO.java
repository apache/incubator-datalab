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

package com.epam.dlab.backendapi.dao;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.domain.BillingReportLine;
import com.epam.dlab.backendapi.resources.dto.BillingFilter;
import com.epam.dlab.backendapi.service.BillingService;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;
import java.util.function.Supplier;

@Slf4j
public class BaseBillingDAO extends BaseDAO implements BillingDAO {
	private static final int ONE_HUNDRED = 100;

	@Inject
	protected SettingsDAO settings;
	@Inject
	private UserSettingsDAO userSettingsDAO;
	@Inject
	private ProjectDAO projectDAO;
	@Inject
	private BillingService billingService;

	@Override
	public Double getTotalCost(UserInfo userInfo) {
		return getSum(userInfo, new BillingFilter());
	}

	@Override
	public Double getUserCost(String user, UserInfo userInfo) {
		BillingFilter filter = new BillingFilter();
		filter.setUsers(Collections.singletonList(user));
		return getSum(userInfo, filter);
	}

	@Override
	public Double getProjectCost(String project, UserInfo userInfo) {
		BillingFilter filter = new BillingFilter();
		filter.setProjects(Collections.singletonList(project));
		return getSum(userInfo, filter);
	}

	@Override
	public int getBillingQuoteUsed(UserInfo userInfo) {
		return toPercentage(() -> settings.getMaxBudget(), getTotalCost(userInfo));
	}

	@Override
	public int getBillingUserQuoteUsed(String user, UserInfo userInfo) {
		return toPercentage(() -> userSettingsDAO.getAllowedBudget(user), getUserCost(user, userInfo));
	}

	@Override
	public boolean isBillingQuoteReached(UserInfo userInfo) {
		return getBillingQuoteUsed(userInfo) >= ONE_HUNDRED;
	}

	@Override
	public boolean isUserQuoteReached(String user, UserInfo userInfo) {
		final Double userCost = getUserCost(user, userInfo);
		return userSettingsDAO.getAllowedBudget(user)
				.filter(allowedBudget -> userCost.intValue() != 0 && allowedBudget <= userCost)
				.isPresent();
	}

	@Override
	public boolean isProjectQuoteReached(String project, UserInfo userInfo) {
		final Double projectCost = getProjectCost(project, userInfo);
		return projectDAO.getAllowedBudget(project)
				.filter(allowedBudget -> projectCost.intValue() != 0 && allowedBudget <= projectCost)
				.isPresent();
	}

	@Override
	public int getBillingProjectQuoteUsed(String project, UserInfo userInfo) {
		return toPercentage(() -> projectDAO.getAllowedBudget(project), getProjectCost(project, userInfo));
	}

	private double getSum(UserInfo userInfo, BillingFilter filter) {
		return billingService.getBillingReportLines(userInfo, filter, true)
				.stream()
				.mapToDouble(BillingReportLine::getCost)
				.sum();
	}

	private Integer toPercentage(Supplier<Optional<Integer>> allowedBudget, Double totalCost) {
		return allowedBudget.get()
				.map(userBudget -> (totalCost * ONE_HUNDRED) / userBudget)
				.map(Double::intValue)
				.orElse(BigDecimal.ZERO.intValue());
	}
}
