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

package com.epam.datalab.backendapi.service.impl.azure;

import com.azure.identity.ClientSecretCredentialBuilder;
import com.azure.storage.blob.BlobClient;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.models.BlobItem;
import com.epam.datalab.auth.UserInfo;
import com.epam.datalab.backendapi.ProvisioningServiceApplicationConfiguration;
import com.epam.datalab.backendapi.service.BucketService;
import com.epam.datalab.dto.bucket.BucketDTO;
import com.epam.datalab.exceptions.DatalabException;
import com.epam.datalab.model.azure.AzureAuthFile;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class BucketServiceAzureImpl implements BucketService {

    private final AzureAuthFile azureAuthFile;

    @Inject
    public BucketServiceAzureImpl(ProvisioningServiceApplicationConfiguration configuration) throws Exception {
        azureAuthFile = getAzureAuthFile(configuration);
    }

    @Override
    public List<BucketDTO> getObjects(String bucket) {
        try {
            AzureStorageAccount account = getAzureStorageAccount(bucket);
            BlobServiceClient blobServiceClient = getBlobServiceClient(account.getStorageAccount());
            BlobContainerClient blobContainerClient = blobServiceClient.getBlobContainerClient(account.getContainer());
            return blobContainerClient.listBlobs()
                    .stream()
                    .map(blob -> toBucketDTO(account.getContainer(), blob))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Cannot retrieve objects from bucket {}. Reason: {}", bucket, e.getMessage(), e);
            throw new DatalabException(String.format("Cannot retrieve objects from bucket %s. Reason: %s", bucket, e.getMessage()));
        }
    }

    @Override
    public void uploadObject(String bucket, String object, InputStream stream, String contentType, long fileSize) {
        log.info("Uploading file {} to bucket {}", object, bucket);
        try {
            AzureStorageAccount account = getAzureStorageAccount(bucket);
            BlobServiceClient blobServiceClient = getBlobServiceClient(account.getStorageAccount());
            BlobContainerClient blobContainerClient = blobServiceClient.getBlobContainerClient(account.getContainer());
            BlobClient blobClient = blobContainerClient.getBlobClient(object);
            blobClient.upload(stream, fileSize);
        } catch (Exception e) {
            log.error("Cannot upload object {} to bucket {}. Reason: {}", object, bucket, e.getMessage(), e);
            throw new DatalabException(String.format("Cannot upload object %s to bucket %s. Reason: %s", object, bucket, e.getMessage()));
        }
        log.info("Finished uploading file {} to bucket {}", object, bucket);
    }

    @Override
    public void uploadFolder(UserInfo userInfo, String bucket, String folder) {
        // Azure doesn't support this feature
    }

    @Override
    public void downloadObject(String bucket, String object, HttpServletResponse resp) {
        log.info("Downloading file {} from bucket {}", object, bucket);
        try (ServletOutputStream outputStream = resp.getOutputStream()) {
            AzureStorageAccount account = getAzureStorageAccount(bucket);
            BlobServiceClient blobServiceClient = getBlobServiceClient(account.getStorageAccount());
            BlobContainerClient blobContainerClient = blobServiceClient.getBlobContainerClient(account.getContainer());
            BlobClient blobClient = blobContainerClient.getBlobClient(object);
            blobClient.download(outputStream);
        } catch (Exception e) {
            log.error("Cannot download object {} from bucket {}. Reason: {}", object, bucket, e.getMessage(), e);
            throw new DatalabException(String.format("Cannot download object %s from bucket %s. Reason: %s", object, bucket, e.getMessage()));
        }
        log.info("Finished downloading file {} from bucket {}", object, bucket);
    }

    @Override
    public void deleteObjects(String bucket, List<String> objects) {
        try {
            AzureStorageAccount account = getAzureStorageAccount(bucket);
            BlobServiceClient blobServiceClient = getBlobServiceClient(account.getStorageAccount());
            BlobContainerClient blobContainerClient = blobServiceClient.getBlobContainerClient(account.getContainer());
            objects.forEach(object -> blobContainerClient.getBlobClient(object).delete());
        } catch (Exception e) {
            log.error("Cannot delete objects {} from bucket {}. Reason: {}", objects, bucket, e.getMessage(), e);
            throw new DatalabException(String.format("Cannot delete objects %s from bucket %s. Reason: %s", objects, bucket, e.getMessage()));
        }
    }

    private BucketDTO toBucketDTO(String bucket, BlobItem blob) {
        return BucketDTO.builder()
                .bucket(bucket)
                .object(blob.getName())
                .size(String.valueOf(blob.getProperties().getContentLength()))
                .lastModifiedDate(blob.getProperties().getLastModified().toEpochSecond()*1000)
                .build();
    }

    private AzureAuthFile getAzureAuthFile(ProvisioningServiceApplicationConfiguration configuration) throws Exception {
        final String authFile = configuration.getCloudConfiguration().getAzureAuthFile();
        Path path = Paths.get(authFile);
        if (path.toFile().exists()) {
            try {
                return new ObjectMapper().readValue(path.toFile(), AzureAuthFile.class);
            } catch (IOException e) {
                log.error("Cannot parse azure auth file {}", authFile, e);
                throw new IOException("Cannot parse azure auth file " + authFile);
            } catch (Exception e) {
                log.error("Something went wrong while parsing azure auth file {}", authFile, e);
                throw new Exception("Something went wrong while parsing azure auth file " + authFile);
            }
        } else {
            throw new FileNotFoundException("Cannot find azure auth file for path" + authFile);
        }
    }

    private BlobServiceClient getBlobServiceClient(String storageAccount) {
        final String endpoint = String.format("https://%s.blob.core.windows.net", storageAccount);;
        return new BlobServiceClientBuilder()
                .endpoint(endpoint)
                .credential(new ClientSecretCredentialBuilder()
                        .clientId(azureAuthFile.getClientId())
                        .clientSecret(azureAuthFile.getClientSecret())
                        .tenantId(azureAuthFile.getTenantId())
                        .build())
                .buildClient();
    }

    private AzureStorageAccount getAzureStorageAccount(String bucket) {
        String[] a = bucket.split("\\.");
        return new AzureStorageAccount(a[0], a[1]);
    }

    @Getter
    @AllArgsConstructor
    @ToString
    private static class AzureStorageAccount {
        private final String storageAccount;
        private final String container;
    }
}
