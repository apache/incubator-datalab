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

package com.epam.dlab.backendapi.service.impl.aws;

import com.epam.dlab.backendapi.service.BucketService;
import com.epam.dlab.dto.bucket.BucketDTO;
import com.epam.dlab.exceptions.DlabException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import software.amazon.awssdk.awscore.exception.AwsServiceException;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class BucketServiceAwsImpl implements BucketService {

    @Override
    public List<BucketDTO> getObjects(String bucket) {
        try {
            S3Client s3 = S3Client.create();
            ListObjectsRequest getRequest = ListObjectsRequest
                    .builder()
                    .bucket(bucket)
                    .build();

            return s3.listObjects(getRequest).contents()
                    .stream()
                    .map(o -> toBucketDTO(bucket, o))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Cannot retrieve objects from bucket {}. Reason: {}", bucket, e.getMessage());
            throw new DlabException(String.format("Cannot retrieve objects from bucket %s. Reason: %s", bucket, e.getMessage()));
        }
    }

    @Override
    public void uploadObject(String bucket, String object, InputStream stream) {
        try {
            S3Client s3 = S3Client.create();
            PutObjectRequest uploadRequest = PutObjectRequest
                    .builder()
                    .bucket(bucket)
                    .key(object)
                    .build();
            s3.putObject(uploadRequest, RequestBody.fromBytes(IOUtils.toByteArray(stream)));
        } catch (Exception e) {
            log.error("Cannot upload object {} to bucket {}. Reason: {}", object, bucket, e.getMessage());
            throw new DlabException(String.format("Cannot upload object %s to bucket %s. Reason: %s", object, bucket, e.getMessage()));
        }
    }

    @Override
    public byte[] downloadObject(String bucket, String object) {
        try {
            S3Client s3 = S3Client.create();
            GetObjectRequest downloadRequest = GetObjectRequest
                    .builder()
                    .bucket(bucket)
                    .key(object)
                    .build();
            return s3.getObject(downloadRequest, ResponseTransformer.toBytes()).asByteArray();
        } catch (Exception e) {
            log.error("Cannot download object {} from bucket {}. Reason: {}", object, bucket, e.getMessage());
            throw new DlabException(String.format("Cannot download object %s from bucket %s. Reason: %s", object, bucket, e.getMessage()));
        }
    }

    @Override
    public void deleteObject(String bucket, String object) {
        try {
            S3Client s3 = S3Client.create();
            DeleteObjectRequest deleteObjectRequest = DeleteObjectRequest
                    .builder()
                    .bucket(bucket)
                    .key(object)
                    .build();
            s3.deleteObject(deleteObjectRequest);
        } catch (AwsServiceException e) {
            log.error("Cannot delete object {} from bucket {}. Reason: {}", object, bucket, e.getMessage());
            throw new DlabException(String.format("Cannot delete object %s from bucket %s. Reason: %s", object, bucket, e.getMessage()));
        }
    }

    private BucketDTO toBucketDTO(String bucket, S3Object s3Object) {
        final String size = FileUtils.byteCountToDisplaySize(s3Object.size());
        Date date = Date.from(s3Object.lastModified());
        SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
        return BucketDTO.builder()
                .bucket(bucket)
                .object(s3Object.key())
                .size(size)
                .lastModifiedDate(formatter.format(date))
                .build();
    }
}
