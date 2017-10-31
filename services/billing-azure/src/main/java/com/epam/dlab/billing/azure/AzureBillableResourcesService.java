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

import com.epam.dlab.billing.DlabResourceType;
import com.epam.dlab.billing.azure.model.AzureDlabBillableResource;
import com.epam.dlab.dto.UserInstanceDTO;
import com.epam.dlab.dto.azure.edge.EdgeInfoAzure;
import com.epam.dlab.dto.computational.UserComputationalResource;
import com.epam.dlab.mongo.MongoKeyWords;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import com.mongodb.client.model.Filters;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Helps to retrieve billable resources that are created in scope of DLab usage. Uses MongoDB as data source
 * for created resources
 */
@Slf4j
public class AzureBillableResourcesService {
    private final ObjectMapper objectMapper = new ObjectMapper();

    private MongoDbBillingClient mongoDbBillingClient;
    private String serviceBaseName;

    /**
     * Constructs the service class
     *
     * @param mongoDbBillingClient mongodb client to retrieve all billable resources
     */
    public AzureBillableResourcesService(MongoDbBillingClient mongoDbBillingClient) {
        this.mongoDbBillingClient = mongoDbBillingClient;

        Document document = mongoDbBillingClient.getDatabase().getCollection(MongoKeyWords.SETTINGS_COLLECTION)
                .find(Filters.eq("_id", MongoKeyWords.SERVICE_BASE_NAME_KEY)).first();

        if (document != null) {
            this.serviceBaseName = document.getString("value");
            if (StringUtils.isEmpty(this.serviceBaseName)) {
                throw new IllegalStateException("Configuration service base name is empty");
            } else {
                this.serviceBaseName = serviceBaseName.replace('_', '-').toLowerCase();
            }
        } else {
            throw new IllegalStateException("Configuration service base name is not present in settings");
        }
    }


    /**
     * Collects billable resources
     *
     * @return set of all billable resources that were created in scope by DLab from its installation to current time
     */
    public Set<AzureDlabBillableResource> getBillableResources() {

        Set<AzureDlabBillableResource> billableResources = new HashSet<>();

        billableResources.addAll(getSsn());
        billableResources.addAll(getEdgeAndContainers());
        billableResources.addAll(getNotebooksAndClusters());

        List<AzureDlabBillableResource> list = billableResources.stream().collect(Collectors.toList());
        list.sort(Comparator.comparing(AzureDlabBillableResource::getId));

        try {
            log.debug("Billable resources is \n {}", objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(list));
        } catch (JsonProcessingException e) {
            log.debug("Error during pretty printing. Show simple list");
            log.debug("Billable resources is {}", list);
        }

        return billableResources;
    }

    private Set<AzureDlabBillableResource> getSsn() {

        return Sets.newHashSet(
                AzureDlabBillableResource.builder().id(serviceBaseName + "-ssn").type(DlabResourceType.SSN).build(),
                AzureDlabBillableResource.builder().id(serviceBaseName + "-ssn-container").type(DlabResourceType.SSN_BUCKET).build());
    }

    private Set<AzureDlabBillableResource> getEdgeAndContainers() {
        Set<AzureDlabBillableResource> billableResources = new HashSet<>();

        try {

            List<EdgeInfoAzure> edgeInfoList = objectMapper.readValue(
                    objectMapper.writeValueAsString(mongoDbBillingClient.getDatabase()
                            .getCollection(MongoKeyWords.EDGE_COLLECTION).find()),
                    new com.fasterxml.jackson.core.type.TypeReference<List<EdgeInfoAzure>>() {
                    });

            if (edgeInfoList != null && !edgeInfoList.isEmpty()) {
                for (EdgeInfoAzure edgeInfoAzure : edgeInfoList) {
                    billableResources.addAll(getEdgeAndContainers(edgeInfoAzure));
                }
            }

            return billableResources;
        } catch (IOException e) {
            log.error("Error during preparation of billable resources", e);
        }
        return billableResources;
    }

    private Set<AzureDlabBillableResource> getEdgeAndContainers(EdgeInfoAzure edgeInfoAzure) {

        Set<AzureDlabBillableResource> billableResources = new HashSet<>();

        if (StringUtils.isNotEmpty(edgeInfoAzure.getSharedContainerName())) {
            billableResources.add(AzureDlabBillableResource.builder()
                    .id(edgeInfoAzure.getSharedContainerName())
                    .type(DlabResourceType.COLLABORATION_BUCKET)
                    .user(edgeInfoAzure.getId()).build());
        }

        if (StringUtils.isNotEmpty(edgeInfoAzure.getUserContainerName())) {
            billableResources.add(AzureDlabBillableResource.builder()
                    .id(edgeInfoAzure.getUserContainerName())
                    .type(DlabResourceType.EDGE_BUCKET)
                    .user(edgeInfoAzure.getId()).build());
        }

        if (StringUtils.isNotEmpty(edgeInfoAzure.getInstanceId())) {
            billableResources.add(AzureDlabBillableResource.builder()
                    .id(edgeInfoAzure.getInstanceId())
                    .type(DlabResourceType.EDGE)
                    .user(edgeInfoAzure.getId()).build());
        }

        return billableResources;
    }

    private Set<AzureDlabBillableResource> getNotebooksAndClusters() {

        Set<AzureDlabBillableResource> billableResources = new HashSet<>();

        try {
            List<UserInstanceDTO> userInstanceDTOS = objectMapper.readValue(
                    objectMapper.writeValueAsString(mongoDbBillingClient.getDatabase()
                            .getCollection(MongoKeyWords.NOTEBOOK_COLLECTION).find()),
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
                    .notebookId(userInstanceDTO.getExploratoryId())
                    .resourceName(userInstanceDTO.getExploratoryName()).build());

            if (userInstanceDTO.getResources() != null && !userInstanceDTO.getResources().isEmpty()) {
                for (UserComputationalResource userComputationalResource : userInstanceDTO.getResources()) {
                    if (StringUtils.isNotEmpty(userComputationalResource.getComputationalId())) {

                        notebookResources.add(AzureDlabBillableResource.builder()
                                .id(userComputationalResource.getComputationalId())
                                .type(DlabResourceType.COMPUTATIONAL)
                                .user(userInstanceDTO.getUser())
                                .notebookId(userInstanceDTO.getExploratoryId())
                                .resourceName(userComputationalResource.getComputationalName()).build());

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
}
