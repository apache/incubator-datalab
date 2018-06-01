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
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.IOException;
import java.net.URISyntaxException;

@Slf4j
public class AzureUsageAggregateClient {
	private ObjectMapper objectMapper = new ObjectMapper();
	private BillingConfigurationAzure billingConfigurationAzure;
	private String authToken;

	public AzureUsageAggregateClient(BillingConfigurationAzure billingConfigurationAzure, String authToken) {
		this.billingConfigurationAzure = billingConfigurationAzure;
		this.authToken = authToken;
	}

	public UsageAggregateResponse getUsageAggregateResponse(String from, String to) throws IOException,
			URISyntaxException {
		try (CloseableHttpClient httpClient = HttpClients.createDefault()) {

			final URIBuilder uriBuilder = new URIBuilder("https://management.azure.com/subscriptions/" +
					billingConfigurationAzure.getSubscriptionId() + "/providers/Microsoft" +
					".Commerce/UsageAggregates")
					.addParameter("api-version", "2015-06-01-preview")
					.addParameter("reportedStartTime", from)
					.addParameter("reportedEndTime", to)
					.addParameter("aggregationGranularity", "daily")
					.addParameter("showDetails", "false");
			final HttpGet request = new HttpGet(uriBuilder.build());
			request.addHeader("Authorization", String.format("Bearer %s", authToken));
			request.addHeader(HttpHeaders.ACCEPT, "application/json");
			final UsageAggregateResponse usageAggregateResponse = objectMapper.readValue(EntityUtils.toString
					(httpClient.execute(request).getEntity()), UsageAggregateResponse.class);
			return postProcess(usageAggregateResponse);
		} catch (URISyntaxException e) {
			log.error("Cannot retrieve usage detail due to ", e);
			throw e;
		}
	}


	public UsageAggregateResponse getUsageAggregateResponse(String nextUrl) throws IOException {
		try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
			final HttpGet request = new HttpGet(nextUrl);
			request.addHeader("Authorization", String.format("Bearer %s", authToken));
			request.addHeader(HttpHeaders.ACCEPT, "application/json");
			final UsageAggregateResponse usageAggregateResponse = objectMapper.readValue(EntityUtils.toString
					(httpClient.execute(request).getEntity()), UsageAggregateResponse.class);
			return postProcess(usageAggregateResponse);
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
