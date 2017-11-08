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

package com.epam.dlab.backendapi.service;

import com.epam.dlab.UserInstanceStatus;
import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.dao.ExploratoryLibDAO;
import com.epam.dlab.backendapi.resources.dto.LibInstallFormDTO;
import com.epam.dlab.backendapi.util.RequestBuilder;
import com.epam.dlab.dto.UserInstanceDTO;
import com.epam.dlab.dto.computational.UserComputationalResource;
import com.epam.dlab.dto.exploratory.LibInstallDTO;
import com.epam.dlab.dto.exploratory.LibStatus;
import com.epam.dlab.dto.exploratory.LibraryInstallDTO;
import com.epam.dlab.exceptions.DlabException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Singleton
public class LibraryService {

    @Inject
    private ExploratoryDAO exploratoryDAO;

    @Inject
    private ExploratoryLibDAO libraryDAO;

    public LibraryInstallDTO generateLibraryInstallDTO(UserInfo userInfo, LibInstallFormDTO formDTO) {
        UserInstanceDTO userInstance;
        LibraryInstallDTO dto;
        if (StringUtils.isEmpty(formDTO.getComputationalName())) {
            userInstance = exploratoryDAO.fetchExploratoryFields(userInfo.getName(), formDTO.getNotebookName());
            if (UserInstanceStatus.RUNNING != UserInstanceStatus.of(userInstance.getStatus())) {
                throw new DlabException("Exploratory " + formDTO.getNotebookName() + " is not running");
            }

            dto = RequestBuilder.newLibInstall(userInfo, userInstance);
        } else {
            userInstance = exploratoryDAO.fetchExploratoryFields(userInfo.getName(), formDTO.getNotebookName(),
                    formDTO.getComputationalName());

            List<UserComputationalResource> resourceList = userInstance.getResources().stream()
                    .filter(e -> e.getComputationalName().equals(formDTO.getComputationalName()))
                    .collect(Collectors.toList());

            if (resourceList.size() == 1) {
                dto = RequestBuilder.newLibInstall(userInfo, userInstance, resourceList.get(0));
            } else {
                throw new DlabException(String.format("Computational with name %s is not unique or absent",
                        formDTO.getComputationalName()));
            }
        }

        return dto;
    }

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

    public LibraryInstallDTO prepareComputationalLibInstallation(String username, LibInstallFormDTO formDTO,
                                                              LibraryInstallDTO dto) {

        for (LibInstallDTO lib : formDTO.getLibs()) {
            LibStatus status = libraryDAO.fetchLibraryStatus(username, formDTO.getNotebookName(),
                    formDTO.getComputationalName(),
                    lib.getGroup(), lib.getName(), lib.getVersion());

            prepare(status, lib, dto);

            libraryDAO.addLibrary(username, formDTO.getNotebookName(), formDTO.getComputationalName(), lib, LibStatus.FAILED == status);
        }

        return dto;

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
