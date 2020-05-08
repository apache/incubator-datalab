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

import com.epam.dlab.exceptions.InitializationException;
import com.epam.dlab.exceptions.ParseException;
import com.epam.dlab.model.aws.ReportLine;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static com.mongodb.client.model.Accumulators.sum;
import static com.mongodb.client.model.Aggregates.group;
import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.gte;
import static com.mongodb.client.model.Filters.lte;
import static org.apache.commons.lang3.StringUtils.EMPTY;

/**
 * Provides Mongo DAO for billing resources in DLab.
 */
public class DlabResourceTypeDAO implements MongoConstants {
	private static final Logger LOGGER = LoggerFactory.getLogger(DlabResourceTypeDAO.class);

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
	 * Instantiate DAO for billing resources.
	 *
	 * @param connection the connection to Mongo DB.
	 * @throws InitializationException
	 */
	public DlabResourceTypeDAO(MongoDbConnection connection) throws InitializationException {
		this.connection = connection;
		setServiceBaseName();
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
		Document d = new Document(ReportLine.FIELD_DLAB_ID, resourceId);
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
	private Document getGroupingFields(String... fieldNames) {
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
				group(getGroupingFields(FIELD_DLAB_RESOURCE_ID,
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
}
