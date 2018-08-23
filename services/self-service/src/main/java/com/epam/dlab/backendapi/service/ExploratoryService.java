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
import com.epam.dlab.dto.UserInstanceDTO;
import com.epam.dlab.dto.UserInstanceStatus;
import com.epam.dlab.model.exploratory.Exploratory;

import java.util.List;
import java.util.Optional;

public interface ExploratoryService {

	String start(UserInfo userInfo, String exploratoryName);

	String stop(UserInfo userInfo, String exploratoryName);

	String terminate(UserInfo userInfo, String exploratoryName);

	String create(UserInfo userInfo, Exploratory exploratory);

	void updateExploratoryStatuses(String user, UserInstanceStatus status);

	void updateExploratoriesReuploadKeyFlag(String user, boolean reuploadKeyRequired,
											UserInstanceStatus... exploratoryStatuses);

	List<UserInstanceDTO> getInstancesWithStatuses(String user, UserInstanceStatus exploratoryStatus,
												   UserInstanceStatus computationalStatus);

	Optional<UserInstanceDTO> getUserInstance(String user, String exploratoryName);

	List<UserInstanceDTO> getInstancesByComputationalIds(List<String> computationalIds);
}
