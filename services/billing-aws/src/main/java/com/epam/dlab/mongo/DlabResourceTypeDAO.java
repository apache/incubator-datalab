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

package com.epam.dlab.mongo;

import com.epam.dlab.billing.BillingCalculationUtils;
import com.epam.dlab.billing.DlabResourceType;
import com.epam.dlab.dto.base.DataEngineType;
import com.epam.dlab.exceptions.InitializationException;
import com.epam.dlab.exceptions.ParseException;
import com.epam.dlab.model.aws.ReportLine;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.Updates;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.*;

import static com.mongodb.client.model.Accumulators.*;
import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Projections.*;
import static org.apache.commons.lang3.StringUtils.EMPTY;

/**
 * Provides Mongo DAO for billing resources in DLab.
 */
public class DlabResourceTypeDAO implements MongoConstants {
	private static final Logger LOGGER = LoggerFactory.getLogger(DlabResourceTypeDAO.class);
	private static final String VOLUME_PRIMARY_SUFFIX = "-volume-primary";
	private static final String VOLUME_SECONDARY_SUFFIX = "-volume-secondary";

	/**
	 * Mongo database connection.
	 */
	private final MongoDbConnection connection;

	/**
	 * Service base name.
	 */
	private String serviceBaseName;
	private String serviceBaseNameId;

	/**
	 * Describe all DLab resources: SSN, EDGE, exploratory, computational and buckets.
	 */
	private ResourceItemList resourceList;

	/**
	 * Instantiate DAO for billing resources.
	 *
	 * @param connection the connection to Mongo DB.
	 * @throws InitializationException
	 */
	public DlabResourceTypeDAO(MongoDbConnection connection) throws InitializationException {
		this.connection = connection;
		setServiceBaseName();
		setResourceList();
	}

	/**
	 * Returns the base name of service.
	 */
	public String getServiceBaseName() {
		return serviceBaseName;
	}

	/**
	 * Set the base name of service.
	 *
	 * @throws InitializationException
	 */
	private void setServiceBaseName() throws InitializationException {
		Document d = connection.getCollection(COLLECTION_SETTINGS)
				.find(eq(FIELD_ID, FIELD_SERIVICE_BASE_NAME))
				.first();
		if (d == null) {
			throw new InitializationException("Service base name property " + COLLECTION_SETTINGS +
					"." + FIELD_SERIVICE_BASE_NAME + " in Mongo DB not found");
		}
		String value = d.getOrDefault("value", EMPTY).toString();
		if (d.isEmpty()) {
			throw new InitializationException("Service base name property " + COLLECTION_SETTINGS +
					"." + FIELD_SERIVICE_BASE_NAME + " in Mongo DB is empty");
		}
		serviceBaseName = value;
		serviceBaseNameId = value + ":";
		LOGGER.debug("serviceBaseName is {}", serviceBaseName);
	}

	/**
	 * Return DLab resources from Mongo DB.
	 *
	 * @throws InitializationException
	 */
	public ResourceItemList getResourceList() {
		return resourceList;
	}

