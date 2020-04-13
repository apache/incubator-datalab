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

package com.epam.dlab.backendapi.service.impl.gcp;

import com.epam.dlab.backendapi.service.BucketService;
import com.epam.dlab.dto.bucket.BucketDTO;
import com.epam.dlab.exceptions.DlabException;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
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
            log.error("Cannot retrieve objects from bucket {}. Reason: {}", bucket, e.getMessage());
            throw new DlabException(String.format("Cannot retrieve objects from bucket %s. Reason: %s", bucket, e.getMessage()));
        }
    }

    @Override
    public void uploadObject(String bucket, String object, InputStream stream) {
        try {
            Storage storage = StorageOptions.getDefaultInstance().getService();
            BlobId blobId = BlobId.of(bucket, object);
            BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
            storage.create(blobInfo, IOUtils.toByteArray(stream));
        } catch (Exception e) {
            log.error("Cannot upload object {} to bucket {}. Reason: {}", object, bucket, e.getMessage());
            throw new DlabException(String.format("Cannot upload object %s to bucket %s. Reason: %s", object, bucket, e.getMessage()));
        }
    }

    @Override
    public byte[] downloadObject(String bucket, String object) {
        try {
            Storage storage = StorageOptions.getDefaultInstance().getService();
            Blob blob = storage.get(BlobId.of(bucket, object));
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            blob.downloadTo(outputStream); //todo add check for blob != null and throw exception
            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("Cannot download object {} from bucket {}. Reason: {}", object, bucket, e.getMessage());
            throw new DlabException(String.format("Cannot download object %s from bucket %s. Reason: %s", object, bucket, e.getMessage()));
        }
    }

    @Override
    public void deleteObject(String bucket, String object) {
        try {
            Storage storage = StorageOptions.getDefaultInstance().getService();
            storage.delete(bucket, object);
        } catch (Exception e) {
            log.error("Cannot delete object {} from bucket {}. Reason: {}", object, bucket, e.getMessage());
            throw new DlabException(String.format("Cannot delete object %s from bucket %s. Reason: %s", object, bucket, e.getMessage()));
        }
    }

    private BucketDTO toBucketDTO(BlobInfo blobInfo) {
        final String size = FileUtils.byteCountToDisplaySize(blobInfo.getSize());
        Date date = new Date(blobInfo.getCreateTime());
        SimpleDateFormat formatter = new SimpleDateFormat("dd-M-yyyy hh:mm:ss");
        return BucketDTO.builder()
                .bucket(blobInfo.getBucket())
                .object(blobInfo.getName())
                .size(size)
                .creationDate(formatter.format(date))
                .build();
    }
}
