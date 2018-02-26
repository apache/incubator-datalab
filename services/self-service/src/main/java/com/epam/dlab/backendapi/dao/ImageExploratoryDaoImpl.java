package com.epam.dlab.backendapi.dao;

import com.epam.dlab.backendapi.resources.dto.ImageInfoRecord;
import com.epam.dlab.dto.exploratory.ImageStatus;
import com.epam.dlab.dto.exploratory.LibStatus;
import com.epam.dlab.model.ResourceType;
import com.epam.dlab.model.exloratory.Image;
import com.epam.dlab.model.library.Library;
import com.google.inject.Singleton;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;
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
	public List<ImageInfoRecord> getImages(String user, ImageStatus status, String dockerImage) {
		return find(MongoCollections.IMAGES,
				userImagesCondition(user, status, dockerImage),
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

	private Bson userImagesCondition(String user, ImageStatus status, String dockerImage) {
		return and(eq(USER, user), eq(STATUS, status.name()), eq(DOCKER_IMAGE, dockerImage));
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
