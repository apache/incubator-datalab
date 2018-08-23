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
import com.google.inject.Singleton;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.*;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Projections.elemMatch;
import static com.mongodb.client.model.Projections.*;

@Singleton
public class ImageExploratoryDaoImpl extends BaseDAO implements ImageExploratoryDao {

	public static final String LIBRARIES = "libraries";
	private static final String IMAGE_NAME = "name";
	private static final String IMAGE_APPLICATION = "application";
	private static final String IMAGE_FULL_NAME = "fullName";
	private static final String EXTERNAL_NAME = "externalName";
	private static final String DOCKER_IMAGE = "dockerImage";

	@Override
	public boolean exist(String user, String name) {
		return findOne(MongoCollections.IMAGES, userImageCondition(user, name)).isPresent();
	}

	@Override
	public void save(Image image) {
		insertOne(MongoCollections.IMAGES, image);
	}

	@Override
	public void updateImageFields(Image image) {
		final Bson condition = userImageCondition(image.getUser(), image.getName());
		final Document updatedFields = getUpdatedFields(image);
		updateOne(MongoCollections.IMAGES, condition, new Document(SET, updatedFields));
	}

	@Override
	public List<ImageInfoRecord> getImages(String user, String dockerImage, ImageStatus... statuses) {
		return find(MongoCollections.IMAGES,
				userImagesCondition(user, dockerImage, statuses),
				ImageInfoRecord.class);
	}

	@Override
	public Optional<ImageInfoRecord> getImage(String user, String name) {
		return findOne(MongoCollections.IMAGES, userImageCondition(user, name), ImageInfoRecord.class);
	}

	@Override
	@SuppressWarnings("unchecked")
	public List<Library> getLibraries(String user, String imageFullName, ResourceType resourceType, LibStatus
			status) {
		return ((List<Document>) libDocument(user, imageFullName, status)
				.orElse(emptyLibrariesDocument()).get(LIBRARIES))
				.stream()
				.map(d -> convertFromDocument(d, Library.class))
				.collect(Collectors.toList());
	}

	private Optional<Document> libDocument(String user, String imageFullName, LibStatus status) {
		return findOne(MongoCollections.IMAGES,
				imageLibraryCondition(user, imageFullName, status),
				fields(include(LIBRARIES), excludeId()));
	}

	private Document emptyLibrariesDocument() {
		return new Document(LIBRARIES, Collections.emptyList());
	}

	private Bson imageLibraryCondition(String user, String imageFullName, LibStatus status) {
		return and(eq(USER, user), eq(IMAGE_FULL_NAME, imageFullName),
				elemMatch(LIBRARIES, eq(STATUS, status.name())));
	}

	private Bson userImagesCondition(String user, String dockerImage, ImageStatus... statuses) {
		final Bson userImagesCondition = userImagesCondition(user, statuses);
		if (Objects.nonNull(dockerImage)) {
			return and(userImagesCondition, eq(DOCKER_IMAGE, dockerImage));
		} else {
			return userImagesCondition;
		}

	}

	private Bson userImagesCondition(String user, ImageStatus... statuses) {

		final List<String> statusList = Arrays
				.stream(statuses)
				.map(ImageStatus::name)
				.collect(Collectors.toList());
		return and(eq(USER, user), in(STATUS, statusList));
	}


	private Bson userImageCondition(String user, String imageName) {
		return and(eq(USER, user), eq(IMAGE_NAME, imageName));
	}

	private Document getUpdatedFields(Image image) {
		return new Document(STATUS, image.getStatus().toString())
				.append(IMAGE_FULL_NAME, image.getFullName())
				.append(IMAGE_APPLICATION, image.getApplication())
				.append(EXTERNAL_NAME, image.getExternalName());
	}
}
