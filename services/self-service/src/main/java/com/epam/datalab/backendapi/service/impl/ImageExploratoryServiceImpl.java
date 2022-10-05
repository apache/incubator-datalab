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
import com.epam.datalab.backendapi.resources.dto.*;
import com.epam.datalab.backendapi.roles.RoleType;
import com.epam.datalab.backendapi.roles.UserRoles;
import com.epam.datalab.backendapi.service.EndpointService;
import com.epam.datalab.backendapi.service.ImageExploratoryService;
import com.epam.datalab.backendapi.service.ProjectService;
import com.epam.datalab.backendapi.util.RequestBuilder;
import com.epam.datalab.constants.ServiceConsts;
import com.epam.datalab.dto.SharedWith;
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
import com.google.common.collect.Lists;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.epam.datalab.backendapi.domain.AuditActionEnum.*;
import static com.epam.datalab.backendapi.domain.AuditResourceTypeEnum.IMAGE;

@Singleton
@Slf4j
public class ImageExploratoryServiceImpl implements ImageExploratoryService {
    private static final String IMAGE_EXISTS_MSG = "Image with name %s is already exist in project %s";
    private static final String IMAGE_NOT_FOUND_FOR_USER_MSG = "Image with name %s was not found for user %s";
    private static final String IMAGE_NOT_FOUND_MSG = "Image with name %s was not found ";

    private static final String SHARE_OWN_IMAGES_PAGE = "/api/image/share";
    private static final String TERMINATE_OWN_IMAGES_PAGE = "/api/image/terminate";

    private static final String CREATE_NOTEBOOK_BASED_ON_OWN_IMAGES = "/api/exploratory/createFromOwnCustomImage";
    private static final String CREATE_NOTEBOOK_BASED_ON_SHARED_IMAGES = "/api/exploratory/createFromSharedCustomImage";

    private static final String AUDIT_SHARE_IMAGE_WITH_GROUPS = "Add group(s): %s\n";
    private static final String AUDIT_SHARE_IMAGE_WITH_USERS = "Add user(s): %s\n";
    private static final String AUDIT_STOP_SHARE_IMAGE_WITH_GROUPS = "Remove group(s): %s\n";
    private static final String AUDIT_STOP_SHARE_IMAGE_WITH_USERS = "Remove user(s): %s\n";

    @Inject
    private ExploratoryDAO exploratoryDAO;
    @Inject
    private ImageExploratoryDAO imageExploratoryDao;
    @Inject
    private ExploratoryLibDAO libDAO;
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
    @Inject
    private UserSettingsDAO userSettingsDAO;

    @Inject
    private SecurityDAO securityDAO;

    @Override
    public boolean imageExistInProject(String imageName, String project) {
        return imageExploratoryDao.exist(imageName, project);
    }

