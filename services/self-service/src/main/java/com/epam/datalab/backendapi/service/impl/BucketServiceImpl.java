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

package com.epam.datalab.backendapi.service.impl;

import com.epam.datalab.auth.UserInfo;
import com.epam.datalab.backendapi.annotation.Audit;
import com.epam.datalab.backendapi.annotation.Info;
import com.epam.datalab.backendapi.annotation.ResourceName;
import com.epam.datalab.backendapi.annotation.User;
import com.epam.datalab.backendapi.domain.EndpointDTO;
import com.epam.datalab.backendapi.service.BucketService;
import com.epam.datalab.backendapi.service.EndpointService;
import com.epam.datalab.constants.ServiceConsts;
import com.epam.datalab.dto.bucket.BucketDTO;
import com.epam.datalab.dto.bucket.BucketDeleteDTO;
import com.epam.datalab.dto.bucket.FolderUploadDTO;
import com.epam.datalab.exceptions.DatalabException;
import com.epam.datalab.rest.client.RESTService;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpStatus;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.multipart.file.StreamDataBodyPart;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.epam.datalab.backendapi.domain.AuditActionEnum.DELETE;
import static com.epam.datalab.backendapi.domain.AuditActionEnum.DOWNLOAD;
import static com.epam.datalab.backendapi.domain.AuditActionEnum.UPLOAD;
import static com.epam.datalab.backendapi.domain.AuditResourceTypeEnum.BUCKET;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM;

@Slf4j
public class BucketServiceImpl implements BucketService {
    private static final String BUCKET_GET_OBJECTS = "%sbucket/%s";
    private static final String BUCKET_UPLOAD_OBJECT = "%sbucket/upload";
    private static final String BUCKET_UPLOAD_FOLDER = "%sbucket/folder/upload";
    private static final String BUCKET_DOWNLOAD_OBJECT = "%sbucket/%s/object/%s/download";
    private static final String BUCKET_DELETE_OBJECT = "%sbucket/objects/delete";
    private static final String SOMETHING_WENT_WRONG_MESSAGE = "Something went wrong. Response status is %s ";

    private final EndpointService endpointService;
    private final RESTService provisioningService;

    @Inject
    public BucketServiceImpl(EndpointService endpointService, @Named(ServiceConsts.BUCKET_SERVICE_NAME) RESTService provisioningService) {
        this.endpointService = endpointService;
        this.provisioningService = provisioningService;
    }

    @Override
    public List<BucketDTO> getObjects(@User UserInfo userInfo, String bucket, String endpoint) {
        try {
            log.info("Trying to get list of the objects: {}, for user: {}", bucket, userInfo);
            EndpointDTO endpointDTO = endpointService.get(endpoint);
            return provisioningService.get(String.format(BUCKET_GET_OBJECTS, endpointDTO.getUrl(), bucket), userInfo.getAccessToken(), new GenericType<List<BucketDTO>>() {
            });
        } catch (Exception e) {
            log.error("Cannot get objects from bucket {} for user {}, endpoint {}. Reason {}", bucket, userInfo.getName(), endpoint, e.getMessage(), e);
            throw new DatalabException(String.format("Cannot get objects from bucket %s for user %s, endpoint %s. Reason %s", bucket, userInfo.getName(), endpoint, e.getMessage()));
        }
    }

    @Audit(action = UPLOAD, type = BUCKET)
    @Override
    public void uploadObject(@User UserInfo userInfo, @ResourceName String bucket, String object, String endpoint, InputStream inputStream, String contentType, long fileSize, @Info String auditInfo) {
        log.info("Uploading file {} for user {} to bucket {}", object, userInfo.getName(), bucket);
        try {
            EndpointDTO endpointDTO = endpointService.get(endpoint);
            FormDataMultiPart formData = getFormDataMultiPart(bucket, object, inputStream, contentType, fileSize);
            Response response = provisioningService.postForm(String.format(BUCKET_UPLOAD_OBJECT, endpointDTO.getUrl()), userInfo.getAccessToken(), formData, Response.class);
            if (response.getStatus() != HttpStatus.SC_OK) {
                throw new DatalabException(String.format(SOMETHING_WENT_WRONG_MESSAGE, response.getStatus()));
            }
        } catch (Exception e) {
            log.error("Cannot upload object {} to bucket {} for user {}, endpoint {}. Reason {}", object, bucket, userInfo.getName(), endpoint, e.getMessage(), e);
            throw new DatalabException(String.format("Cannot upload object %s to bucket %s for user %s, endpoint %s. Reason %s", object, bucket, userInfo.getName(), endpoint, e.getMessage()));
        }
        log.info("Finished uploading file {} for user {} to bucket {}", object, userInfo.getName(), bucket);
    }

