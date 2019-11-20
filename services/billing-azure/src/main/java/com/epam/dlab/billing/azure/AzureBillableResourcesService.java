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

package com.epam.dlab.billing.azure;

import com.epam.dlab.MongoKeyWords;
import com.epam.dlab.billing.DlabResourceType;
import com.epam.dlab.billing.azure.model.AzureDlabBillableResource;
import com.epam.dlab.dto.UserInstanceDTO;
import com.epam.dlab.dto.azure.edge.EdgeInfoAzure;
import com.epam.dlab.dto.computational.UserComputationalResource;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import com.mongodb.client.FindIterable;
import com.mongodb.client.model.Filters;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.mongodb.client.model.Projections.exclude;
import static com.mongodb.client.model.Projections.fields;

/**
 * Helps to retrieve billable resources that are created in scope of DLab usage. Uses MongoDB as data source
 * for created resources
 */
@Slf4j
public class AzureBillableResourcesService {
	private static final String SHARED_RESOURCE = "Shared resource";
	private static final String[] USER_INSTANCES_EXCLUDED_FIELDS = {"scheduler_data", "last_activity",
			"computational_resources.scheduler_data", "computational_resources.last_activity"};
	private final ObjectMapper objectMapper = new ObjectMapper();

	private MongoDbBillingClient mongoDbBillingClient;
	private String serviceBaseName;
	private String sharedStorageAccountTagName;
	private String ssnStorageAccountTagName;
	private String azureDataLakeTagName;

	/**
	 * Constructs the service class
	 *
	 * @param mongoDbBillingClient        mongodb client to retrieve all billable resources
	 * @param sharedStorageAccountTagName shared storage account tag name
	 * @param ssnStorageAccountTagName    ssn storage account tag name
	 * @param azureDataLakeTagName        azure DataLake tag name
	 */
	public AzureBillableResourcesService(MongoDbBillingClient mongoDbBillingClient, String sharedStorageAccountTagName,
										 String ssnStorageAccountTagName, String azureDataLakeTagName) {
		this.mongoDbBillingClient = mongoDbBillingClient;

		this.serviceBaseName = getConfigurationSettingValue(MongoKeyWords.SERVICE_BASE_NAME_KEY)
				.replace('_', '-').toLowerCase();

		this.sharedStorageAccountTagName = sharedStorageAccountTagName;
		this.ssnStorageAccountTagName = ssnStorageAccountTagName;
		this.azureDataLakeTagName = azureDataLakeTagName;
	}


	/**
	 * Collects billable resources
	 *
	 * @return set of all billable resources that were created in scope by DLab from its installation to current time
	 */
	public Set<AzureDlabBillableResource> getBillableResources() {

		Set<AzureDlabBillableResource> billableResources = new HashSet<>();

		billableResources.addAll(getSsn());
		billableResources.addAll(getDataLake());
		billableResources.addAll(getEdgeAndStorageAccount());
		billableResources.addAll(getNotebooksAndClusters());

		List<AzureDlabBillableResource> list = new ArrayList<>(billableResources);
		list.sort(Comparator.comparing(AzureDlabBillableResource::getId));

		try {
			log.debug("Billable resources is \n {}", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString
					(list));
		} catch (JsonProcessingException e) {
			log.debug("Error during pretty printing. Show simple list", e);
			log.debug("Billable resources is {}", list);
		}

		return billableResources;
	}

	private String getConfigurationSettingValue(String key) {

		Document document = mongoDbBillingClient.getDatabase().getCollection(MongoKeyWords.SETTINGS_COLLECTION)
				.find(Filters.eq(MongoKeyWords.MONGO_ID, key)).first();

		if (document != null) {
			String value = document.getString("value");
			if (StringUtils.isEmpty(value)) {
				throw new IllegalStateException("Configuration " + key + " does not have value in settings");
			}
			log.info("Key {} has value {}", key, value);
			return value;
		} else {
			throw new IllegalStateException("Configuration " + key + " is not present in settings");
		}

	}

