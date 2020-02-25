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

package com.epam.dlab.billing.gcp.service;

import com.epam.dlab.billing.gcp.dao.BillingDAO;
import com.epam.dlab.billing.gcp.documents.Project;
import com.epam.dlab.billing.gcp.documents.UserInstance;
import com.epam.dlab.billing.gcp.model.BillingData;
import com.epam.dlab.billing.gcp.model.GcpBillingData;
import com.epam.dlab.billing.gcp.repository.BillingRepository;
import com.epam.dlab.billing.gcp.repository.ProjectRepository;
import com.epam.dlab.billing.gcp.repository.UserInstanceRepository;
import com.epam.dlab.billing.gcp.util.BillingUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.epam.dlab.billing.gcp.util.BillingUtils.edgeBillingDataStream;
import static org.springframework.data.mongodb.core.query.Criteria.where;

@Service
@Slf4j
public class BillingServiceImpl implements BillingService {

	private static final String DATE_FORMAT = "yyyy-MM-dd";
	private static final String USAGE_DATE_FORMAT = "yyyy-MM";
	private final BillingDAO billingDAO;
	private final ProjectRepository projectRepository;
	private final UserInstanceRepository userInstanceRepository;
	private final BillingRepository billingRepository;
	private final MongoTemplate mongoTemplate;
	@Value("${dlab.sbn}")
	private String sbn;

	@Autowired
	public BillingServiceImpl(BillingDAO billingDAO, ProjectRepository projectRepository,
							  UserInstanceRepository userInstanceRepository, BillingRepository billingRepository,
							  MongoTemplate mongoTemplate) {
		this.billingDAO = billingDAO;
		this.projectRepository = projectRepository;
		this.userInstanceRepository = userInstanceRepository;
		this.billingRepository = billingRepository;
		this.mongoTemplate = mongoTemplate;
	}

	@Override
	public void updateBillingData() {
		try {

			final Stream<BillingData> ssnBillingDataStream = BillingUtils.ssnBillingDataStream(sbn);
			final Stream<BillingData> billableUserInstances = userInstanceRepository.findAll()
					.stream()
					.filter(userInstance -> userInstance.getExploratoryId() != null)
					.flatMap(BillingUtils::exploratoryBillingDataStream);

			final Stream<BillingData> billableEdges = projectRepository.findAll()
					.stream()
					.collect(Collectors.toMap(Project::getName, Project::getEndpoints))
					.entrySet()
					.stream()
					.flatMap(e -> projectEdges(e.getKey(), e.getValue()));


			final Map<String, BillingData> billableResources = Stream.of(billableUserInstances, billableEdges,
					ssnBillingDataStream)
					.flatMap(s -> s)
					.filter(bd -> bd.getDlabId() != null)
					.collect(Collectors.toMap(BillingData::getDlabId, b -> b));
			log.info("Billable resources are: {}", billableResources);
//			final Map<String, List<BillingData>> billingDataMap = billingDAO.getBillingData()
//					.stream()
//					.map(bd -> toBillingData(bd, getOrDefault(billableResources, bd.getTag())))
//					.collect(Collectors.groupingBy(bd -> bd.getUsageDate().substring(0,
//							USAGE_DATE_FORMAT.length())));

//			billingDataMap.forEach((usageDate, billingDataList) -> {
//				log.info("Updating billing information for month {}", usageDate);
//				billingRepository.deleteByUsageDateRegex("^" + usageDate);
//				billingRepository.insert(billingDataList);
//				updateExploratoryCost(billingDataList);
//			});

			log.info("Finished updating billing data");


		} catch (Exception e) {
			log.error("Can not update billing due to: {}", e.getMessage(), e);
		}
	}

	@Override
	public void updateBillingData2() {
		try {
			Map<String, List<GcpBillingData>> collect = billingDAO.getBillingData()
					.stream()
					.collect(Collectors.groupingBy(bd -> bd.getUsageDate().substring(0, USAGE_DATE_FORMAT.length())));

			collect.forEach((usageDate, billingDataList) -> {
				log.info("Updating billing information for month {}", usageDate);
				billingRepository.deleteByUsageDateRegex("^" + usageDate);
				billingRepository.insert(billingDataList);
			});
		} catch (Exception e) {
			log.error("Can not update billing due to: {}", e.getMessage(), e);
		}
	}

	private Stream<BillingData> projectEdges(String projectName, List<Project.Endpoint> endpoints) {
		return endpoints
				.stream()
				.flatMap(endpoint -> edgeBillingDataStream(projectName, sbn, endpoint.getName()));
	}

	private BillingData getOrDefault(Map<String, BillingData> billableResources, String tag) {
		return billableResources.getOrDefault(tag, BillingData.builder().dlabId(tag).build());
	}

	private void updateExploratoryCost(List<BillingData> billingDataList) {
		billingDataList.stream()
				.filter(this::userAndExploratoryNamePresent)
				.collect(groupByUserNameExploratoryNameCollector())
				.forEach(this::updateUserExploratoryBillingData);
	}

	private void updateUserExploratoryBillingData(String user,
												  Map<String, List<BillingData>> billableExploratoriesMap) {
		billableExploratoriesMap.forEach((exploratoryName, billingInfoList) ->
				updateExploratoryBillingData(user, exploratoryName, billingInfoList)
		);
	}

	private Collector<BillingData, ?, Map<String, Map<String, List<BillingData>>>> groupByUserNameExploratoryNameCollector() {
		return Collectors.groupingBy(BillingData::getUser, Collectors.groupingBy(BillingData::getExploratoryName));
	}

	private boolean userAndExploratoryNamePresent(BillingData bd) {
		return Objects.nonNull(bd.getUser()) && Objects.nonNull(bd.getExploratoryName());
	}

	private void updateExploratoryBillingData(String user, String exploratoryName, List<BillingData> billingInfoList) {
		userInstanceRepository.findByUserAndExploratoryName(user, exploratoryName).ifPresent(userInstance ->
				mongoTemplate.updateFirst(Query.query(where("user").is(user).and("exploratory_name").is(exploratoryName)),
						Update.update("cost", getTotalCost(billingInfoList) + "$").set("billing", billingInfoList),
						UserInstance.class));
	}

	private double getTotalCost(List<BillingData> billingInfoList) {
		return new BigDecimal(billingInfoList.stream().mapToDouble(BillingData::getCost).sum())
				.setScale(2, BigDecimal.ROUND_HALF_UP)
				.doubleValue();

	}

	private BillingData toBillingData(GcpBillingData bd) {

		return BillingData.builder()
				.cost(bd.getCost().setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue())
				.currency(bd.getCurrency())
				.product(bd.getProduct())
				.usageDateTo(bd.getUsageDateTo())
				.usageDateFrom(bd.getUsageDateFrom())
				.usageDate(bd.getUsageDateFrom().format((DateTimeFormatter.ofPattern(DATE_FORMAT))))
				.usageType(bd.getUsageType())
				.dlabId(bd.getTag())
				.build();
	}
}
