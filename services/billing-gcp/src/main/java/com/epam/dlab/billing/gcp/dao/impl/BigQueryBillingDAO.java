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

package com.epam.dlab.billing.gcp.dao.impl;

import com.epam.dlab.billing.gcp.conf.DlabConfiguration;
import com.epam.dlab.billing.gcp.dao.BillingDAO;
import com.epam.dlab.billing.gcp.model.BillingHistory;
import com.epam.dlab.billing.gcp.model.GcpBillingData;
import com.epam.dlab.billing.gcp.repository.BillingHistoryRepository;
import com.epam.dlab.dto.billing.BillingData;
import com.epam.dlab.exceptions.DlabException;
import com.google.cloud.bigquery.BigQuery;
import com.google.cloud.bigquery.FieldValueList;
import com.google.cloud.bigquery.QueryJobConfiguration;
import com.google.cloud.bigquery.QueryParameterValue;
import com.google.cloud.bigquery.Table;
import com.google.cloud.bigquery.TableInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationOperation;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.springframework.data.mongodb.core.aggregation.Aggregation.group;
import static org.springframework.data.mongodb.core.aggregation.Aggregation.newAggregation;

@Component
@Slf4j
public class BigQueryBillingDAO implements BillingDAO {
	private static final String DATE_FORMAT = "yyyy-MM-dd";
	private static final String SBN_PARAM = "sbn";
	private static final String DATASET_PARAM = "dataset";

	private final BillingHistoryRepository billingHistoryRepo;
	private final MongoTemplate mongoTemplate;
	private final BigQuery service;
	private final String dataset;
	private final String sbn;

	private static final String GET_BILLING_DATA_QUERY = "SELECT b.sku.description usageType," +
			"TIMESTAMP_TRUNC(usage_start_time, DAY, 'UTC') usage_date_from, TIMESTAMP_TRUNC(usage_end_time, DAY, " +
			"'UTC')" +
			" usage_date_to, sum(b.cost) cost, b.service.description product, label.value, currency\n" +
			"FROM `%s` b\n" +
			"CROSS JOIN UNNEST(b.labels) as label\n" +
			"where label.key = 'name' and cost != 0 and label.value like @sbn\n" +
			"group by usageType, usage_date_from, usage_date_to, product, value, currency";

	@Autowired
	public BigQueryBillingDAO(DlabConfiguration conf, BillingHistoryRepository billingHistoryRepo,
							  BigQuery service, MongoTemplate mongoTemplate) {
		dataset = conf.getBigQueryDataset();
		this.service = service;
		this.billingHistoryRepo = billingHistoryRepo;
		this.mongoTemplate = mongoTemplate;
		sbn = conf.getSbn();
	}

	@Override
	public List<BillingData> getBillingData() {
		final Map<String, Long> processedBillingTables = billingHistoryRepo.findAll()
				.stream()
				.collect(Collectors.toMap(BillingHistory::getTableName, BillingHistory::getLastModified));
		log.debug("Already processed billing data: {}", processedBillingTables);

		return StreamSupport.stream(service.listTables(dataset).iterateAll().spliterator(), false)
				.map(TableInfo::getTableId)
				.map(service::getTable)
				.filter(t -> processedBillingTables.getOrDefault(t.getTableId().getTable(), 0L) < t.getLastModifiedTime())
				.peek(t -> log.info("Processing table {}", t.getTableId().getTable()))
				.flatMap(this::bigQueryResultSetStream)
				.collect(Collectors.toList());
	}

	@Override
	public List<BillingData> getBillingReport(String dateStart, String dateEnd, String dlabId, List<String> products) {
		try {
			List<AggregationOperation> aggregationOperations = new ArrayList<>();
			aggregationOperations.add(Aggregation.match(Criteria.where("dlabId").regex(dlabId, "i")));
			if (!products.isEmpty()) {
				aggregationOperations.add(Aggregation.match(Criteria.where("product").in(products)));
			}
			getMatchCriteria(dateStart, Criteria.where("usage_date").gte(dateStart))
					.ifPresent(aggregationOperations::add);
			getMatchCriteria(dateEnd, Criteria.where("usage_date").lte(dateEnd))
					.ifPresent(aggregationOperations::add);
			aggregationOperations.add(getGroupOperation());

			Aggregation aggregation = newAggregation(aggregationOperations);

			return mongoTemplate.aggregate(aggregation, "billing", GcpBillingData.class).getMappedResults()
					.stream()
					.map(this::toBillingData)
					.collect(Collectors.toList());
		} catch (Exception e) {
			log.error("Cannot retrieve billing information ", e);
			throw new DlabException("Cannot retrieve billing information", e);
		}
	}

