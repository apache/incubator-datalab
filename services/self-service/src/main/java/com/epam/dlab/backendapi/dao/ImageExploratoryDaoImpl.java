package com.epam.dlab.backendapi.dao;

import com.epam.dlab.model.exloratory.Image;
import com.google.inject.Singleton;

import static com.mongodb.client.model.Filters.eq;

@Singleton
public class ImageExploratoryDaoImpl extends BaseDAO implements ImageExploratoryDao {

    public static final String IMAGE_NAME_FIELD = "name";

    @Override
    public boolean exist(String name) {
        return findOne(MongoCollections.IMAGES, eq(IMAGE_NAME_FIELD, name)).isPresent();
    }

    @Override
    public void save(Image image) {
        insertOne(MongoCollections.IMAGES, image);
    }
}
