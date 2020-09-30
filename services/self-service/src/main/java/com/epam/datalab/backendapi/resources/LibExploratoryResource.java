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

package com.epam.datalab.backendapi.resources;

import com.epam.datalab.auth.UserInfo;
import com.epam.datalab.backendapi.dao.ExploratoryDAO;
import com.epam.datalab.backendapi.domain.ExploratoryLibCache;
import com.epam.datalab.backendapi.resources.dto.LibInfoRecord;
import com.epam.datalab.backendapi.resources.dto.LibInstallFormDTO;
import com.epam.datalab.backendapi.resources.dto.LibraryAutoCompleteDTO;
import com.epam.datalab.backendapi.resources.dto.SearchLibsFormDTO;
import com.epam.datalab.backendapi.service.ExternalLibraryService;
import com.epam.datalab.backendapi.service.LibraryService;
import com.epam.datalab.backendapi.validation.annotation.LibNameValid;
import com.epam.datalab.dto.UserInstanceDTO;
import com.epam.datalab.dto.exploratory.LibInstallDTO;
import com.epam.datalab.exceptions.DatalabException;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.hibernate.validator.constraints.NotBlank;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
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
    private static final String AUDIT_MESSAGE = "Install libs: %s";

    private final ExternalLibraryService externalLibraryService;
    private final ExploratoryDAO exploratoryDAO;
    private final LibraryService libraryService;

    @Inject
    public LibExploratoryResource(ExploratoryDAO exploratoryDAO, LibraryService libraryService,
                                  ExternalLibraryService externalLibraryService) {
        this.exploratoryDAO = exploratoryDAO;
        this.libraryService = libraryService;
        this.externalLibraryService = externalLibraryService;
    }

    @GET
    @Path("/lib-groups/exploratory")
    public Response getExploratoryLibGroupList(@Auth UserInfo userInfo,
                                               @QueryParam("project") @NotBlank String projectName,
                                               @QueryParam("exploratory") @NotBlank String exploratoryName) {
        return Response.ok(libraryService.getExploratoryLibGroups(userInfo, projectName, exploratoryName)).build();
    }

    @GET
    @Path("/lib-groups/compute")
    public Response getComputeLibGroupList(@Auth UserInfo userInfo) {
        return Response.ok(libraryService.getComputeLibGroups()).build();
    }

    /**
     * Returns list of installed/failed libraries for datalab resource <code>exploratoryName<code/>
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
                                     @QueryParam("project_name") @NotBlank String projectName,
                                     @QueryParam("exploratory_name") @NotBlank String exploratoryName,
                                     @QueryParam("computational_name") String computationalName) {

        log.debug("Loading list of libraries for user {} and exploratory {} and computational {}", userInfo.getName(),
                exploratoryName, computationalName);
        try {
            return libraryService.getLibs(userInfo.getName(), projectName, exploratoryName, computationalName);

        } catch (Exception t) {
            log.error("Cannot load installed libraries for user {} and exploratory {} an", userInfo.getName(),
                    exploratoryName, t);
            throw new DatalabException("Cannot load installed libraries: " + t.getLocalizedMessage(), t);
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
                                                   @QueryParam("project_name") @NotBlank String projectName,
                                                   @QueryParam("exploratory_name") @NotBlank String exploratoryName) {

        log.debug("Loading formatted list of libraries for user {} and exploratory {}", userInfo.getName(),
                exploratoryName);
        try {
            return libraryService.getLibInfo(userInfo.getName(), projectName, exploratoryName);
        } catch (Exception t) {
            log.error("Cannot load list of libraries for user {} and exploratory {}", userInfo.getName(),
                    exploratoryName, t);
            throw new DatalabException("Cannot load  formatted list of installed libraries: " + t.getLocalizedMessage(),
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
    public Response libInstall(@Auth UserInfo userInfo,
                               @Valid @NotNull LibInstallFormDTO formDTO) {
        log.debug("Installing libs to environment {} for user {}", formDTO, userInfo.getName());
        String project = formDTO.getProject();
        final String exploratoryName = formDTO.getNotebookName();
        final List<LibInstallDTO> libs = formDTO.getLibs();
        final String computationalName = formDTO.getComputationalName();
        final String auditInfo = getAuditInfo(libs);
        String uuid = StringUtils.isEmpty(computationalName) ?
                libraryService.installExploratoryLibs(userInfo, project, exploratoryName, libs, auditInfo) :
                libraryService.installComputationalLibs(userInfo, project, exploratoryName, computationalName, libs, auditInfo);
        return Response.ok(uuid)
                .build();
    }

    /**
     * Returns the list of available libraries for exploratory basing on search conditions provided in @searchDTO.
     *
     * @param userInfo  user info.
     * @param searchDTO search condition for find libraries for the exploratory environment.
     * @return found libraries
     */
    @POST
    @Path("search/lib_list")
    public Response getLibList(@Auth UserInfo userInfo,
                               @Valid @NotNull SearchLibsFormDTO searchDTO) {
        try {
            UserInstanceDTO userInstance;
            if (StringUtils.isNotEmpty(searchDTO.getComputationalName())) {
                userInstance = exploratoryDAO.fetchExploratoryFields(userInfo.getName(), searchDTO.getProjectName(),
                        searchDTO.getNotebookName(), searchDTO.getComputationalName());
                userInstance.setResources(userInstance.getResources().stream()
                        .filter(e -> e.getComputationalName().equals(searchDTO.getComputationalName()))
                        .collect(Collectors.toList()));
            } else {
                userInstance = exploratoryDAO.fetchExploratoryFields(userInfo.getName(), searchDTO.getProjectName(), searchDTO.getNotebookName());
            }

            LibraryAutoCompleteDTO autoCompleteDTO = ExploratoryLibCache.getCache().getLibList(userInfo, userInstance, searchDTO.getGroup(), searchDTO.getStartWith());
            return Response.ok(autoCompleteDTO).build();
        } catch (Exception e) {
            log.error("Cannot search libs for user {} with condition {}", userInfo.getName(), searchDTO, e);
            throw new DatalabException("Cannot search libraries: " + e.getLocalizedMessage(), e);
        }
    }


    @GET
    @Path("search/lib_list/maven")
    public Response getMavenArtifactInfo(@Auth UserInfo userInfo,
                                         @LibNameValid @QueryParam("artifact") String artifact) {
        final String[] libNameParts = artifact.split(":");
        return Response.ok(externalLibraryService.getLibrary(libNameParts[0], libNameParts[1], libNameParts[2])).build();
    }

    private String getAuditInfo(List<LibInstallDTO> libs) {
        return String.format(AUDIT_MESSAGE, libs
                .stream()
                .map(LibInstallDTO::getName)
                .collect(Collectors.joining(", ")));
    }
}
