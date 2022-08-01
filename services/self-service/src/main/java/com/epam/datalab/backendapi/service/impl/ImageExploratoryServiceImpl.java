/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.epam.datalab.backendapi.service.impl;

import com.epam.datalab.auth.UserInfo;
import com.epam.datalab.backendapi.annotation.Audit;
import com.epam.datalab.backendapi.annotation.Info;
import com.epam.datalab.backendapi.annotation.Project;
import com.epam.datalab.backendapi.annotation.ResourceName;
import com.epam.datalab.backendapi.annotation.User;
import com.epam.datalab.backendapi.dao.*;
import com.epam.datalab.backendapi.domain.EndpointDTO;
import com.epam.datalab.backendapi.domain.ProjectDTO;
import com.epam.datalab.backendapi.resources.dto.ImageFilter;
import com.epam.datalab.backendapi.resources.dto.ImageInfoRecord;
import com.epam.datalab.backendapi.resources.dto.ProjectImagesInfo;
import com.epam.datalab.backendapi.resources.dto.UserRoleDTO;
import com.epam.datalab.backendapi.roles.RoleType;
import com.epam.datalab.backendapi.roles.UserRoles;
import com.epam.datalab.backendapi.service.EndpointService;
import com.epam.datalab.backendapi.service.ImageExploratoryService;
import com.epam.datalab.backendapi.service.ProjectService;
import com.epam.datalab.backendapi.util.RequestBuilder;
import com.epam.datalab.constants.ServiceConsts;
import com.epam.datalab.dto.UserInstanceDTO;
import com.epam.datalab.dto.UserInstanceStatus;
import com.epam.datalab.dto.exploratory.ExploratoryStatusDTO;
import com.epam.datalab.dto.exploratory.ImageSharingStatus;
import com.epam.datalab.dto.exploratory.ImageStatus;
import com.epam.datalab.exceptions.ResourceAlreadyExistException;
import com.epam.datalab.exceptions.ResourceNotFoundException;
import com.epam.datalab.model.ResourceType;
import com.epam.datalab.model.exploratory.Image;
import com.epam.datalab.model.library.Library;
import com.epam.datalab.rest.client.RESTService;
import com.epam.datalab.rest.contracts.ExploratoryAPI;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.epam.datalab.backendapi.domain.AuditActionEnum.CREATE;
import static com.epam.datalab.backendapi.domain.AuditResourceTypeEnum.IMAGE;

@Singleton
@Slf4j
public class ImageExploratoryServiceImpl implements ImageExploratoryService {
    private static final String IMAGE_EXISTS_MSG = "Image with name %s is already exist in project %s";
    private static final String IMAGE_NOT_FOUND_MSG = "Image with name %s was not found for user %s";
    private static final String PATH_TO_IMAGE_ROLES = "/mongo/image/mongo_roles.json";

    private static final String IMAGE_ROLE = "img_%s_%s_%s_%s";

    /**
     * projectName-endpointName-exploratoryName-imageName
     */
    private static final String IMAGE_MONIKER = "%s_%s_%s_%s";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    @Inject
    private ExploratoryDAO exploratoryDAO;
    @Inject
    private ImageExploratoryDAO imageExploratoryDao;
    @Inject
    private ExploratoryLibDAO libDAO;

    @Inject
    private UserRoleDAO userRoleDAO;

    @Inject
    private UserGroupDAO userGroupDAO;
    @Inject
    @Named(ServiceConsts.PROVISIONING_SERVICE_NAME)
    private RESTService provisioningService;
    @Inject
    private RequestBuilder requestBuilder;
    @Inject
    private EndpointService endpointService;
    @Inject
    private ProjectService projectService;

