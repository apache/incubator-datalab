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

package com.epam.dlab.backendapi.resources;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.domain.ExploratoryLibCache;
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.resources.dto.LibInfoRecord;
import com.epam.dlab.backendapi.resources.dto.LibInstallFormDTO;
import com.epam.dlab.backendapi.resources.dto.SearchLibsFormDTO;
import com.epam.dlab.backendapi.service.ExternalLibraryService;
import com.epam.dlab.backendapi.service.LibraryService;
import com.epam.dlab.backendapi.validation.annotation.LibNameValid;
import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.dto.LibraryDTO;
import com.epam.dlab.dto.UserInstanceDTO;
import com.epam.dlab.dto.exploratory.LibraryInstallDTO;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.client.RESTService;
import com.epam.dlab.rest.contracts.ComputationalAPI;
import com.epam.dlab.rest.contracts.ExploratoryAPI;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Manages libraries for exploratory and computational environment
 */
@Path("/infrastructure_provision/exploratory_environment")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class LibExploratoryResource {


	private final ExternalLibraryService externalLibraryService;
	private ExploratoryDAO exploratoryDAO;
	private LibraryService libraryService;
	private RESTService provisioningService;
	private RequestId requestId;

	@Inject
	public LibExploratoryResource(ExploratoryDAO exploratoryDAO, LibraryService libraryService,
								  @Named(ServiceConsts.PROVISIONING_SERVICE_NAME) RESTService provisioningService,
								  RequestId requestId, ExternalLibraryService externalLibraryService) {
		this.exploratoryDAO = exploratoryDAO;
		this.libraryService = libraryService;
		this.provisioningService = provisioningService;
		this.requestId = requestId;
		this.externalLibraryService = externalLibraryService;
	}

	/**
	 * Returns the list of libraries groups for exploratory.
	 *
	 * @param userInfo          user info.
	 * @param exploratoryName   name of exploratory
	 * @param computationalName name of computational cluster
	 * @return library groups
	 */
	@GET
	@Path("/lib_groups")
	public Iterable<String> getLibGroupList(@Auth UserInfo userInfo,
											@QueryParam("exploratory_name") @NotBlank String exploratoryName,
											@QueryParam("computational_name") String computationalName) {

		log.trace("Loading list of lib groups for user {} and exploratory {}, computational {}", userInfo.getName(),
				exploratoryName, computationalName);
		try {
			if (StringUtils.isEmpty(computationalName)) {
				UserInstanceDTO userInstance = exploratoryDAO.fetchExploratoryFields(userInfo.getName(),
						exploratoryName);
				return ExploratoryLibCache.getCache().getLibGroupList(userInfo, userInstance);
			} else {
				UserInstanceDTO userInstance = exploratoryDAO.fetchExploratoryFields(userInfo.getName(),
						exploratoryName, computationalName);

				userInstance.setResources(userInstance.getResources().stream()
						.filter(e -> e.getComputationalName().equals(computationalName))
						.collect(Collectors.toList()));

				return ExploratoryLibCache.getCache().getLibGroupList(userInfo, userInstance);
			}
		} catch (Exception t) {
			log.error("Cannot load list of lib groups for user {} and exploratory {}", userInfo.getName(),
					exploratoryName, t);
			throw new DlabException("Cannot load list of libraries groups: " + t.getLocalizedMessage(), t);
		}
	}

	/**
	 * Returns list of installed/failed libraries for dlab resource <code>exploratoryName<code/>
	 * and <code>computationalName<code/> resource
	 *
	 * @param userInfo          user info
	 * @param exploratoryName   name of exploratory resource
	 * @param computationalName name of computational cluster
	 * @return list of libraries
	 */
	@GET
	@Path("/lib_list")
	public List<Document> getLibList(@Auth UserInfo userInfo,
									 @QueryParam("exploratory_name") @NotBlank String exploratoryName,
									 @QueryParam("computational_name") String computationalName) {

		log.debug("Loading list of libraries for user {} and exploratory {} and computational {}", userInfo.getName(),
				exploratoryName, computationalName);
		try {
			return libraryService.getLibs(userInfo.getName(), exploratoryName, computationalName);

		} catch (Exception t) {
			log.error("Cannot load installed libraries for user {} and exploratory {} an", userInfo.getName(),
					exploratoryName, t);
			throw new DlabException("Cannot load installed libraries: " + t.getLocalizedMessage(), t);
		}
	}

	/**
	 * Returns formatted representation of installed libraries or libraries that were tried to be installed for
	 * exploratory
	 * and computational resources that relate to <code>exploratoryName<code/> exploratory resource with its's
	 * statuses.
	 *
	 * @param userInfo        user info.
	 * @param exploratoryName name of exploratory resource.
	 * @return list of installed/failed libraries
	 */
	@GET
	@Path("/lib_list/formatted")
	public List<LibInfoRecord> getLibListFormatted(@Auth UserInfo userInfo,
												   @QueryParam("exploratory_name") @NotBlank String exploratoryName) {

		log.debug("Loading formatted list of libraries for user {} and exploratory {}", userInfo.getName(),
				exploratoryName);
		try {
			return libraryService.getLibInfo(userInfo.getName(), exploratoryName);
		} catch (Exception t) {
			log.error("Cannot load list of libraries for user {} and exploratory {}", userInfo.getName(),
					exploratoryName, t);
			throw new DlabException("Cannot load  formatted list of installed libraries: " + t.getLocalizedMessage(),
					t);
		}
	}

	/**
	 * Install libraries to the exploratory environment.
	 *
	 * @param userInfo user info.
	 * @param formDTO  description of libraries which will be installed to the exploratory environment.
	 * @return Invocation response as JSON string.
	 */
	@POST
	@Path("/lib_install")
	public Response libInstall(@Auth UserInfo userInfo, @Valid @NotNull LibInstallFormDTO formDTO) {
		log.debug("Installing libs to environment {} for user {}", formDTO, userInfo.getName());
		try {

			LibraryInstallDTO dto = libraryService.generateLibraryInstallDTO(userInfo, formDTO);
			String uuid;

			if (StringUtils.isEmpty(formDTO.getComputationalName())) {
				uuid = provisioningService.post(ExploratoryAPI.EXPLORATORY_LIB_INSTALL, userInfo.getAccessToken(),
						libraryService.prepareExploratoryLibInstallation(userInfo.getName(), formDTO, dto),
						String.class);
			} else {
				uuid = provisioningService.post(ComputationalAPI.COMPUTATIONAL_LIB_INSTALL, userInfo.getAccessToken(),
						libraryService.prepareComputationalLibInstallation(userInfo.getName(), formDTO, dto),
						String.class);
			}

			requestId.put(userInfo.getName(), uuid);
			return Response.ok(uuid).build();
		} catch (DlabException e) {
			log.error("Cannot install libs to exploratory environment {} for user {}: {}",
					formDTO.getNotebookName(), userInfo.getName(), e.getLocalizedMessage(), e);
			throw new DlabException("Cannot install libraries: " + e.getLocalizedMessage(), e);
		}
	}

	/**
	 * Returns the list of available libraries for exploratory basing on search conditions provided in @formDTO.
	 *
	 * @param userInfo user info.
	 * @param formDTO  search condition for find libraries for the exploratory environment.
	 * @return found libraries
	 */
	@POST
	@Path("search/lib_list")
	public List<LibraryDTO> getLibList(@Auth UserInfo userInfo, @Valid @NotNull SearchLibsFormDTO formDTO) {
		log.trace("Search list of libs for user {} with condition {}", userInfo.getName(), formDTO);
		try {

			UserInstanceDTO userInstance;

			if (StringUtils.isNotEmpty(formDTO.getComputationalName())) {

				userInstance = exploratoryDAO.fetchExploratoryFields(userInfo.getName(),
						formDTO.getNotebookName(), formDTO.getComputationalName());

				userInstance.setResources(userInstance.getResources().stream()
						.filter(e -> e.getComputationalName().equals(formDTO.getComputationalName()))
						.collect(Collectors.toList()));

			} else {
				userInstance = exploratoryDAO.fetchExploratoryFields(userInfo.getName(), formDTO.getNotebookName());
			}

			return ExploratoryLibCache.getCache().getLibList(userInfo, userInstance, formDTO.getGroup(), formDTO
					.getStartWith());
		} catch (Exception t) {
			log.error("Cannot search libs for user {} with condition {}",
					userInfo.getName(), formDTO, t);
			throw new DlabException("Cannot search libraries: " + t.getLocalizedMessage(), t);
		}
	}

	@GET
	@Path("search/lib_list/maven")
	public Response getMavenArtifactInfo(@Auth UserInfo userInfo,
										 @LibNameValid @QueryParam("artifact") String artifact) {
		final String[] libNameParts = artifact.split(":");
		return Response.ok(externalLibraryService.getLibrary(libNameParts[0], libNameParts[1], libNameParts[2])).build();
	}
}
