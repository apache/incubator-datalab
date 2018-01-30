package com.epam.dlab.backendapi.dao;

import com.epam.dlab.backendapi.resources.dto.ImageInfoRecord;
import com.epam.dlab.model.exloratory.Image;

import java.util.List;
import java.util.Optional;

public interface ImageExploratoryDao {

    boolean exist(String name);

    void save(Image image);

    void updateImageFields(Image image);

    List<ImageInfoRecord> getCreatedImages(String user);

    Optional<ImageInfoRecord> getImage(String user, String name);
}
