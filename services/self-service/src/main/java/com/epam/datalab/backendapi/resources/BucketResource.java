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

package com.epam.datalab.backendapi.resources;

import com.epam.datalab.auth.UserInfo;
import com.epam.datalab.backendapi.resources.dto.BucketDeleteDTO;
import com.epam.datalab.backendapi.resources.dto.FolderUploadDTO;
import com.epam.datalab.backendapi.service.BucketService;
import com.epam.datalab.exceptions.DatalabException;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;

import javax.annotation.security.RolesAllowed;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.nio.file.Paths;

@Path("/bucket")
@Slf4j
public class BucketResource {
    private static final String AUDIT_FOLDER_UPLOAD_MESSAGE = "Upload folder: %s";
    private static final String AUDIT_FILE_UPLOAD_MESSAGE = "Upload file: %s";
    private static final String AUDIT_FILE_DOWNLOAD_MESSAGE = "Download file: %s";
    private static final String AUDIT_FILE_DELETE_MESSAGE = "Delete file: %s";
    private static final String OBJECT_FORM_FIELD = "object";
    private static final String BUCKET_FORM_FIELD = "bucket";
    private static final String ENDPOINT_FORM_FIELD = "endpoint";
    private static final String SIZE_FORM_FIELD = "size";

    private final BucketService bucketService;

    @Inject
    public BucketResource(BucketService bucketService) {
        this.bucketService = bucketService;
    }

    @GET
    @Path("/{bucket}/endpoint/{endpoint}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("/api/bucket/view")
    public Response getListOfObjects(@Auth UserInfo userInfo,
                                     @PathParam("bucket") String bucket,
                                     @PathParam("endpoint") String endpoint) {
        log.info("Trying to get list of the objects: {}, for user: {}", bucket, userInfo);
        return Response.ok(bucketService.getObjects(userInfo, bucket, endpoint)).build();
    }

    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("/api/bucket/upload")
    public Response uploadObject(@Auth UserInfo userInfo, @Context HttpServletRequest request) {
        upload(userInfo, request);
        return Response.ok().build();
    }

    @POST
    @Path("/folder/upload")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("/api/bucket/upload")
    public Response uploadFolder(@Auth UserInfo userInfo, @Valid FolderUploadDTO dto) {
        bucketService.uploadFolder(userInfo, dto.getBucket(), dto.getFolder(), dto.getEndpoint(), String.format(AUDIT_FOLDER_UPLOAD_MESSAGE, dto.getFolder()));
        return Response.ok().build();
    }

    @GET
    @Path("/{bucket}/object/{object}/endpoint/{endpoint}/download")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    @RolesAllowed("/api/bucket/download")
    public Response downloadObject(@Auth UserInfo userInfo, @Context HttpServletResponse resp,
                                   @PathParam("bucket") String bucket,
                                   @PathParam("object") String object,
                                   @PathParam("endpoint") String endpoint) {
        bucketService.downloadObject(userInfo, bucket, object, endpoint, resp, String.format(AUDIT_FILE_DOWNLOAD_MESSAGE, object));
        return Response.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + Paths.get(object).getFileName() + "\"")
                .build();
    }

    @POST
    @Path("/objects/delete")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @RolesAllowed("/api/bucket/delete")
    public Response deleteObject(@Auth UserInfo userInfo, @Valid BucketDeleteDTO bucketDto) {
        final String listOfDeletedObject = String.join(", ", bucketDto.getObjects());
        bucketService.deleteObjects(userInfo, bucketDto.getBucket(), bucketDto.getObjects(), bucketDto.getEndpoint(), String.format(AUDIT_FILE_DELETE_MESSAGE, listOfDeletedObject));
        return Response.ok().build();
    }

    private void upload(UserInfo userInfo, HttpServletRequest request) {
        String object = null;
        String bucket = null;
        String endpoint = null;
        long fileSize = 0;

        ServletFileUpload upload = new ServletFileUpload();
        try {
            FileItemIterator iterStream = upload.getItemIterator(request);
            while (iterStream.hasNext()) {
                FileItemStream item = iterStream.next();
                try (InputStream stream = item.openStream()) {
                    if (item.isFormField()) {
                        if (OBJECT_FORM_FIELD.equals(item.getFieldName())) {
                            object = Streams.asString(stream);
                        } else if (BUCKET_FORM_FIELD.equals(item.getFieldName())) {
                            bucket = Streams.asString(stream);
                        } else if (ENDPOINT_FORM_FIELD.equals(item.getFieldName())) {
                            endpoint = Streams.asString(stream);
                        } else if (SIZE_FORM_FIELD.equals(item.getFieldName())) {
                            fileSize = Long.parseLong(Streams.asString(stream));
                        }
                    } else {
                        bucketService.uploadObject(userInfo, bucket, object, endpoint, stream, item.getContentType(), fileSize, String.format(AUDIT_FILE_UPLOAD_MESSAGE, object));
                    }
                } catch (Exception e) {
                    log.error("Cannot upload object {} to bucket {}. {}", object, bucket, e.getMessage(), e);
                    throw new DatalabException(String.format("Cannot upload object %s to bucket %s. %s", object, bucket, e.getMessage()));
                }
            }
        } catch (Exception e) {
            log.error("User {} cannot upload object {} to bucket {}. {}", userInfo.getName(), object, bucket, e.getMessage(), e);
            throw new DatalabException(String.format("User %s cannot upload object %s to bucket %s. %s", userInfo.getName(), object, bucket, e.getMessage()));
        }
    }
}
