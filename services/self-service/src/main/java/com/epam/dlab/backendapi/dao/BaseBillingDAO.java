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

package com.epam.dlab.backendapi.dao;

import com.epam.dlab.MongoKeyWords;
import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.domain.BaseShape;
import com.epam.dlab.backendapi.domain.DataEngineServiceShape;
import com.epam.dlab.backendapi.domain.DataEngineShape;
import com.epam.dlab.backendapi.domain.EndpointShape;
import com.epam.dlab.backendapi.domain.ExploratoryShape;
import com.epam.dlab.backendapi.domain.SsnShape;
import com.epam.dlab.backendapi.resources.dto.BillingFilter;
import com.epam.dlab.backendapi.roles.RoleType;
import com.epam.dlab.backendapi.roles.UserRoles;
import com.epam.dlab.billing.BillingCalculationUtils;
import com.epam.dlab.billing.DlabResourceType;
import com.epam.dlab.dto.UserInstanceStatus;
import com.epam.dlab.dto.base.DataEngineType;
import com.epam.dlab.model.aws.ReportLine;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.mongodb.client.AggregateIterable;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Filters;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.math.BigDecimal;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.StreamSupport;

import static com.epam.dlab.backendapi.dao.ComputationalDAO.COMPUTATIONAL_ID;
import static com.epam.dlab.backendapi.dao.ExploratoryDAO.COMPUTATIONAL_RESOURCES;
import static com.epam.dlab.backendapi.dao.ExploratoryDAO.EXPLORATORY_ID;
import static com.epam.dlab.backendapi.dao.MongoCollections.BILLING;
import static com.epam.dlab.backendapi.dao.MongoCollections.USER_INSTANCES;
import static com.epam.dlab.model.aws.ReportLine.FIELD_RESOURCE_TYPE;
import static com.epam.dlab.model.aws.ReportLine.FIELD_USAGE_DATE;
import static com.mongodb.client.model.Accumulators.sum;
import static com.mongodb.client.model.Aggregates.group;
import static com.mongodb.client.model.Aggregates.match;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Projections.*;
import static java.util.Collections.singletonList;

@Slf4j
public abstract class BaseBillingDAO<T extends BillingFilter> extends BaseDAO implements BillingDAO<T> {

	public static final String SHAPE = "shape";
	public static final String SERVICE_BASE_NAME = "service_base_name";
	public static final String ITEMS = "lines";
	public static final String COST_TOTAL = "cost_total";
	public static final String FULL_REPORT = "full_report";

	private static final String PROJECT = "project";
	private static final String MASTER_NODE_SHAPE = "master_node_shape";
	private static final String SLAVE_NODE_SHAPE = "slave_node_shape";
	private static final String TOTAL_INSTANCE_NUMBER = "total_instance_number";

	private static final String DATAENGINE_SHAPE = "dataengine_instance_shape";
	private static final String DATAENGINE_INSTANCE_COUNT = "dataengine_instance_count";

	private static final String DATAENGINE_DOCKER_IMAGE = "image";
	private static final int ONE_HUNDRED = 100;
	private static final String TOTAL_FIELD_NAME = "total";
	private static final String COST_FIELD = "$cost";
	public static final String SHARED_RESOURCE_NAME = "Shared resource";
	protected static final String FIELD_PROJECT = "project";
	private static final String EDGE_FORMAT = "%s-%s-%s-edge";
	private static final String PROJECT_COLLECTION = "Projects";
	private static final String TAGS = "tags";

	@Inject
	protected SettingsDAO settings;
	@Inject
	private UserSettingsDAO userSettingsDAO;
	@Inject
	private ProjectDAO projectDAO;

	@Override
	public Document getReport(UserInfo userInfo, T filter) {
		boolean isFullReport = UserRoles.checkAccess(userInfo, RoleType.PAGE, "/api/infrastructure_provision/billing",
				userInfo.getRoles());
		setUserFilter(userInfo, filter, isFullReport);
		List<Bson> matchCriteria = matchCriteria(filter);
		List<Bson> pipeline = new ArrayList<>();
		if (!matchCriteria.isEmpty()) {
			pipeline.add(Aggregates.match(Filters.and(matchCriteria)));
		}
		pipeline.add(groupCriteria());
		pipeline.add(sortCriteria());
		final Map<String, BaseShape> shapes = getShapes(filter.getShapes());
		return prepareReport(filter.getStatuses(), !filter.getShapes().isEmpty(),
				getCollection(BILLING).aggregate(pipeline), shapes, isFullReport);
	}