	private String getConfigurationSettingValueOrEmpty(String key) {
		try {
			return getConfigurationSettingValue(key);
		} catch (IllegalStateException e) {
			log.warn("key {} is not found", key, e);
			return null;
		}
	}

	private Set<AzureDlabBillableResource> getSsn() {

		return Sets.newHashSet(
				AzureDlabBillableResource.builder().id(serviceBaseName + "-ssn").type(DlabResourceType.SSN).build(),
				AzureDlabBillableResource.builder().id(ssnStorageAccountTagName).type(DlabResourceType
						.SSN_STORAGE_ACCOUNT).build(),
				AzureDlabBillableResource.builder().id(sharedStorageAccountTagName).type(DlabResourceType
						.COLLABORATION_STORAGE_ACCOUNT).build(),
				AzureDlabBillableResource.builder().id(serviceBaseName + "-ssn-volume-primary")
						.type(DlabResourceType.VOLUME).build()
		);
	}

	private Set<AzureDlabBillableResource> getDataLake() {

		if (azureDataLakeTagName != null) {
			return Sets.newHashSet(AzureDlabBillableResource.builder().id(azureDataLakeTagName)
					.type(DlabResourceType.DATA_LAKE_STORE).build());
		}

		return Sets.newHashSet();
	}

	private Set<AzureDlabBillableResource> getEdgeAndStorageAccount() {
		Map<String, List<Document>> projectEndpoints = StreamSupport.stream(mongoDbBillingClient.getDatabase()
				.getCollection("Projects").find().spliterator(), false)
				.collect(Collectors.toMap(key -> key.getString("name").toLowerCase(),
						value -> (List<Document>) value.get("endpoints")));

		return projectEndpoints.entrySet()
				.stream()
				.flatMap(projectEndpoint -> getEdgeAndStoragePerProject(projectEndpoint.getKey(), projectEndpoint.getValue()))
				.collect(Collectors.toSet());
	}

	private Stream<AzureDlabBillableResource> getEdgeAndStoragePerProject(String projectName, List<Document> endpoints) {
		return endpoints
				.stream()
				.flatMap(endpoint -> {
					try {
						return getEdgeAndStorageAccount(projectName, objectMapper.readValue(
								objectMapper.writeValueAsString(endpoint.get("edgeInfo")),
								new com.fasterxml.jackson.core.type.TypeReference<EdgeInfoAzure>() {
								})).stream();
					} catch (IOException e) {
						log.error("Error during preparation of billable resources", e);
					}
					return Stream.empty();
				});
	}

	private Set<AzureDlabBillableResource> getEdgeAndStorageAccount(String projectName, EdgeInfoAzure edgeInfoAzure) {
		Set<AzureDlabBillableResource> billableResources = new HashSet<>();

		if (StringUtils.isNotEmpty(edgeInfoAzure.getUserContainerName())) {
			billableResources.add(AzureDlabBillableResource.builder()
					.id(edgeInfoAzure.getUserStorageAccountTagName())
					.type(DlabResourceType.EDGE_STORAGE_ACCOUNT)
					.user(SHARED_RESOURCE)
					.project(projectName)
					.build());
		}

		if (StringUtils.isNotEmpty(edgeInfoAzure.getInstanceId())) {
			billableResources.add(AzureDlabBillableResource.builder()
					.id(edgeInfoAzure.getInstanceId())
					.type(DlabResourceType.EDGE)
					.user(SHARED_RESOURCE)
					.project(projectName)
					.build());

			billableResources.add(AzureDlabBillableResource.builder()
					.id(edgeInfoAzure.getInstanceId() + "-volume-primary")
					.type(DlabResourceType.VOLUME)
					.user(SHARED_RESOURCE)
					.project(projectName)
					.build());
		}

		return billableResources;
	}

