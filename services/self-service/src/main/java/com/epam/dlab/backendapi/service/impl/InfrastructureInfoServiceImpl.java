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
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.domain.BillingReport;
import com.epam.dlab.backendapi.domain.EndpointDTO;
import com.epam.dlab.backendapi.domain.ProjectDTO;
import com.epam.dlab.backendapi.domain.ProjectEndpointDTO;
import com.epam.dlab.backendapi.resources.dto.HealthStatusEnum;
import com.epam.dlab.backendapi.resources.dto.HealthStatusPageDTO;
import com.epam.dlab.backendapi.resources.dto.ProjectInfrastructureInfo;
import com.epam.dlab.backendapi.roles.RoleType;
import com.epam.dlab.backendapi.roles.UserRoles;
import com.epam.dlab.backendapi.service.BillingService;
import com.epam.dlab.backendapi.service.EndpointService;
import com.epam.dlab.backendapi.service.InfrastructureInfoService;
import com.epam.dlab.backendapi.service.ProjectService;
import com.epam.dlab.dto.InfrastructureMetaInfoDTO;
import com.epam.dlab.dto.UserInstanceDTO;
import com.epam.dlab.dto.aws.edge.EdgeInfoAws;
import com.epam.dlab.dto.azure.edge.EdgeInfoAzure;
import com.epam.dlab.dto.base.edge.EdgeInfo;
import com.epam.dlab.dto.computational.UserComputationalResource;
import com.epam.dlab.dto.gcp.edge.EdgeInfoGcp;
import com.epam.dlab.exceptions.DlabException;
import com.google.inject.Inject;
import com.jcabi.manifests.Manifests;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class InfrastructureInfoServiceImpl implements InfrastructureInfoService {
	private static final String RELEASE_NOTES_FORMAT = "https://github.com/apache/incubator-dlab/blob/%s/RELEASE_NOTES.md";
	private static final String PERMISSION_VIEW = "/api/bucket/view";
	private static final String PERMISSION_UPLOAD = "/api/bucket/upload";
	private static final String PERMISSION_DOWNLOAD = "/api/bucket/download";
	private static final String PERMISSION_DELETE = "/api/bucket/delete";

	private final ExploratoryDAO expDAO;
	private final SelfServiceApplicationConfiguration configuration;
	private final ProjectService projectService;
	private final EndpointService endpointService;
	private final BillingService billingService;

	@Inject
	public InfrastructureInfoServiceImpl(ExploratoryDAO expDAO, SelfServiceApplicationConfiguration configuration,
	                                     ProjectService projectService, EndpointService endpointService,
	                                     BillingService billingService) {
		this.expDAO = expDAO;
		this.configuration = configuration;
		this.projectService = projectService;
		this.endpointService = endpointService;
		this.billingService = billingService;
	}

	@Override
	public List<ProjectInfrastructureInfo> getUserResources(UserInfo user) {
		log.debug("Loading list of provisioned resources for user {}", user);
		try {
			List<EndpointDTO> allEndpoints = endpointService.getEndpoints();
			return projectService.getUserProjects(user, false)
					.stream()
					.map(p -> {
						List<UserInstanceDTO> exploratories = expDAO.findExploratories(user.getName(), p.getName());
						return new ProjectInfrastructureInfo(p.getName(), billingService.getBillingProjectQuoteUsed(p.getName()),
								getSharedInfo(p.getName()), exploratories, getExploratoryBillingData(exploratories),
								getEndpoints(allEndpoints, p));
					})
					.collect(Collectors.toList());
		} catch (Exception e) {
			log.error("Could not load list of provisioned resources for user: {}", user, e);
			throw new DlabException("Could not load list of provisioned resources for user: ");
		}
	}

	@Override
	public HealthStatusPageDTO getHeathStatus(UserInfo userInfo, boolean fullReport) {
		final String user = userInfo.getName();
		log.debug("Request the status of resources for user {}, report type {}", user, fullReport);
		try {
			return HealthStatusPageDTO.builder()
					.status(HealthStatusEnum.OK.toString())
					.listResources(Collections.emptyList())
					.billingEnabled(configuration.isBillingSchedulerEnabled())
					.auditEnabled(configuration.isAuditEnabled())
					.projectAdmin(UserRoles.isProjectAdmin(userInfo))
					.admin(UserRoles.isAdmin(userInfo))
					.projectAssigned(projectService.isAnyProjectAssigned(userInfo))
					.bucketBrowser(HealthStatusPageDTO.BucketBrowser.builder()
							.view(checkAccess(userInfo, PERMISSION_VIEW))
							.upload(checkAccess(userInfo, PERMISSION_UPLOAD))
							.download(checkAccess(userInfo, PERMISSION_DOWNLOAD))
							.delete(checkAccess(userInfo, PERMISSION_DELETE))
							.build())
					.build();
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

	private List<BillingReport> getExploratoryBillingData(List<UserInstanceDTO> exploratories) {
		return exploratories
				.stream()
				.map(exp -> billingService.getExploratoryBillingData(exp.getProject(), exp.getEndpoint(),
						exp.getExploratoryName(), exp.getResources()
								.stream()
								.map(UserComputationalResource::getComputationalName)
								.collect(Collectors.toList())
				))
				.collect(Collectors.toList());
	}

	private List<EndpointDTO> getEndpoints(List<EndpointDTO> allEndpoints, ProjectDTO projectDTO) {
		return allEndpoints.stream().filter(endpoint -> projectDTO.getEndpoints().stream()
				.anyMatch(endpoint1 -> endpoint1.getName().equals(endpoint.getName())))
				.collect(Collectors.toList());
	}

	private Map<String, Map<String, String>> getSharedInfo(String name) {
		return projectService.get(name).getEndpoints().stream()
				.collect(Collectors.toMap(ProjectEndpointDTO::getName, this::getSharedInfo));
	}

	private Map<String, String> getSharedInfo(ProjectEndpointDTO endpointDTO) {
		Optional<EdgeInfo> edgeInfo = Optional.ofNullable(endpointDTO.getEdgeInfo());
		if (!edgeInfo.isPresent()) {
			return Collections.emptyMap();
		}
		EdgeInfo edge = edgeInfo.get();
		Map<String, String> shared = new HashMap<>();

		shared.put("status", endpointDTO.getStatus().toString());
		shared.put("edge_node_ip", edge.getPublicIp());
		if (edge instanceof EdgeInfoAws) {
			EdgeInfoAws edgeInfoAws = (EdgeInfoAws) edge;
			shared.put("user_own_bicket_name", edgeInfoAws.getUserOwnBucketName());
			shared.put("shared_bucket_name", edgeInfoAws.getSharedBucketName());
		} else if (edge instanceof EdgeInfoAzure) {
			EdgeInfoAzure edgeInfoAzure = (EdgeInfoAzure) edge;
			shared.put("user_container_name", edgeInfoAzure.getUserContainerName());
			shared.put("shared_container_name", edgeInfoAzure.getSharedContainerName());
			shared.put("user_storage_account_name", edgeInfoAzure.getUserStorageAccountName());
			shared.put("shared_storage_account_name", edgeInfoAzure.getSharedStorageAccountName());
			shared.put("datalake_name", edgeInfoAzure.getDataLakeName());
			shared.put("datalake_user_directory_name", edgeInfoAzure.getDataLakeDirectoryName());
			shared.put("datalake_shared_directory_name", edgeInfoAzure.getDataLakeSharedDirectoryName());
		} else if (edge instanceof EdgeInfoGcp) {
			EdgeInfoGcp edgeInfoGcp = (EdgeInfoGcp) edge;
			shared.put("user_own_bucket_name", edgeInfoGcp.getUserOwnBucketName());
			shared.put("shared_bucket_name", edgeInfoGcp.getSharedBucketName());
		}

		return shared;
	}

	private boolean checkAccess(UserInfo userInfo, String permission) {
		return UserRoles.checkAccess(userInfo, RoleType.PAGE, permission, userInfo.getRoles());
	}
}
