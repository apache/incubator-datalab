/***************************************************************************

Copyright (c) 2016, EPAM SYSTEMS INC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

****************************************************************************/

package com.epam.dlab.backendapi.resources;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.domain.KeyUploader;
import com.epam.dlab.dto.keyload.KeyLoadStatus;
import com.epam.dlab.dto.keyload.UploadFileResultDTO;
import com.epam.dlab.exceptions.DlabException;
import com.google.inject.Inject;

import io.dropwizard.auth.Auth;

/** Provides the REST API for upload the user key.
 */
@Path("/user/access_key")
@Produces(MediaType.APPLICATION_JSON)
public class KeyUploaderResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(KeyUploaderResource.class);

    @Inject
    private KeyUploader keyUploader;

    /** Finds and returns the status of the user key.
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
    	LOGGER.debug("Check the status of the user key for {}", userInfo.getName());
    	KeyLoadStatus status;
    	try {
    		status = keyUploader.checkKey(userInfo);
        	LOGGER.debug("The status of the user key for {} is {}", userInfo.getName(), status.name());
    	} catch (DlabException e) {
    		LOGGER.error("Check the status of the user key for {} fails", userInfo.getName(), e);
    		status = KeyLoadStatus.ERROR;
    	}
		return Response.status(status.getHttpStatus()).build();
    }

    /** Uploads the user key to server. Stores the user key to database and calls the post method
     * of the provisioning service for the upload of the key to the provisioning service and creation
     * of the EDGE notebook for user.
     * @param userInfo user info.
     * @param uploadedInputStream content of the user key.
     * @param fileDetail
     * @return 200 OK
     * @throws IOException, DlabException
     */
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadKey(@Auth UserInfo userInfo,
                         @FormDataParam("file") InputStream uploadedInputStream,
                         @FormDataParam("file") FormDataContentDisposition fileDetail) throws IOException, DlabException {
        LOGGER.debug("Upload the key for user {}", userInfo.getName());
        String content;
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(uploadedInputStream))) {
            content = buffer.lines().collect(Collectors.joining("\n"));
            keyUploader.startKeyUpload(userInfo, content);
        } catch (IOException|DlabException e) {
    		LOGGER.error("Could not upload the key for user {}", userInfo.getName(), e);
    		throw new DlabException("Could not upload the key for user " + userInfo.getName() + ": " + e.getLocalizedMessage(), e);
    	}
        return Response.ok().build();
    }

    /** Stores the result of the upload the user key.
     * @param uploadKeyResult result of the upload the user key.
     * @return 200 OK
     */
    @POST
    @Path("/callback")
    public Response loadKeyResponse(@Auth UserInfo ui, UploadFileResultDTO uploadKeyResult) {
        LOGGER.debug("Upload the key result for user {}", uploadKeyResult.getUser(), uploadKeyResult.isSuccess());
        try {
        	keyUploader.onKeyUploadComplete(uploadKeyResult);
        } catch (DlabException e) {
    		LOGGER.error("Could not upload the key result for user {}", uploadKeyResult.getUser(), e);
    		throw new DlabException("Could not upload the key result for user " + uploadKeyResult.getUser() + ": " + e.getLocalizedMessage(), e);
    	}
        return Response.ok().build();
    }
}