	private Document prepareReport(List<UserInstanceStatus> statuses, boolean filterByShape,
								   AggregateIterable<Document> agg,
								   Map<String, BaseShape> shapes, boolean fullReport) {

		List<Document> reportItems = new ArrayList<>();

		String usageDateStart = null;
		String usageDateEnd = null;
		double costTotal = 0D;

		for (Document d : agg) {
			Document id = (Document) d.get(MongoKeyWords.MONGO_ID);
			String resourceId = id.getString(dlabIdFieldName());
			BaseShape shape = shapes.get(resourceId);
			final UserInstanceStatus status = Optional.ofNullable(shape).map(BaseShape::getStatus).orElse(null);
			if ((filterByShape && shape == null) ||
					(!statuses.isEmpty() && statuses.stream().noneMatch(s -> s.equals(status)))) {
				continue;
			}


			String dateStart = d.getString(MongoKeyWords.USAGE_FROM);
			if (StringUtils.compare(usageDateStart, dateStart, false) > 0) {
				usageDateStart = dateStart;
			}
			String dateEnd = d.getString(MongoKeyWords.USAGE_TO);
			if (StringUtils.compare(usageDateEnd, dateEnd) < 0) {
				usageDateEnd = dateEnd;
			}


			costTotal += d.getDouble(MongoKeyWords.COST);

			final String dlabResourceType = id.getString("dlab_resource_type");
			final String statusString = Optional
					.ofNullable(status)
					.map(UserInstanceStatus::toString)
					.orElse(StringUtils.EMPTY);

			Document item = new Document()
					.append(MongoKeyWords.DLAB_USER, getOrDefault(id.getString(USER)))
					.append(dlabIdFieldName(), resourceId)
					.append(shapeFieldName(), Optional.ofNullable(shape).map(BaseShape::format)
							.orElse(StringUtils.EMPTY))
					.append("dlab_resource_type", DlabResourceType
							.getResourceTypeName(dlabResourceType)) //todo check on azure!!!
					.append(STATUS, statusString)
					.append(FIELD_RESOURCE_TYPE, resourceType(id))
					.append(productFieldName(), id.getString(productFieldName()))
					.append(PROJECT, getOrDefault(id.getString(PROJECT)))
					.append(MongoKeyWords.COST, d.getDouble(MongoKeyWords.COST))
					.append(costFieldName(), BillingCalculationUtils.formatDouble(d.getDouble(MongoKeyWords
							.COST)))
					.append(currencyCodeFieldName(), id.getString(currencyCodeFieldName()))
					.append(usageDateFromFieldName(), dateStart)
					.append(usageDateToFieldName(), dateEnd);

			reportItems.add(item);
		}

		return new Document()
				.append(SERVICE_BASE_NAME, settings.getServiceBaseName())
				.append(usageDateFromFieldName(), usageDateStart)
				.append(usageDateToFieldName(), usageDateEnd)
				.append(ITEMS, reportItems)
				.append(COST_TOTAL, BillingCalculationUtils.formatDouble(BillingCalculationUtils.round
						(costTotal, 2)))
				.append(currencyCodeFieldName(), (reportItems.isEmpty() ? null :
						reportItems.get(0).getString(currencyCodeFieldName())))
				.append(FULL_REPORT, fullReport);

	}

	protected String resourceType(Document id) {
		return id.getString(FIELD_RESOURCE_TYPE);
	}

	protected String currencyCodeFieldName() {
		return "currency_code";
	}

	protected String usageDateToFieldName() {
		return MongoKeyWords.USAGE_TO;
	}

	protected String costFieldName() {
		return MongoKeyWords.COST;
	}

	protected String productFieldName() {
		return ReportLine.FIELD_PRODUCT;
	}

	protected String usageDateFromFieldName() {
		return MongoKeyWords.USAGE_FROM;
	}

	protected String dlabIdFieldName() {
		return ReportLine.FIELD_DLAB_ID;
	}

	protected String shapeFieldName() {
		return SHAPE;
	}

	protected abstract Bson sortCriteria();

	protected abstract Bson groupCriteria();

	private Map<String, BaseShape> getShapes(List<String> shapeNames) {
		FindIterable<Document> userInstances = getUserInstances();
		final Map<String, BaseShape> shapes = new HashMap<>();

		for (Document d : userInstances) {
			getExploratoryShape(shapeNames, d)
					.ifPresent(shape -> shapes.put(d.getString(EXPLORATORY_ID), shape));
			@SuppressWarnings("unchecked")
			List<Document> comp = (List<Document>) d.get(COMPUTATIONAL_RESOURCES);
			comp.forEach(c -> (isDataEngine(c.getString(DATAENGINE_DOCKER_IMAGE)) ? getDataEngineShape(shapeNames, c) :
					getDataEngineServiceShape(shapeNames, c))
					.ifPresent(shape -> shapes.put(c.getString(COMPUTATIONAL_ID), shape)));
		}

		StreamSupport.stream(getCollection(PROJECT_COLLECTION).find().spliterator(), false)
				.forEach(d -> ((List<Document>) d.get("endpoints"))
						.forEach(endpoint -> getEndpointShape(shapeNames, endpoint)
								.ifPresent(shape -> shapes.put(String.format(EDGE_FORMAT, getServiceBaseName(),
										d.getString("name").toLowerCase(),
										endpoint.getString("name")), shape))));

		getSsnShape(shapeNames)
				.ifPresent(shape -> shapes.put(getServiceBaseName() + "-ssn", shape));

		log.trace("Loaded shapes is {}", shapes);
		return shapes;
	}

