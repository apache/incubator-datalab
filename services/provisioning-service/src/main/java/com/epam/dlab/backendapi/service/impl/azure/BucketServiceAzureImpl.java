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

package com.epam.dlab.backendapi.service.impl.azure;

import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import com.epam.dlab.backendapi.service.BucketService;
import com.epam.dlab.dto.bucket.BucketDTO;
import com.epam.dlab.exceptions.DlabException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class BucketServiceAzureImpl implements BucketService {
    @Override
    public List<BucketDTO> getObjects(String bucket) {
        try {
            BlobServiceClient blobServiceClient = new BlobServiceClientBuilder().connectionString(System.getenv("AZURE_STORAGE_CONNECTION_STRING")).buildClient();
            BlobContainerClient blobContainerClient = blobServiceClient.getBlobContainerClient(bucket);
            return blobContainerClient.listBlobs()
                    .stream()
                    .map(blob -> toBucketDTO(bucket, blob))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Cannot retrieve objects from bucket {}. Reason: {}", bucket, e.getMessage());
            throw new DlabException(String.format("Cannot retrieve objects from bucket %s. Reason: %s", bucket, e.getMessage()));
        }
    }

    @Override
    public void uploadObject(String bucket, String object, InputStream stream, long fileSize) {
        try {
            BlobServiceClient blobServiceClient = new BlobServiceClientBuilder().connectionString(System.getenv("AZURE_STORAGE_CONNECTION_STRING")).buildClient();
            BlobContainerClient blobContainerClient = blobServiceClient.getBlobContainerClient(bucket);
            BlobClient blobClient = blobContainerClient.getBlobClient(object);
            blobClient.upload(stream, fileSize);
        } catch (Exception e) {
            log.error("Cannot upload object {} to bucket {}. Reason: {}", object, bucket, e.getMessage());
            throw new DlabException(String.format("Cannot upload object %s to bucket %s. Reason: %s", object, bucket, e.getMessage()));
        }
    }

    @Override
    public void downloadObject(String bucket, String object, HttpServletResponse resp) {
        try (ServletOutputStream outputStream = resp.getOutputStream()) {
            BlobServiceClient blobServiceClient = new BlobServiceClientBuilder().connectionString(System.getenv("AZURE_STORAGE_CONNECTION_STRING")).buildClient();
            BlobContainerClient blobContainerClient = blobServiceClient.getBlobContainerClient(bucket);
            BlobClient blobClient = blobContainerClient.getBlobClient(object);
            blobClient.download(outputStream);
        } catch (Exception e) {
            log.error("Cannot download object {} from bucket {}. Reason: {}", object, bucket, e.getMessage());
            throw new DlabException(String.format("Cannot download object %s from bucket %s. Reason: %s", object, bucket, e.getMessage()));
        }
    }

    @Override
    public void deleteObjects(String bucket, List<String> objects) {
        try {
            BlobServiceClient blobServiceClient = new BlobServiceClientBuilder().connectionString(System.getenv("AZURE_STORAGE_CONNECTION_STRING")).buildClient();
            BlobContainerClient blobContainerClient = blobServiceClient.getBlobContainerClient(bucket);
            objects.forEach(object -> blobContainerClient.getBlobClient(object).delete());
        } catch (Exception e) {
            log.error("Cannot delete objects {} from bucket {}. Reason: {}", objects, bucket, e.getMessage());
            throw new DlabException(String.format("Cannot delete objects %s from bucket %s. Reason: %s", objects, bucket, e.getMessage()));
        }
    }

    private BucketDTO toBucketDTO(String bucket, BlobItem blob) {
        final String size = FileUtils.byteCountToDisplaySize(blob.getProperties().getContentLength());
        String lastModifiedDate = blob.getProperties().getLastModified().format(DateTimeFormatter.ofPattern(DATE_FORMAT));
        return BucketDTO.builder()
                .bucket(bucket)
                .object(blob.getName())
                .lastModifiedDate(lastModifiedDate)
                .size(size)
                .build();
    }
}
