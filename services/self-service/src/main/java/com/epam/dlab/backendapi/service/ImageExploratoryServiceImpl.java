package com.epam.dlab.backendapi.service;

import com.epam.dlab.UserInstanceStatus;
import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.dao.ImageExploratoryDao;
import com.epam.dlab.backendapi.resources.dto.ImageInfoRecord;
import com.epam.dlab.backendapi.util.RequestBuilder;
import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.dto.UserInstanceDTO;
import com.epam.dlab.dto.exploratory.ExploratoryStatusDTO;
import com.epam.dlab.dto.exploratory.ImageStatus;
import com.epam.dlab.exceptions.ResourceAlreadyExistException;
import com.epam.dlab.exceptions.ResourceNotFoundException;
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

    private static final String IMAGE_EXISTS_MSG = "Image with name %s is already exist";
    private static final String IMAGE_NOT_FOUND_MSG = "Image with name %s was not found for user %s";
    @Inject
    private ExploratoryDAO exploratoryDAO;

    @Inject
    private ImageExploratoryDao imageExploratotyDao;

    @Inject
    @Named(ServiceConsts.PROVISIONING_SERVICE_NAME)
    private RESTService provisioningService;

    @Override
    public String createImage(UserInfo user, String exploratoryName, String imageName, String imageDescription) {

        UserInstanceDTO userInstance = exploratoryDAO.fetchRunningExploratoryFields(user.getName(), exploratoryName);

        if (imageExploratotyDao.exist(imageName)) {
            log.error(String.format(IMAGE_EXISTS_MSG, imageName));
            throw new ResourceAlreadyExistException(String.format(IMAGE_EXISTS_MSG, imageName));
        }
        imageExploratotyDao.save(Image.builder()
                .name(imageName)
                .description(imageDescription)
                .status(ImageStatus.CREATING)
                .user(user.getName())
                .exploratoryId(userInstance.getId()).build());

        exploratoryDAO.updateExploratoryStatus(new ExploratoryStatusDTO()
                .withUser(user.getName())
                .withExploratoryName(exploratoryName)
                .withStatus(UserInstanceStatus.CREATING_IMAGE));

        return provisioningService.post(ExploratoryAPI.EXPLORATORY_IMAGE, user.getAccessToken(),
                RequestBuilder.newExploratoryImageCreate(user, userInstance, imageName), String.class);
    }

    @Override
    public void finishImageCreate(Image image, String exploratoryName, String newNotebookIp) {
        log.debug("Returning exploratory status with name {} to RUNNING for user {}", exploratoryName, image.getUser());
        exploratoryDAO.updateExploratoryStatus(new ExploratoryStatusDTO()
                .withUser(image.getUser())
                .withExploratoryName(exploratoryName)
                .withStatus(UserInstanceStatus.RUNNING));
        imageExploratotyDao.updateImageFields(image);
        if (newNotebookIp != null) {
            log.debug("Changing exploratory ip with name {} for user {} to {}", exploratoryName, image.getUser(), newNotebookIp);
            exploratoryDAO.updateExploratoryIp(image.getUser(), newNotebookIp, exploratoryName);
        }

    }

    @Override
    public List<ImageInfoRecord> getCreatedImages(String user) {
        return imageExploratotyDao.getCreatedImages(user);
    }

    @Override
    public ImageInfoRecord getImage(String user, String name) {
        return imageExploratotyDao.getImage(user, name).orElseThrow(() ->
                new ResourceNotFoundException(String.format(IMAGE_NOT_FOUND_MSG, name, user)));
    }
}