	/**
	 * Load and return DLab resources from Mongo DB.
	 *
	 * @throws InitializationException
	 */
	private void setResourceList() {
		resourceList = new ResourceItemList();

		// Add SSN
		String sbName = getServiceBaseName();
		resourceList.append(sbName + "-ssn", "SSN", DlabResourceType.SSN);
		resourceList.append(sbName + "-ssn-volume-primary", "SSN volume", DlabResourceType.VOLUME);
		resourceList.append(sbName + "-ssn-bucket", "SSN bucket", DlabResourceType.SSN_BUCKET);

		// collaboration bucket
		resourceList.append(sbName + "-shared-bucket", "Collaboration bucket", DlabResourceType
				.COLLABORATION_BUCKET);

		// Add PROJECTS
		Bson projection = fields(include("name", "endpoints"));
		Iterable<Document> docs = connection.getCollection("Projects").find().projection(projection);
		for (Document d : docs) {
			String projectName = d.getString("name");
			((List<Document>) d.get("endpoints"))
					.stream()
					.map(endpoint -> endpoint.getString("name"))
					.forEach(endpoint -> {
						resourceList.append(sbName + "-" + endpoint + "-shared-bucket", "Shared endpoint bucket",
								DlabResourceType.COLLABORATION_BUCKET);
						resourceList.append(sbName + "-" + projectName + "-" + endpoint + "-bucket", "Project bucket",
								DlabResourceType.COLLABORATION_BUCKET, null, null, projectName);
						resourceList.append(sbName + "-" + projectName + "-" + endpoint + "-edge", "EDGE Node",
								DlabResourceType.EDGE, null, null, projectName);
						resourceList.append(sbName + "-" + projectName+ "-" + endpoint + "-edge-volume-primary",
								"EDGE Volume", DlabResourceType.VOLUME, null, null, projectName);
					});
		}

		// Add exploratory
		projection = fields(include(FIELD_USER,
				FIELD_EXPLORATORY_NAME,
				FIELD_EXPLORATORY_ID,
				FIELD_PROJECT,
				FIELD_COMPUTATIONAL_RESOURCES + "." + FIELD_COMPUTATIONAL_ID,
				FIELD_COMPUTATIONAL_RESOURCES + "." + FIELD_COMPUTATIONAL_NAME,
				FIELD_COMPUTATIONAL_RESOURCES + "." + FIELD_IMAGE,
				FIELD_COMPUTATIONAL_RESOURCES + "." + FIELD_DATAENGINE_INSTANCE_COUNT),
				excludeId());
		docs = connection.getCollection(COLLECTION_USER_INSTANCES).find().projection(projection);
		for (Document exp : docs) {
			String username = exp.getString(FIELD_USER);
			String exploratoryName = exp.getString(FIELD_EXPLORATORY_NAME);
			String exploratoryId = exp.getString(FIELD_EXPLORATORY_ID);
			String project = exp.getString(FIELD_PROJECT);
			resourceList.append(exploratoryId, exploratoryName, DlabResourceType.EXPLORATORY, username,
					exploratoryName, project);
			appendExploratoryVolumes(username, exploratoryName, exploratoryId, project);

			// Add computational
			@SuppressWarnings("unchecked")
			List<Document> compList = (List<Document>) exp.get(FIELD_COMPUTATIONAL_RESOURCES);
			if (compList == null) {
				continue;
			}
			for (Document comp : compList) {
				String computationalId = comp.getString(FIELD_COMPUTATIONAL_ID);
				String computationalName = comp.getString(FIELD_COMPUTATIONAL_NAME);
				final DataEngineType dataEngineType = DataEngineType.fromDockerImageName(comp.getString(FIELD_IMAGE));
				resourceList.append(computationalId, computationalName, DlabResourceType.COMPUTATIONAL, username,
						exploratoryName, project);
				if (DataEngineType.CLOUD_SERVICE == dataEngineType) {
					appendDataengineServiceVolumes(username, exploratoryName, computationalId, computationalName,
							project);
				} else {
					appendDataengineVolumes(username, exploratoryName, comp, computationalId, computationalName,
							project);
				}
			}
		}
		LOGGER.debug("resourceList is {}", resourceList);
	}

	private void appendExploratoryVolumes(String username, String exploratoryName, String exploratoryId,
										  String project) {
		resourceList.append(exploratoryId + VOLUME_PRIMARY_SUFFIX, "Volume primary", DlabResourceType.VOLUME,
				username, exploratoryName, project);
		resourceList.append(exploratoryId + VOLUME_SECONDARY_SUFFIX, "Volume secondary", DlabResourceType.VOLUME,
				username, exploratoryName, project);
	}

	private void appendDataengineServiceVolumes(String username, String exploratoryName, String computationalId,
												String computationalName, String project) {
		resourceList.append(computationalId + VOLUME_PRIMARY_SUFFIX, computationalName + " volume primary",
				DlabResourceType.VOLUME, username, exploratoryName, project);
		resourceList.append(computationalId + VOLUME_SECONDARY_SUFFIX, computationalName + " volume secondary",
				DlabResourceType.VOLUME, username, exploratoryName, project);
	}

