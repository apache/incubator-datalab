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
import com.epam.dlab.backendapi.dao.BaseDAO;
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.dao.ExploratoryLibDAO;
import com.epam.dlab.backendapi.domain.EndpointDTO;
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.resources.dto.LibInfoRecord;
import com.epam.dlab.backendapi.resources.dto.LibKey;
import com.epam.dlab.backendapi.resources.dto.LibraryStatus;
import com.epam.dlab.backendapi.service.EndpointService;
import com.epam.dlab.backendapi.service.LibraryService;
import com.epam.dlab.backendapi.util.RequestBuilder;
import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.dto.UserInstanceDTO;
import com.epam.dlab.dto.UserInstanceStatus;
import com.epam.dlab.dto.computational.UserComputationalResource;
import com.epam.dlab.dto.exploratory.LibInstallDTO;
import com.epam.dlab.dto.exploratory.LibStatus;
import com.epam.dlab.dto.exploratory.LibraryInstallDTO;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.model.library.Library;
import com.epam.dlab.rest.client.RESTService;
import com.epam.dlab.rest.contracts.ComputationalAPI;
import com.epam.dlab.rest.contracts.ExploratoryAPI;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class LibraryServiceImpl implements LibraryService {

	private static final String COMPUTATIONAL_NOT_FOUND_MSG = "Computational with name %s was not found";
	private static final String LIB_ALREADY_INSTALLED = "Library %s is already installing";
	@Inject
	private ExploratoryDAO exploratoryDAO;

	@Inject
	private ExploratoryLibDAO libraryDAO;

	@Inject
	private RequestBuilder requestBuilder;

	@Named(ServiceConsts.PROVISIONING_SERVICE_NAME)
	@Inject
	private RESTService provisioningService;

	@Inject
	private RequestId requestId;
	@Inject
	private EndpointService endpointService;


	@Override
	@SuppressWarnings("unchecked")
	public List<Document> getLibs(String user, String project, String exploratoryName, String computationalName) {
		if (StringUtils.isEmpty(computationalName)) {
			return (List<Document>) libraryDAO.findExploratoryLibraries(user, project, exploratoryName)
					.getOrDefault(ExploratoryLibDAO.EXPLORATORY_LIBS, new ArrayList<>());
		} else {
			Document document = (Document) libraryDAO.findComputationalLibraries(user, project,
					exploratoryName, computationalName)
					.getOrDefault(ExploratoryLibDAO.COMPUTATIONAL_LIBS, new Document());

			return (List<Document>) document.getOrDefault(computationalName, new ArrayList<>());
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<LibInfoRecord> getLibInfo(String user, String project, String exploratoryName) {
		Document document = libraryDAO.findAllLibraries(user, project, exploratoryName);

		Map<LibKey, List<LibraryStatus>> model = new TreeMap<>(Comparator.comparing(LibKey::getName)
				.thenComparing(LibKey::getVersion)
				.thenComparing(LibKey::getGroup));

		if (document.get(ExploratoryLibDAO.EXPLORATORY_LIBS) != null) {
			List<Document> exploratoryLibs = (List<Document>) document.get(ExploratoryLibDAO.EXPLORATORY_LIBS);
			exploratoryLibs.forEach(e -> populateModel(exploratoryName, e, model, "notebook"));

		}

		if (document.get(ExploratoryLibDAO.COMPUTATIONAL_LIBS) != null) {
			Document computationalLibs = getLibsOfActiveComputationalResources(document);
			populateComputational(computationalLibs, model, "cluster");
		}

		List<LibInfoRecord> libInfoRecords = new ArrayList<>();

		for (Map.Entry<LibKey, List<LibraryStatus>> entry : model.entrySet()) {
			libInfoRecords.add(new LibInfoRecord(entry.getKey(), entry.getValue()));

		}

		return libInfoRecords;
	}

	@Override
	public String installComputationalLibs(UserInfo ui, String project, String expName, String compName,
										   List<LibInstallDTO> libs) {

		final UserInstanceDTO userInstance = exploratoryDAO.fetchExploratoryFields(ui.getName(), project, expName, compName);
		EndpointDTO endpointDTO = endpointService.get(userInstance.getEndpoint());
		final String uuid =
				provisioningService.post(endpointDTO.getUrl() + ComputationalAPI.COMPUTATIONAL_LIB_INSTALL,
						ui.getAccessToken(),
						toComputationalLibraryInstallDto(ui, project, expName, compName, libs, userInstance, endpointDTO),
						String.class);
		requestId.put(ui.getName(), uuid);
		return uuid;
	}

	@Override
	public String installExploratoryLibs(UserInfo ui, String project, String expName, List<LibInstallDTO> libs) {
		final UserInstanceDTO userInstance = exploratoryDAO.fetchRunningExploratoryFields(ui.getName(), project, expName);
		EndpointDTO endpointDTO = endpointService.get(userInstance.getEndpoint());
		final String uuid =
				provisioningService.post(endpointDTO.getUrl() + ExploratoryAPI.EXPLORATORY_LIB_INSTALL,
						ui.getAccessToken(), toExploratoryLibraryInstallDto(ui, project, expName, libs, userInstance, endpointDTO),
						String.class);
		requestId.put(ui.getName(), uuid);
		return uuid;
	}

	private LibraryInstallDTO toExploratoryLibraryInstallDto(UserInfo userInfo, String project, String exploratoryName,
															 List<LibInstallDTO> libs, UserInstanceDTO userInstance, EndpointDTO endpointDTO) {
		final List<LibInstallDTO> libsToInstall = libs.stream()
				.map(lib -> toLibInstallDto(lib, libraryDAO.getLibrary(userInfo.getName(), project, exploratoryName,
						lib.getGroup(), lib.getName())))
				.peek(l -> libraryDAO.addLibrary(userInfo.getName(), project, exploratoryName, l, l.isOverride()))
				.collect(Collectors.toList());
		return requestBuilder.newLibInstall(userInfo, userInstance, endpointDTO, libsToInstall);
	}

	private LibraryInstallDTO toComputationalLibraryInstallDto(UserInfo userInfo, String project, String expName,
															   String compName, List<LibInstallDTO> libs,
															   UserInstanceDTO userInstance, EndpointDTO endpointDTO) {

		final UserComputationalResource computationalResource = getComputationalResource(compName, userInstance);
		final List<LibInstallDTO> libsToInstall = libs.stream()
				.map(lib -> toLibInstallDto(lib, libraryDAO.getLibrary(userInfo.getName(), project,
						expName, compName, lib.getGroup(), lib.getName())))
				.peek(l -> libraryDAO.addLibrary(userInfo.getName(), project, expName, compName,
						l, l.isOverride()))
				.collect(Collectors.toList());
		return requestBuilder.newLibInstall(userInfo, userInstance, computationalResource, libsToInstall, endpointDTO);
	}

	private UserComputationalResource getComputationalResource(String computationalName,
															   UserInstanceDTO userInstance) {
		return userInstance.getResources()
				.stream()
				.filter(computational -> computational.getComputationalName().equals(computationalName))
				.findAny()
				.orElseThrow(() -> new DlabException(String.format(COMPUTATIONAL_NOT_FOUND_MSG, computationalName)));
	}

	private LibInstallDTO toLibInstallDto(LibInstallDTO lib, Library existingLibrary) {
		final LibInstallDTO l = new LibInstallDTO(lib.getGroup(), lib.getName(), lib.getVersion());
		l.setStatus(LibStatus.INSTALLING.toString());
		l.setOverride(shouldOverride(existingLibrary));
		return l;
	}

	private boolean shouldOverride(Library library) {
		if (Objects.nonNull(library) && library.getStatus() == LibStatus.INSTALLING) {
			throw new DlabException(String.format(LIB_ALREADY_INSTALLED, library.getName()));
		} else {
			return Objects.nonNull(library);
		}
	}

	@SuppressWarnings("unchecked")
	private Document getLibsOfActiveComputationalResources(Document document) {
		Document computationalLibs = (Document) document.get(ExploratoryLibDAO.COMPUTATIONAL_LIBS);

		if (document.get(ExploratoryDAO.COMPUTATIONAL_RESOURCES) != null) {
			List<Document> computationalResources = (List<Document>) document.get(ExploratoryDAO
					.COMPUTATIONAL_RESOURCES);

			Set<String> terminated = computationalResources.stream()
					.filter(doc -> doc.getString(BaseDAO.STATUS).equalsIgnoreCase(UserInstanceStatus.TERMINATED
							.toString()))
					.map(doc -> doc.getString("computational_name")).collect(Collectors.toSet());

			terminated.forEach(computationalLibs::remove);
		}

		return computationalLibs;
	}


	private void populateModel(String exploratoryName, Document document, Map<LibKey, List<LibraryStatus>> model,
							   String resourceType) {
		String name = document.getString(ExploratoryLibDAO.LIB_NAME);
		String version = document.getString(ExploratoryLibDAO.LIB_VERSION);
		String group = document.getString(ExploratoryLibDAO.LIB_GROUP);
		String status = document.getString(ExploratoryLibDAO.STATUS);
		String error = document.getString(ExploratoryLibDAO.ERROR_MESSAGE);

		LibKey libKey = new LibKey(name, version, group);
		List<LibraryStatus> statuses = model.getOrDefault(libKey, new ArrayList<>());

		if (statuses.isEmpty()) {
			model.put(libKey, statuses);
		}

		statuses.add(new LibraryStatus(exploratoryName, resourceType, status, error));
	}

	@SuppressWarnings("unchecked")
	private void populateComputational(Document computationalLibs, Map<LibKey, List<LibraryStatus>> model, String
			resourceType) {
		for (Map.Entry<String, Object> entry : computationalLibs.entrySet()) {
			if (entry.getValue() != null) {
				List<Document> docs = (List<Document>) entry.getValue();
				docs.forEach(e -> populateModel(entry.getKey(), e, model, resourceType));
			}
		}
	}
}
