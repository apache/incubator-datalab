package com.epam.dlab.backendapi.dao;

import com.epam.dlab.backendapi.resources.dto.ImageInfoRecord;
import com.epam.dlab.dto.exploratory.ImageStatus;
import com.epam.dlab.model.exloratory.Image;
import com.google.inject.Singleton;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.List;
import java.util.Optional;

import static com.mongodb.client.model.Filters.and;
import static com.mongodb.client.model.Filters.eq;

@Singleton
public class ImageExploratoryDaoImpl extends BaseDAO implements ImageExploratoryDao {

    private static final String IMAGE_NAME = "name";
    private static final String IMAGE_APPLICATION = "application";
    private static final String IMAGE_FULL_NAME = "fullName";
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
        final Bson condition = userImageCondition(image.getUser(), image.getName());
        final Document updatedFields = getUpdatedFields(image);
        updateOne(MongoCollections.IMAGES, condition, new Document(SET, updatedFields));
    }

    @Override
    public List<ImageInfoRecord> getCreatedImages(String user) {
        return find(MongoCollections.IMAGES,
                userCreatedImagesCondition(user),
                ImageInfoRecord.class);
    }

    @Override
    public Optional<ImageInfoRecord> getImage(String user, String name) {
        return findOne(MongoCollections.IMAGES, userImageCondition(user, name), ImageInfoRecord.class);
    }

    private Bson userCreatedImagesCondition(String user) {
        return and(eq(USER, user), eq(STATUS, String.valueOf(ImageStatus.CREATED)));
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