	private void appendDataengineVolumes(String username, String exploratoryName, Document comp, String
			computationalId, String computationalName, String project) {
		resourceList.append(computationalId + "-m-volume-primary", computationalName + " master volume primary",
				DlabResourceType.VOLUME, username, exploratoryName, project);
		resourceList.append(computationalId + "-m-volume-secondary", computationalName + " master volume secondary",
				DlabResourceType.VOLUME, username, exploratoryName, project);
		final Integer instanceCount = Integer.valueOf(comp.getString(FIELD_DATAENGINE_INSTANCE_COUNT));
		for (int i = instanceCount - 1; i > 0; i--) {
			final String slaveId = computationalId + "-s" + i;
			final String slaveName = computationalName + "-s" + i;
			resourceList.append(slaveId + VOLUME_PRIMARY_SUFFIX, slaveName + " volume primary", DlabResourceType
					.VOLUME, username, exploratoryName, project);
			resourceList.append(slaveId + VOLUME_SECONDARY_SUFFIX, slaveName + " volume secondary", DlabResourceType
					.VOLUME, username, exploratoryName, project);
		}
	}


	/**
	 * Convert and return the report line of billing to Mongo document.
	 *
	 * @param row report line.
	 * @return Mongo document.
	 * @throws ParseException
	 */
	public Document transform(ReportLine row) throws ParseException {
		String resourceId = row.getDlabId();
		if (resourceId == null || !resourceId.startsWith(serviceBaseNameId)) {
			throw new ParseException("DlabId is not match: expected start with " + serviceBaseNameId + ", actual " +
					resourceId);
		}
		resourceId = resourceId.substring(serviceBaseNameId.length());

		ResourceItem resource = resourceList.getById(resourceId);
		Document d = new Document(ReportLine.FIELD_DLAB_ID, resourceId);
		if (resource == null) {
			d.put(FIELD_DLAB_RESOURCE_ID, null);
			d.put(FIELD_DLAB_RESOURCE_TYPE, null);
			d.put(ReportLine.FIELD_USER_ID, null);
			d.put(FIELD_EXPLORATORY_NAME, null);
		} else {
			d.put(FIELD_DLAB_RESOURCE_ID, resource.getResourceId());
			d.put(FIELD_DLAB_RESOURCE_TYPE, resource.getType().toString());
			d.put(ReportLine.FIELD_USER_ID, resource.getUser());
			d.put(FIELD_EXPLORATORY_NAME, resource.getExploratoryName());
			d.put(FIELD_PROJECT, resource.getProject());
		}
		return d.append(ReportLine.FIELD_USAGE_DATE, row.getUsageDate())
				.append(ReportLine.FIELD_PRODUCT, row.getProduct())
				.append(ReportLine.FIELD_USAGE_TYPE, row.getUsageType())
				.append(ReportLine.FIELD_USAGE, row.getUsage())
				.append(ReportLine.FIELD_COST, row.getCost())
				.append(ReportLine.FIELD_CURRENCY_CODE, row.getCurrencyCode())
				.append(ReportLine.FIELD_RESOURCE_TYPE, row.getResourceType().category())
				.append(ReportLine.FIELD_RESOURCE_ID, row.getResourceId())
				.append(ReportLine.FIELD_TAGS, row.getTags());
	}


	/**
	 * Return field condition for groupping.
	 *
	 * @param fieldNames the list of field names.
	 */
	private Document getGrouppingFields(String... fieldNames) {
		Document d = new Document();
		for (String name : fieldNames) {
			d.put(name, "$" + name);
		}
		return d;
	}

