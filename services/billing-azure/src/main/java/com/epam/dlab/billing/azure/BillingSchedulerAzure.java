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
import com.epam.dlab.billing.azure.config.AzureAuthFile;
import com.epam.dlab.billing.azure.config.BillingConfigurationAzure;
import com.epam.dlab.billing.azure.logging.AppenderConsole;
import com.epam.dlab.billing.azure.logging.AppenderFile;
import com.epam.dlab.billing.azure.model.AzureDailyResourceInvoice;
import com.epam.dlab.billing.azure.model.AzureDlabBillableResource;
import com.epam.dlab.billing.azure.model.BillingPeriod;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.exceptions.InitializationException;
import com.epam.dlab.util.mongo.IsoDateModule;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
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

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class BillingSchedulerAzure {
	private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();
	private BillingConfigurationAzure billingConfigurationAzure;
	private MongoDbBillingClient mongoDbBillingClient;

	public BillingSchedulerAzure(String filePath) throws IOException, InitializationException {
		try (FileInputStream fin = new FileInputStream(filePath)) {
			final ObjectMapper objectMapper = new ObjectMapper(new YAMLFactory()).registerModule(new GuavaModule());
			objectMapper.registerSubtypes(AppenderFile.class, AppenderConsole.class);
			this.billingConfigurationAzure = objectMapper.readValue(fin,
							BillingConfigurationAzure.class);

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
			this.billingConfigurationAzure.getLogging().configure();
		}
	}

	public static void main(String[] args) throws Exception {
		if (args != null && args.length == 2) {
			BillingSchedulerAzure billingSchedulerAzure = new BillingSchedulerAzure(args[1]);
			billingSchedulerAzure.start();

		} else {
			log.info("Wrong arguments. Please provide with path to billing configuration");
		}
	}

	public void start() {
		if (billingConfigurationAzure.isBillingEnabled()) {
			executorService.scheduleWithFixedDelay(new CalculateBilling(billingConfigurationAzure,
							mongoDbBillingClient), billingConfigurationAzure.getInitialDelay(),
					billingConfigurationAzure.getPeriod(), TimeUnit.MINUTES);
		} else {
			log.info("======Billing is disabled======");
		}
	}

	public void stop() {
		try {
			log.info("Stopping Azure billing scheduler");
			if (!executorService.awaitTermination(30, TimeUnit.SECONDS)) {
				log.error("Force shut down");
				executorService.shutdownNow();
			}
			mongoDbBillingClient.getClient().close();
		} catch (InterruptedException e) {
			executorService.shutdownNow();
			mongoDbBillingClient.getClient().close();
			Thread.currentThread().interrupt();
		}
	}


	@Slf4j
	private static class CalculateBilling implements Runnable {
		private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormat.forPattern
				("yyyy-MM-dd'T'HH:mm:ss" +
						".SSS'Z");
		private static final String SCHEDULER_ID = "azureBillingScheduler";
		private AzureBillingDetailsService azureBillingDetailsService;
		private BillingConfigurationAzure billingConfigurationAzure;
		private MongoDbBillingClient client;
		private ObjectMapper objectMapper = new ObjectMapper().registerModule(new IsoDateModule());


		public CalculateBilling(BillingConfigurationAzure billingConfigurationAzure, MongoDbBillingClient client) {
			this.billingConfigurationAzure = billingConfigurationAzure;
			this.client = client;
			this.azureBillingDetailsService = new AzureBillingDetailsService(client,
					billingConfigurationAzure.getCurrency());
		}

		@Override
		public void run() {
			try {
				BillingPeriod billingPeriod = getBillingPeriod();
				DateTime currentTime = new DateTime().withZone(DateTimeZone.UTC);
				if (billingPeriod == null) {
					saveBillingPeriod(initialSchedulerInfo(currentTime));
				} else {
					log.info("Billing period from db is {}", billingPeriod);

					if (shouldTriggerJobByTime(currentTime, billingPeriod)) {

						boolean hasNew = run(billingPeriod);
						if (hasNew) {
							log.info("Updating billing details");
							azureBillingDetailsService.updateBillingDetails();
						}


						updateBillingPeriod(billingPeriod, currentTime, hasNew);
					}
				}
			} catch (RuntimeException e) {
				log.error("Cannot update billing information", e);
			}
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

		private boolean run(BillingPeriod billingPeriod) {

			AzureBillableResourcesService azureBillableResourcesService = new AzureBillableResourcesService(client);
			Set<AzureDlabBillableResource> billableResources = azureBillableResourcesService.getBillableResources();

			AzureInvoiceCalculationService azureInvoiceCalculationService
					= new AzureInvoiceCalculationService(billingConfigurationAzure, billableResources);

			List<AzureDailyResourceInvoice> dailyInvoices = azureInvoiceCalculationService.generateInvoiceData(
					DATE_TIME_FORMATTER.print(new DateTime(billingPeriod.getFrom()).withZone(DateTimeZone.UTC)),
					DATE_TIME_FORMATTER.print(new DateTime(billingPeriod.getTo()).withZone(DateTimeZone.UTC)));


			if (!dailyInvoices.isEmpty()) {

				client.getDatabase().getCollection(MongoKeyWords.BILLING_DETAILS)
						.insertMany(dailyInvoices.stream().map(AzureDailyResourceInvoice::to)
								.collect(Collectors.toList()));

				return true;

			} else {
				log.warn("Daily invoices is empty for period {}", billingPeriod);

				return false;
			}
		}

		private void updateBillingPeriod(BillingPeriod billingPeriod, DateTime currentTime, boolean updates) {

			try {
				client.getDatabase().getCollection(MongoKeyWords.AZURE_BILLING_SCHEDULER_HISTORY).insertOne(
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
				UpdateResult updateResult = client.getDatabase().getCollection(MongoKeyWords.AZURE_BILLING_SCHEDULER)
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
				Document document = client.getDatabase().getCollection(MongoKeyWords.AZURE_BILLING_SCHEDULER)
						.find(Filters.eq(MongoKeyWords.MONGO_ID, SCHEDULER_ID)).first();

				log.debug("Retrieved billing period document {}", document);
				if (document != null) {
					return objectMapper.readValue(document.toJson(), BillingPeriod.class);
				}

				return null;

			} catch (IOException e) {
				log.error("Cannot save billing period", e);
				throw new DlabException("Cannot parse string", e);
			}
		}
	}
}
