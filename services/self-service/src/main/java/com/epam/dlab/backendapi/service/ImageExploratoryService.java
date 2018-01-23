package com.epam.dlab.backendapi.service;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.dto.exploratory.ExploratoryImageDTO;
import com.epam.dlab.model.exloratory.Image;

import java.util.List;

public interface ImageExploratoryService {

    String createImage(UserInfo user, String exploratoryName, String imageName, String imageDescription);
    void updateImage(Image image);
    List<ExploratoryImageDTO> getImages(String user);
}
