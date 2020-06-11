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
import com.epam.dlab.backendapi.annotation.Audit;
import com.epam.dlab.backendapi.annotation.User;
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.dao.GitCredsDAO;
import com.epam.dlab.backendapi.domain.EndpointDTO;
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.service.EndpointService;
import com.epam.dlab.backendapi.service.GitCredentialService;
import com.epam.dlab.backendapi.util.RequestBuilder;
import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.dto.UserInstanceDTO;
import com.epam.dlab.dto.exploratory.ExploratoryGitCredsDTO;
import com.epam.dlab.dto.exploratory.ExploratoryGitCredsUpdateDTO;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.client.RESTService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.stream.Collectors;

import static com.epam.dlab.backendapi.domain.AuditActionEnum.MANAGE_GIT_ACCOUNT;
import static com.epam.dlab.rest.contracts.ExploratoryAPI.EXPLORATORY_GIT_CREDS;

@Slf4j
@Singleton
public class GitCredentialServiceImpl implements GitCredentialService {

	private static final boolean CLEAR_USER_PASSWORD = true;
	@Inject
	private GitCredsDAO gitCredsDAO;
	@Inject
	private ExploratoryDAO exploratoryDAO;
	@Inject
	@Named(ServiceConsts.PROVISIONING_SERVICE_NAME)
	private RESTService provisioningService;
	@Inject
	private RequestBuilder requestBuilder;
	@Inject
	private RequestId requestId;
	@Inject
	private EndpointService endpointService;

	@Audit(action = MANAGE_GIT_ACCOUNT)
	@Override
	public void updateGitCredentials(@User UserInfo userInfo, ExploratoryGitCredsDTO formDTO) {
		log.debug("Updating GIT creds for user {} to {}", userInfo.getName(), formDTO);
		try {
			gitCredsDAO.updateGitCreds(userInfo.getName(), formDTO);
			final String failedNotebooks = exploratoryDAO.fetchRunningExploratoryFields(userInfo.getName())
					.stream()
					.filter(ui -> !updateNotebookGitCredentials(userInfo, formDTO, ui))
					.map(UserInstanceDTO::getExploratoryName)
					.collect(Collectors.joining(","));
			if (StringUtils.isNotEmpty(failedNotebooks)) {
				throw new DlabException("Requests for notebooks failed: " + failedNotebooks);
			}
		} catch (Exception t) {
			log.error("Cannot update the GIT creds for user {}", userInfo.getName(), t);
			throw new DlabException("Cannot update the GIT credentials: " + t.getLocalizedMessage(), t);
		}
	}

	@Override
	public ExploratoryGitCredsDTO getGitCredentials(String user) {
		log.debug("Loading GIT creds for user {}", user);
		try {
			return gitCredsDAO.findGitCreds(user, CLEAR_USER_PASSWORD);
		} catch (Exception t) {
			log.error("Cannot load list of GIT creds for user: {}", user, t);
			throw new DlabException(String.format("Cannot load GIT credentials for user %s: %s",
					user, t.getLocalizedMessage()), t);
		}
	}

	private boolean updateNotebookGitCredentials(UserInfo userInfo, ExploratoryGitCredsDTO formDTO,
												 UserInstanceDTO instance) {
		boolean gitCredentialsUpdated = true;
		try {
			log.debug("Updating GIT creds for user {} on exploratory {}",
					userInfo.getName(), instance.getExploratoryName());
			EndpointDTO endpointDTO = endpointService.get(instance.getEndpoint());
			ExploratoryGitCredsUpdateDTO dto = requestBuilder.newGitCredentialsUpdate(userInfo, instance, endpointDTO, formDTO);
			final String uuid = provisioningService.post(endpointDTO.getUrl() + EXPLORATORY_GIT_CREDS,
							userInfo.getAccessToken(), dto, String.class);
			requestId.put(userInfo.getName(), uuid);
		} catch (Exception t) {
			log.error("Cannot update the GIT creds for user {} on exploratory {}", userInfo.getName(),
					instance.getExploratoryName(), t);
			gitCredentialsUpdated = false;
		}
		return gitCredentialsUpdated;
	}
}
