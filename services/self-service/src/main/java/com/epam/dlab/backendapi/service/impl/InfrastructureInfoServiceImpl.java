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

package com.epam.dlab.backendapi.service.impl;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.conf.SelfServiceApplicationConfiguration;
import com.epam.dlab.backendapi.dao.BillingDAO;
import com.epam.dlab.backendapi.dao.EnvDAO;
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.domain.EndpointDTO;
import com.epam.dlab.backendapi.domain.ProjectEndpointDTO;
import com.epam.dlab.backendapi.resources.dto.HealthStatusPageDTO;
import com.epam.dlab.backendapi.resources.dto.ProjectInfrastructureInfo;
import com.epam.dlab.backendapi.service.EndpointService;
import com.epam.dlab.backendapi.service.InfrastructureInfoService;
import com.epam.dlab.backendapi.service.ProjectService;
import com.epam.dlab.dto.InfrastructureMetaInfoDTO;
import com.epam.dlab.dto.aws.edge.EdgeInfoAws;
import com.epam.dlab.dto.azure.edge.EdgeInfoAzure;
import com.epam.dlab.dto.base.edge.EdgeInfo;
import com.epam.dlab.dto.gcp.edge.EdgeInfoGcp;
import com.epam.dlab.exceptions.DlabException;
import com.google.inject.Inject;
import com.jcabi.manifests.Manifests;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
public class InfrastructureInfoServiceImpl implements InfrastructureInfoService {

	private static final String RELEASE_NOTES_FORMAT = "https://github.com/apache/incubator-dlab/blob/%s" +
			"/RELEASE_NOTES.md";
	@Inject
	private ExploratoryDAO expDAO;
	@Inject
	private EnvDAO envDAO;
	@Inject
	private SelfServiceApplicationConfiguration configuration;
	@Inject
	private BillingDAO billingDAO;
	@Inject
	private ProjectService projectService;
	@Inject
	private EndpointService endpointService;


	@Override
	public List<ProjectInfrastructureInfo> getUserResources(String user) {
		log.debug("Loading list of provisioned resources for user {}", user);
		try {
			Iterable<Document> documents = expDAO.findExploratory(user);
			List<EndpointDTO> allEndpoints = endpointService.getEndpoints();
			return StreamSupport.stream(documents.spliterator(),
					false)
					.collect(Collectors.groupingBy(d -> d.getString("project")))
					.entrySet()
					.stream()
					.map(e -> {
						List<ProjectEndpointDTO> endpoints = projectService.get(e.getKey()).getEndpoints();
						List<EndpointDTO> endpointResult = allEndpoints.stream()
								.filter(endpoint -> endpoints.stream()
										.anyMatch(endpoint1 -> endpoint1.getName().equals(endpoint.getName())))
								.collect(Collectors.toList());
						final Map<String, Map<String, String>> projectEdges =
								endpoints.stream()
										.collect(Collectors.toMap(ProjectEndpointDTO::getName,
												endpointDTO -> getSharedInfo(endpointDTO.getEdgeInfo())));
						return new ProjectInfrastructureInfo(e.getKey(),
								billingDAO.getBillingProjectQuoteUsed(e.getKey()), projectEdges, e.getValue(), endpointResult);
					})
					.collect(Collectors.toList());
		} catch (Exception e) {
			log.error("Could not load list of provisioned resources for user: {}", user, e);
			throw new DlabException("Could not load list of provisioned resources for user: ");
		}
	}

	@Override
	public HealthStatusPageDTO getHeathStatus(UserInfo userInfo, boolean fullReport, boolean isAdmin) {
		final String user = userInfo.getName();
		log.debug("Request the status of resources for user {}, report type {}", user, fullReport);
		try {

			return envDAO.getHealthStatusPageDTO(user, fullReport)
					.withBillingEnabled(configuration.isBillingSchedulerEnabled())
					.withAdmin(isAdmin)
					.withProjectAssinged(projectService.isAnyProjectAssigned(userInfo))
					.withBillingQuoteUsed(billingDAO.getBillingQuoteUsed())
					.withBillingUserQuoteUsed(billingDAO.getBillingUserQuoteUsed(user));
		} catch (Exception e) {
			log.warn("Could not return status of resources for user {}: {}", user, e.getLocalizedMessage(), e);
			throw new DlabException(e.getMessage(), e);
		}
	}

	@Override
	public InfrastructureMetaInfoDTO getInfrastructureMetaInfo() {
		final String branch = Manifests.read("GIT-Branch");
		return InfrastructureMetaInfoDTO.builder()
				.branch(branch)
				.commit(Manifests.read("GIT-Commit"))
				.version(Manifests.read("DLab-Version"))
				.releaseNotes(String.format(RELEASE_NOTES_FORMAT, branch))
				.build();
	}

	private Map<String, String> getSharedInfo(EdgeInfo edgeInfo) {
		Map<String, String> shared = new HashMap<>();
		if (Objects.isNull(edgeInfo)) {
			return shared;
		}
		shared.put("edge_node_ip", edgeInfo.getPublicIp());
		if (edgeInfo instanceof EdgeInfoAws) {
			EdgeInfoAws edgeInfoAws = (EdgeInfoAws) edgeInfo;
			shared.put("user_own_bicket_name", edgeInfoAws.getUserOwnBucketName());
			shared.put("shared_bucket_name", edgeInfoAws.getSharedBucketName());
		} else if (edgeInfo instanceof EdgeInfoAzure) {
			EdgeInfoAzure edgeInfoAzure = (EdgeInfoAzure) edgeInfo;
			shared.put("user_container_name", edgeInfoAzure.getUserContainerName());
			shared.put("shared_container_name", edgeInfoAzure.getSharedContainerName());
			shared.put("user_storage_account_name", edgeInfoAzure.getUserStorageAccountName());
			shared.put("shared_storage_account_name", edgeInfoAzure.getSharedStorageAccountName());
			shared.put("datalake_name", edgeInfoAzure.getDataLakeName());
			shared.put("datalake_user_directory_name", edgeInfoAzure.getDataLakeDirectoryName());
			shared.put("datalake_shared_directory_name", edgeInfoAzure.getDataLakeSharedDirectoryName());
		} else if (edgeInfo instanceof EdgeInfoGcp) {
			EdgeInfoGcp edgeInfoGcp = (EdgeInfoGcp) edgeInfo;
			shared.put("user_own_bucket_name", edgeInfoGcp.getUserOwnBucketName());
			shared.put("shared_bucket_name", edgeInfoGcp.getSharedBucketName());
		}

		return shared;
	}
}
