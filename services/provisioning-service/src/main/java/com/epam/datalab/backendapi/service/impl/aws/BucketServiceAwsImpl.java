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

package com.epam.datalab.backendapi.service.impl.aws;

import com.epam.datalab.auth.UserInfo;
import com.epam.datalab.backendapi.service.BucketService;
import com.epam.datalab.dto.bucket.BucketDTO;
import com.epam.datalab.exceptions.DatalabException;
import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Delete;
import software.amazon.awssdk.services.s3.model.DeleteObjectsRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsRequest;
import software.amazon.awssdk.services.s3.model.ObjectIdentifier;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
public class BucketServiceAwsImpl implements BucketService {

    @Override
    public List<BucketDTO> getObjects(String bucket) {
        try (S3Client s3 = S3Client.create()) {
            ListObjectsRequest getRequest = ListObjectsRequest
                    .builder()
                    .bucket(bucket)
                    .build();

            return s3.listObjects(getRequest).contents()
                    .stream()
                    .map(o -> toBucketDTO(bucket, o))
                    .collect(Collectors.toList());
        } catch (Exception e) {
            log.error("Cannot retrieve objects from bucket {}. Reason: {}", bucket, e.getMessage(), e);
            throw new DatalabException(String.format("Cannot retrieve objects from bucket %s. Reason: %s", bucket, e.getMessage()));
        }
    }

    @Override
    public void uploadObject(String bucket, String object, InputStream stream, String contentType, long fileSize) {
        log.info("Uploading file {} to bucket {}", object, bucket);
        try (S3Client s3 = S3Client.create()) {
            PutObjectRequest uploadRequest = PutObjectRequest
                    .builder()
                    .bucket(bucket)
                    .key(object)
                    .contentType(contentType)
                    .build();
            s3.putObject(uploadRequest, RequestBody.fromInputStream(stream, fileSize));
        } catch (Exception e) {
            log.error("Cannot upload object {} to bucket {}. Reason: {}", object, bucket, e.getMessage(), e);
            throw new DatalabException(String.format("Cannot upload object %s to bucket %s. Reason: %s", object, bucket, e.getMessage()));
        }
        log.info("Finished uploading file {} to bucket {}", object, bucket);
    }

    @Override
    public void uploadFolder(UserInfo userInfo, String bucket, String folder) {
        log.info("Uploading folder {} to bucket {}", folder, bucket);
        try (S3Client s3 = S3Client.create()) {
            PutObjectRequest uploadRequest = PutObjectRequest
                    .builder()
                    .bucket(bucket)
                    .key(folder)
                    .build();
            s3.putObject(uploadRequest, RequestBody.empty());
        } catch (Exception e) {
            log.error("Cannot upload folder {} to bucket {}. Reason: {}", folder, bucket, e.getMessage(), e);
            throw new DatalabException(String.format("Cannot upload folder %s to bucket %s. Reason: %s", folder, bucket, e.getMessage()));
        }
        log.info("Finished uploading folder {} to bucket {}", folder, bucket);
    }

    @Override
    public void downloadObject(String bucket, String object, HttpServletResponse resp) {
        log.info("Downloading file {} from bucket {}", object, bucket);
        try (ServletOutputStream outputStream = resp.getOutputStream(); S3Client s3 = S3Client.create()) {
            GetObjectRequest downloadRequest = GetObjectRequest
                    .builder()
                    .bucket(bucket)
                    .key(object)
                    .build();
            s3.getObject(downloadRequest, ResponseTransformer.toOutputStream(outputStream));
        } catch (Exception e) {
            log.error("Cannot download object {} from bucket {}. Reason: {}", object, bucket, e.getMessage(), e);
            throw new DatalabException(String.format("Cannot download object %s from bucket %s. Reason: %s", object, bucket, e.getMessage()));
        }
        log.info("Finished downloading file {} from bucket {}", object, bucket);
    }

    @Override
    public void deleteObjects(String bucket, List<String> objects) {
        try (S3Client s3 = S3Client.create()) {
            List<ObjectIdentifier> objectsToDelete = objects
                    .stream()
                    .map(o -> ObjectIdentifier.builder()
                            .key(o)
                            .build())
                    .collect(Collectors.toList());

            DeleteObjectsRequest deleteObjectsRequests = DeleteObjectsRequest.builder()
                    .bucket(bucket)
                    .delete(Delete.builder()
                            .objects(objectsToDelete)
                            .build())
                    .build();

            s3.deleteObjects(deleteObjectsRequests);
        } catch (Exception e) {
            log.error("Cannot delete objects {} from bucket {}. Reason: {}", objects, bucket, e.getMessage(), e);
            throw new DatalabException(String.format("Cannot delete objects %s from bucket %s. Reason: %s", objects, bucket, e.getMessage()));
        }
    }

    private BucketDTO toBucketDTO(String bucket, S3Object s3Object) {
        return BucketDTO.builder()
                .bucket(bucket)
                .object(s3Object.key())
                .size(String.valueOf(s3Object.size()))
                .lastModifiedDate(s3Object.lastModified().toEpochMilli())
                .build();
    }
}
