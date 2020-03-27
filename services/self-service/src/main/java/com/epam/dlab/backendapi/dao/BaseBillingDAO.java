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

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.bson.conversions.Bson;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static com.epam.dlab.backendapi.dao.MongoCollections.BILLING;
import static com.mongodb.client.model.Accumulators.sum;
import static com.mongodb.client.model.Aggregates.group;
import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Filters.eq;
import static java.util.Collections.singletonList;

@Slf4j
public class BaseBillingDAO extends BaseDAO implements BillingDAO {

	private static final String PROJECT = "project";
	private static final int ONE_HUNDRED = 100;
	private static final String TOTAL_FIELD_NAME = "total";
	private static final String COST_FIELD = "$cost";

	@Inject
	protected SettingsDAO settings;
	@Inject
	private UserSettingsDAO userSettingsDAO;
	@Inject
	private ProjectDAO projectDAO;

	@Override
	public Double getTotalCost() {
		return aggregateBillingData(singletonList(group(null, sum(TOTAL_FIELD_NAME, COST_FIELD))));
	}

	@Override
	public Double getUserCost(String user) {
		final List<Bson> pipeline = Arrays.asList(match(eq(USER, user)),
				group(null, sum(TOTAL_FIELD_NAME, COST_FIELD)));
		return aggregateBillingData(pipeline);
	}

	@Override
	public Double getProjectCost(String project) {
		final List<Bson> pipeline = Arrays.asList(match(eq(PROJECT, project)),
				group(null, sum(TOTAL_FIELD_NAME, COST_FIELD)));
		return aggregateBillingData(pipeline);
	}

	@Override
	public int getBillingQuoteUsed() {
		return toPercentage(() -> settings.getMaxBudget(), getTotalCost());
	}

	@Override
	public int getBillingUserQuoteUsed(String user) {
		return toPercentage(() -> userSettingsDAO.getAllowedBudget(user), getUserCost(user));
	}

	@Override
	public boolean isBillingQuoteReached() {
		return getBillingQuoteUsed() >= ONE_HUNDRED;
	}

	@Override
	public boolean isUserQuoteReached(String user) {
		final Double userCost = getUserCost(user);
		return userSettingsDAO.getAllowedBudget(user)
				.filter(allowedBudget -> userCost.intValue() != 0 && allowedBudget <= userCost)
				.isPresent();
	}

	@Override
	public boolean isProjectQuoteReached(String project) {
		final Double projectCost = getProjectCost(project);
		return projectDAO.getAllowedBudget(project)
				.filter(allowedBudget -> projectCost.intValue() != 0 && allowedBudget <= projectCost)
				.isPresent();
	}

	@Override
	public int getBillingProjectQuoteUsed(String project) {
		return toPercentage(() -> projectDAO.getAllowedBudget(project), getProjectCost(project));
	}

	private Integer toPercentage(Supplier<Optional<Integer>> allowedBudget, Double totalCost) {
		return allowedBudget.get()
				.map(userBudget -> (totalCost * ONE_HUNDRED) / userBudget)
				.map(Double::intValue)
				.orElse(BigDecimal.ZERO.intValue());
	}

	private Double aggregateBillingData(List<Bson> pipeline) {
		return Optional.ofNullable(aggregate(BILLING, pipeline).first())
				.map(d -> d.getDouble(TOTAL_FIELD_NAME))
				.orElse(BigDecimal.ZERO.doubleValue());
	}
}
