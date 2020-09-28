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

import com.epam.datalab.billing.BillingCalculationUtils;
import com.epam.datalab.billing.azure.config.BillingConfigurationAzure;
import com.epam.datalab.billing.azure.model.AzureDailyResourceInvoice;
import com.epam.datalab.billing.azure.rate.AzureRateCardClient;
import com.epam.datalab.billing.azure.rate.Meter;
import com.epam.datalab.billing.azure.rate.RateCardResponse;
import com.epam.datalab.billing.azure.usage.AzureUsageAggregateClient;
import com.epam.datalab.billing.azure.usage.UsageAggregateRecord;
import com.epam.datalab.billing.azure.usage.UsageAggregateResponse;
import com.epam.datalab.exceptions.DatalabException;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Filters billing data and calculate prices for each
 * resource using combination of Microsoft Azure RateCard API and Usage API
 */
@Slf4j
public class AzureInvoiceCalculationService {

    /**
     * According to Microsoft Azure documentation
     * https://docs.microsoft.com/en-us/azure/active-directory/active-directory-configurable-token-lifetimes
     * min TTL time of token 10 minutes
     */
    private static final long MAX_AUTH_TOKEN_TTL_MILLIS = 9L * 60L * 1000L;

    private BillingConfigurationAzure billingConfigurationAzure;

    /**
     * Constructs service class
     *
     * @param billingConfigurationAzure contains <code>billing-azure</code> module configuration
     */
    public AzureInvoiceCalculationService(BillingConfigurationAzure billingConfigurationAzure) {
        this.billingConfigurationAzure = billingConfigurationAzure;
    }

    /**
     * Prepares invoice records aggregated by day
     *
     * @param from start usage period
     * @param to   end usage period
     * @return list of invoice records for each meter category aggregated by day
     */
    public List<AzureDailyResourceInvoice> generateInvoiceData(String from, String to) {

        long refreshTokenTime = System.currentTimeMillis() + MAX_AUTH_TOKEN_TTL_MILLIS;

        String authenticationToken = getNewToken();
        AzureRateCardClient azureRateCardClient = new AzureRateCardClient(billingConfigurationAzure,
                authenticationToken);
        AzureUsageAggregateClient azureUsageAggregateClient = new AzureUsageAggregateClient(billingConfigurationAzure,
                authenticationToken);

        List<AzureDailyResourceInvoice> invoiceData = new ArrayList<>();

        try {

            UsageAggregateResponse usageAggregateResponse = null;
            Map<String, Meter> rates = transformRates(azureRateCardClient.getRateCard());

            do {

                if (usageAggregateResponse != null && StringUtils.isNotEmpty(usageAggregateResponse.getNextLink())) {
                    log.info("Get usage of resources using link {}", usageAggregateResponse.getNextLink());
                    usageAggregateResponse = azureUsageAggregateClient.getUsageAggregateResponse
                            (usageAggregateResponse.getNextLink());
                    log.info("Received usage of resources. Items {} ", usageAggregateResponse.getValue() != null ?
                            usageAggregateResponse.getValue().size() : 0);
                    log.info("Next link is {}", usageAggregateResponse.getNextLink());
                } else if (usageAggregateResponse == null) {
                    log.info("Get usage of resources from {} to {}", from, to);
                    usageAggregateResponse = azureUsageAggregateClient.getUsageAggregateResponse(from, to);
                    log.info("Received usage of resources. Items {} ", usageAggregateResponse.getValue() != null ?
                            usageAggregateResponse.getValue().size() : 0);
                    log.info("Next link is {}", usageAggregateResponse.getNextLink());
                }

                invoiceData.addAll(generateBillingInfo(rates, usageAggregateResponse));

                if (System.currentTimeMillis() > refreshTokenTime) {
                    authenticationToken = getNewToken();
                    azureUsageAggregateClient.setAuthToken(authenticationToken);
                }

            } while (StringUtils.isNotEmpty(usageAggregateResponse.getNextLink()));

        } catch (IOException | RuntimeException | URISyntaxException e) {
            log.error("Cannot calculate billing information", e);
            throw new DatalabException("Cannot prepare invoice data", e);
        }

        return invoiceData;
    }