    @Audit(action = CREATE, type = IMAGE)
    @Override
    public String createImage(@User UserInfo user, @Project String project, @ResourceName String exploratoryName, String imageName, String imageDescription, @Info String info) {
        ProjectDTO projectDTO = projectService.get(project);
        UserInstanceDTO userInstance = exploratoryDAO.fetchRunningExploratoryFields(user.getName(), project, exploratoryName);

        if (imageExploratoryDao.exist(imageName, userInstance.getProject())) {
            log.error(String.format(IMAGE_EXISTS_MSG, imageName, userInstance.getProject()));
            throw new ResourceAlreadyExistException(String.format(IMAGE_EXISTS_MSG, imageName, userInstance.getProject()));
        }
        final List<Library> libraries = libDAO.getLibraries(user.getName(), project, exploratoryName);

        imageExploratoryDao.save(Image.builder()
                .name(imageName)
                .description(imageDescription)
                .status(ImageStatus.CREATING)
                .user(user.getName())
                .libraries(fetchExploratoryLibs(libraries))
                .computationalLibraries(fetchComputationalLibs(libraries))
                .clusterConfig(userInstance.getClusterConfig())
                .dockerImage(userInstance.getImageName())
                .exploratoryId(userInstance.getId())
                .templateName(userInstance.getTemplateName())
                .instanceName(userInstance.getExploratoryName())
                .cloudProvider(userInstance.getCloudProvider())
                .project(userInstance.getProject())
                .endpoint(userInstance.getEndpoint())
                .build());

        exploratoryDAO.updateExploratoryStatus(new ExploratoryStatusDTO()
                .withUser(user.getName())
                .withProject(project)
                .withExploratoryName(exploratoryName)
                .withStatus(UserInstanceStatus.CREATING_IMAGE));
        EndpointDTO endpointDTO = endpointService.get(userInstance.getEndpoint());
        return provisioningService.post(endpointDTO.getUrl() + ExploratoryAPI.EXPLORATORY_IMAGE,
                user.getAccessToken(),
                requestBuilder.newExploratoryImageCreate(user, userInstance, imageName, endpointDTO, projectDTO), String.class);
    }

    @Override
    public void finishImageCreate(Image image, String exploratoryName, String newNotebookIp) {
        log.debug("Returning exploratory status with name {} to RUNNING for user {}",
                exploratoryName, image.getUser());
        exploratoryDAO.updateExploratoryStatus(new ExploratoryStatusDTO()
                .withUser(image.getUser())
                .withProject(image.getProject())
                .withExploratoryName(exploratoryName)
                .withStatus(UserInstanceStatus.RUNNING));
        imageExploratoryDao.updateImageFields(image);

        log.debug("Image {}", image);
        // Create image roles
        createImageRole(image, exploratoryName);

        if (newNotebookIp != null) {
            log.debug("Changing exploratory ip with name {} for user {} to {}", exploratoryName, image.getUser(),
                    newNotebookIp);
            exploratoryDAO.updateExploratoryIp(image.getUser(), image.getProject(), newNotebookIp, exploratoryName);
        }

    }

    @Override
    public List<ImageInfoRecord> getNotFailedImages(UserInfo user, String dockerImage, String project, String endpoint) {
        List<ImageInfoRecord> images = imageExploratoryDao.getImages(user.getName(), dockerImage, project, endpoint, ImageStatus.ACTIVE, ImageStatus.CREATING);
        images.addAll(getSharedImages(user,dockerImage,project,endpoint));
        return images;
    }

    @Override
    public List<ImageInfoRecord> getNotFailedImages(String dockerImage, String project, String endpoint) {
        return imageExploratoryDao.getImages(project, endpoint, dockerImage);
    }

    @Override
    public ImageInfoRecord getImage(String user, String name, String project, String endpoint) {
        return imageExploratoryDao.getImage(user, name, project, endpoint).orElseThrow(() ->
                new ResourceNotFoundException(String.format(IMAGE_NOT_FOUND_MSG, name, user)));
    }

    @Override
    public List<ImageInfoRecord> getImagesForProject(String project) {
        return imageExploratoryDao.getImagesForProject(project);
    }

