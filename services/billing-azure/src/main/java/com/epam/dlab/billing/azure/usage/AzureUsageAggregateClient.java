/*
 * Copyright (c) 2017, EPAM SYSTEMS INC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.dlab.billing.azure.usage;

import com.epam.dlab.billing.azure.config.BillingConfigurationAzure;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import java.io.IOException;

@Slf4j
public class AzureUsageAggregateClient {
    private ObjectMapper objectMapper = new ObjectMapper();
    private BillingConfigurationAzure billingConfigurationAzure;
    private String authToken;

    public AzureUsageAggregateClient(BillingConfigurationAzure billingConfigurationAzure, String authToken) {
        this.billingConfigurationAzure = billingConfigurationAzure;
        this.authToken = authToken;
    }

    public UsageAggregateResponse getUsageAggregateResponse(String from, String to) throws IOException {
        Client client = null;

        try {
            client = ClientBuilder.newClient();

            UsageAggregateResponse usageAggregateResponse = client
                    .target("https://management.azure.com/subscriptions/{subscriptionId}/providers/Microsoft.Commerce/UsageAggregates")
                    .resolveTemplate("subscriptionId", billingConfigurationAzure.getSubscriptionId())
                    .queryParam("api-version", "2015-06-01-preview")
                    .queryParam("reportedStartTime", from)
                    .queryParam("reportedEndTime", to)
                    .queryParam("aggregationGranularity", "daily")
                    .queryParam("showDetails", false)
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .header("Authorization", String.format("Bearer %s", authToken))
                    .get(UsageAggregateResponse.class);

            return postProcess(usageAggregateResponse);

        } catch (ClientErrorException e) {
            log.error("Cannot get usage details due to {}", (e.getResponse() != null && e.getResponse().hasEntity())
                    ? e.getResponse().readEntity(String.class) : "");
            log.error("Error during using Usage Details API", e);
            throw e;
        } catch (IOException | RuntimeException e) {
            log.error("Cannot retrieve usage detail due to ", e);
            throw e;
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }


    public UsageAggregateResponse getUsageAggregateResponse(String nextUrl) throws IOException {
        Client client = null;

        try {
            client = ClientBuilder.newClient();

            UsageAggregateResponse usageAggregateResponse = client
                    .target(nextUrl)
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .header("Authorization", String.format("Bearer %s", authToken))
                    .get(UsageAggregateResponse.class);

            return postProcess(usageAggregateResponse);

        } catch (IOException | RuntimeException e) {
            log.error("Cannot retrieve rate card due to ", e);
            throw e;
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    private UsageAggregateResponse postProcess(UsageAggregateResponse usageAggregateResponse) throws IOException {
        for (UsageAggregateRecord usageAggregateRecord : usageAggregateResponse.getValue()) {
            InstanceData instanceData = objectMapper.readValue(usageAggregateRecord.getProperties().getInstanceData(),
                    InstanceData.class);

            usageAggregateRecord.getProperties().setParsedInstanceData(instanceData);
        }

        return usageAggregateResponse;
    }
}
