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

package com.epam.dlab.billing.gcp.service;

import com.epam.dlab.billing.gcp.dao.BillingDAO;
import com.epam.dlab.billing.gcp.documents.Edge;
import com.epam.dlab.billing.gcp.documents.UserInstance;
import com.epam.dlab.billing.gcp.model.BillingData;
import com.epam.dlab.billing.gcp.model.GcpBillingData;
import com.epam.dlab.billing.gcp.repository.BillingRepository;
import com.epam.dlab.billing.gcp.repository.EdgeRepository;
import com.epam.dlab.billing.gcp.repository.UserInstanceRepository;
import com.epam.dlab.billing.gcp.util.BillingUtils;
import com.google.common.collect.Iterables;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.epam.dlab.billing.gcp.util.BillingUtils.edgeBillingDataStream;
import static org.springframework.data.mongodb.core.query.Criteria.where;

@Service
@Slf4j
public class BillingServiceImpl implements BillingService {

    private final BillingDAO billingDAO;
    private final EdgeRepository edgeRepository;
    private final UserInstanceRepository userInstanceRepository;
    private final BillingRepository billingRepository;
    private final MongoTemplate mongoTemplate;
    @Value("${ssnBaseName}")
    private String ssnBaseName;

    @Autowired
    public BillingServiceImpl(BillingDAO billingDAO, EdgeRepository edgeRepository,
                              UserInstanceRepository userInstanceRepository, BillingRepository billingRepository,
                              MongoTemplate mongoTemplate) {
        this.billingDAO = billingDAO;
        this.edgeRepository = edgeRepository;
        this.userInstanceRepository = userInstanceRepository;
        this.billingRepository = billingRepository;
        this.mongoTemplate = mongoTemplate;
    }

    @Override
    public void updateBillingData() {
        try {

            final Stream<BillingData> ssnBillingDataStream = BillingUtils.ssnBillingDataStream(ssnBaseName);
            final Stream<BillingData> billableUserInstances = userInstanceRepository.findAll()
                    .stream()
                    .flatMap(BillingUtils::exploratoryBillingDataStream);

            final Stream<BillingData> billableEdges = edgeRepository.findAll()
                    .stream()
                    .map(Edge::getId)
                    .flatMap(e -> edgeBillingDataStream(e, ssnBaseName));

            final Map<String, BillingData> billableResources = Stream.of(billableUserInstances, billableEdges, ssnBillingDataStream)
                    .flatMap(s -> s)
                    .collect(Collectors.toMap(BillingData::getDlabId, b -> b));
            log.trace("Billable resources are: {}", billableResources);
            final List<BillingData> billingDataList = billingDAO.getBillingData(ssnBaseName)
                    .stream()
                    .map(bd -> toBillingData(bd, getOrDefault(billableResources, bd.getTag())))
                    .collect(Collectors.toList());

            billingRepository.insert(billingDataList);
            updateExploratoryCost(billingDataList);


        } catch (Exception e) {
            log.error("Can not update billing due to: {}", e.getMessage());
        }
    }

    private BillingData getOrDefault(Map<String, BillingData> billableResources, String tag) {
        return billableResources.getOrDefault(tag, BillingData.builder().dlabId(tag).build());
    }

    private void updateExploratoryCost(List<BillingData> billingDataList) {
        billingDataList.stream()
                .filter(this::userAndExploratoryNamePresent)
                .collect(groupByUserNameExploratoryNameCollector())
                .forEach(this::updateUserExploratoryBillingData);
    }

    private void updateUserExploratoryBillingData(String user, Map<String, List<BillingData>> billableExploratoriesMap) {
        billableExploratoriesMap.forEach((exploratoryName, billingInfoList) ->
                updateExploratoryBillingData(user, exploratoryName, billingInfoList)
        );
    }

    private Collector<BillingData, ?, Map<String, Map<String, List<BillingData>>>> groupByUserNameExploratoryNameCollector() {
        return Collectors.groupingBy(BillingData::getUser, Collectors.groupingBy(BillingData::getExploratoryName));
    }

    private boolean userAndExploratoryNamePresent(BillingData bd) {
        return Objects.nonNull(bd.getUser()) && Objects.nonNull(bd.getExploratoryName());
    }

    private void updateExploratoryBillingData(String user, String exploratoryName, List<BillingData> billingInfoList) {
        userInstanceRepository.findByUserAndExploratoryName(user, exploratoryName).ifPresent(userInstance ->
                mongoTemplate.updateFirst(Query.query(where("user").is(user).and("exploratory_name").is(exploratoryName)),
                        Update.update("cost", getTotalCost(billingInfoList)).set("billing", billingInfoList), UserInstance.class));
    }

    private double getTotalCost(List<BillingData> billingInfoList) {
        return new BigDecimal(billingInfoList.stream().mapToDouble(BillingData::getCost).sum())
                .setScale(2, BigDecimal.ROUND_HALF_UP)
                .doubleValue();

    }

    private BillingData toBillingData(GcpBillingData bd, BillingData billableResource) {

        return BillingData.builder()
                .displayName(billableResource.getDisplayName())
                .cost(bd.getCost().doubleValue())
                .currency(bd.getCurrency())
                .product(bd.getProduct())
                .usageDateTo(bd.getUsageDateTo())
                .usageDateFrom(bd.getUsageDateFrom())
                .usageDate(bd.getUsageDateFrom().format((DateTimeFormatter.ofPattern("yyyy-MM-dd"))))
                .usageType(bd.getUsageType())
                .user(billableResource.getUser())
                .exploratoryName(billableResource.getExploratoryName())
                .computationalName(billableResource.getComputationalName())
                .dlabId(bd.getTag())
                .resourceType(billableResource.getResourceType())
                .build();
    }
}
