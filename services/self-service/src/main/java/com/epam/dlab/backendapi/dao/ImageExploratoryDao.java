package com.epam.dlab.backendapi.dao;

import com.epam.dlab.model.exloratory.Image;

public interface ImageExploratoryDao {

    boolean exist(String name);

    void save(Image image);
}
