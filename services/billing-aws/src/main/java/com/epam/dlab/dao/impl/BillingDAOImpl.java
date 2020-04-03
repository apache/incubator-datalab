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

package com.epam.dlab.dao.impl;

import com.epam.dlab.dao.BillingDAO;
import com.epam.dlab.dto.billing.BillingData;
import com.epam.dlab.exceptions.DlabException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
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

import static com.epam.dlab.model.aws.ReportLine.FIELD_COST;
import static com.epam.dlab.model.aws.ReportLine.FIELD_CURRENCY_CODE;
import static com.epam.dlab.model.aws.ReportLine.FIELD_DLAB_ID;
import static com.epam.dlab.model.aws.ReportLine.FIELD_PRODUCT;
import static com.epam.dlab.model.aws.ReportLine.FIELD_RESOURCE_TYPE;
import static com.epam.dlab.model.aws.ReportLine.FIELD_USAGE_DATE;
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
            aggregationOperations.add(Aggregation.match(Criteria.where(FIELD_DLAB_ID).regex(dlabId, "i")));
            if (!products.isEmpty()) {
                aggregationOperations.add(Aggregation.match(Criteria.where(FIELD_PRODUCT).in(products)));
            }
            getMatchCriteria(dateStart, Criteria.where(FIELD_USAGE_DATE).gte(dateStart))
                    .ifPresent(aggregationOperations::add);
            getMatchCriteria(dateEnd, Criteria.where(FIELD_USAGE_DATE).lte(dateEnd))
                    .ifPresent(aggregationOperations::add);
            aggregationOperations.add(getGroupOperation());

            Aggregation aggregation = newAggregation(aggregationOperations);

            return mongoTemplate.aggregate(aggregation, "billing", Document.class).getMappedResults()
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

            return mongoTemplate.aggregate(aggregation, "billing", Document.class).getMappedResults()
                    .stream()
                    .map(this::toBillingData)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Cannot retrieve billing information ", e);
            throw new DlabException("Cannot retrieve billing information", e);
        }
    }

    private GroupOperation getGroupOperation() {
        return group(FIELD_PRODUCT, FIELD_CURRENCY_CODE, FIELD_RESOURCE_TYPE, FIELD_DLAB_ID)
                .min(FIELD_USAGE_DATE).as("from")
                .max(FIELD_USAGE_DATE).as("to")
                .sum(FIELD_COST).as(FIELD_COST);
    }

    private Optional<MatchOperation> getMatchCriteria(String dateStart, Criteria criteria) {
        return Optional.ofNullable(dateStart)
                .filter(StringUtils::isNotEmpty)
                .map(date -> Aggregation.match(criteria));
    }

    private BillingData toBillingData(Document billingData) {
        return BillingData.builder()
                .tag(billingData.getString(FIELD_DLAB_ID))
                .usageDateFrom(Optional.ofNullable(billingData.getString("from")).map(LocalDate::parse).orElse(null))
                .usageDateTo(Optional.ofNullable(billingData.getString("to")).map(LocalDate::parse).orElse(null))
                .product(billingData.getString(FIELD_PRODUCT))
                .usageType(billingData.getString(FIELD_RESOURCE_TYPE))
                .cost(BigDecimal.valueOf(billingData.getDouble(FIELD_COST)).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue())
                .currency(billingData.getString(FIELD_CURRENCY_CODE))
                .build();
    }
}
