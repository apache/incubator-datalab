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
import com.epam.dlab.backendapi.dao.KeyDAO;
import com.epam.dlab.backendapi.resources.dto.HealthStatusPageDTO;
import com.epam.dlab.backendapi.resources.dto.ProjectInfrastructureInfo;
import com.epam.dlab.backendapi.service.InfrastructureInfoService;
import com.epam.dlab.backendapi.service.ProjectService;
import com.epam.dlab.dto.InfrastructureMetaInfoDTO;
import com.epam.dlab.dto.base.edge.EdgeInfo;
import com.epam.dlab.exceptions.DlabException;
import com.google.inject.Inject;
import com.jcabi.manifests.Manifests;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
public abstract class InfrastructureInfoServiceBase<T> implements InfrastructureInfoService {

	private static final String RELEASE_NOTES_FORMAT = "https://github.com/apache/incubator-dlab/blob/%s" +
			"/RELEASE_NOTES.md";
	@Inject
	private ExploratoryDAO expDAO;
	@Inject
	private KeyDAO keyDAO;
	@Inject
	private EnvDAO envDAO;
	@Inject
	private SelfServiceApplicationConfiguration configuration;
	@Inject
	private BillingDAO billingDAO;
	@Inject
	private ProjectService projectService;


	@SuppressWarnings("unchecked")
	private Map<String, String> getSharedInfo(EdgeInfo edgeInfo) {
		return getSharedInfo((T) edgeInfo);
	}

	@Override
	public List<ProjectInfrastructureInfo> getUserResources(String user) {
		log.debug("Loading list of provisioned resources for user {}", user);
		try {
			Iterable<Document> documents = expDAO.findExploratory(user);

			return StreamSupport.stream(documents.spliterator(),
					false)
					.collect(Collectors.groupingBy(d -> d.getString("project")))
					.entrySet()
					.stream()
					.map(e -> new ProjectInfrastructureInfo(e.getKey(),
							billingDAO.getBillingProjectQuoteUsed(e.getKey()),
							getSharedInfo(projectService.get(e.getKey()).getEdgeInfo()),
							e.getValue()))
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

	protected abstract Map<String, String> getSharedInfo(T sharedInfo);
}