    private List<AzureDailyResourceInvoice> generateBillingInfo(Map<String, Meter> rates, UsageAggregateResponse
            usageAggregateResponse) {
        List<UsageAggregateRecord> usageAggregateRecordList = usageAggregateResponse.getValue();
        List<AzureDailyResourceInvoice> invoices = new ArrayList<>();

        if (usageAggregateRecordList != null && !usageAggregateRecordList.isEmpty()) {
            log.info("Processing {} usage records", usageAggregateRecordList.size());
            usageAggregateRecordList = usageAggregateRecordList.stream().filter(e ->
                    matchProperStructure(e) && isBillableDatalabResource(e))
                    .collect(Collectors.toList());

            log.info("Applicable records number is {}", usageAggregateRecordList.size());

            for (UsageAggregateRecord record : usageAggregateRecordList) {
                invoices.add(calculateInvoice(rates, record, record.getProperties().getParsedInstanceData()
                        .getMicrosoftResources().getTags().get("Name")));
            }
        } else {
            log.error("No usage records in response.");
        }

        return invoices;
    }

    private Map<String, Meter> transformRates(RateCardResponse rateCardResponse) {
        Map<String, Meter> rates = new HashMap<>();
        for (Meter meter : rateCardResponse.getMeters()) {
            rates.put(meter.getMeterId(), meter);
        }

        return rates;
    }

    private boolean matchProperStructure(UsageAggregateRecord record) {
        if (record.getProperties() == null) {
            return false;
        }

        if (record.getProperties().getMeterId() == null || record.getProperties().getMeterId().isEmpty()) {
            return false;
        }

        return !(record.getProperties().getParsedInstanceData() == null
                || record.getProperties().getParsedInstanceData().getMicrosoftResources() == null
                || record.getProperties().getParsedInstanceData().getMicrosoftResources().getTags() == null
                || record.getProperties().getParsedInstanceData().getMicrosoftResources().getTags().isEmpty());
    }

    private boolean isBillableDatalabResource(UsageAggregateRecord record) {
        String datalabId = record.getProperties().getParsedInstanceData().getMicrosoftResources().getTags().get("Name");
        return datalabId != null && !datalabId.isEmpty() && datalabId.startsWith(billingConfigurationAzure.getSbn());
    }

    private AzureDailyResourceInvoice calculateInvoice(Map<String, Meter> rates, UsageAggregateRecord record, String datalabId) {
        String meterId = record.getProperties().getMeterId();
        Meter rateCard = rates.get(meterId);

        if (rateCard != null) {
            Map<String, Double> meterRates = rateCard.getMeterRates();
            if (meterRates != null) {
                Double rate = meterRates.get(AzureRateCardClient.MAIN_RATE_KEY);
                if (rate != null) {
                    return AzureDailyResourceInvoice.builder()
                            .datalabId(datalabId)
                            .usageStartDate(getDay(record.getProperties().getUsageStartTime()))
                            .usageEndDate(getDay(record.getProperties().getUsageEndTime()))
                            .meterCategory(record.getProperties().getMeterCategory())
                            .cost(BillingCalculationUtils.round(rate * record.getProperties().getQuantity(), 3))
                            .day(getDay(record.getProperties().getUsageStartTime()))
                            .currencyCode(billingConfigurationAzure.getCurrency())
                            .build();
                } else {
                    log.error("Rate Card {} has no rate for meter id {} and rate id {}. Skip record {}.",
                            rateCard, meterId, AzureRateCardClient.MAIN_RATE_KEY, record);
                }
            } else {
                log.error("Rate Card {} has no meter rates fro meter id {}. Skip record {}",
                        rateCard, meterId, record);
            }
        } else {
            log.error("Meter rate {} form usage aggregate is not found in rate card. Skip record {}.", meterId, record);
        }

        return null;
    }

    private String getNewToken() {
        try {
            log.info("Requesting authentication token ... ");
            ApplicationTokenCredentials applicationTokenCredentials = new ApplicationTokenCredentials(
                    billingConfigurationAzure.getClientId(),
                    billingConfigurationAzure.getTenantId(),
                    billingConfigurationAzure.getClientSecret(),
                    AzureEnvironment.AZURE);

            return applicationTokenCredentials.getToken(AzureEnvironment.AZURE.resourceManagerEndpoint());
        } catch (IOException e) {
            log.error("Cannot retrieve authentication token due to", e);
            throw new DatalabException("Cannot retrieve authentication token", e);
        }
    }

    private String getDay(String dateTime) {
        if (dateTime != null) {
            String[] parts = dateTime.split("T");
            if (parts.length == 2) {
                return parts[0];
            }
        }

        log.error("Wrong usage date format {} ", dateTime);
        return null;
    }

}