    @Audit(action = UPLOAD, type = BUCKET)
    @Override
    public void uploadFolder(@User UserInfo userInfo, @ResourceName String bucket, String folder, String endpoint, @Info String auditInfo) {
        log.info("Uploading folder {} for user {} to bucket {}", folder, userInfo.getName(), bucket);
        try {
            if (!folder.endsWith("/")) {
                throw new DatalabException("Folder doesn't end with '/'");
            }
            EndpointDTO endpointDTO = endpointService.get(endpoint);
            FolderUploadDTO dto = new FolderUploadDTO(bucket, folder);
            Response response = provisioningService.post(String.format(BUCKET_UPLOAD_FOLDER, endpointDTO.getUrl()), userInfo.getAccessToken(), dto, Response.class);
            if (response.getStatus() != HttpStatus.SC_OK) {
                throw new DatalabException(String.format(SOMETHING_WENT_WRONG_MESSAGE, response.getStatus()));
            }
        } catch (Exception e) {
            log.error("Cannot upload folder {} to bucket {} for user {}, endpoint {}. Reason {}", folder, bucket, userInfo.getName(), endpoint, e.getMessage(), e);
            throw new DatalabException(String.format("Cannot upload object %s to bucket %s for user %s, endpoint %s. Reason %s", folder, bucket, userInfo.getName(), endpoint, e.getMessage()));
        }
        log.info("Finished uploading folder {} for user {} to bucket {}", folder, userInfo.getName(), bucket);
    }

    @Audit(action = DOWNLOAD, type = BUCKET)
    @Override
    public void downloadObject(@User UserInfo userInfo, @ResourceName String bucket, String object, String endpoint, HttpServletResponse resp, @Info String auditInfo) {
        log.info("Downloading file {} for user {} from bucket {}", object, userInfo.getName(), bucket);
        EndpointDTO endpointDTO = endpointService.get(endpoint);
        try (InputStream inputStream = provisioningService.getWithMediaTypes(String.format(BUCKET_DOWNLOAD_OBJECT, endpointDTO.getUrl(), bucket, encodeObject(object)), userInfo.getAccessToken(),
                InputStream.class, APPLICATION_JSON, APPLICATION_OCTET_STREAM); ServletOutputStream outputStream = resp.getOutputStream()) {
            IOUtils.copyLarge(inputStream, outputStream);
            log.info("Finished downloading file {} for user {} from bucket {}", object, userInfo.getName(), bucket);
        } catch (Exception e) {
            log.error("Cannot upload object {} from bucket {} for user {}, endpoint {}. Reason {}", object, bucket, userInfo.getName(), endpoint, e.getMessage(), e);
            throw new DatalabException(String.format("Cannot download object %s from bucket %s for user %s, endpoint %s. Reason %s", object, bucket, userInfo.getName(), endpoint, e.getMessage()));
        }
    }

    @Audit(action = DELETE, type = BUCKET)
    @Override
    public void deleteObjects(@User UserInfo userInfo, @ResourceName String bucket, List<String> objects, String endpoint, @Info String auditInfo) {
        try {
            EndpointDTO endpointDTO = endpointService.get(endpoint);
            BucketDeleteDTO bucketDeleteDTO = new BucketDeleteDTO(bucket, objects);
            Response response = provisioningService.post(String.format(BUCKET_DELETE_OBJECT, endpointDTO.getUrl()), userInfo.getAccessToken(), bucketDeleteDTO, Response.class);
            if (response.getStatus() != HttpStatus.SC_OK) {
                throw new DatalabException(String.format(SOMETHING_WENT_WRONG_MESSAGE, response.getStatus()));
            }
        } catch (Exception e) {
            log.error("Cannot delete objects {} from bucket {} for user {}, endpoint {}. Reason {}", objects, bucket, userInfo.getName(), endpoint, e.getMessage(), e);
            throw new DatalabException(String.format("Cannot delete objects %s from bucket %s for user %s, endpoint %s. Reason %s", objects, bucket, userInfo.getName(), endpoint, e.getMessage()));
        }
    }

    private String encodeObject(String object) throws UnsupportedEncodingException {
        return URLEncoder.encode(object, StandardCharsets.UTF_8.toString()).replace("+", "%20");
    }

    private FormDataMultiPart getFormDataMultiPart(String bucket, String object, InputStream inputStream, String contentType, long fileSize) {
        StreamDataBodyPart filePart = new StreamDataBodyPart("file", inputStream, object, MediaType.valueOf(contentType));
        FormDataMultiPart formData = new FormDataMultiPart();
        formData.field("bucket", bucket);
        formData.field("object", object);
        formData.field("file-size", String.valueOf(fileSize));
        formData.bodyPart(filePart);
        return formData;
    }
}
