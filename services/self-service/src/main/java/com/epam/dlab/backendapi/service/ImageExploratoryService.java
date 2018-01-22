package com.epam.dlab.backendapi.service;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.dto.exploratory.ExploratoryImage;

import java.util.List;

public interface ImageExploratoryService {

    String createImage(UserInfo user, String exploratoryName, ExploratoryImage exploratoryImage);

    List<ExploratoryImage> getImages(String user);
}
