package com.epam.dlab.backendapi.dao;

import com.epam.dlab.model.exloratory.Image;
import com.google.inject.Singleton;
import org.bson.Document;
import org.bson.conversions.Bson;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

@Singleton
public class ImageExploratoryDaoImpl extends BaseDAO implements ImageExploratoryDao {

    private static final String IMAGE_NAME = "name";
    private static final String FULL_NAME = "fullName";
    private static final String EXTERNAL_IMAGE_ID = "externalId";

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

    private Bson imageCondition(String user, String imageName) {
        return and(eq(USER, user), eq(IMAGE_NAME, imageName));
    }

    private Document getUpdatedFields(Image image) {
        return new Document(STATUS, image.getStatus().toString())
                .append(FULL_NAME, image.getFullName())
                .append(EXTERNAL_IMAGE_ID, image.getExternalId());
    }
}
