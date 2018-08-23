/*
 * Copyright (c) 2018, EPAM SYSTEMS INC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.dlab.backendapi.dao;

import com.epam.dlab.backendapi.resources.dto.ImageInfoRecord;
import com.epam.dlab.dto.exploratory.ImageStatus;
import com.epam.dlab.dto.exploratory.LibStatus;
import com.epam.dlab.model.ResourceType;
import com.epam.dlab.model.exploratory.Image;
import com.epam.dlab.model.library.Library;

import java.util.List;
import java.util.Optional;

public interface ImageExploratoryDao {

	boolean exist(String user, String name);

	void save(Image image);

	void updateImageFields(Image image);

	List<ImageInfoRecord> getImages(String user, String dockerImage, ImageStatus ... statuses);

	Optional<ImageInfoRecord> getImage(String user, String name);

	List<Library> getLibraries(String user, String imageFullName, ResourceType resourceType, LibStatus status);
}
