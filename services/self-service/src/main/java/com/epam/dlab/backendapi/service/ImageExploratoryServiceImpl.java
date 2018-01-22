package com.epam.dlab.backendapi.service;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.dao.ImageExploratoryDao;
import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.dto.UserInstanceDTO;
import com.epam.dlab.dto.exploratory.ExploratoryImage;
import com.epam.dlab.dto.exploratory.ImageStatus;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.model.exloratory.Image;
import com.epam.dlab.rest.client.RESTService;
import com.epam.dlab.rest.contracts.ExploratoryAPI;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Singleton
@Slf4j
public class ImageExploratoryServiceImpl implements ImageExploratoryService {

    public static final String IMAGE_EXISTS_MSG = "Image with name %s is already exist";
    @Inject
    private ExploratoryDAO exploratoryDAO;

    @Inject
    private ImageExploratoryDao imageExploratotyDao;

    @Inject
    @Named(ServiceConsts.PROVISIONING_SERVICE_NAME)
    private RESTService provisioningService;

    @Override
    public String createImage(UserInfo user, String exploratoryName, ExploratoryImage image) {

        UserInstanceDTO userInstance = exploratoryDAO.fetchRunningExploratoryFields(user.getName(), exploratoryName);

        if (imageExploratotyDao.exist(image.getName())) {
            log.error(String.format(IMAGE_EXISTS_MSG, image.getName()));
            throw new DlabException(String.format(IMAGE_EXISTS_MSG, image.getName()));
        }
        imageExploratotyDao.save(Image.builder()
                .name(image.getName())
                .description(image.getDescription())
                .status(ImageStatus.CREATING)
                .exploratoryId(userInstance.getId()).build());

        return provisioningService.post(ExploratoryAPI.EXPLORATORY_IMAGE, user.getAccessToken(), image, String.class);
    }

    @Override
    public List<ExploratoryImage> getImages(String user) {
        return null;
    }
}
