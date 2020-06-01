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

package com.epam.dlab.backendapi.resources;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.service.BucketService;
import com.epam.dlab.dto.bucket.BucketDeleteDTO;
import com.epam.dlab.exceptions.DlabException;
import com.google.inject.Inject;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.fileupload.FileItemIterator;
import org.apache.commons.fileupload.FileItemStream;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.fileupload.util.Streams;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;

@Slf4j
@Path("/bucket")
public class BucketResource {
    private static final String OBJECT_FORM_FIELD = "object";
    private static final String BUCKET_FORM_FIELD = "bucket";
    private static final String SIZE_FORM_FIELD = "file-size";

    private final BucketService bucketService;

    @Inject
    public BucketResource(BucketService bucketService) {
        this.bucketService = bucketService;
    }

    @GET
    @Path("/{bucket}")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response getListOfObjects(@Auth UserInfo userInfo,
                                     @PathParam("bucket") String bucket) {
        return Response.ok(bucketService.getObjects(bucket)).build();
    }

    @POST
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadObject(@Auth UserInfo userInfo, @Context HttpServletRequest request) {
        upload(request);
        return Response.ok().build();
    }

    @GET
    @Path("/{bucket}/object/{object}/download")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_OCTET_STREAM)
    public Response downloadObject(@Auth UserInfo userInfo, @Context HttpServletResponse resp,
                                   @PathParam("object") String object,
                                   @PathParam("bucket") String bucket) {
        bucketService.downloadObject(bucket, object, resp);
        return Response.ok().build();
    }

    @POST
    @Path("/objects/delete")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response uploadObject(@Auth UserInfo userInfo, BucketDeleteDTO bucketDeleteDTO) {
        bucketService.deleteObjects(bucketDeleteDTO.getBucket(), bucketDeleteDTO.getObjects());
        return Response.ok().build();
    }

    private void upload(HttpServletRequest request) {
        String object = null;
        String bucket = null;
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
                        }
                        if (BUCKET_FORM_FIELD.equals(item.getFieldName())) {
                            bucket = Streams.asString(stream);
                        }
                        if (SIZE_FORM_FIELD.equals(item.getFieldName())) {
                            fileSize = Long.parseLong(Streams.asString(stream));
                        }
                    } else {
                        bucketService.uploadObject(bucket, object, stream, fileSize);
                    }
                } catch (Exception e) {
                    log.error("Cannot upload object {} to bucket {}. {}", object, bucket, e.getMessage(), e);
                    throw new DlabException(String.format("Cannot upload object %s to bucket %s. %s", object, bucket, e.getMessage()));
                }
            }
        } catch (Exception e) {
            log.error("Cannot upload object {} to bucket {}. {}", object, bucket, e.getMessage(), e);
            throw new DlabException(String.format("Cannot upload object %s to bucket %s. %s", object, bucket, e.getMessage()));
        }
    }
}
