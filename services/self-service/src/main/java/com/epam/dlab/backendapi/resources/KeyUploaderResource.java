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
import com.epam.dlab.backendapi.dao.KeyDAO;
import com.epam.dlab.backendapi.dao.SettingsDAO;
import com.epam.dlab.backendapi.domain.KeyUploader;
import com.epam.dlab.dto.edge.EdgeCreateDTO;
import com.epam.dlab.dto.keyload.KeyLoadStatus;
import com.epam.dlab.dto.keyload.UploadFileDTO;
import com.epam.dlab.dto.keyload.UploadFileResultDTO;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.client.RESTService;
import com.epam.dlab.rest.contracts.KeyLoaderAPI;
import com.epam.dlab.utils.UsernameUtils;
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

@Path("/user/access_key")
@Produces(MediaType.APPLICATION_JSON)
public class KeyUploaderResource {
    private static final Logger LOGGER = LoggerFactory.getLogger(KeyUploaderResource.class);

    @Inject
    private KeyUploader keyUploader;

    @GET
    public Response checkKey(@Auth UserInfo userInfo) {
        return Response.status(keyUploader.checkKey(userInfo).getHttpStatus()).build();
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response post(@Auth UserInfo userInfo,
                         @FormDataParam("file") InputStream uploadedInputStream,
                         @FormDataParam("file") FormDataContentDisposition fileDetail) throws IOException {
        LOGGER.debug("upload key for user {}", userInfo.getName());
        String content;
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(uploadedInputStream))) {
            content = buffer.lines().collect(Collectors.joining("\n"));
        }
        keyUploader.startKeyUpload(userInfo, content);
        return Response.ok().build();
    }

    @POST
    @Path("/callback")
    public Response loadKeyResponse(UploadFileResultDTO result) {
        LOGGER.debug("upload key result for user {}", result.getUser(), result.isSuccess());
        keyUploader.onKeyUploadComplete(result);
        return Response.ok().build();
    }
}