	/**
	 * Update monthly total in Mongo DB.
	 *
	 * @param month the month in format YYYY-MM.
	 * @throws InitializationException
	 */
	public void updateMonthTotalCost(String month) throws InitializationException {
		LOGGER.debug("Update total cost for month {}", month);
		try {
			//Check month
			SimpleDateFormat fd = new SimpleDateFormat("yyyy-MM");
			fd.parse(month);
		} catch (java.text.ParseException e) {
			throw new InitializationException("Invalid month value. " + e.getLocalizedMessage(), e);
		}

		List<? extends Bson> pipeline = Arrays.asList(
				match(and(gte(ReportLine.FIELD_USAGE_DATE, month + "-01"),
						lte(ReportLine.FIELD_USAGE_DATE, month + "-31"))),
				group(getGrouppingFields(FIELD_DLAB_RESOURCE_ID,
						FIELD_DLAB_RESOURCE_TYPE,
						FIELD_USER,
						FIELD_EXPLORATORY_NAME,
						ReportLine.FIELD_CURRENCY_CODE,
						ReportLine.FIELD_RESOURCE_TYPE),
						sum(ReportLine.FIELD_COST, "$" + ReportLine.FIELD_COST))
		);
		AggregateIterable<Document> docs = connection.getCollection(COLLECTION_BILLING).aggregate(pipeline);

		MongoCollection<Document> collection = connection.getCollection(COLLECTION_BILLING_TOTAL);
		long deletedCount = collection.deleteMany(eq(ReportLine.FIELD_USAGE_DATE, month)).getDeletedCount();
		LOGGER.debug("{} documents has been deleted from collection {}", deletedCount, COLLECTION_BILLING_TOTAL);
		List<Document> totals = new ArrayList<>();
		for (Document d : docs) {
			Document total = (Document) d.get(FIELD_ID);
			total
					.append(ReportLine.FIELD_USAGE_DATE, month)
					.append(ReportLine.FIELD_COST, d.getDouble(ReportLine.FIELD_COST));
			totals.add(total);
		}
		if (!totals.isEmpty()) {
			LOGGER.debug("{} documents will be inserted into collection {}", totals.size(), COLLECTION_BILLING_TOTAL);
			collection.insertMany(totals);
		}
	}

	/**
	 * Comparator to sort billing exploratory details.
	 */
	class BillingComparator implements Comparator<Document> {
		@Override
		public int compare(Document d1, Document d2) {
			int result = StringUtils.compare(d1.getString(FIELD_RESOURCE_NAME), d2.getString(FIELD_RESOURCE_NAME));
			if (result == 0) {
				result = StringUtils.compare(d1.getString(ReportLine.FIELD_PRODUCT), d2.getString(ReportLine
						.FIELD_PRODUCT));
				if (result == 0) {
					return StringUtils.compare(d1.getString(ReportLine.FIELD_RESOURCE_TYPE), d2.getString(ReportLine
							.FIELD_RESOURCE_TYPE));
				}
			}
			return result;
		}
	}

