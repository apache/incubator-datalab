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

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.client.rest.KeyLoaderAPI;
import com.epam.dlab.backendapi.dao.KeyDAO;
import com.epam.dlab.backendapi.dao.SettingsDAO;
import com.epam.dlab.client.restclient.RESTService;
import com.epam.dlab.dto.keyload.KeyLoadStatus;
import com.epam.dlab.dto.keyload.UploadFileDTO;
import com.epam.dlab.dto.keyload.UploadFileResultDTO;
import com.epam.dlab.exceptions.DlabException;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.dropwizard.auth.Auth;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import static com.epam.dlab.backendapi.SelfServiceApplicationConfiguration.PROVISIONING_SERVICE;

@Path("/user/access_key")
@Produces(MediaType.APPLICATION_JSON)
public class KeyUploaderResource implements KeyLoaderAPI {
    private static final Logger LOGGER = LoggerFactory.getLogger(KeyUploaderResource.class);

    @Inject
    private KeyDAO keyDAO;
    @Inject
    private SettingsDAO settingsDAO;
    @Inject
    @Named(PROVISIONING_SERVICE)
    private RESTService provisioningService;


    @GET
    public Response checkKey(@Auth UserInfo userInfo) {
        return Response.status(keyDAO.findKeyStatus(userInfo).getHttpStatus()).build();
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response post(@Auth UserInfo userInfo,
                         @FormDataParam("file") InputStream uploadedInputStream,
                         @FormDataParam("file") FormDataContentDisposition fileDetail) throws IOException {
        LOGGER.debug("upload key for user {}", userInfo.getName());
        String content = "";
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(uploadedInputStream))) {
            content = buffer.lines().collect(Collectors.joining("\n"));
        }
        keyDAO.uploadKey(userInfo.getName(), content);
        try {
            UploadFileDTO dto = new UploadFileDTO()
                    .withUser(userInfo.getName())
                    .withContent(content)
                    .withServiceBaseName(settingsDAO.getServiceBaseName())
                    .withSecurityGroup(settingsDAO.getSecurityGroups());
            Response response = provisioningService.post(KEY_LOADER, dto, Response.class);
            if (Response.Status.ACCEPTED.getStatusCode() != response.getStatus()) {
                keyDAO.deleteKey(userInfo.getName());
            }
        } catch (Exception e) {
            keyDAO.deleteKey(userInfo.getName());
            throw new DlabException("Could not upload the key", e);
        }

        return Response.ok().build();
    }

    @POST
    @Path("/callback")
    public Response loadKeyResponse(UploadFileResultDTO result) {
        LOGGER.debug("upload key result for user {}", result.getUser(), result.isSuccess());
        keyDAO.updateKey(result.getUser(), KeyLoadStatus.getStatus(result.isSuccess()));
        if (result.isSuccess()) {
            keyDAO.saveCredential(result.getUser(), result.getCredential());
        } else {
            keyDAO.deleteKey(result.getUser());
        }
        return Response.ok().build();
    }
}
