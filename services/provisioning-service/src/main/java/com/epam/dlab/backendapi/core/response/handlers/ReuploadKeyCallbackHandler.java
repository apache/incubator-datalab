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

package com.epam.dlab.backendapi.core.response.handlers;

import com.epam.dlab.backendapi.core.FileHandlerCallback;
import com.epam.dlab.backendapi.core.commands.DockerAction;
import com.epam.dlab.rest.client.RESTService;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ReuploadKeyCallbackHandler implements FileHandlerCallback {

	private static final ObjectMapper MAPPER = new ObjectMapper()
			.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true);
	private final RESTService selfService;
	private final String callbackUrl;
	private final String user;
	private final String uuid;
	private DockerAction action;

	public ReuploadKeyCallbackHandler(RESTService selfService, String callbackUrl, String user, String uuid,
									  DockerAction action) {
		this.selfService = selfService;
		this.callbackUrl = callbackUrl;
		this.user = user;
		this.uuid = uuid;
		this.action = action;
	}

	@Override
	public String getUUID() {
		return uuid;
	}

	@Override
	public boolean checkUUID(String uuid) {
		return this.uuid.equals(uuid);
	}

	@Override
	public boolean handle(String fileName, byte[] content) throws Exception {
		return false;
	}

	@Override
	public void handleError(String errorMessage) {

	}
}
