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

package com.epam.datalab.billing.azure.rate;

import com.epam.datalab.billing.azure.config.BillingConfigurationAzure;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.ProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultRedirectStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpContext;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.function.Predicate;

@Slf4j
public class AzureRateCardClient {
    private ObjectMapper objectMapper = new ObjectMapper();
    public static final String MAIN_RATE_KEY = "0";
    private BillingConfigurationAzure billingConfigurationAzure;
    private String authToken;

    public AzureRateCardClient(BillingConfigurationAzure billingConfigurationAzure, String authToken) {
        this.billingConfigurationAzure = billingConfigurationAzure;
        this.authToken = authToken;
    }

    public RateCardResponse getRateCard() throws IOException, URISyntaxException {

        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setRedirectStrategy(new CustomRedirectWithoutAuthorizationStrategy())
                .build()) {
            final URIBuilder uriBuilder = new URIBuilder("https://management.azure.com/subscriptions/" +
                    billingConfigurationAzure.getSubscriptionId() + "/providers/Microsoft.Commerce/RateCard")
                    .addParameter("api-version", "2016-08-31-preview")
                    .addParameter("$filter", String.format("OfferDurableId eq '%s' and Currency eq '%s' and Locale " +
                                    "eq '%s' and RegionInfo eq '%s'", billingConfigurationAzure.getOfferNumber(),
                            billingConfigurationAzure.getCurrency(), billingConfigurationAzure.getLocale(),
                            billingConfigurationAzure.getRegionInfo()));

            final HttpGet request = new HttpGet(uriBuilder.build());
            request.addHeader("Authorization", String.format("Bearer %s", authToken));
            request.addHeader(HttpHeaders.ACCEPT, "application/json");
            return objectMapper.readValue(EntityUtils.toString
                    (httpClient.execute(request).getEntity()), RateCardResponse.class);
        } catch (IOException | URISyntaxException e) {
            log.error("Cannot retrieve rate card due to ", e);
            throw e;
        }
    }

    class CustomRedirectWithoutAuthorizationStrategy extends DefaultRedirectStrategy {
        @Override
        public HttpUriRequest getRedirect(HttpRequest request, HttpResponse response, HttpContext context) throws
                ProtocolException {
            if (HttpGet.METHOD_NAME.equals(request.getRequestLine().getMethod())) {
                return httpGetWithoutAuthorization(request, response, context);
            } else {
                return super.getRedirect(request, response, context);
            }
        }

        private HttpUriRequest httpGetWithoutAuthorization(HttpRequest request, HttpResponse response, HttpContext
                context) throws ProtocolException {
            return new HttpGet(getLocationURI(request, response, context)) {
                @Override
                public void setHeaders(Header[] headers) {
                    super.setHeaders(filter(headers, h -> !h.getName().equals(HttpHeaders.AUTHORIZATION)));
                }

                private Header[] filter(Header[] headers, Predicate<Header> predicate) {
                    return Arrays.stream(headers)
                            .filter(predicate)
                            .toArray(Header[]::new);
                }
            };
        }
    }


}