	private Set<AzureDlabBillableResource> getNotebooksAndClusters() {

		Set<AzureDlabBillableResource> billableResources = new HashSet<>();

		try {
			final FindIterable<Document> userInstanceDocuments = mongoDbBillingClient.getDatabase()
					.getCollection(MongoKeyWords.NOTEBOOK_COLLECTION)
					.find()
					.projection(fields(exclude(USER_INSTANCES_EXCLUDED_FIELDS)));
			List<UserInstanceDTO> userInstanceDTOS = objectMapper.readValue(
					objectMapper.writeValueAsString(userInstanceDocuments),
					new com.fasterxml.jackson.core.type.TypeReference<List<UserInstanceDTO>>() {
					});

			if (userInstanceDTOS != null && !userInstanceDTOS.isEmpty()) {
				userInstanceDTOS.forEach(e -> billableResources.addAll(getNotebookAndClusters(e)));
			}

		} catch (IOException e) {
			log.error("Error during preparation of billable resources", e);
		}

		return billableResources;
	}

	private Set<AzureDlabBillableResource> getNotebookAndClusters(UserInstanceDTO userInstanceDTO) {
		Set<AzureDlabBillableResource> notebookResources = new HashSet<>();

		if (StringUtils.isNotEmpty(userInstanceDTO.getExploratoryId())) {
			notebookResources.add(AzureDlabBillableResource.builder()
					.id(userInstanceDTO.getExploratoryId())
					.type(DlabResourceType.EXPLORATORY)
					.user(userInstanceDTO.getUser())
					.project(userInstanceDTO.getProject())
					.notebookId(userInstanceDTO.getExploratoryId())
					.resourceName(userInstanceDTO.getExploratoryName())
					.build());
			notebookResources.addAll(getVolumes(userInstanceDTO, userInstanceDTO.getExploratoryId(), "Volume primary",
					"Volume secondary"));

			if (userInstanceDTO.getResources() != null && !userInstanceDTO.getResources().isEmpty()) {
				for (UserComputationalResource userComputationalResource : userInstanceDTO.getResources()) {
					if (StringUtils.isNotEmpty(userComputationalResource.getComputationalId())) {

						notebookResources.add(AzureDlabBillableResource.builder()
								.id(userComputationalResource.getComputationalId())
								.type(DlabResourceType.COMPUTATIONAL)
								.user(userInstanceDTO.getUser())
								.project(userInstanceDTO.getProject())
								.notebookId(userInstanceDTO.getExploratoryId())
								.resourceName(userComputationalResource.getComputationalName())
								.build());
						final List<AzureDlabBillableResource> volumes = getVolumes(userInstanceDTO,
								userComputationalResource.getComputationalId(),
								userComputationalResource.getComputationalName() + " volume primary",
								userComputationalResource.getComputationalName() + " volume secondary");
						notebookResources.addAll(volumes);

					} else {
						log.error("Computational with empty id {} is found in notebook {}. Skip it.",
								userComputationalResource, userInstanceDTO);
					}
				}
			}

		} else {
			log.error("Notebook {} with empty id id found. Skip it.", userInstanceDTO);
		}

		return notebookResources;
	}

	private List<AzureDlabBillableResource> getVolumes(UserInstanceDTO userInstanceDTO, String exploratoryId, String
			primaryVolumeName, String secondaryVolumeName) {

		return Arrays.asList(
				AzureDlabBillableResource.builder()
						.id(exploratoryId + "-volume-primary")
						.type(DlabResourceType.VOLUME)
						.user(userInstanceDTO.getUser())
						.project(userInstanceDTO.getProject())
						.notebookId(userInstanceDTO.getExploratoryId())
						.resourceName(primaryVolumeName)
						.build(),
				AzureDlabBillableResource.builder()
						.id(exploratoryId + "-volume-secondary")
						.type(DlabResourceType.VOLUME)
						.user(userInstanceDTO.getUser())
						.project(userInstanceDTO.getProject())
						.notebookId(userInstanceDTO.getExploratoryId())
						.resourceName(secondaryVolumeName)
						.build()
		);
	}
}
