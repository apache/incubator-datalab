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
import com.google.inject.Singleton;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Singleton
public class TagServiceImpl implements TagService {

    @Override
    public Map<String, String> getResourceTags(UserInfo userInfo, String endpoint, String project,
                                               String customTag, boolean gpuEnabled) {
        Map<String, String> tags = new HashMap<>();
        tags.put("user_tag", userInfo.getName());
        tags.put("endpoint_tag", endpoint);
        tags.put("project_tag", project);
        Optional.ofNullable(customTag).ifPresent(t -> tags.put("custom_tag", t));
        if (gpuEnabled) {
            tags.put("gpu_tag", "gpu");
        }
        return tags;
    }
}
