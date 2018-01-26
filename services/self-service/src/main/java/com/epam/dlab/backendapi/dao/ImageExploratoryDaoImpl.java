package com.epam.dlab.backendapi.dao;

import com.epam.dlab.backendapi.resources.dto.ImageInfoRecord;
import com.epam.dlab.dto.exploratory.ImageStatus;
import com.epam.dlab.model.exloratory.Image;
import com.google.inject.Singleton;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

@Singleton
public class ImageExploratoryDaoImpl extends BaseDAO implements ImageExploratoryDao {

    private static final String IMAGE_NAME = "name";
    private static final String IMAGE_APPLICATION = "application";
    private static final String IMAGE_DESCRIPTION = "description";
    private static final String IMAGE_FULL_NAME = "fullName";
    private static final String EXTERNAL_IMAGE_ID = "externalId";
    private static final String EXTERNAL_NAME = "externalName";

    @Override
    public boolean exist(String name) {
        return findOne(MongoCollections.IMAGES, eq(IMAGE_NAME, name)).isPresent();
    }

    @Override
    public void save(Image image) {
        insertOne(MongoCollections.IMAGES, image);
    }

    @Override
    public void updateImageFields(Image image) {
        final Bson condition = imageCondition(image.getUser(), image.getName());
        final Document updatedFields = getUpdatedFields(image);
        updateOne(MongoCollections.IMAGES, condition, new Document(SET, updatedFields));
    }

    @Override
    public List<ImageInfoRecord> getCreatedImages(String user) {

        final List<Document> documents = find(MongoCollections.IMAGES, and(eq(USER, user), eq(STATUS, String.valueOf(ImageStatus.CREATED)))).into(new ArrayList<>());
        return documents.stream()
                .map(this::toImageInfo).collect(Collectors.toList());
    }

    private ImageInfoRecord toImageInfo(Document d) {
        final String imageName = d.getString(IMAGE_NAME);
        final String description = d.getString(IMAGE_DESCRIPTION);
        final String application = d.getString(IMAGE_APPLICATION);
        final String fullName = d.getString(IMAGE_FULL_NAME);
        return new ImageInfoRecord(imageName, description, application, fullName);
    }

    private Bson imageCondition(String user, String imageName) {
        return and(eq(USER, user), eq(IMAGE_NAME, imageName));
    }

    private Document getUpdatedFields(Image image) {
        return new Document(STATUS, image.getStatus().toString())
                .append(IMAGE_FULL_NAME, image.getFullName())
                .append(EXTERNAL_IMAGE_ID, image.getExternalId())
                .append(IMAGE_APPLICATION, image.getApplication())
                .append(EXTERNAL_NAME, image.getExternalName());
    }
}
