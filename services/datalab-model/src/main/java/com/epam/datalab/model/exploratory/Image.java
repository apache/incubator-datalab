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

package com.epam.datalab.model.exploratory;

import com.epam.datalab.dto.SharedWith;
import com.epam.datalab.dto.aws.computational.ClusterConfig;
import com.epam.datalab.dto.exploratory.ImageStatus;
import com.epam.datalab.model.library.Library;
import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class Image {
    private final String name;
    private final String description;
    private final ImageStatus status;
    private final String exploratoryId;
    private final String project;
    private final String endpoint;
    private final String user;
    private final String fullName;
    private final String externalName;
    private final String application;
    private final String templateName;
    private final String instanceName;
    private final String cloudProvider;
    private final String dockerImage;
    private final SharedWith sharedWith;
    private final List<ClusterConfig> clusterConfig;
    private final List<Library> libraries;
    private final Map<String, List<Library>> computationalLibraries;
}