	@Override
	public Double getTotalCost() {
		return aggregateBillingData(singletonList(group(null, sum(TOTAL_FIELD_NAME, COST_FIELD))));
	}

	@Override
	public Double getUserCost(String user) {
		final List<Bson> pipeline = Arrays.asList(match(eq(USER, user)),
				group(null, sum(TOTAL_FIELD_NAME, COST_FIELD)));
		return aggregateBillingData(pipeline);
	}

	@Override
	public Double getProjectCost(String project) {
		final List<Bson> pipeline = Arrays.asList(match(eq(PROJECT, project)),
				group(null, sum(TOTAL_FIELD_NAME, COST_FIELD)));
		return aggregateBillingData(pipeline);
	}

	@Override
	public int getBillingQuoteUsed() {
		return toPercentage(() -> settings.getMaxBudget(), getTotalCost());
	}

	@Override
	public int getBillingUserQuoteUsed(String user) {
		return toPercentage(() -> userSettingsDAO.getAllowedBudget(user), getUserCost(user));
	}

	@Override
	public boolean isBillingQuoteReached() {
		return getBillingQuoteUsed() >= ONE_HUNDRED;
	}

	@Override
	public boolean isUserQuoteReached(String user) {
		final Double userCost = getUserCost(user);
		return userSettingsDAO.getAllowedBudget(user)
				.filter(allowedBudget -> userCost.intValue() != 0 && allowedBudget <= userCost)
				.isPresent();
	}


	@Override
	public boolean isProjectQuoteReached(String project) {
		final Double projectCost = getProjectCost(project);
		return projectDAO.getAllowedBudget(project)
				.filter(allowedBudget -> projectCost.intValue() != 0 && allowedBudget <= projectCost)
				.isPresent();
	}

	@Override
	public int getBillingProjectQuoteUsed(String project) {
		return toPercentage(() -> projectDAO.getAllowedBudget(project), getProjectCost(project));
	}

	private String getOrDefault(String value) {
		return StringUtils.isNotBlank(value) ? value : SHARED_RESOURCE_NAME;
	}

	private Integer toPercentage(Supplier<Optional<Integer>> allowedBudget, Double totalCost) {
		return allowedBudget.get()
				.map(userBudget -> (totalCost * ONE_HUNDRED) / userBudget)
				.map(Double::intValue)
				.orElse(BigDecimal.ZERO.intValue());
	}

	private List<Bson> matchCriteria(BillingFilter filter) {

		List<Bson> searchCriteria = new ArrayList<>();

		if (filter.getUser() != null && !filter.getUser().isEmpty()) {
			searchCriteria.add(Filters.in(MongoKeyWords.DLAB_USER, filter.getUser()));
		}

		if (filter.getResourceType() != null && !filter.getResourceType().isEmpty()) {
			searchCriteria.add(Filters.in("dlab_resource_type",
					DlabResourceType.getResourceTypeIds(filter.getResourceType())));
		}

		if (filter.getDlabId() != null && !filter.getDlabId().isEmpty()) {
			searchCriteria.add(regex(dlabIdFieldName(), filter.getDlabId(), "i"));
		}

		if (filter.getDateStart() != null && !filter.getDateStart().isEmpty()) {
			searchCriteria.add(gte(FIELD_USAGE_DATE, filter.getDateStart()));
		}
		if (filter.getDateEnd() != null && !filter.getDateEnd().isEmpty()) {
			searchCriteria.add(lte(FIELD_USAGE_DATE, filter.getDateEnd()));
		}
		if (filter.getProjects() != null && !filter.getProjects().isEmpty()) {
			searchCriteria.add(in(PROJECT, filter.getProjects()));
		}

		searchCriteria.addAll(cloudMatchCriteria((T) filter));
		return searchCriteria;
	}

	protected abstract List<Bson> cloudMatchCriteria(T filter);

	private Double aggregateBillingData(List<Bson> pipeline) {
		return Optional.ofNullable(aggregate(BILLING, pipeline).first())
				.map(d -> d.getDouble(TOTAL_FIELD_NAME))
				.orElse(BigDecimal.ZERO.doubleValue());
	}

