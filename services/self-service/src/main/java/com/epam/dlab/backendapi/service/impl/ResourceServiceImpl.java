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


package com.epam.dlab.backendapi.service.impl;

import com.epam.dlab.backendapi.service.ComputationalService;
import com.epam.dlab.backendapi.service.EdgeService;
import com.epam.dlab.backendapi.service.ExploratoryService;
import com.epam.dlab.backendapi.service.ResourceService;
import com.epam.dlab.dto.UserInstanceDTO;
import com.epam.dlab.dto.base.DataEngineType;
import com.epam.dlab.model.ResourceData;
import com.epam.dlab.model.ResourceType;
import com.google.inject.Inject;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.epam.dlab.UserInstanceStatus.*;

public class ResourceServiceImpl implements ResourceService {

	@Inject
	private ExploratoryService exploratoryService;
	@Inject
	private ComputationalService computationalService;
	@Inject
	private EdgeService edgeService;

	/**
	 * Converts user's instances to another data type.
	 *
	 * @param userInstances list of user instances.
	 * @return converted list of resources' data.
	 */
	@Override
	public List<ResourceData> convertToResourceData(List<UserInstanceDTO> userInstances) {
		List<ResourceData> resources = new ArrayList<>();
		userInstances.forEach(ui -> {
			resources.add(
					new ResourceData(ResourceType.EXPLORATORY, ui.getExploratoryId(),
							ui.getExploratoryName(), null));
			ui.getResources().forEach(cr -> resources.add(
					new ResourceData(ResourceType.COMPUTATIONAL, cr.getComputationalId(),
							ui.getExploratoryName(), cr.getComputationalName())));
		});
		return resources;
	}

	/**
	 * Updates flag 'reuploadKeyRequired' for user's resources with predefined statuses.
	 *
	 * @param user                user's name.
	 * @param reuploadKeyRequired true/false.
	 */
	@Override
	public void updateReuploadKeyFlagForUserResources(String user, boolean reuploadKeyRequired) {
		exploratoryService.updateExploratoriesReuploadKeyFlag(user, reuploadKeyRequired,
				CREATING, CONFIGURING, STARTING, RUNNING, STOPPING, STOPPED);
		computationalService.updateComputationalsReuploadKeyFlag(user,
				Arrays.asList(STARTING, RUNNING, STOPPING, STOPPED),
				Collections.singletonList(DataEngineType.SPARK_STANDALONE),
				reuploadKeyRequired,
				CREATING, CONFIGURING, STARTING, RUNNING, STOPPING, STOPPED);
		computationalService.updateComputationalsReuploadKeyFlag(user,
				Collections.singletonList(RUNNING),
				Collections.singletonList(DataEngineType.CLOUD_SERVICE),
				reuploadKeyRequired,
				CREATING, CONFIGURING, STARTING, RUNNING);
		edgeService.updateReuploadKeyFlag(user, reuploadKeyRequired, STARTING, RUNNING, STOPPING, STOPPED);
	}
}