	/**
	 * Update exploratory cost in Mongo DB.
	 *
	 * @param user            the name of user.
	 * @param exploratoryName id of exploratory.
	 */
	private void updateExploratoryCost(String user, String exploratoryName) {
		LOGGER.debug("Update explorartory {} cost for user {}", exploratoryName, user);
		List<? extends Bson> pipeline = Arrays.asList(
				match(and(eq(FIELD_USER, user),
						eq(FIELD_EXPLORATORY_NAME, exploratoryName))),
				group(getGrouppingFields(FIELD_DLAB_RESOURCE_ID,
						ReportLine.FIELD_PRODUCT,
						ReportLine.FIELD_RESOURCE_TYPE,
						ReportLine.FIELD_CURRENCY_CODE),
						sum(ReportLine.FIELD_COST, "$" + ReportLine.FIELD_COST),
						min(FIELD_USAGE_DATE_START, "$" + ReportLine.FIELD_USAGE_DATE),
						max(FIELD_USAGE_DATE_END, "$" + ReportLine.FIELD_USAGE_DATE)
				),
				sort(new Document(FIELD_ID + "." + FIELD_DLAB_RESOURCE_ID, 1).append(FIELD_ID + "." + ReportLine
						.FIELD_PRODUCT, 1))
		);
		AggregateIterable<Document> docs = connection.getCollection(COLLECTION_BILLING)
				.aggregate(pipeline);
		LinkedList<Document> billing = new LinkedList<>();
		ResourceItemList resources = getResourceList();
		Double costTotal = null;
		String currencyCode = null;
		for (Document d : docs) {
			Document id = (Document) d.get(FIELD_ID);
			double cost = BillingCalculationUtils.round(d.getDouble(ReportLine.FIELD_COST), 2);
			costTotal = (costTotal == null ? cost : costTotal + cost);
			if (currencyCode == null) {
				currencyCode = id.getString(ReportLine.FIELD_CURRENCY_CODE);
			}

			Document total = new Document()
					.append(FIELD_RESOURCE_NAME, resources.getById(id.getString(FIELD_DLAB_RESOURCE_ID))
							.getResourceName())
					.append(ReportLine.FIELD_PRODUCT, id.getString(ReportLine.FIELD_PRODUCT))
					.append(ReportLine.FIELD_RESOURCE_TYPE, id.getString(ReportLine.FIELD_RESOURCE_TYPE))
					.append(ReportLine.FIELD_COST, BillingCalculationUtils.formatDouble(cost))
					.append(ReportLine.FIELD_CURRENCY_CODE, id.getString(ReportLine.FIELD_CURRENCY_CODE))
					.append(FIELD_USAGE_DATE_START, d.getString(FIELD_USAGE_DATE_START))
					.append(FIELD_USAGE_DATE_END, d.getString(FIELD_USAGE_DATE_END));
			billing.add(total);
		}

		LOGGER.debug("Total explorartory {} cost for user {} is {} {}, detail count is {}",
				exploratoryName, user, costTotal, currencyCode, billing.size());
		billing.sort(new BillingComparator());

		MongoCollection<Document> cExploratory = connection.getCollection(COLLECTION_USER_INSTANCES);
		Bson values = Updates.combine(
				Updates.set(ReportLine.FIELD_COST, BillingCalculationUtils.formatDouble(costTotal)),
				Updates.set(FIELD_CURRENCY_CODE, currencyCode),
				Updates.set(COLLECTION_BILLING, (!billing.isEmpty() ? billing : null)));
		cExploratory.updateOne(
				and(and(eq(FIELD_USER, user),
						eq(FIELD_EXPLORATORY_NAME, exploratoryName))),
				values);
	}

	/**
	 * Update EDGE cost in Mongo DB.
	 *
	 * @param user the name of user.
	 */
	private void updateEdgeCost(String user) {
		List<? extends Bson> pipeline = Arrays.asList(
				match(and(eq(FIELD_USER, user),
						eq(FIELD_EXPLORATORY_NAME, null))),
				group(getGrouppingFields(ReportLine.FIELD_CURRENCY_CODE),
						sum(ReportLine.FIELD_COST, "$" + ReportLine.FIELD_COST))
		);
		AggregateIterable<Document> docs = connection.getCollection(COLLECTION_BILLING_TOTAL)
				.aggregate(pipeline);

		MongoCollection<Document> cEdge = connection.getCollection(COLLECTION_USER_EDGE);
		for (Document d : docs) {
			Document id = (Document) d.get(FIELD_ID);
			Bson values = Updates.combine(
					Updates.set(ReportLine.FIELD_COST, BillingCalculationUtils.round(d.getDouble(ReportLine
							.FIELD_COST), 2)),
					Updates.set(FIELD_CURRENCY_CODE, id.get(ReportLine.FIELD_CURRENCY_CODE)));
			cEdge.updateOne(
					eq(FIELD_ID, user),
					values);
		}
	}

	/**
	 * Update the cost of exploratory environment for all users in Mongo DB.
	 */
	public void updateExploratoryCost() {
		for (int i = 0; i < resourceList.size(); i++) {
			ResourceItem item = resourceList.get(i);
			if (item.getType() == DlabResourceType.EXPLORATORY) {
				updateExploratoryCost(item.getUser(), item.getExploratoryName());
			} else if (item.getType() == DlabResourceType.EDGE) {
				updateEdgeCost(item.getUser());
			}
		}
	}
}
