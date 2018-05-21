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

package com.epam.dlab.billing.azure.rate;

import com.epam.dlab.billing.azure.config.BillingConfigurationAzure;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.client.ClientProperties;

import javax.ws.rs.ClientErrorException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Slf4j
public class AzureRateCardClient {
    public static final String MAIN_RATE_KEY = "0";
    private BillingConfigurationAzure billingConfigurationAzure;
    private String authToken;

    public AzureRateCardClient(BillingConfigurationAzure billingConfigurationAzure, String authToken) {
        this.billingConfigurationAzure = billingConfigurationAzure;
        this.authToken = authToken;
    }

    public RateCardResponse getRateCard() {
        Client client = null;

        try {
            client = ClientBuilder.newClient();

            Response response = client
                    .target("https://management.azure.com/subscriptions/{subscriptionId}/providers/Microsoft.Commerce/RateCard")
                    .resolveTemplate("subscriptionId", billingConfigurationAzure.getSubscriptionId())
                    .property(ClientProperties.FOLLOW_REDIRECTS, Boolean.FALSE)
                    .queryParam("api-version", "2016-08-31-preview")
                    .queryParam("$filter",
                            String.format("OfferDurableId eq '%s' and Currency eq '%s' and Locale eq '%s' and RegionInfo eq '%s'",
                                    billingConfigurationAzure.getOfferNumber(), billingConfigurationAzure.getCurrency(),
                                    billingConfigurationAzure.getLocale(), billingConfigurationAzure.getRegionInfo()))
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .header("Authorization", String.format("Bearer %s", authToken))
                    .get(Response.class);

            final RateCardResponse rateCardResponse = client.target(response.getLocation())
                    .request(MediaType.APPLICATION_JSON_TYPE)
                    .get(RateCardResponse.class);

            log.info("RateCard is retrieved. Meter counts is {}", rateCardResponse.getMeters().size());

            return rateCardResponse;

        } catch (ClientErrorException e) {
            log.error("Cannot get rate card due to {}", (e.getResponse() != null && e.getResponse().hasEntity())
                    ? e.getResponse().readEntity(String.class) : "");
            log.error("Error during using RateCard API", e);
            throw e;
        } catch (RuntimeException e) {
            log.error("Cannot retrieve rate card due to ", e);
            throw e;
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }
}
