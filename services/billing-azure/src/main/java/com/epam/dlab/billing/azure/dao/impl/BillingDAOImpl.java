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

package com.epam.dlab.billing.azure.dao.impl;

import com.epam.dlab.billing.azure.dao.BillingDAO;
import com.epam.dlab.billing.azure.model.AzureDailyResourceInvoice;
import com.epam.dlab.dto.billing.BillingData;
import com.epam.dlab.exceptions.DlabException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;

@Component
@Slf4j
public class BillingDAOImpl implements BillingDAO {
    private final MongoTemplate mongoTemplate;

    public BillingDAOImpl(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public List<BillingData> getBillingReport(String dateStart, String dateEnd, String dlabId, List<String> products) {
        try {
            List<AggregationOperation> aggregationOperations = new ArrayList<>();
            aggregationOperations.add(Aggregation.match(Criteria.where("dlabId").regex(dlabId, "i")));
            if (!products.isEmpty()) {
                aggregationOperations.add(Aggregation.match(Criteria.where("meterCategory").in(products)));
            }
            getMatchCriteria(dateStart, Criteria.where("day").gte(dateStart))
                    .ifPresent(aggregationOperations::add);
            getMatchCriteria(dateEnd, Criteria.where("day").lte(dateEnd))
                    .ifPresent(aggregationOperations::add);
            aggregationOperations.add(getGroupOperation());

            Aggregation aggregation = newAggregation(aggregationOperations);

            return mongoTemplate.aggregate(aggregation, "billing", AzureDailyResourceInvoice.class).getMappedResults()
                    .stream()
                    .map(this::toBillingData)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Cannot retrieve billing information ", e);
            throw new DlabException("Cannot retrieve billing information", e);
        }
    }

    @Override
    public List<BillingData> getBillingReport(List<String> dlabIds) {
        try {
            GroupOperation groupOperation = getGroupOperation();
            MatchOperation matchOperation = Aggregation.match(Criteria.where("dlab_id").in(dlabIds));
            Aggregation aggregation = newAggregation(matchOperation, groupOperation);

            return mongoTemplate.aggregate(aggregation, "billing", AzureDailyResourceInvoice.class).getMappedResults()
                    .stream()
                    .map(this::toBillingData)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Cannot retrieve billing information ", e);
            throw new DlabException("Cannot retrieve billing information", e);
        }
    }

    private GroupOperation getGroupOperation() {
        return group("meterCategory", "currencyCode", "dlabId")
                .min("usageStartDate").as("usageStartDate")
                .max("usageEndDate").as("usageEndDate")
                .sum("cost").as("cost");
    }

    private Optional<MatchOperation> getMatchCriteria(String dateStart, Criteria criteria) {
        return Optional.ofNullable(dateStart)
                .filter(StringUtils::isNotEmpty)
                .map(date -> Aggregation.match(criteria));
    }

    private BillingData toBillingData(AzureDailyResourceInvoice billingData) {
        return BillingData.builder()
                .tag(billingData.getDlabId())
                .usageDateFrom(Optional.ofNullable(billingData.getUsageStartDate()).map(LocalDate::parse).orElse(null))
                .usageDateTo(Optional.ofNullable(billingData.getUsageEndDate()).map(LocalDate::parse).orElse(null))
                .product(billingData.getMeterCategory())
                .cost(BigDecimal.valueOf(billingData.getCost()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue())
                .currency(billingData.getCurrencyCode())
                .build();
    }
}
