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

package com.epam.datalab.backendapi.dao;

import com.epam.datalab.backendapi.domain.BillingReportLine;
import com.epam.datalab.backendapi.resources.dto.BillingFilter;
import com.epam.datalab.dto.billing.BillingResourceType;
import com.google.inject.Inject;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Sorts;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.epam.datalab.backendapi.dao.MongoCollections.BILLING;
import static com.mongodb.client.model.Accumulators.max;
import static com.mongodb.client.model.Accumulators.min;
import static com.mongodb.client.model.Accumulators.sum;
import static com.mongodb.client.model.Aggregates.group;
import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Aggregates.sort;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.gte;
import static com.mongodb.client.model.Filters.in;
import static com.mongodb.client.model.Filters.lte;
import static com.mongodb.client.model.Filters.regex;
import static java.time.temporal.TemporalAdjusters.firstDayOfMonth;
import static java.time.temporal.TemporalAdjusters.lastDayOfMonth;
import static java.util.Collections.singletonList;

@Slf4j
public class BaseBillingDAO extends BaseDAO implements BillingDAO {
    private static final int ONE_HUNDRED = 100;
    private static final String COST_FIELD = "$cost";
    private static final String TOTAL_FIELD_NAME = "total";
    private static final String PROJECT = "project";
    private static final String APPLICATION = "application";
    private static final String USAGE_DATE = "usageDate";
    private static final String USER = "user";
    private static final String RESOURCE_TYPE = "resource_type";
    private static final String DATALAB_ID = "datalabId";
    private static final String FROM = "from";
    private static final String TO = "to";
    private static final String PRODUCT = "product";
    private static final String CURRENCY = "currency";
    private static final String COST = "cost";
    private static final String RESOURCE_NAME = "resource_name";
    private static final String ENDPOINT = "endpoint";
    private static final String SHAPE = "shape";
    private static final String EXPLORATORY = "exploratoryName";

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
    public Double getOverallProjectCost(String project) {
        final List<Bson> pipeline = Arrays.asList(match(eq(PROJECT, project)),
                group(null, sum(TOTAL_FIELD_NAME, COST_FIELD)));
        return aggregateBillingData(pipeline);
    }

    @Override
    public Double getMonthlyProjectCost(String project, LocalDate date) {
        final List<Bson> pipeline = Arrays.asList(match(
                and(
                        eq(PROJECT, project),
                        gte(USAGE_DATE, date.with(firstDayOfMonth()).toString()),
                        lte(USAGE_DATE, date.with(lastDayOfMonth()).toString()))
                ),
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
    public List<BillingReportLine> findBillingData(String project, String endpoint, List<String> resourceNames) {
        return find(BILLING, and(eq(PROJECT, project), eq(ENDPOINT, endpoint), in(RESOURCE_NAME, resourceNames)), BillingReportLine.class);
    }

    public List<BillingReportLine> aggregateBillingData(BillingFilter filter) {
        List<Bson> pipeline = new ArrayList<>();
        List<Bson> matchCriteria = matchCriteria(filter);
        if (!matchCriteria.isEmpty()) {
            pipeline.add(Aggregates.match(Filters.and(matchCriteria)));
        }
        pipeline.add(groupCriteria());
        pipeline.add(usageDateSort());
        return StreamSupport.stream(getCollection(BILLING).aggregate(pipeline).spliterator(), false)
                .map(this::toBillingReport)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteByUsageDate(String application, String usageDate) {
        deleteMany(BILLING, and(eq(APPLICATION, application), eq(USAGE_DATE, usageDate)));
    }

    @Override
    public void deleteByUsageDateRegex(String application, String usageDate) {
        deleteMany(BILLING, and(eq(APPLICATION, application), regex(USAGE_DATE, "^" + usageDate)));
    }

    @Override
    public void save(List<BillingReportLine> billingData) {
        if (CollectionUtils.isNotEmpty(billingData)) {
            insertMany(BILLING, new ArrayList<>(billingData));
        }
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

    private Bson usageDateSort() {
        return sort(Sorts.descending(USAGE_DATE));
    }

    private Bson groupCriteria() {
        return group(getGroupingFields(USER, DATALAB_ID, RESOURCE_TYPE, RESOURCE_NAME, PROJECT, PRODUCT, CURRENCY, SHAPE, EXPLORATORY),
                sum(COST, "$" + COST),
                min(FROM, "$" + FROM),
                max(TO, "$" + TO));
    }

    private List<Bson> matchCriteria(BillingFilter filter) {
        List<Bson> searchCriteria = new ArrayList<>();

        if (CollectionUtils.isNotEmpty(filter.getUsers())) {
            searchCriteria.add(in(USER, filter.getUsers()));
        }
        if (CollectionUtils.isNotEmpty(filter.getResourceTypes())) {
            searchCriteria.add(in(RESOURCE_TYPE, filter.getResourceTypes()));
        }
        if (StringUtils.isNotEmpty(filter.getDatalabId())) {
            searchCriteria.add(regex(DATALAB_ID, filter.getDatalabId(), "i"));
        }
        if (StringUtils.isNotEmpty(filter.getDateStart())) {
            searchCriteria.add(gte(USAGE_DATE, filter.getDateStart()));
        }
        if (StringUtils.isNotEmpty(filter.getDateEnd())) {
            searchCriteria.add(lte(USAGE_DATE, filter.getDateEnd()));
        }
        if (CollectionUtils.isNotEmpty(filter.getProjects())) {
            searchCriteria.add(in(PROJECT, filter.getProjects()));
        }
        if (CollectionUtils.isNotEmpty(filter.getProducts())) {
            searchCriteria.add(in(PRODUCT, filter.getProducts()));
        }
        if (CollectionUtils.isNotEmpty(filter.getShapes())) {
            searchCriteria.add(regex(SHAPE, "(" + String.join("|", filter.getShapes()) + ")"));
        }

        return searchCriteria;
    }

    private BillingReportLine toBillingReport(Document d) {
        Document id = (Document) d.get("_id");
        return BillingReportLine.builder()
                .datalabId(id.getString(DATALAB_ID))
                .project(id.getString(PROJECT))
                .resourceName(id.getString(RESOURCE_NAME))
                .exploratoryName(id.getString(EXPLORATORY))
                .shape(id.getString(SHAPE))
                .user(id.getString(USER))
                .product(id.getString(PRODUCT))
                .resourceType(Optional.ofNullable(id.getString(RESOURCE_TYPE)).map(BillingResourceType::valueOf).orElse(null))
                .usageDateFrom(d.getDate(FROM).toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
                .usageDateTo(d.getDate(TO).toInstant().atZone(ZoneId.systemDefault()).toLocalDate())
                .cost(BigDecimal.valueOf(d.getDouble(COST)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue())
                .currency(id.getString(CURRENCY))
                .build();
    }
}
