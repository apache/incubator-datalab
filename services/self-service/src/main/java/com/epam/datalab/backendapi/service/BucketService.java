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

package com.epam.datalab.backendapi.service;

import com.epam.datalab.auth.UserInfo;
import com.epam.datalab.dto.bucket.BucketDTO;

import javax.servlet.http.HttpServletResponse;
import java.io.InputStream;
import java.util.List;

public interface BucketService {
    List<BucketDTO> getObjects(UserInfo userInfo, String bucket, String endpoint);

    void uploadObject(UserInfo userInfo, String bucket, String object, String endpoint, InputStream inputStream, String contentType, long fileSize, String auditInfo);

    void uploadFolder(UserInfo userInfo, String bucket, String folder, String endpoint, String auditInfo);

    void downloadObject(UserInfo userInfo, String bucket, String object, String endpoint, HttpServletResponse resp, String auditInfo);

    void deleteObjects(UserInfo userInfo, String bucket, List<String> objects, String endpoint, String auditInfo);
}
