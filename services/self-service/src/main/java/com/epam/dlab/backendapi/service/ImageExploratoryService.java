package com.epam.dlab.backendapi.service;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.resources.dto.ImageInfoRecord;
import com.epam.dlab.model.exloratory.Image;

import java.util.List;

public interface ImageExploratoryService {

    String createImage(UserInfo user, String exploratoryName, String imageName, String imageDescription);

    void finishImageCreate(Image image, String exploratoryName,String newNotebookIp);

    List<ImageInfoRecord> getCreatedImages(String user);

    ImageInfoRecord getImage(String user, String name);
}