    @Audit(action = CREATE, type = IMAGE)
    @Override
    public String createImage(@User UserInfo user, @Project String project, String exploratoryName, @ResourceName String imageName, String imageDescription) {
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
                .sharedWith(new SharedWith())
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

    @Audit(action = TERMINATE, type = IMAGE)
    @Override
    public void terminateImage(@User UserInfo user, @Project String project, String endpoint, @ResourceName String imageName) {
        Optional<ImageInfoRecord> image = imageExploratoryDao.getImage(user.getName(), imageName, project, endpoint);
        if (image.isPresent()) {
            ImageInfoRecord imageInfoRecord = image.get();
            imageExploratoryDao.updateImageStatus(user.getName(), imageName, project, endpoint, ImageStatus.TERMINATING);
            EndpointDTO endpointDTO = endpointService.get(endpoint);
            ProjectDTO projectDTO = projectService.get(project);
            UserInstanceDTO userInstance = exploratoryDAO.fetchExploratoryFields(user.getName(), project, imageInfoRecord.getInstanceName());

            log.info("ExploratoryImageCreate {}", requestBuilder.newExploratoryImageCreate(user, userInstance, imageName, endpointDTO, projectDTO));
            provisioningService.post(endpointDTO.getUrl() + ExploratoryAPI.EXPLORATORY_IMAGE_TERMINATE,
                    user.getAccessToken(), requestBuilder.newExploratoryImageCreate(user, userInstance, imageName, endpointDTO, projectDTO)
                    , String.class);
        }

    }

    @Override
    public void finishTerminateImage(String imageName, String projectName, String endpoint) {
        imageExploratoryDao.updateImageStatus(imageName, projectName, endpoint, ImageStatus.TERMINATED);
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

        if (newNotebookIp != null) {
            log.debug("Changing exploratory ip with name {} for user {} to {}", exploratoryName, image.getUser(),
                    newNotebookIp);
            exploratoryDAO.updateExploratoryIp(image.getUser(), image.getProject(), newNotebookIp, exploratoryName);
        }

    }

    @Override
    public List<ImageInfoDTO> getNotFailedImages(UserInfo user, String dockerImage, String project, String endpoint) {
        List<ImageInfoRecord> images = new ArrayList<>();
        if (UserRoles.checkAccess(user, RoleType.PAGE, CREATE_NOTEBOOK_BASED_ON_OWN_IMAGES, user.getRoles())) {
            images.addAll(imageExploratoryDao.getImages(user.getName(), dockerImage, project, endpoint, ImageStatus.ACTIVE, ImageStatus.CREATING));
        }
        if (UserRoles.checkAccess(user, RoleType.PAGE, CREATE_NOTEBOOK_BASED_ON_SHARED_IMAGES, user.getRoles())) {
            images.addAll(getSharedImages(user, dockerImage, project, endpoint));
        }
        List<ImageInfoDTO> imageInfoDTOs = images.stream().map(img -> toImageInfoDTO(img, user)).collect(Collectors.toList());
        return removeSharedWithIfNotOwner(user,imageInfoDTOs);
    }

    @Override
    public ImageInfoRecord getImage(String user, String name, String project, String endpoint) {
        return imageExploratoryDao.getImage(user, name, project, endpoint).orElseThrow(() ->
                new ResourceNotFoundException(String.format(IMAGE_NOT_FOUND_FOR_USER_MSG, name, user)));
    }

    @Override
    public ImageInfoRecord getImage(String name, String project, String endpoint) {
        return imageExploratoryDao.getImage(name, project, endpoint).orElseThrow(() ->
                new ResourceNotFoundException(String.format(IMAGE_NOT_FOUND_MSG, name)));
    }

    @Override
    public List<ImageInfoRecord> getImagesForProject(String project) {
        return imageExploratoryDao.getImagesForProject(project);
    }

    @Override
    public ImagesPageInfo getImagesOfUser(UserInfo user, ImageFilter imageFilter) {
        log.debug("Loading list of images for user {}", user.getName());
        List<ImageInfoRecord> images = imageExploratoryDao.getImagesOfUser(user.getName());

        List<ImageInfoDTO> imagesDTOs = images.stream().map(img -> toImageInfoDTO(img, user)).collect(Collectors.toList());
        imagesDTOs.addAll(getSharedImages(user).stream().map(img -> toImageInfoDTO(img, user)).collect(Collectors.toList()));
        ImageFilterFormData filterData = getDataForFilter(imagesDTOs);

        if (imageFilter == null) {
            if (userSettingsDAO.getImageFilter(user.getName()).isPresent()) {
                imageFilter = userSettingsDAO.getImageFilter(user.getName()).get();
                imagesDTOs = filterImages(imagesDTOs, imageFilter);
            } else {
                imageFilter = new ImageFilter();
                userSettingsDAO.setUserImageFilter(user.getName(), imageFilter);
            }
        } else {
            imagesDTOs = filterImages(imagesDTOs, imageFilter);
            userSettingsDAO.setUserImageFilter(user.getName(), imageFilter);
        }

        // do not show sharing if not owner
        final List<ImageInfoDTO> finalImages = removeSharedWithIfNotOwner(user, imagesDTOs);
        List<ProjectImagesInfo> projectImagesInfoList = projectService.getUserProjects(user, Boolean.FALSE)
                .stream()
                .map(p -> {
                    List<ImageInfoDTO> im = finalImages.stream().filter(img -> img.getProject().equals(p.getName())).collect(Collectors.toList());
                    return ProjectImagesInfo.builder()
                            .project(p.getName())
                            .images(im)
                            .build();
                })
                .collect(Collectors.toList());
        return ImagesPageInfo.builder()
                .projectImagesInfos(projectImagesInfoList)
                .filterData(filterData)
                .imageFilter(imageFilter)
                .build();
    }

    @Override
    public void updateImageSharing(UserInfo user, ImageShareDTO imageShareDTO){
        String info = updateImageSharingAudit(imageShareDTO);
        shareImage(user, imageShareDTO.getImageName(), imageShareDTO.getProjectName(), imageShareDTO.getEndpoint(), imageShareDTO.getSharedWith(), info);
    }

    @Override
    public SharingInfo getSharingInfo(String userName, String imageName, String project, String endpoint) {
        SharingInfo sharingInfo = new SharingInfo();
        sharingInfo.setSharedWith(getImageSharingInfo(userName, imageName, project, endpoint));
        sharingInfo.setCanBeSharedWith(getUsersAndGroupsForSharing(userName, imageName, project, endpoint, ""));
        return sharingInfo;
    }

    @Override
    public List<ImageInfoRecord> getSharedImages(UserInfo userInfo) {
        return imageExploratoryDao.getAllImages().stream()
                .filter(img -> !img.getUser().equals(userInfo.getName()))
                .filter(img -> hasAccess(userInfo.getName(), img.getSharedWith()))
                .collect(Collectors.toList());
    }

    @Override
    public List<ImageInfoRecord> getSharedImages(UserInfo userInfo, String dockerImage, String project, String endpoint) {
        List<ImageInfoRecord> sharedImages = imageExploratoryDao.getAllImages().stream()
                .filter(img -> img.getStatus().equals(ImageStatus.ACTIVE))
                .filter(img -> !img.getUser().equals(userInfo.getName()))
                .filter(img -> img.getDockerImage().equals(dockerImage) && img.getProject().equals(project) && img.getEndpoint().equals(endpoint))
                .filter(img -> hasAccess(userInfo.getName(), img.getSharedWith()))
                .collect(Collectors.toList());
        log.info("Found shared with user {} images {}", userInfo.getName(), sharedImages);
        return sharedImages;
    }

    @Override
    public ImageUserPermissions getUserImagePermissions(UserInfo userInfo, ImageInfoRecord image) {
        boolean canShare = image.getStatus().equals(ImageStatus.ACTIVE) && image.getUser().equals(userInfo.getName())
                && UserRoles.checkAccess(userInfo, RoleType.PAGE, SHARE_OWN_IMAGES_PAGE, userInfo.getRoles());
        boolean canTerminate = (image.getStatus().equals(ImageStatus.ACTIVE) || image.getStatus().equals(ImageStatus.FAILED)) &&
                (image.getUser().equals(userInfo.getName())
                        && UserRoles.checkAccess(userInfo, RoleType.PAGE, TERMINATE_OWN_IMAGES_PAGE, userInfo.getRoles()));
        return new ImageUserPermissions(canShare, canTerminate);
    }

    @Override
    public boolean canCreateFromImage(UserInfo userInfo, String imageName, String project, String endpoint) {
        ImageInfoRecord image = getImage(imageName, project, endpoint);
        if (image.getUser().equals(userInfo.getName())) {
            return UserRoles.checkAccess(userInfo, RoleType.PAGE, CREATE_NOTEBOOK_BASED_ON_OWN_IMAGES, userInfo.getRoles());
        } else {
            return hasAccess(userInfo.getName(), image.getSharedWith())
                    && UserRoles.checkAccess(userInfo, RoleType.PAGE, CREATE_NOTEBOOK_BASED_ON_SHARED_IMAGES, userInfo.getRoles());
        }
    }

    @Override
    public Set<SharedWithDTO> getUsersAndGroupsForSharing(String userName, String imageName, String project, String endpoint, String value) {
        Set<SharedWithDTO> sharedWith = getImageSharingInfo(userName, imageName, project, endpoint);

        // Find users in groups
        Map<String, Set<String>> groupsAndUsers = securityDAO.getGroups();
        final Set<String> usersInGroups = new HashSet<>();
        groupsAndUsers.entrySet().stream().forEach(entry -> usersInGroups.addAll(entry.getValue()));
        Set<String> usersFiltered = usersInGroups.stream()
                .filter(name -> name.toLowerCase().contains(value.toLowerCase())).collect(Collectors.toSet());
        // Find users who logged in
        usersFiltered.addAll(securityDAO.getUserNames(value));

        Set<SharedWithDTO> canBeSharedWith = usersFiltered.stream()
                .map(s -> new SharedWithDTO(SharedWithDTO.Type.USER, s)).collect(Collectors.toSet());

        canBeSharedWith.addAll(userGroupDAO.getGroupNames(value).stream()
                .map(s -> new SharedWithDTO(SharedWithDTO.Type.GROUP, s)).collect(Collectors.toSet()));

        canBeSharedWith.removeAll(sharedWith);
        return new TreeSet<>(canBeSharedWith);
    }

    @Audit(action = UPDATE_SHARING, type = IMAGE)
    public void shareImage(@User UserInfo user, @ResourceName String imageName, @Project String projectName, String endpoint,
                            Set<SharedWithDTO> sharedWithDTOS, @Info String info) {
        Optional<ImageInfoRecord> image = imageExploratoryDao.getImage(user.getName(), imageName, projectName, endpoint);
        image.ifPresent(img -> {
            log.info("image {}", img);
            imageExploratoryDao.updateSharing(toSharedWith(sharedWithDTOS), img.getName(), img.getProject(), img.getEndpoint());
        });
    }

    private boolean hasAccess(String userName, SharedWith sharedWith) {
        boolean accessByUserName = sharedWith.getUsers().contains(userName);
        boolean accessByGroup = sharedWith.getGroups().stream().anyMatch(groupName -> userGroupDAO.getUsers(groupName).contains(userName));
        return accessByUserName || accessByGroup;
    }

    private Set<SharedWithDTO> toSharedWithDTOs(SharedWith sharedWith) {
        Set<SharedWithDTO> sharedWithDTO = sharedWith.getGroups().stream()
                .map(s -> new SharedWithDTO(SharedWithDTO.Type.GROUP, s)).collect(Collectors.toSet());
        Set<SharedWithDTO> users = sharedWith.getUsers().stream()
                .map(s -> new SharedWithDTO(SharedWithDTO.Type.USER, s)).collect(Collectors.toSet());
        sharedWithDTO.addAll(users);
        return sharedWithDTO;

    }

    private SharedWith toSharedWith(Set<SharedWithDTO> dtos) {
        SharedWith sharedWith = new SharedWith();
        dtos.forEach(dto -> {
            if (dto.getType().equals(SharedWithDTO.Type.GROUP)) {
                sharedWith.getGroups().add(dto.getValue());
            } else sharedWith.getUsers().add(dto.getValue());
        });
        return sharedWith;
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

    private Set<SharedWithDTO> getImageSharingInfo(String userName, String imageName, String project, String endpoint) {
        Optional<ImageInfoRecord> image = imageExploratoryDao.getImage(userName, imageName, project, endpoint);
        if (image.isPresent()) {
            return toSharedWithDTOs(image.get().getSharedWith());
        } else {
            throw new ResourceNotFoundException(IMAGE_NOT_FOUND_FOR_USER_MSG);
        }
    }

    private ImageSharingStatus getImageSharingStatus(String username, ImageInfoRecord image) {
        boolean notShared = image.getSharedWith().getUsers().isEmpty() && image.getSharedWith().getGroups().isEmpty();
        if (notShared && image.getUser().equals(username)) {
            return ImageSharingStatus.PRIVATE;
        } else if (!notShared && image.getUser().equals(username)) {
            return ImageSharingStatus.SHARED;
        }
        return ImageSharingStatus.RECEIVED;
    }

    private List<ImageInfoDTO> removeSharedWithIfNotOwner(UserInfo user, List<ImageInfoDTO> imageInfoDTOS){
        imageInfoDTOS.forEach(img -> {
            if(!img.getUser().equals(user.getName())){
                img.setSharedWith(new SharedWith());
            }
        });
        return imageInfoDTOS;
    }

    private List<ImageInfoDTO> filterImages(List<ImageInfoDTO> images, ImageFilter filter) {
        return images.stream()
                .filter(img -> img.getName().toLowerCase().contains(filter.getImageName().toLowerCase()))
                .filter(img -> CollectionUtils.isEmpty(filter.getStatuses()) || filter.getStatuses().contains(img.getStatus()))
                .filter(img -> CollectionUtils.isEmpty(filter.getEndpoints()) || filter.getEndpoints().contains(img.getEndpoint()))
                .filter(img -> CollectionUtils.isEmpty(filter.getTemplateNames()) || filter.getTemplateNames().contains(img.getTemplateName()))
                .filter(img -> CollectionUtils.isEmpty(filter.getSharingStatuses()) || filter.getSharingStatuses().contains(img.getSharingStatus()))
                .collect(Collectors.toList());

    }

    private ImageFilterFormData getDataForFilter(List<ImageInfoDTO> images) {
        ImageFilterFormData filterData = new ImageFilterFormData();
        filterData.setImageNames(images.stream().map(ImageInfoDTO::getName).collect(Collectors.toSet()));
        filterData.setStatuses(images.stream().map(ImageInfoDTO::getStatus).collect(Collectors.toSet()));
        filterData.setEndpoints(images.stream().map(ImageInfoDTO::getEndpoint).collect(Collectors.toSet()));
        filterData.setTemplateNames(images.stream().map(ImageInfoDTO::getTemplateName).collect(Collectors.toSet()));
        filterData.setSharingStatuses(images.stream().map(ImageInfoDTO::getSharingStatus).collect(Collectors.toSet()));
        return filterData;
    }

    private String updateImageSharingAudit(ImageShareDTO imageShareDTO){
        StringBuilder audit = new StringBuilder();
        imageExploratoryDao.getImage(imageShareDTO.getImageName(), imageShareDTO.getProjectName(), imageShareDTO.getEndpoint()).ifPresent((img)->{
            Set<String> oldGroups = new TreeSet<>(img.getSharedWith().getGroups());
            Set<String> oldUsers = new TreeSet<>(img.getSharedWith().getUsers());
            SharedWith oldSharedWith = img.getSharedWith();
            SharedWith newSharedWith = toSharedWith(imageShareDTO.getSharedWith());

            oldSharedWith.getGroups().removeAll(newSharedWith.getGroups());
            oldSharedWith.getUsers().removeAll(newSharedWith.getUsers());

            newSharedWith.getGroups().removeAll(oldGroups);
            newSharedWith.getUsers().removeAll(oldUsers);

            if(!oldSharedWith.getGroups().isEmpty()){
                audit.append(String.format(AUDIT_STOP_SHARE_IMAGE_WITH_GROUPS, String.join(", ", oldSharedWith.getGroups())));
            }
            if(!oldSharedWith.getUsers().isEmpty()){
                audit.append(String.format(AUDIT_STOP_SHARE_IMAGE_WITH_USERS, String.join(", ", oldSharedWith.getUsers())));
            }
            if(!newSharedWith.getGroups().isEmpty()){
                audit.append(String.format(AUDIT_SHARE_IMAGE_WITH_GROUPS, String.join(", ", newSharedWith.getGroups())));
            }
            if(!newSharedWith.getUsers().isEmpty()){
                audit.append(String.format(AUDIT_SHARE_IMAGE_WITH_USERS, String.join(", ", newSharedWith.getUsers())));
            }

        });
        return audit.toString();
    }

    private ImageInfoDTO toImageInfoDTO(ImageInfoRecord imageInfoRecord, UserInfo userInfo){
        return ImageInfoDTO.builder()
                .name(imageInfoRecord.getName())
                .timestamp(imageInfoRecord.getTimestamp())
                .description(imageInfoRecord.getDescription())
                .project(imageInfoRecord.getProject())
                .endpoint(imageInfoRecord.getEndpoint())
                .user(imageInfoRecord.getUser())
                .application(imageInfoRecord.getApplication())
                .templateName(imageInfoRecord.getTemplateName())
                .instanceName(imageInfoRecord.getInstanceName())
                .cloudProvider(imageInfoRecord.getCloudProvider())
                .dockerImage(imageInfoRecord.getDockerImage())
                .fullName(imageInfoRecord.getFullName())
                .status(imageInfoRecord.getStatus())
                .sharingStatus(getImageSharingStatus(userInfo.getName(), imageInfoRecord))
                .imageUserPermissions(getUserImagePermissions(userInfo, imageInfoRecord))
                .sharedWith(imageInfoRecord.getSharedWith())
                .clusterConfig(imageInfoRecord.getClusterConfig())
                .exploratoryURL(imageInfoRecord.getExploratoryURL())
                .libraries(imageInfoRecord.getLibraries())
                .computationalLibraries(imageInfoRecord.getComputationalLibraries())
        .build();
    }
}
