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

package com.epam.dlab.backendapi.service.impl;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.BaseDAO;
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.dao.ExploratoryLibDAO;
import com.epam.dlab.backendapi.resources.dto.LibInfoRecord;
import com.epam.dlab.backendapi.resources.dto.LibInstallFormDTO;
import com.epam.dlab.backendapi.resources.dto.LibKey;
import com.epam.dlab.backendapi.resources.dto.LibraryStatus;
import com.epam.dlab.backendapi.service.LibraryService;
import com.epam.dlab.backendapi.util.RequestBuilder;
import com.epam.dlab.dto.UserInstanceDTO;
import com.epam.dlab.dto.UserInstanceStatus;
import com.epam.dlab.dto.computational.UserComputationalResource;
import com.epam.dlab.dto.exploratory.LibInstallDTO;
import com.epam.dlab.dto.exploratory.LibStatus;
import com.epam.dlab.dto.exploratory.LibraryInstallDTO;
import com.epam.dlab.exceptions.DlabException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class LibraryServiceImpl implements LibraryService {

	@Inject
	private ExploratoryDAO exploratoryDAO;

	@Inject
	private ExploratoryLibDAO libraryDAO;

	@Inject
	private RequestBuilder requestBuilder;

	@Override
	@SuppressWarnings("unchecked")
	public List<Document> getLibs(String user, String exploratoryName, String computationalName) {
		if (StringUtils.isEmpty(computationalName)) {
			return (List<Document>) libraryDAO.findExploratoryLibraries(user, exploratoryName)
					.getOrDefault(ExploratoryLibDAO.EXPLORATORY_LIBS, new ArrayList<>());
		} else {
			Document document = (Document) libraryDAO.findComputationalLibraries(user, exploratoryName,
					computationalName)
					.getOrDefault(ExploratoryLibDAO.COMPUTATIONAL_LIBS, new Document());

			return (List<Document>) document.getOrDefault(computationalName, new ArrayList<>());
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<LibInfoRecord> getLibInfo(String user, String exploratoryName) {
		Document document = libraryDAO.findAllLibraries(user, exploratoryName);

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
	public LibraryInstallDTO generateLibraryInstallDTO(UserInfo userInfo, LibInstallFormDTO formDTO) {
		UserInstanceDTO userInstance;
		LibraryInstallDTO dto;
		if (StringUtils.isEmpty(formDTO.getComputationalName())) {
			userInstance = exploratoryDAO.fetchExploratoryFields(userInfo.getName(), formDTO.getNotebookName());
			if (UserInstanceStatus.RUNNING != UserInstanceStatus.of(userInstance.getStatus())) {
				throw new DlabException("Exploratory " + formDTO.getNotebookName() + " is not running");
			}

			dto = requestBuilder.newLibInstall(userInfo, userInstance);
		} else {
			userInstance = exploratoryDAO.fetchExploratoryFields(userInfo.getName(), formDTO.getNotebookName(),
					formDTO.getComputationalName());

			List<UserComputationalResource> resourceList = userInstance.getResources().stream()
					.filter(e -> e.getComputationalName().equals(formDTO.getComputationalName()))
					.collect(Collectors.toList());

			if (resourceList.size() == 1) {
				dto = requestBuilder.newLibInstall(userInfo, userInstance, resourceList.get(0));
			} else {
				throw new DlabException(String.format("Computational with name %s is not unique or absent",
						formDTO.getComputationalName()));
			}
		}

		return dto;
	}

	@Override
	public LibraryInstallDTO prepareExploratoryLibInstallation(String username, LibInstallFormDTO formDTO,
															   LibraryInstallDTO dto) {
		for (LibInstallDTO lib : formDTO.getLibs()) {
			LibStatus status = libraryDAO.fetchLibraryStatus(username, formDTO.getNotebookName(),
					lib.getGroup(), lib.getName(), lib.getVersion());

			prepare(status, lib, dto);

			libraryDAO.addLibrary(username, formDTO.getNotebookName(), lib, LibStatus.FAILED == status);
		}

		return dto;

	}

	@Override
	public LibraryInstallDTO prepareComputationalLibInstallation(String username, LibInstallFormDTO formDTO,
																 LibraryInstallDTO dto) {

		for (LibInstallDTO lib : formDTO.getLibs()) {
			LibStatus status = libraryDAO.fetchLibraryStatus(username, formDTO.getNotebookName(),
					formDTO.getComputationalName(),
					lib.getGroup(), lib.getName(), lib.getVersion());

			prepare(status, lib, dto);

			libraryDAO.addLibrary(username, formDTO.getNotebookName(), formDTO.getComputationalName(), lib, LibStatus
					.FAILED == status);
		}
		return dto;
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

	private void prepare(LibStatus status, LibInstallDTO lib, LibraryInstallDTO dto) {
		if (status == LibStatus.INSTALLING) {
			throw new DlabException("Library " + lib.getName() + " is already installing");
		}

		LibInstallDTO newLib = new LibInstallDTO(lib.getGroup(), lib.getName(), lib.getVersion());
		if (dto.getLibs().contains(newLib)) {
			return;
		}
		dto.getLibs().add(newLib);
		lib.setStatus(LibStatus.INSTALLING.toString());
	}
}
