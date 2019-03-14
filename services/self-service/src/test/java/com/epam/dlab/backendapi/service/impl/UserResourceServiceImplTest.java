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


import com.epam.dlab.backendapi.service.ComputationalService;
import com.epam.dlab.backendapi.service.EdgeService;
import com.epam.dlab.backendapi.service.ExploratoryService;
import com.epam.dlab.dto.UserInstanceDTO;
import com.epam.dlab.dto.base.DataEngineType;
import com.epam.dlab.dto.computational.UserComputationalResource;
import com.epam.dlab.model.ResourceData;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.epam.dlab.dto.UserInstanceStatus.*;
import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class UserResourceServiceImplTest {

	private final String USER = "test";
	private final String EXPLORATORY_NAME = "explName";

	@Mock
	private ExploratoryService exploratoryService;
	@Mock
	private ComputationalService computationalService;
	@Mock
	private EdgeService edgeService;

	@InjectMocks
	private UserResourceServiceImpl userResourceService;

	@Test
	public void convertToResourceData() {
		List<UserInstanceDTO> userInstances = Collections.singletonList(getUserInstance());
		List<ResourceData> expectedResourceList = Arrays.asList(
				ResourceData.exploratoryResource("explId", EXPLORATORY_NAME),
				ResourceData.computationalResource("compId", EXPLORATORY_NAME, "compName")
		);
		List<ResourceData> actualResourceList = userResourceService.convertToResourceData(userInstances);
		assertEquals(2, actualResourceList.size());
		assertEquals(expectedResourceList.get(0).toString(), actualResourceList.get(0).toString());
		assertEquals(expectedResourceList.get(1).toString(), actualResourceList.get(1).toString());
	}

	@Test
	@SuppressWarnings("unchecked")
	public void updateReuploadKeyFlagForUserResources() {
		doNothing().when(exploratoryService).updateExploratoriesReuploadKeyFlag(anyString(), anyBoolean(), anyVararg
				());
		doNothing().when(computationalService).updateComputationalsReuploadKeyFlag(anyString(), any(List.class),
				any(List.class), anyBoolean(), anyVararg());
		doNothing().when(edgeService).updateReuploadKeyFlag(anyString(), anyBoolean(), anyVararg());

		userResourceService.updateReuploadKeyFlagForUserResources(USER, false);

		verify(exploratoryService).updateExploratoriesReuploadKeyFlag(USER, false,
				CREATING, CONFIGURING, STARTING, RUNNING, STOPPING, STOPPED);
		verify(computationalService).updateComputationalsReuploadKeyFlag(USER,
				Arrays.asList(STARTING, RUNNING, STOPPING, STOPPED),
				Collections.singletonList(DataEngineType.SPARK_STANDALONE),
				false,
				CREATING, CONFIGURING, STARTING, RUNNING, STOPPING, STOPPED);
		verify(computationalService).updateComputationalsReuploadKeyFlag(USER,
				Collections.singletonList(RUNNING),
				Collections.singletonList(DataEngineType.CLOUD_SERVICE),
				false,
				CREATING, CONFIGURING, STARTING, RUNNING);
		verify(edgeService).updateReuploadKeyFlag(USER, false, STARTING, RUNNING, STOPPING, STOPPED);
		verifyNoMoreInteractions(exploratoryService, computationalService, edgeService);
	}

	private UserInstanceDTO getUserInstance() {
		UserComputationalResource computationalResource = new UserComputationalResource();
		computationalResource.setComputationalId("compId");
		computationalResource.setComputationalName("compName");
		return new UserInstanceDTO()
				.withUser(USER)
				.withExploratoryId("explId")
				.withExploratoryName(EXPLORATORY_NAME)
				.withResources(Collections.singletonList(computationalResource));
	}
}
