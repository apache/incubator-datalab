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
import com.google.cloud.bigquery.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

@Component
@Slf4j
public class BigQueryBillingDAO implements BillingDAO {

	private static final String SBN_PARAM = "sbn";
	private static final String DATASET_PARAM = "dataset";
	private final BillingHistoryRepository billingHistoryRepo;
	private final String sbn;

	private static final String GET_BILLING_DATA_QUERY = "SELECT b.sku.description usageType," +
			"TIMESTAMP_TRUNC(usage_start_time, DAY, 'UTC') usage_date_from, TIMESTAMP_TRUNC(usage_end_time, DAY, " +
			"'UTC')" +
			" usage_date_to, sum(b.cost) cost, b.service.description product, label.value, currency\n" +
			"FROM `%s` b\n" +
			"CROSS JOIN UNNEST(b.labels) as label\n" +
			"where label.key = 'name' and cost != 0 and label.value like @sbn\n" +
			"group by usageType, usage_date_from, usage_date_to, product, value, currency";
	private final BigQuery service;
	private final String dataset;

	@Autowired
	public BigQueryBillingDAO(DlabConfiguration conf, BigQuery service, BillingHistoryRepository billingHistoryRepo) {
		dataset = conf.getBigQueryDataset();
		sbn = conf.getSbn();
		this.service = service;
		this.billingHistoryRepo = billingHistoryRepo;
	}

	@Override
	public List<GcpBillingData> getBillingData() {
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

	private Stream<? extends GcpBillingData> bigQueryResultSetStream(Table table) {
		try {
			final String tableName = table.getTableId().getTable();
			final String tableId = table.getTableId().getDataset() + "." + tableName;
			QueryJobConfiguration queryConfig = QueryJobConfiguration
					.newBuilder(String.format(GET_BILLING_DATA_QUERY, tableId))
					.addNamedParameter(SBN_PARAM, QueryParameterValue.string(sbn + "%"))
					.addNamedParameter(DATASET_PARAM, QueryParameterValue.string(tableId))
					.build();
			final Stream<GcpBillingData> gcpBillingDataStream =
					StreamSupport.stream(service.query(queryConfig).getValues().spliterator(), false)
							.map(this::toBillingData);
			billingHistoryRepo.save(new BillingHistory(tableName, table.getLastModifiedTime()));
			return gcpBillingDataStream;
		} catch (InterruptedException e) {
			throw new IllegalStateException("Can not get billing info from BigQuery due to: " + e.getMessage(), e);
		}
	}

	private GcpBillingData toBillingData(FieldValueList fields) {

		return GcpBillingData.builder()
				.usageDateFrom(toLocalDate(fields, "usage_date_from"))
				.usageDateTo(toLocalDate(fields, "usage_date_to"))
				.cost(fields.get("cost").getNumericValue())
				.product(fields.get("product").getStringValue())
				.usageType(fields.get("usageType").getStringValue())
				.currency(fields.get("currency").getStringValue())
				.tag(fields.get("value").getStringValue()).build();
	}

	private LocalDate toLocalDate(FieldValueList fieldValues, String timestampFieldName) {
		return LocalDate.from(Instant.ofEpochMilli(fieldValues.get(timestampFieldName).getTimestampValue() / 1000)
				.atZone(ZoneId.systemDefault()));
	}
}
