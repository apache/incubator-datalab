/*
 * Copyright (c) 2018, EPAM SYSTEMS INC
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

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.resources.dto.LibInfoRecord;
import com.epam.dlab.backendapi.resources.dto.LibInstallFormDTO;
import com.epam.dlab.dto.exploratory.LibraryInstallDTO;
import org.bson.Document;

import java.util.List;

public interface LibraryService {
	List<Document> getLibs(String user, String exploratoryName, String computationalName);

	List<LibInfoRecord> getLibInfo(String user, String exploratoryName);

	LibraryInstallDTO generateLibraryInstallDTO(UserInfo userInfo, LibInstallFormDTO formDTO);

	LibraryInstallDTO prepareExploratoryLibInstallation(String username, LibInstallFormDTO formDTO,
														LibraryInstallDTO dto);

	LibraryInstallDTO prepareComputationalLibInstallation(String username, LibInstallFormDTO formDTO,
														  LibraryInstallDTO dto);
}
