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

package com.epam.datalab.backendapi.dao;

import com.epam.datalab.backendapi.resources.dto.ImageInfoRecord;
import com.epam.datalab.dto.SharedWith;
import com.epam.datalab.dto.exploratory.ImageStatus;
import com.epam.datalab.dto.exploratory.LibStatus;
import com.epam.datalab.model.exploratory.Image;
import com.epam.datalab.model.library.Library;

import java.util.List;
import java.util.Optional;

public interface ImageExploratoryDAO {

    boolean exist(String image, String project);

    void save(Image image);

    void updateImageFields(Image image);

    void updateImageStatus(String user, String imageName, String project, String endpoint, ImageStatus status);
    void updateImageStatus(String imageName, String projectName, String endpoint, ImageStatus status);

    List<ImageInfoRecord> getImages(String user, String dockerImage, String project, String endpoint, ImageStatus... statuses);

    List<ImageInfoRecord> getImages(String project, String endpoint, String dockerImage);

    List<ImageInfoRecord> getImagesOfUser(String user);

    List<ImageInfoRecord> getImagesForProject(String project);

    List<ImageInfoRecord> getAllImages();
    Optional<ImageInfoRecord> getImage(String user, String name, String project, String endpoint);

    Optional<ImageInfoRecord> getImage(String name, String project, String endpoint);

    List<Library> getLibraries(String imageName,  String project, String endpoint, LibStatus status);

    void updateSharing(SharedWith sharedWith, String imageName, String project, String endpoint);
}