    @Override
    public List<ProjectImagesInfo> getImagesOfUser(UserInfo user) {
        log.debug("Loading list of images for user {}", user.getName());
        return projectService.getUserProjects(user, Boolean.FALSE)
                .stream()
                .map(p -> {
                    List<ImageInfoRecord> images = imageExploratoryDao.getImagesOfUser(user.getName(), p.getName());
                    images.forEach(img -> img.setSharingStatus(getImageSharingStatus(user.getName(),img)));
                    images.addAll(getSharedImages(user, p.getName()));
                    return ProjectImagesInfo.builder()
                            .project(p.getName())
                            .images(images)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public List<ProjectImagesInfo> getImagesOfUserWithFilter(UserInfo user, ImageFilter imageFilter) {
        log.debug("Loading list of images for user {}", user.getName());
        return projectService.getUserProjects(user, Boolean.FALSE)
                .stream()
                .map(p -> {
                    List<ImageInfoRecord> images = imageExploratoryDao.getImagesOfUser(user.getName(), p.getName());
                    images.forEach(img -> img.setSharingStatus(getImageSharingStatus(user.getName(),img)));
                    List<ImageInfoRecord> sharedImages = getSharedImages(user, p.getName());
                    images.addAll(sharedImages);
                    images = filterImages(images, imageFilter);
                    return ProjectImagesInfo.builder()
                            .project(p.getName())
                            .images(images)
                            .build();
                })
                .collect(Collectors.toList());
    }

    @Override
    public void shareImageWithProjectGroups(UserInfo user, String imageName, String projectName, String endpoint) {
        Set<String> projectGroups = projectService.get(projectName).getGroups();
        Optional<ImageInfoRecord> image = imageExploratoryDao.getImage(user.getName(),imageName,projectName,endpoint);
        if(image.isPresent()){
            String exploratoryName = image.get().getInstanceName();
            userRoleDAO.addGroupToRole(projectGroups,
                    Collections.singleton(String.format(IMAGE_ROLE,
                            projectName, endpoint, exploratoryName ,imageName)));
        }

    }

    private Map<String, List<Library>> fetchComputationalLibs(List<Library> libraries) {
        return libraries.stream()
                .filter(resourceTypePredicate(ResourceType.COMPUTATIONAL))
                .collect(Collectors.toMap(Library::getResourceName, Lists::newArrayList, this::merge));
    }

    private List<Library> merge(List<Library> oldValue, List<Library> newValue) {
        oldValue.addAll(newValue);
        return oldValue;
    }

    private List<Library> fetchExploratoryLibs(List<Library> libraries) {
        return libraries.stream()
                .filter(resourceTypePredicate(ResourceType.EXPLORATORY))
                .collect(Collectors.toList());
    }

    private Predicate<Library> resourceTypePredicate(ResourceType resourceType) {
        return l -> resourceType == l.getType();
    }

    private UserRoleDTO getUserImageRoleFromFile() {
        try (InputStream is = getClass().getResourceAsStream(PATH_TO_IMAGE_ROLES)) {
            return MAPPER.readValue(is, new TypeReference<UserRoleDTO>() {
            });
        } catch (IOException e) {
            log.error("Can not marshall datalab image roles due to: {}", e.getMessage(), e);
            throw new IllegalStateException("Can not marshall datalab image roles due to: " + e.getMessage());
        }
    }

    public List<ImageInfoRecord> getSharedImages(UserInfo userInfo) {
        List<ImageInfoRecord> sharedImages = imageExploratoryDao.getAllImages().stream()
                .filter(img -> !img.getUser().equals(userInfo.getName()))
                .filter(img ->
                        UserRoles.checkAccess(userInfo, RoleType.IMAGE,
                                String.format(IMAGE_MONIKER, img.getProject(), img.getEndpoint(), img.getInstanceName(), img.getName()),
                                userInfo.getRoles()))
                .collect(Collectors.toList());
        sharedImages.forEach(img -> img.setSharingStatus(getImageSharingStatus(userInfo.getName(),img)));
        log.info("Shared with user {} images : {}", userInfo.getName(), sharedImages);
        return sharedImages;
    }

    public List<ImageInfoRecord> getSharedImages(UserInfo userInfo, String dockerImage, String project, String endpoint) {
        List<ImageInfoRecord> sharedImages = imageExploratoryDao.getAllImages().stream()
                .filter(img -> img.getStatus().equals(ImageStatus.ACTIVE))
                .filter(img -> !img.getUser().equals(userInfo.getName()))
                .filter(img -> img.getDockerImage().equals(dockerImage) && img.getProject().equals(project) && img.getEndpoint().equals(endpoint))
                .filter(img -> UserRoles.checkAccess(userInfo, RoleType.IMAGE,
                        String.format(IMAGE_MONIKER, img.getProject(), img.getEndpoint(), img.getInstanceName(), img.getName()),
                        userInfo.getRoles()))
                .collect(Collectors.toList());
        sharedImages.forEach(img -> img.setSharingStatus(getImageSharingStatus(userInfo.getName(),img)));
        log.info("Found shared with user {} images {}", userInfo.getName(), sharedImages);
        return sharedImages;
    }

    public List<ImageInfoRecord> getSharedImages(UserInfo userInfo, String project){
        List<ImageInfoRecord> sharedImages = imageExploratoryDao.getAllImages().stream()
                .filter(img -> img.getStatus().equals(ImageStatus.ACTIVE))
                .filter(img -> !img.getUser().equals(userInfo.getName()))
                .filter(img -> img.getProject().equals(project) )
                .filter(img -> UserRoles.checkAccess(userInfo, RoleType.IMAGE,
                        String.format(IMAGE_MONIKER, img.getProject(), img.getEndpoint(), img.getInstanceName(), img.getName()),
                        userInfo.getRoles()))
                .collect(Collectors.toList());
        sharedImages.forEach(img -> img.setSharingStatus(getImageSharingStatus(userInfo.getName(),img)));
        log.info("Found shared with user {} images {}", userInfo.getName(), sharedImages);
        return sharedImages;
    }

    private ImageSharingStatus getImageSharingStatus(String username, ImageInfoRecord image){
        String anyUser = "$anyuser";
        UserRoleDTO role = getImageRole(image);
        boolean roleHasGroups = (role.getGroups().contains(anyUser) && role.getGroups().size() >= 2)
                || (!role.getGroups().contains(anyUser) && !role.getGroups().isEmpty());
        if(!roleHasGroups && image.getUser().equals(username)){
            return ImageSharingStatus.PRIVATE;
        } else if (roleHasGroups && image.getUser().equals(username)){
            return ImageSharingStatus.SHARED;
        }
        return ImageSharingStatus.RECEIVED ;
    }

    private UserRoleDTO getImageRole(ImageInfoRecord image){
        // projectName-endpointName-exploratoryName-imageName
        String imageId = String.format(IMAGE_ROLE,
                image.getProject(), image.getEndpoint(), image.getInstanceName(),image.getName());
       return userRoleDAO.findById(imageId);
    }

    private String getImageMoniker(String project, String endpoint, String exploratoryName, String imageName){
        return String.format(IMAGE_MONIKER, project, endpoint, exploratoryName, imageName);
    }

    private void createImageRole(Image image, String exploratoryName){
        if (image.getStatus().equals(ImageStatus.ACTIVE)){
            UserRoleDTO role = getUserImageRoleFromFile();
            role.setId(String.format(role.getId(), image.getProject(), image.getEndpoint(), exploratoryName ,image.getName()));
            role.setDescription(String.format(role.getDescription(), getImageMoniker(image.getProject(), image.getEndpoint(), exploratoryName, image.getName()).replaceAll("_","-")));
            role.setCloud(endpointService.get(image.getEndpoint()).getCloudProvider());
            role.setImages(new HashSet<>(Collections.singletonList(getImageMoniker(image.getProject(), image.getEndpoint(), exploratoryName, image.getName()))));
            userRoleDAO.insert(role);
        }
    }

    private List<ImageInfoRecord> filterImages(List<ImageInfoRecord> images, ImageFilter filter){
        return images.stream()
                .filter(img -> img.getName().toLowerCase().contains(filter.getImageName().toLowerCase()))
                .filter(img -> CollectionUtils.isEmpty(filter.getStatuses()) || filter.getStatuses().contains(img.getStatus()))
                .filter(img -> CollectionUtils.isEmpty(filter.getEndpoints()) || filter.getEndpoints().contains(img.getEndpoint()))
                .filter(img -> CollectionUtils.isEmpty(filter.getTemplateNames()) || filter.getTemplateNames().contains(img.getTemplateName()))
                .filter(img -> CollectionUtils.isEmpty(filter.getSharingStatuses()) || filter.getSharingStatuses().contains(img.getSharingStatus()))
                .collect(Collectors.toList());

    }
}
