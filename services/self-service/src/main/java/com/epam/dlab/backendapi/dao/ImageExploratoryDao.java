package com.epam.dlab.backendapi.dao;

import com.epam.dlab.backendapi.resources.dto.ImageInfoRecord;
import com.epam.dlab.model.exloratory.Image;

import java.util.List;

public interface ImageExploratoryDao {

    boolean exist(String name);

    void save(Image image);

    void updateImageFields(Image image);

    List<ImageInfoRecord> getImages(String user);
}