	@Override
	public List<BillingData> getBillingReport(List<String> dlabIds) {
		try {
			GroupOperation groupOperation = getGroupOperation();
			MatchOperation matchOperation = Aggregation.match(Criteria.where("dlabId").in(dlabIds));
			Aggregation aggregation = newAggregation(matchOperation, groupOperation);

			return mongoTemplate.aggregate(aggregation, "billing", GcpBillingData.class).getMappedResults()
					.stream()
					.map(this::toBillingData)
					.collect(Collectors.toList());
		} catch (Exception e) {
			log.error("Cannot retrieve billing information ", e);
			throw new DlabException("Cannot retrieve billing information", e);
		}
	}

	private GroupOperation getGroupOperation() {
		return group("product", "currency", "dlabId")
				.min("from").as("from")
				.max("to").as("to")
				.sum("cost").as("cost");
	}

	private Optional<MatchOperation> getMatchCriteria(String dateStart, Criteria criteria) {
		return Optional.ofNullable(dateStart)
				.filter(StringUtils::isNotEmpty)
				.map(date -> Aggregation.match(criteria));
	}

	private Stream<? extends BillingData> bigQueryResultSetStream(Table table) {
		try {
			final String tableName = table.getTableId().getTable();
			final String tableId = table.getTableId().getDataset() + "." + tableName;
			QueryJobConfiguration queryConfig = QueryJobConfiguration
					.newBuilder(String.format(GET_BILLING_DATA_QUERY, tableId))
					.addNamedParameter(SBN_PARAM, QueryParameterValue.string(sbn + "%"))
					.addNamedParameter(DATASET_PARAM, QueryParameterValue.string(tableId))
					.build();
			final Stream<BillingData> gcpBillingDataStream =
					StreamSupport.stream(service.query(queryConfig).getValues().spliterator(), false)
							.map(this::toGcpBillingData);
			billingHistoryRepo.save(new BillingHistory(tableName, table.getLastModifiedTime()));
			return gcpBillingDataStream;
		} catch (InterruptedException e) {
			throw new IllegalStateException("Can not get billing info from BigQuery due to: " + e.getMessage(), e);
		}
	}

	private BillingData toGcpBillingData(FieldValueList fields) {
		return BillingData.builder()
				.usageDateFrom(toLocalDate(fields, "usage_date_from"))
				.usageDateTo(toLocalDate(fields, "usage_date_to"))
				.cost(fields.get("cost").getNumericValue().doubleValue())
				.product(fields.get("product").getStringValue())
				.usageType(fields.get("usageType").getStringValue())
				.currency(fields.get("currency").getStringValue())
				.tag(fields.get("value").getStringValue())
				.usageDate(toLocalDate(fields, "usage_date_from").format((DateTimeFormatter.ofPattern(DATE_FORMAT))))
				.build();
	}

	private LocalDate toLocalDate(FieldValueList fieldValues, String timestampFieldName) {
		return LocalDate.from(Instant.ofEpochMilli(fieldValues.get(timestampFieldName).getTimestampValue() / 1000)
				.atZone(ZoneId.systemDefault()));
	}

	private BillingData toBillingData(GcpBillingData billingData) {
		return BillingData.builder()
				.usageDateFrom(billingData.getUsageDateFrom())
				.usageDateTo(billingData.getUsageDateTo())
				.product(billingData.getProduct())
				.usageType(billingData.getUsageType())
				.cost(BigDecimal.valueOf(billingData.getCost()).setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue())
				.currency(billingData.getCurrency())
				.tag(billingData.getTag())
				.build();
	}
}
