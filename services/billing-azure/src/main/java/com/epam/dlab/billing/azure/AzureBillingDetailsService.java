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

package com.epam.dlab.billing.azure;

import com.epam.dlab.MongoKeyWords;
import com.epam.dlab.billing.BillingCalculationUtils;
import com.epam.dlab.billing.DlabResourceType;
import com.google.common.collect.Lists;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.*;
import com.mongodb.client.result.UpdateResult;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Slf4j
public class AzureBillingDetailsService {
	private MongoDbBillingClient mongoDbBillingClient;
	private String currencyCode;

	public AzureBillingDetailsService(MongoDbBillingClient mongoDbBillingClient, String currencyCode) {
		this.mongoDbBillingClient = mongoDbBillingClient;
		this.currencyCode = currencyCode;
	}

	public void updateBillingDetails() {
		final List<String> users = new ArrayList<>();
		FindIterable<Document> iterable = mongoDbBillingClient.getDatabase()
				.getCollection(MongoKeyWords.EDGE_COLLECTION)
				.find().projection(Projections.include(MongoKeyWords.MONGO_ID));

		for (Document document : iterable) {
			String user = document.getString(MongoKeyWords.MONGO_ID);
			if (StringUtils.isNotEmpty(user)) {
				users.add(user);
			} else {
				log.warn("Empty user is found");
			}
		}

		if (!users.isEmpty()) {
			users.forEach(this::updateBillingDetails);
		} else {
			log.warn("No users found");
		}
	}

	public void updateBillingDetails(String user) {
		log.debug("Updating billing details for user {}", user);

		try {
			AggregateIterable<Document> aggregateIterable = mongoDbBillingClient.getDatabase()
					.getCollection(MongoKeyWords.BILLING_DETAILS)
					.aggregate(Lists.newArrayList(
							Aggregates.match(
									Filters.and(
											Filters.eq(MongoKeyWords.DLAB_USER, user),
											Filters.in(MongoKeyWords.RESOURCE_TYPE,
													DlabResourceType.EXPLORATORY.toString(),
													DlabResourceType.COMPUTATIONAL.toString())
									)
							),

							Aggregates.group(getGroupingFields(
									MongoKeyWords.DLAB_ID,
									MongoKeyWords.DLAB_USER,
									MongoKeyWords.EXPLORATORY_ID,
									MongoKeyWords.RESOURCE_TYPE,
									MongoKeyWords.RESOURCE_NAME,
									MongoKeyWords.COMPUTATIONAL_ID,
									MongoKeyWords.METER_CATEGORY),
									Accumulators.sum(MongoKeyWords.COST, MongoKeyWords.prepend$(MongoKeyWords.COST)),
									Accumulators.min(MongoKeyWords.USAGE_FROM, MongoKeyWords.prepend$(MongoKeyWords
                                            .USAGE_DAY)),
									Accumulators.max(MongoKeyWords.USAGE_TO, MongoKeyWords.prepend$(MongoKeyWords
                                            .USAGE_DAY))
							),

							Aggregates.sort(Sorts.ascending(
									MongoKeyWords.prependId(MongoKeyWords.RESOURCE_NAME),
									MongoKeyWords.prependId(MongoKeyWords.METER_CATEGORY)))
							)
					);

			updateBillingDetails(user, mapToDetails(aggregateIterable));
		} catch (RuntimeException e) {
			log.error("Updating billing details for user {} is failed", user, e);
		}
	}

	private List<Document> mapToDetails(AggregateIterable<Document> aggregateIterable) {
		List<Document> billingDetails = new ArrayList<>();
		for (Document document : aggregateIterable) {
			Document oldRef = (Document) document.get(MongoKeyWords.MONGO_ID);
			Document newDocument = new Document();

			newDocument.append(MongoKeyWords.USAGE_FROM, document.getString(MongoKeyWords.USAGE_FROM));
			newDocument.append(MongoKeyWords.USAGE_TO, document.getString(MongoKeyWords.USAGE_TO));
			newDocument.append(MongoKeyWords.COST, document.getDouble(MongoKeyWords.COST));

			newDocument.append(MongoKeyWords.METER_CATEGORY, oldRef.getString(MongoKeyWords.METER_CATEGORY));
			newDocument.append(MongoKeyWords.RESOURCE_NAME, oldRef.getString(MongoKeyWords.RESOURCE_NAME));
			newDocument.append(MongoKeyWords.EXPLORATORY_ID, oldRef.getString(MongoKeyWords.EXPLORATORY_ID));
			newDocument.append(MongoKeyWords.RESOURCE_TYPE, oldRef.getString(MongoKeyWords.RESOURCE_TYPE));
			newDocument.append(MongoKeyWords.CURRENCY_CODE, currencyCode);

			billingDetails.add(newDocument);
		}

		return billingDetails;
	}


	private void updateBillingDetails(String user, List<Document> billingDetails) {
		if (!billingDetails.isEmpty()) {
			Map<String, List<Document>> info = new HashMap<>();

			Consumer<Document> aggregator = e -> {

				String notebookId = e.getString(MongoKeyWords.EXPLORATORY_ID);
				List<Document> documents = info.get(notebookId);
				if (documents == null) {
					documents = new ArrayList<>();
				}

				documents.add(e);
				info.put(notebookId, documents);
			};

			billingDetails.stream()
					.filter(e -> DlabResourceType.EXPLORATORY.toString().equals(e.getString(MongoKeyWords
                            .RESOURCE_TYPE)))
					.forEach(aggregator);

			billingDetails.stream()
					.filter(e -> DlabResourceType.COMPUTATIONAL.toString().equals(e.getString(MongoKeyWords
                            .RESOURCE_TYPE)))
					.forEach(aggregator);


			for (Map.Entry<String, List<Document>> entry : info.entrySet()) {
				double sum = entry.getValue().stream().mapToDouble(e -> e.getDouble(MongoKeyWords.COST)).sum();

				entry.getValue().forEach(e -> e.put(MongoKeyWords.COST_STRING,
						BillingCalculationUtils.formatDouble(e.getDouble(MongoKeyWords.COST))));

				log.debug("Update billing for notebook {}, cost is {} {}", entry.getKey(), sum, currencyCode);

				Bson updates = Updates.combine(
						Updates.set(MongoKeyWords.COST_STRING, BillingCalculationUtils.formatDouble(sum)),
						Updates.set(MongoKeyWords.COST, sum),
						Updates.set(MongoKeyWords.CURRENCY_CODE, currencyCode),
						Updates.set(MongoKeyWords.BILLING_DETAILS, entry.getValue()));

				UpdateResult updateResult = mongoDbBillingClient.getDatabase()
						.getCollection(MongoKeyWords.NOTEBOOK_COLLECTION)
						.updateOne(
								Filters.and(
										Filters.eq(MongoKeyWords.DLAB_USER, user),
										Filters.eq(MongoKeyWords.EXPLORATORY_ID_OLD, entry.getKey())
								),
								updates
						);

				log.debug("Update result for {}/{} is {}", user, entry.getKey(), updateResult);
			}
		} else {
			log.warn("No billing details found for notebooks for user {}", user);
		}
	}


	private Document getGroupingFields(String... fieldNames) {
		Document d = new Document();
		for (String name : fieldNames) {
			d.put(name, "$" + name);
		}
		return d;
	}
}
