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

import com.epam.dlab.backendapi.ProvisioningServiceApplicationConfiguration;
import com.epam.dlab.backendapi.core.commands.CommandBuilder;
import com.epam.dlab.backendapi.core.commands.ICommandExecutor;
import com.epam.dlab.backendapi.core.response.folderlistener.FolderListenerExecutor;
import com.epam.dlab.rest.client.RESTService;
import com.google.inject.Inject;

public abstract class DockerService {

	@Inject
	protected ProvisioningServiceApplicationConfiguration configuration;
	@Inject
	protected FolderListenerExecutor folderListenerExecutor;
	@Inject
	protected ICommandExecutor commandExecutor;
	@Inject
	protected CommandBuilder commandBuilder;
	@Inject
	protected RESTService selfService;

}
