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

package com.epam.dlab.backendapi.dao.aws;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.BillingDAO;
import com.epam.dlab.backendapi.resources.dto.aws.AwsBillingFilter;
import com.epam.dlab.backendapi.roles.RoleType;
import com.epam.dlab.backendapi.roles.UserRoles;
import com.epam.dlab.billing.BillingCalculationUtils;
import com.epam.dlab.billing.DlabResourceType;
import com.epam.dlab.util.UsernameUtils;
import com.google.common.collect.Lists;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.epam.dlab.model.aws.ReportLine.*;
import static com.epam.dlab.backendapi.dao.MongoCollections.BILLING;
import static com.epam.dlab.backendapi.dao.MongoCollections.USER_EDGE;
import static com.mongodb.client.model.Accumulators.*;
import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Projections.fields;
import static com.mongodb.client.model.Projections.include;

/**
 * DAO for user billing.
 */
public class AwsBillingDAO extends BillingDAO {
	private static final Logger LOGGER = LoggerFactory.getLogger(AwsBillingDAO.class);

	public static final String DLAB_RESOURCE_TYPE = "dlab_resource_type";
	public static final String USAGE_DATE_START = "usage_date_start";
	public static final String USAGE_DATE_END = "usage_date_end";
	public static final String TAG_RESOURCE_ID = "tag_resource_id";

	/**
	 * Add the conditions to the list.
	 *
	 * @param conditions the list of conditions.
	 * @param fieldName  the name of field.
	 * @param values     the values.
	 */
	private void addCondition(List<Bson> conditions, String fieldName, List<String> values) {
		if (values != null && !values.isEmpty()) {
			conditions.add(in(fieldName, values));
		}
	}

