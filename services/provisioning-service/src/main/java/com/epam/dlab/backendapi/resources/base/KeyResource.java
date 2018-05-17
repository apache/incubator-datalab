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


package com.epam.dlab.backendapi.resources.base;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.ProvisioningServiceApplicationConfiguration;
import com.epam.dlab.backendapi.core.commands.DockerAction;
import com.epam.dlab.backendapi.service.ReuploadKeyService;
import com.epam.dlab.dto.reuploadkey.ReuploadKeyDTO;
import com.epam.dlab.rest.contracts.KeyAPI;
import com.epam.dlab.utils.FileUtils;
import com.epam.dlab.utils.UsernameUtils;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;

/**
 * Provides API for reuploading keys
 */
@Path(KeyAPI.REUPLOAD_KEY)
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class KeyResource {

	@Inject
	private ReuploadKeyService reuploadKeyService;
	@Inject
	private ProvisioningServiceApplicationConfiguration configuration;

	@POST
	public void reuploadKey(@Auth UserInfo ui, ReuploadKeyDTO dto) throws IOException {
		String edgeUserName = dto.getEdgeUserName();
		String filename = UsernameUtils.replaceWhitespaces(edgeUserName) + KeyAPI.KEY_EXTENTION;
		FileUtils.deleteFile(filename, configuration.getKeyDirectory());
		FileUtils.saveToFile(filename, configuration.getKeyDirectory(), dto.getContent());
		reuploadKeyService.reuploadKeyAction(ui.getName(), dto, DockerAction.REUPLOAD_KEY);
	}

}
