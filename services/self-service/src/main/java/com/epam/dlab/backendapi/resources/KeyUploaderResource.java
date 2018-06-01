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
import com.epam.dlab.backendapi.service.AccessKeyService;
import com.epam.dlab.dto.keyload.KeyLoadStatus;
import com.epam.dlab.exceptions.DlabValidationException;
import com.epam.dlab.rest.contracts.EdgeAPI;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.ws.rs.*;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * Provides the REST API for upload the user key.
 */
@Path("/user/access_key")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class KeyUploaderResource implements EdgeAPI {

	private static final String FILE_ATTACHMENT_FORMAT = "attachment; filename=\"%s.pem\"";
	private AccessKeyService keyService;

	@Inject
	public KeyUploaderResource(AccessKeyService keyService) {
		this.keyService = keyService;
	}

	/**
	 * Finds and returns the status of the user key.
	 *
	 * @param userInfo user info.
	 * @return <pre>
	 * {@link Status#NOT_FOUND} the user key has been not found.
	 * {@link Status#ACCEPTED} the user key is uploading now.
	 * {@link Status#OK} the user key is valid.
	 * {@link Status#INTERNAL_SERVER_ERROR} the check of the status is failed.
	 * </pre>
	 */
	@GET
	public Response checkKey(@Auth UserInfo userInfo) {
		final KeyLoadStatus status = keyService.getUserKeyStatus(userInfo.getName());
		return Response.status(status.getHttpStatus()).build();
	}

	/**
	 * Uploads/reuploads the user key to server. If param 'isPrimaryUploading' equals 'true', then it stores
	 * the user key to the database and calls the post method of the provisioning service for the key uploading
	 * and edge creating for user. Else if this param equals 'false', then only replacing keys in the database
	 * will be performed (user's key will be reuploaded).
	 *
	 * @param userInfo            user info.
	 * @param fileContent content of the user key.
	 * @param isPrimaryUploading  true if key is being primarily uploaded, false - in case of reuploading
	 *
	 * @return 200 Ok
	 */
	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public Response loadKey(@Auth UserInfo userInfo,
							@FormDataParam("file") String fileContent,
							@DefaultValue("true") @QueryParam("is_primary_uploading") boolean isPrimaryUploading) {

		validate(fileContent);
		keyService.uploadKey(userInfo, fileContent, isPrimaryUploading);
		return Response.ok().build();
	}

	/**
	 * Creates the EDGE node and upload the user key  for user.
	 *
	 * @param userInfo user info.
	 * @return {@link Response.Status#OK} request for provisioning service has been accepted.<br>
	 */
	@POST
	@Path("/recover")
	public Response recover(@Auth UserInfo userInfo) {
		return Response.ok(keyService.recoverEdge(userInfo)).build();
	}


	@POST
	@Path("/generate")
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	public Response generate(@Auth UserInfo userInfo) {
		final Response.ResponseBuilder builder = Response.ok(keyService.generateKey(userInfo));
		builder.header(HttpHeaders.CONTENT_DISPOSITION, String.format(FILE_ATTACHMENT_FORMAT, userInfo.getName()));
		return builder.build();
	}

	private void validate(String publicKey) {
		if (!publicKey.startsWith("ssh-")) {
			log.error("Wrong key format. Key should be in openSSH format");
			log.trace("Key content:\n{}", publicKey);
			throw new DlabValidationException("Wrong key format. Key should be in openSSH format");
		}
	}
}
