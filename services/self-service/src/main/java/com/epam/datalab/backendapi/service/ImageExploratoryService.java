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
import com.epam.datalab.backendapi.resources.dto.*;
import com.epam.datalab.model.exploratory.Image;

import java.util.List;
import java.util.Set;

public interface ImageExploratoryService {
    boolean imageExistInProject(String imageName, String project);

    String createImage(UserInfo user, String project, String exploratoryName, String imageName, String imageDescription);

    void terminateImage(UserInfo user, String project, String endpoint, String imageName);

    void finishTerminateImage(String imageName, String projectName, String endpoint);

    void finishImageCreate(Image image, String exploratoryName, String newNotebookIp);

    List<ImageInfoDTO> getNotFailedImages(UserInfo user, String dockerImage, String project, String endpoint);

    ImageInfoRecord getImage(String user, String name, String project, String endpoint);

    ImageInfoRecord getImage(String name, String project, String endpoint);

    List<ImageInfoRecord> getImagesForProject(String project);

    ImagesPageInfo getImagesOfUser(UserInfo user, ImageFilter imageFilter);

    void updateImageSharing(UserInfo user, ImageShareDTO imageShareDTO);

    List<ImageInfoRecord> getSharedImages(UserInfo user);

    List<ImageInfoRecord> getSharedImages(UserInfo userInfo, String dockerImage, String project, String endpoint);

    ImageUserPermissions getUserImagePermissions(UserInfo userInfo, ImageInfoRecord image);

    SharingInfo getSharingInfo(String userName, String imageName, String project, String endpoint);

    boolean canCreateFromImage(UserInfo userInfo, String imageName, String project, String endpoint);

    Set<SharedWithDTO> getUsersAndGroupsForSharing(String userName, String imageName, String project, String endpoint, String value);
}