	/**
	 * Build and returns the billing report.
	 *
	 * @param userInfo user info
	 * @param filter   the filter for report data.
	 * @return billing report
	 */
	public Document getReport(UserInfo userInfo, AwsBillingFilter filter) {
		// Create filter
		List<Bson> conditions = new ArrayList<>();
		boolean isFullReport = UserRoles.checkAccess(userInfo, RoleType.PAGE, "/api/infrastructure_provision/billing");
		if (isFullReport) {
			if (filter.getUser() != null) {
				filter.getUser().replaceAll(String::toLowerCase);
			}
		} else {
			filter.setUser(Lists.newArrayList(userInfo.getName().toLowerCase()));
		}
		addCondition(conditions, USER, filter.getUser());
		addCondition(conditions, FIELD_PRODUCT, filter.getProduct());
		addCondition(conditions, DLAB_RESOURCE_TYPE, DlabResourceType.getResourceTypeIds(filter.getResourceType()));

		addAnotherConditionsIfNecessary(conditions, filter);

		// Create aggregation conditions

		List<Bson> pipeline = new ArrayList<>();
		if (!conditions.isEmpty()) {
			LOGGER.trace("Filter conditions is {}", conditions);
			pipeline.add(match(and(conditions)));
		}
		pipeline.add(
				group(getGroupingFields(USER, FIELD_DLAB_ID, DLAB_RESOURCE_TYPE, FIELD_PRODUCT, FIELD_RESOURCE_TYPE,
						FIELD_CURRENCY_CODE),
						sum(FIELD_COST, "$" + FIELD_COST),
						min(USAGE_DATE_START, "$" + FIELD_USAGE_DATE),
						max(USAGE_DATE_END, "$" + FIELD_USAGE_DATE)
				));
		pipeline.add(
				sort(new Document(ID + "." + USER, 1)
						.append(ID + "." + FIELD_DLAB_ID, 1)
						.append(ID + "." + DLAB_RESOURCE_TYPE, 1)
						.append(ID + "." + FIELD_PRODUCT, 1))
		);

		// Get billing report and the list of shape info
		AggregateIterable<Document> agg = getCollection(BILLING).aggregate(pipeline);
		Map<String, ShapeInfo> shapes = getShapes(filter.getShape());

		// Build billing report lines
		List<Document> reportItems = new ArrayList<>();
		boolean filterByShape = !(filter.getShape() == null || filter.getShape().isEmpty());
		String usageDateStart = null;
		String usageDateEnd = null;
		double costTotal = 0;

		for (Document d : agg) {
			Document id = (Document) d.get(ID);
			String resourceId = id.getString(FIELD_DLAB_ID);
			ShapeInfo shape = shapes.get(resourceId);
			if (filterByShape && shape == null) {
				continue;
			}

			String resourceTypeId = DlabResourceType.getResourceTypeName(id.getString(DLAB_RESOURCE_TYPE));
			String shapeName = generateShapeName(shape);
			String dateStart = d.getString(USAGE_DATE_START);
			if (StringUtils.compare(usageDateStart, dateStart, false) > 0) {
				usageDateStart = dateStart;
			}
			String dateEnd = d.getString(USAGE_DATE_END);
			if (StringUtils.compare(usageDateEnd, dateEnd) < 0) {
				usageDateEnd = dateEnd;
			}
			double cost = BillingCalculationUtils.round(d.getDouble(FIELD_COST), 2);
			costTotal += cost;

			Document item = new Document()
					.append(FIELD_USER_ID, id.getString(USER))
					.append(FIELD_DLAB_ID, resourceId)
					.append(DLAB_RESOURCE_TYPE, resourceTypeId)
					.append(SHAPE, shapeName)
					.append(FIELD_PRODUCT, id.getString(FIELD_PRODUCT))
					.append(FIELD_RESOURCE_TYPE, id.getString(FIELD_RESOURCE_TYPE))
					.append(FIELD_COST, BillingCalculationUtils.formatDouble(cost))
					.append(FIELD_CURRENCY_CODE, id.getString(FIELD_CURRENCY_CODE))
					.append(USAGE_DATE_START, dateStart)
					.append(USAGE_DATE_END, dateEnd);
			reportItems.add(item);
		}

		return new Document()
				.append(SERVICE_BASE_NAME, settings.getServiceBaseName())
				.append(TAG_RESOURCE_ID, settings.getConfTagResourceId())
				.append(USAGE_DATE_START, usageDateStart)
				.append(USAGE_DATE_END, usageDateEnd)
				.append(ITEMS, reportItems)
				.append(COST_TOTAL, BillingCalculationUtils.formatDouble(BillingCalculationUtils.round(costTotal, 2)))
				.append(FIELD_CURRENCY_CODE, (reportItems.isEmpty() ? null :
						reportItems.get(0).getString(FIELD_CURRENCY_CODE)))
				.append(FULL_REPORT, isFullReport);
	}

	private void addAnotherConditionsIfNecessary(List<Bson> conditions, AwsBillingFilter filter) {
		if (filter.getDlabId() != null && !filter.getDlabId().isEmpty()) {
			conditions.add(regex(FIELD_DLAB_ID, filter.getDlabId(), "i"));
		}

		if (filter.getDateStart() != null && !filter.getDateStart().isEmpty()) {
			conditions.add(gte(FIELD_USAGE_DATE, filter.getDateStart()));
		}
		if (filter.getDateEnd() != null && !filter.getDateEnd().isEmpty()) {
			conditions.add(lte(FIELD_USAGE_DATE, filter.getDateEnd()));
		}
	}

	protected void appendSsnAndEdgeNodeType(List<String> shapeNames, Map<String, BillingDAO.ShapeInfo> shapes) {
		// Add SSN and EDGE nodes
		final String ssnShape = "t2.medium";
		if (shapeNames == null || shapeNames.isEmpty() || shapeNames.contains(ssnShape)) {
			String serviceBaseName = settings.getServiceBaseName();
			shapes.put(serviceBaseName + "-ssn", new BillingDAO.ShapeInfo(ssnShape));
			FindIterable<Document> docs = getCollection(USER_EDGE)
					.find()
					.projection(fields(include(ID)));
			for (Document d : docs) {
				shapes.put(String.join("-", serviceBaseName, UsernameUtils.removeDomain(d.getString(ID)), "edge"),
						new BillingDAO.ShapeInfo(ssnShape));
			}
		}
	}
}