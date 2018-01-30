package com.epam.dlab.backendapi.service;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.resources.dto.ImageInfoRecord;
import com.epam.dlab.model.exloratory.Image;

import java.util.List;

public interface ImageExploratoryService {

    String createImage(UserInfo user, String exploratoryName, String imageName, String imageDescription);
    void updateImage(Image image);
    void updateImage(Image image, String newNotebookIp, String exploratoryName);
    List<ImageInfoRecord> getImages(String user);
}