	private FindIterable<Document> getUserInstances() {
		return getCollection(USER_INSTANCES)
				.find()
				.projection(
						fields(excludeId(),
								include(SHAPE, EXPLORATORY_ID, STATUS, TAGS,
										COMPUTATIONAL_RESOURCES + "." + COMPUTATIONAL_ID,
										COMPUTATIONAL_RESOURCES + "." + MASTER_NODE_SHAPE,
										COMPUTATIONAL_RESOURCES + "." + SLAVE_NODE_SHAPE,
										COMPUTATIONAL_RESOURCES + "." + TOTAL_INSTANCE_NUMBER,
										COMPUTATIONAL_RESOURCES + "." + DATAENGINE_SHAPE,
										COMPUTATIONAL_RESOURCES + "." + DATAENGINE_INSTANCE_COUNT,
										COMPUTATIONAL_RESOURCES + "." + DATAENGINE_DOCKER_IMAGE,
										COMPUTATIONAL_RESOURCES + "." + STATUS,
										COMPUTATIONAL_RESOURCES + "." + TAGS
								)));
	}

	private Optional<ExploratoryShape> getExploratoryShape(List<String> shapeNames, Document d) {
		final String shape = d.getString(SHAPE);
		if (isShapeAcceptable(shapeNames, shape)) {
			return Optional.of(ExploratoryShape.builder()
					.shape(shape)
					.status(UserInstanceStatus.of(d.getString(STATUS)))
					.tags((Map<String, String>) d.get(TAGS))
					.build());
		}
		return Optional.empty();
	}

	private Optional<DataEngineServiceShape> getDataEngineServiceShape(List<String> shapeNames, Document c) {
		final String desMasterShape = c.getString(MASTER_NODE_SHAPE);
		final String desSlaveShape = c.getString(SLAVE_NODE_SHAPE);
		if (isShapeAcceptable(shapeNames, desMasterShape, desSlaveShape)) {
			return Optional.of(DataEngineServiceShape.builder()
					.shape(desMasterShape)
					.status(UserInstanceStatus.of(c.getString(STATUS)))
					.slaveCount(c.getString(TOTAL_INSTANCE_NUMBER))
					.slaveShape(desSlaveShape)
					.tags((Map<String, String>) c.get(TAGS))
					.build());
		}
		return Optional.empty();
	}

	private Optional<DataEngineShape> getDataEngineShape(List<String> shapeNames, Document c) {
		final String shape = c.getString(DATAENGINE_SHAPE);
		if ((isShapeAcceptable(shapeNames, shape)) && StringUtils.isNotEmpty(c.getString(COMPUTATIONAL_ID))) {

			return Optional.of(DataEngineShape.builder()
					.shape(shape)
					.status(UserInstanceStatus.of(c.getString(STATUS)))
					.slaveCount(c.getString(DATAENGINE_INSTANCE_COUNT))
					.tags((Map<String, String>) c.get(TAGS))
					.build());
		}
		return Optional.empty();
	}

	private Optional<SsnShape> getSsnShape(List<String> shapeNames) {
		final String shape = getSsnShape();
		if (isShapeAcceptable(shapeNames, shape)) {
			return Optional.of(SsnShape.builder()
					.shape(shape)
					.status(UserInstanceStatus.RUNNING)
					.build());
		}
		return Optional.empty();
	}

	private Optional<EndpointShape> getEndpointShape(List<String> shapeNames, Document endpoint) {
		if (isShapeAcceptable(shapeNames, getSsnShape())) {
			return Optional.of(EndpointShape.builder()
					.shape(StringUtils.EMPTY)
					.status(UserInstanceStatus.of(endpoint.getString("status")))
					.build());
		}
		return Optional.empty();
	}

	private boolean isDataEngine(String dockerImage) {
		return DataEngineType.fromDockerImageName(dockerImage) == DataEngineType.SPARK_STANDALONE;
	}

	private boolean isShapeAcceptable(List<String> shapeNames, String... shapes) {
		return shapeNames == null || shapeNames.isEmpty() || Arrays.stream(shapes).anyMatch(shapeNames::contains);
	}

	protected String getServiceBaseName() {
		return settings.getServiceBaseName();
	}

	protected abstract String getSsnShape();

	protected void usersToLowerCase(List<String> users) {
		if (users != null) {
			users.replaceAll(u -> u != null ? u.toLowerCase() : null);
		}
	}

	protected void setUserFilter(UserInfo userInfo, BillingFilter filter, boolean isFullReport) {
		if (isFullReport) {
			usersToLowerCase(filter.getUser());
		} else {
			filter.setUser(Lists.newArrayList(userInfo.getName().toLowerCase()));
		}
	}
}
