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

package com.epam.datalab.billing.azure;

import com.epam.datalab.MongoKeyWords;
import com.epam.datalab.billing.azure.config.BillingConfigurationAzure;
import com.epam.datalab.billing.azure.model.AzureDailyResourceInvoice;
import com.epam.datalab.billing.azure.model.BillingPeriod;
import com.epam.datalab.dto.billing.BillingData;
import com.epam.datalab.exceptions.DatalabException;
import com.epam.datalab.model.azure.AzureAuthFile;
import com.epam.datalab.util.mongo.modules.IsoDateModule;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mongodb.BasicDBObject;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.result.UpdateResult;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CalculateBillingServiceImpl implements CalculateBillingService {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss.SSS'Z");
    private static final String SCHEDULER_ID = "azureBillingScheduler";
    private final BillingConfigurationAzure billingConfigurationAzure;
    private final MongoDbBillingClient mongoDbBillingClient;
    private ObjectMapper objectMapper;

    @Autowired
    public CalculateBillingServiceImpl(BillingConfigurationAzure configuration) throws IOException {
        billingConfigurationAzure = configuration;
        objectMapper = new ObjectMapper().registerModule(new IsoDateModule());
        Path path = Paths.get(billingConfigurationAzure.getAuthenticationFile());

        if (path.toFile().exists()) {
            log.info("Read and override configs using auth file");
            try {
                AzureAuthFile azureAuthFile = new ObjectMapper().readValue(path.toFile(), AzureAuthFile.class);
                this.billingConfigurationAzure.setClientId(azureAuthFile.getClientId());
                this.billingConfigurationAzure.setClientSecret(azureAuthFile.getClientSecret());
                this.billingConfigurationAzure.setTenantId(azureAuthFile.getTenantId());
                this.billingConfigurationAzure.setSubscriptionId(azureAuthFile.getSubscriptionId());
            } catch (IOException e) {
                log.error("Cannot read configuration file", e);
                throw e;
            }
            log.info("Configs from auth file are used");
        } else {
            log.info("Configs from yml file are used");
        }

        this.mongoDbBillingClient = new MongoDbBillingClient
                (billingConfigurationAzure.getAggregationOutputMongoDataSource().getHost(),
                        billingConfigurationAzure.getAggregationOutputMongoDataSource().getPort(),
                        billingConfigurationAzure.getAggregationOutputMongoDataSource().getDatabase(),
                        billingConfigurationAzure.getAggregationOutputMongoDataSource().getUsername(),
                        billingConfigurationAzure.getAggregationOutputMongoDataSource().getPassword());
    }

    @Override
    public List<BillingData> getBillingData() {
        try {
            BillingPeriod billingPeriod = getBillingPeriod();
            DateTime currentTime = new DateTime().withZone(DateTimeZone.UTC);
            if (billingPeriod == null) {
                saveBillingPeriod(initialSchedulerInfo(currentTime));
            } else {
                log.info("Billing period from db is {}", billingPeriod);

                if (shouldTriggerJobByTime(currentTime, billingPeriod)) {
                    List<BillingData> billingData = getBillingData(billingPeriod);
                    boolean hasNew = !billingData.isEmpty();
                    updateBillingPeriod(billingPeriod, currentTime, hasNew);
                    return billingData;
                }
            }
        } catch (RuntimeException e) {
            log.error("Cannot update billing information", e);
        }
        return Collections.emptyList();
    }

    private BillingPeriod initialSchedulerInfo(DateTime currentTime) {

        BillingPeriod initialBillingPeriod = new BillingPeriod();
        initialBillingPeriod.setFrom(currentTime.minusDays(2).toDateMidnight().toDate());
        initialBillingPeriod.setTo(currentTime.toDateMidnight().toDate());

        log.info("Initial scheduler info {}", initialBillingPeriod);

        return initialBillingPeriod;

    }

    private boolean shouldTriggerJobByTime(DateTime currentTime, BillingPeriod billingPeriod) {

        DateTime dateTimeToFromBillingPeriod = new DateTime(billingPeriod.getTo()).withZone(DateTimeZone.UTC);

        log.info("Comparing current time[{}, {}] and from scheduler info [{}, {}]", currentTime,
                currentTime.toDateMidnight(),
                dateTimeToFromBillingPeriod, dateTimeToFromBillingPeriod.toDateMidnight());

        if (currentTime.toDateMidnight().isAfter(dateTimeToFromBillingPeriod.toDateMidnight())
                || currentTime.toDateMidnight().isEqual(dateTimeToFromBillingPeriod.toDateMidnight())) {
            log.info("Should trigger the job by time");
            return true;
        }

        log.info("Should not trigger the job by time");
        return false;
    }

    private List<BillingData> getBillingData(BillingPeriod billingPeriod) {
        AzureInvoiceCalculationService azureInvoiceCalculationService
                = new AzureInvoiceCalculationService(billingConfigurationAzure);

        List<AzureDailyResourceInvoice> dailyInvoices = azureInvoiceCalculationService.generateInvoiceData(
                DATE_TIME_FORMATTER.print(new DateTime(billingPeriod.getFrom()).withZone(DateTimeZone.UTC)),
                DATE_TIME_FORMATTER.print(new DateTime(billingPeriod.getTo()).withZone(DateTimeZone.UTC)));

        if (!dailyInvoices.isEmpty()) {
            return dailyInvoices
                    .stream()
                    .filter(Objects::nonNull)
                    .map(this::toBillingData)
                    .collect(Collectors.toList());
        } else {
            log.warn("Daily invoices is empty for period {}", billingPeriod);
            return Collections.emptyList();
        }
    }

    private void updateBillingPeriod(BillingPeriod billingPeriod, DateTime currentTime, boolean updates) {

        try {
            mongoDbBillingClient.getDatabase().getCollection(MongoKeyWords.AZURE_BILLING_SCHEDULER_HISTORY).insertOne(
                    Document.parse(objectMapper.writeValueAsString(billingPeriod)).append("updates", updates));
            log.debug("History of billing periods is updated with {}",
                    objectMapper.writeValueAsString(billingPeriod));
        } catch (JsonProcessingException e) {
            log.error("Cannot update history of billing periods", e);

        }

        billingPeriod.setFrom(billingPeriod.getTo());

        if (new DateTime(billingPeriod.getFrom()).withZone(DateTimeZone.UTC).toDateMidnight()
                .isEqual(currentTime.toDateMidnight())) {

            log.info("Setting billing to one day later");
            billingPeriod.setTo(currentTime.plusDays(1).toDateMidnight().toDate());

        } else {
            billingPeriod.setTo(currentTime.toDateMidnight().toDate());
        }

        saveBillingPeriod(billingPeriod);
    }

    private boolean saveBillingPeriod(BillingPeriod billingPeriod) {
        log.debug("Saving billing period {}", billingPeriod);

        try {
            UpdateResult updateResult = mongoDbBillingClient.getDatabase().getCollection(MongoKeyWords.AZURE_BILLING_SCHEDULER)
                    .updateMany(Filters.eq(MongoKeyWords.MONGO_ID, SCHEDULER_ID),
                            new BasicDBObject("$set",
                                    Document.parse(objectMapper.writeValueAsString(billingPeriod))
                                            .append(MongoKeyWords.MONGO_ID, SCHEDULER_ID))
                            , new UpdateOptions().upsert(true)
                    );

            log.debug("Billing period save operation result is {}", updateResult);
            return true;
        } catch (JsonProcessingException e) {
            log.error("Cannot save billing period", e);
        }

        return false;
    }

    private BillingPeriod getBillingPeriod() {
        log.debug("Get billing period");

        try {
            Document document = mongoDbBillingClient.getDatabase().getCollection(MongoKeyWords.AZURE_BILLING_SCHEDULER)
                    .find(Filters.eq(MongoKeyWords.MONGO_ID, SCHEDULER_ID)).first();

            log.debug("Retrieved billing period document {}", document);
            if (document != null) {
                return objectMapper.readValue(document.toJson(), BillingPeriod.class);
            }

            return null;

        } catch (IOException e) {
            log.error("Cannot save billing period", e);
            throw new DatalabException("Cannot parse string", e);
        }
    }

    private BillingData toBillingData(AzureDailyResourceInvoice billingData) {
        return BillingData.builder()
                .tag(billingData.getDatalabId().toLowerCase())
                .usageDateFrom(Optional.ofNullable(billingData.getUsageStartDate()).map(LocalDate::parse).orElse(null))
                .usageDateTo(Optional.ofNullable(billingData.getUsageEndDate()).map(LocalDate::parse).orElse(null))
                .usageDate(billingData.getDay())
                .product(billingData.getMeterCategory())
                .cost(billingData.getCost())
                .currency(billingData.getCurrencyCode())
                .build();
    }
}
