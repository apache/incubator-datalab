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

package com.epam.datalab.backendapi.service.impl.gcp;

import com.epam.datalab.auth.UserInfo;
import com.epam.datalab.backendapi.service.BucketService;
import com.epam.datalab.dto.bucket.BucketDTO;
import com.epam.datalab.exceptions.DatalabException;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Slf4j
public class BucketServiceGcpImpl implements BucketService {

    @Override
    public List<BucketDTO> getObjects(String bucket) {
        try {
            Storage storage = StorageOptions.getDefaultInstance().getService();
            Bucket gcpBucket = storage.get(bucket);
            return StreamSupport.stream(gcpBucket.list().getValues().spliterator(), false)
                    .map(this::toBucketDTO)
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
            Storage storage = StorageOptions.getDefaultInstance().getService();
            BlobId blobId = BlobId.of(bucket, object);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                    .setContentType(contentType)
                    .build();
            storage.create(blobInfo, stream);
        } catch (Exception e) {
            log.error("Cannot upload object {} to bucket {}. Reason: {}", object, bucket, e.getMessage(), e);
            throw new DatalabException(String.format("Cannot upload object %s to bucket %s. Reason: %s", object, bucket, e.getMessage()));
        }
        log.info("Finished uploading file {} to bucket {}", object, bucket);
    }

    @Override
    public void uploadFolder(UserInfo userInfo, String bucket, String folder) {
        log.info("Uploading file {} to bucket {}", folder, bucket);
        try {
            Storage storage = StorageOptions.getDefaultInstance().getService();
            BlobId blobId = BlobId.of(bucket, folder);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
            storage.create(blobInfo);
        } catch (Exception e) {
            log.error("Cannot upload folder {} to bucket {}. Reason: {}", folder, bucket, e.getMessage(), e);
            throw new DatalabException(String.format("Cannot upload folder %s to bucket %s. Reason: %s", folder, bucket, e.getMessage()));
        }
        log.info("Finished uploading folder {} to bucket {}", folder, bucket);
    }

    @Override
    public void downloadObject(String bucket, String object, HttpServletResponse resp) {
        log.info("Downloading file {} from bucket {}", object, bucket);
        try (ServletOutputStream outputStream = resp.getOutputStream()) {
            Storage storage = StorageOptions.getDefaultInstance().getService();
            Blob blob = storage.get(BlobId.of(bucket, object));
            blob.downloadTo(outputStream);
            log.info("Finished downloading file {} from bucket {}", object, bucket);
        } catch (Exception e) {
            log.error("Cannot download object {} from bucket {}. Reason: {}", object, bucket, e.getMessage(), e);
            throw new DatalabException(String.format("Cannot download object %s from bucket %s. Reason: %s", object, bucket, e.getMessage()));
        }
        log.info("Finished downloading file {} from bucket {}", object, bucket);
    }

    @Override
    public void deleteObjects(String bucket, List<String> objects) {
        try {
            Storage storage = StorageOptions.getDefaultInstance().getService();
            List<BlobId> blobIds = objects
                    .stream()
                    .map(o -> BlobId.of(bucket, o))
                    .collect(Collectors.toList());
            storage.delete(blobIds);
        } catch (Exception e) {
            log.error("Cannot delete objects {} from bucket {}. Reason: {}", objects, bucket, e.getMessage(), e);
            throw new DatalabException(String.format("Cannot delete objects %s from bucket %s. Reason: %s", objects, bucket, e.getMessage()));
        }
    }

    private BucketDTO toBucketDTO(BlobInfo blobInfo) {
        return BucketDTO.builder()
                .bucket(blobInfo.getBucket())
                .object(blobInfo.getName())
                .size(String.valueOf(blobInfo.getSize()))
                .lastModifiedDate(blobInfo.getUpdateTime())
                .build();
    }
}
