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


package com.epam.datalab.backendapi.resources.dto;

import com.epam.datalab.cloud.CloudProvider;
import com.epam.datalab.dto.SharedWith;
import com.epam.datalab.dto.aws.computational.ClusterConfig;
import com.epam.datalab.dto.exploratory.ImageSharingStatus;
import com.epam.datalab.dto.exploratory.ImageStatus;
import com.epam.datalab.model.library.Library;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.Date;
import java.util.List;
import java.util.Map;
@Data
@Builder
@AllArgsConstructor
public class ImageInfoDTO {
    private String name;
    private Date timestamp;
    private String description;
    private String project;
    private String endpoint;
    private String user;
    private String application;
    private String templateName;
    private String instanceName;
    private CloudProvider cloudProvider;
    private String dockerImage;
    private String fullName;
    private ImageStatus status;
    private ImageSharingStatus sharingStatus;
    private ImageUserPermissions imageUserPermissions;
    private SharedWith sharedWith;
    private List<ClusterConfig> clusterConfig;
    private String exploratoryURL;
    private List<Library> libraries;
    private Map<String, List<Library>> computationalLibraries;
}
