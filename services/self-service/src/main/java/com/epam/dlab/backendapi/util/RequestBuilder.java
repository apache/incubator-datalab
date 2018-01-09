/*
 * Copyright (c) 2017, EPAM SYSTEMS INC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.dlab.backendapi.util;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.SelfServiceApplicationConfiguration;
import com.epam.dlab.backendapi.dao.SettingsDAO;
import com.epam.dlab.backendapi.domain.ExploratoryLibCache;
import com.epam.dlab.backendapi.resources.dto.ComputationalCreateFormDTO;
import com.epam.dlab.backendapi.resources.dto.ExploratoryCreateFormDTO;
import com.epam.dlab.backendapi.resources.dto.SparkStandaloneClusterCreateForm;
import com.epam.dlab.backendapi.resources.dto.aws.AwsComputationalCreateForm;
import com.epam.dlab.cloud.CloudProvider;
import com.epam.dlab.dto.*;
import com.epam.dlab.dto.aws.AwsCloudSettings;
import com.epam.dlab.dto.aws.computational.AwsComputationalTerminateDTO;
import com.epam.dlab.dto.aws.computational.ComputationalCreateAws;
import com.epam.dlab.dto.aws.computational.SparkComputationalCreateAws;
import com.epam.dlab.dto.aws.edge.EdgeCreateAws;
import com.epam.dlab.dto.aws.exploratory.ExploratoryCreateAws;
import com.epam.dlab.dto.aws.keyload.UploadFileAws;
import com.epam.dlab.dto.azure.AzureCloudSettings;
import com.epam.dlab.dto.azure.computational.SparkComputationalCreateAzure;
import com.epam.dlab.dto.azure.edge.EdgeCreateAzure;
import com.epam.dlab.dto.azure.exploratory.ExploratoryActionStartAzure;
import com.epam.dlab.dto.azure.exploratory.ExploratoryActionStopAzure;
import com.epam.dlab.dto.azure.exploratory.ExploratoryCreateAzure;
import com.epam.dlab.dto.azure.keyload.UploadFileAzure;
import com.epam.dlab.dto.base.CloudSettings;
import com.epam.dlab.dto.base.DataEngineType;
import com.epam.dlab.dto.base.computational.ComputationalBase;
import com.epam.dlab.dto.base.edge.EdgeInfo;
import com.epam.dlab.dto.base.keyload.UploadFile;
import com.epam.dlab.dto.computational.ComputationalTerminateDTO;
import com.epam.dlab.dto.computational.UserComputationalResource;
import com.epam.dlab.dto.exploratory.*;
import com.epam.dlab.dto.gcp.GcpCloudSettings;
import com.epam.dlab.dto.gcp.computational.GcpComputationalTerminateDTO;
import com.epam.dlab.dto.gcp.computational.SparkComputationalCreateGcp;
import com.epam.dlab.dto.gcp.edge.EdgeCreateGcp;
import com.epam.dlab.dto.gcp.exploratory.ExploratoryCreateGcp;
import com.epam.dlab.dto.gcp.keyload.UploadFileGcp;
import com.epam.dlab.exceptions.DlabException;
import com.google.inject.Inject;

import java.util.ArrayList;

public class RequestBuilder {
    private static final String UNSUPPORTED_CLOUD_PROVIDER_MESSAGE = "Unsupported cloud provider ";
    private static final String AZURE_REFRESH_TOKEN_KEY = "refresh_token";

    @Inject
    private static SelfServiceApplicationConfiguration configuration;
    @Inject
    private static SettingsDAO settingsDAO;

    private RequestBuilder() {
    }

    private static CloudSettings cloudSettings(UserInfo userInfo) {
        switch (cloudProvider()) {
            case AWS:
                return AwsCloudSettings.builder()
                        .awsRegion(settingsDAO.getAwsRegion())
                        .awsSecurityGroupIds(settingsDAO.getAwsSecurityGroups())
                        .awsSubnetId(settingsDAO.getAwsSubnetId())
                        .awsVpcId(settingsDAO.getAwsVpcId())
                        .confTagResourceId(settingsDAO.getConfTagResourceId())
                        .awsIamUser(userInfo.getName()).build();
            case AZURE:
                return AzureCloudSettings.builder()
                        .azureRegion(settingsDAO.getAzureRegion())
                        .azureResourceGroupName(settingsDAO.getAzureResourceGroupName())
                        .azureSecurityGroupName(settingsDAO.getAzureSecurityGroupName())
                        .azureSubnetName(settingsDAO.getAzureSubnetName())
                        .azureVpcName(settingsDAO.getAzureVpcName())
                        .azureIamUser(userInfo.getName()).build();
            case GCP:
                return GcpCloudSettings.builder().gcpRegion(settingsDAO.getGcpRegion())
                        .gcpProjectId(settingsDAO.getGcpProjectId())
                        .gcpVpcName(settingsDAO.getGcpVpcName())
                        .gcpSubnetName(settingsDAO.getGcpSubnetName())
                        .gcpIamUser(userInfo.getName()).build();
            default:
                throw new IllegalArgumentException(UNSUPPORTED_CLOUD_PROVIDER_MESSAGE + cloudProvider());
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends ResourceBaseDTO<?>> T newResourceBaseDTO(UserInfo userInfo, Class<T> resourceClass) {
        try {
            T resource = resourceClass.newInstance();
            switch (cloudProvider()) {
                case AWS:
                case AZURE:
                case GCP:
                    return (T) resource
                            .withEdgeUserName(userInfo.getSimpleName())
                            .withCloudSettings(cloudSettings(userInfo));
                default:
                    throw new DlabException(UNSUPPORTED_CLOUD_PROVIDER_MESSAGE + cloudProvider());
            }
        } catch (Exception e) {
            throw new DlabException("Cannot create instance of resource class " + resourceClass.getName() + ". " +
                    e.getLocalizedMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends ResourceSysBaseDTO<?>> T newResourceSysBaseDTO(UserInfo userInfo, Class<T> resourceClass) {
        T resource = newResourceBaseDTO(userInfo, resourceClass);
        return (T) resource
                .withServiceBaseName(settingsDAO.getServiceBaseName())
                .withConfOsFamily(settingsDAO.getConfOsFamily());
    }

    @SuppressWarnings("unchecked")
    public static UploadFile newEdgeKeyUpload(UserInfo userInfo, String content, EdgeInfo edgeInfo) {

        switch (cloudProvider()) {
            case AWS:
                EdgeCreateAws edgeCreateAws = newResourceSysBaseDTO(userInfo, EdgeCreateAws.class);
                if (edgeInfo != null) {
                    edgeCreateAws.setEdgeElasticIp(edgeInfo.getPublicIp());
                }
                UploadFileAws uploadFileAws = new UploadFileAws();
                uploadFileAws.setEdge(edgeCreateAws);
                uploadFileAws.setContent(content);

                return uploadFileAws;

            case AZURE:
                EdgeCreateAzure edgeCreateAzure = newResourceSysBaseDTO(userInfo, EdgeCreateAzure.class)
                        .withAzureDataLakeEnable(Boolean.toString(settingsDAO.isAzureDataLakeEnabled()));

                UploadFileAzure uploadFileAzure = new UploadFileAzure();
                uploadFileAzure.setEdge(edgeCreateAzure);
                uploadFileAzure.setContent(content);

                return uploadFileAzure;

            case GCP:
                return new UploadFileGcp(newResourceSysBaseDTO(userInfo, EdgeCreateGcp.class), content);
            default:
                throw new IllegalArgumentException(UNSUPPORTED_CLOUD_PROVIDER_MESSAGE + cloudProvider());
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends ResourceSysBaseDTO<?>> T newEdgeAction(UserInfo userInfo) {
        switch (cloudProvider()) {
            case AWS:
            case AZURE:
            case GCP:
                return (T) newResourceSysBaseDTO(userInfo, ResourceSysBaseDTO.class);
            default:
                throw new IllegalArgumentException(UNSUPPORTED_CLOUD_PROVIDER_MESSAGE + cloudProvider());
        }
    }

    public static UserEnvironmentResources newUserEnvironmentStatus(UserInfo userInfo) {
        switch (cloudProvider()) {
            case AWS:
            case AZURE:
            case GCP:
                return newResourceSysBaseDTO(userInfo, UserEnvironmentResources.class);
            default:
                throw new IllegalArgumentException(UNSUPPORTED_CLOUD_PROVIDER_MESSAGE + cloudProvider());
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends ExploratoryCreateDTO<T>> T newExploratoryCreate(ExploratoryCreateFormDTO formDTO, UserInfo userInfo,
                                                                             ExploratoryGitCredsDTO exploratoryGitCredsDTO) {

        T exploratoryCreate;

        switch (cloudProvider()) {
            case AWS:
                exploratoryCreate = (T) newResourceSysBaseDTO(userInfo, ExploratoryCreateAws.class)
                        .withNotebookInstanceType(formDTO.getShape());
                break;
            case AZURE:
                exploratoryCreate = (T) newResourceSysBaseDTO(userInfo, ExploratoryCreateAzure.class)
                        .withNotebookInstanceSize(formDTO.getShape());
                if (settingsDAO.isAzureDataLakeEnabled()) {
                    ((ExploratoryCreateAzure) exploratoryCreate)
                            .withAzureClientId(settingsDAO.getAzureDataLakeClientId())
                            .withAzureUserRefreshToken(userInfo.getKeys().get(AZURE_REFRESH_TOKEN_KEY));
                }

                ((ExploratoryCreateAzure) exploratoryCreate)
                        .withAzureDataLakeEnabled(Boolean.toString(settingsDAO.isAzureDataLakeEnabled()));
                break;
            case GCP:
                exploratoryCreate = (T) newResourceSysBaseDTO(userInfo, ExploratoryCreateGcp.class)
                        .withNotebookInstanceType(formDTO.getShape());
                break;

            default:
                throw new IllegalArgumentException(UNSUPPORTED_CLOUD_PROVIDER_MESSAGE + cloudProvider());
        }

        return exploratoryCreate.withExploratoryName(formDTO.getName())
                .withNotebookImage(formDTO.getImage())
                .withApplicationName(getApplicationNameFromImage(formDTO.getImage()))
                .withGitCreds(exploratoryGitCredsDTO.getGitCreds());
    }

    @SuppressWarnings("unchecked")
    public static <T extends ExploratoryGitCredsUpdateDTO> T newExploratoryStart(UserInfo userInfo, UserInstanceDTO userInstance,
                                                                                 ExploratoryGitCredsDTO exploratoryGitCredsDTO) {

        switch (cloudProvider()) {
            case AWS:
                return (T) newResourceSysBaseDTO(userInfo, ExploratoryGitCredsUpdateDTO.class)
                        .withNotebookInstanceName(userInstance.getExploratoryId())
                        .withGitCreds(exploratoryGitCredsDTO.getGitCreds())
                        .withNotebookImage(userInstance.getImageName())
                        .withExploratoryName(userInstance.getExploratoryName());
            case AZURE:
                T exploratoryStart = (T) newResourceSysBaseDTO(userInfo, ExploratoryActionStartAzure.class)
                        .withNotebookInstanceName(userInstance.getExploratoryId())
                        .withGitCreds(exploratoryGitCredsDTO.getGitCreds())
                        .withNotebookImage(userInstance.getImageName())
                        .withExploratoryName(userInstance.getExploratoryName());

                if (settingsDAO.isAzureDataLakeEnabled()) {
                    ((ExploratoryActionStartAzure) exploratoryStart)
                            .withAzureClientId(settingsDAO.getAzureDataLakeClientId())
                            .withAzureUserRefreshToken(userInfo.getKeys().get(AZURE_REFRESH_TOKEN_KEY));
                }

                ((ExploratoryActionStartAzure) exploratoryStart)
                        .withAzureDataLakeEnabled(Boolean.toString(settingsDAO.isAzureDataLakeEnabled()));

                return exploratoryStart;
            default:
                throw new IllegalArgumentException(UNSUPPORTED_CLOUD_PROVIDER_MESSAGE + cloudProvider());
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends ExploratoryActionDTO<T>> T newExploratoryStop(UserInfo userInfo, UserInstanceDTO userInstance) {

        T exploratoryStop;

        switch (cloudProvider()) {
            case AWS:
                exploratoryStop = (T) newResourceSysBaseDTO(userInfo, ExploratoryActionDTO.class);
                break;
            case AZURE:
                exploratoryStop = (T) newResourceSysBaseDTO(userInfo, ExploratoryActionStopAzure.class);
                break;
            default:
                throw new IllegalArgumentException(UNSUPPORTED_CLOUD_PROVIDER_MESSAGE + cloudProvider());
        }

        return exploratoryStop
                .withNotebookInstanceName(userInstance.getExploratoryId())
                .withNotebookImage(userInstance.getImageName())
                .withExploratoryName(userInstance.getExploratoryName())
                .withNotebookImage(userInstance.getImageName());
    }

    public static ExploratoryGitCredsUpdateDTO newGitCredentialsUpdate(UserInfo userInfo, UserInstanceDTO instanceDTO,
                                                                       ExploratoryGitCredsDTO exploratoryGitCredsDTO) {

        switch (cloudProvider()) {
            case AWS:
            case AZURE:
                return newResourceSysBaseDTO(userInfo, ExploratoryGitCredsUpdateDTO.class)
                        .withNotebookImage(instanceDTO.getImageName())
                        .withApplicationName(getApplicationNameFromImage(instanceDTO.getImageName()))
                        .withNotebookInstanceName(instanceDTO.getExploratoryId())
                        .withExploratoryName(instanceDTO.getExploratoryName())
                        .withGitCreds(exploratoryGitCredsDTO.getGitCreds());

            default:
                throw new IllegalArgumentException(UNSUPPORTED_CLOUD_PROVIDER_MESSAGE + cloudProvider());
        }
    }

    public static LibraryInstallDTO newLibInstall(UserInfo userInfo, UserInstanceDTO userInstance) {

        switch (cloudProvider()) {
            case AWS:
            case AZURE:
                return newResourceSysBaseDTO(userInfo, LibraryInstallDTO.class)
                        .withNotebookImage(userInstance.getImageName())
                        .withApplicationName(getApplicationNameFromImage(userInstance.getImageName()))
                        .withNotebookInstanceName(userInstance.getExploratoryId())
                        .withExploratoryName(userInstance.getExploratoryName())
                        .withLibs(new ArrayList<>());

            default:
                throw new IllegalArgumentException(UNSUPPORTED_CLOUD_PROVIDER_MESSAGE + cloudProvider());
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends ExploratoryActionDTO<T>> T newLibExploratoryList(UserInfo userInfo, UserInstanceDTO userInstance) {

        switch (cloudProvider()) {
            case AWS:
            case AZURE:
            case GCP:
                return (T) newResourceSysBaseDTO(userInfo, ExploratoryActionDTO.class)
                        .withNotebookInstanceName(userInstance.getExploratoryId())
                        .withNotebookImage(userInstance.getImageName())
                        .withApplicationName(getApplicationNameFromImage(userInstance.getImageName()))
                        .withExploratoryName(userInstance.getExploratoryName());

            default:
                throw new IllegalArgumentException(UNSUPPORTED_CLOUD_PROVIDER_MESSAGE + cloudProvider());
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends LibraryInstallDTO> T newLibInstall(UserInfo userInfo, UserInstanceDTO userInstance,
                                                                UserComputationalResource computationalResource) {

        switch (cloudProvider()) {
            case AWS:
            case AZURE:
                return (T) newResourceSysBaseDTO(userInfo, LibraryInstallDTO.class)
                        .withComputationalId(computationalResource.getComputationalId())
                        .withComputationalName(computationalResource.getComputationalName())
                        .withExploratoryName(userInstance.getExploratoryName())
                        .withComputationalImage(computationalResource.getImageName())
                        .withApplicationName(getApplicationNameFromImage(userInstance.getImageName()))
                        .withLibs(new ArrayList<>());

            default:
                throw new IllegalArgumentException(UNSUPPORTED_CLOUD_PROVIDER_MESSAGE + cloudProvider());
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends LibListComputationalDTO> T newLibComputationalList(UserInfo userInfo, UserInstanceDTO userInstance,
                                                                                UserComputationalResource computationalResource) {

        switch (cloudProvider()) {
            case AWS:
            case AZURE:
            case GCP:
                return (T) newResourceSysBaseDTO(userInfo, LibListComputationalDTO.class)
                        .withComputationalId(computationalResource.getComputationalId())
                        .withComputationalImage(computationalResource.getImageName())
                        .withLibCacheKey(ExploratoryLibCache.libraryCacheKey(userInstance))
                        .withApplicationName(getApplicationNameFromImage(userInstance.getImageName()));

            default:
                throw new IllegalArgumentException(UNSUPPORTED_CLOUD_PROVIDER_MESSAGE + cloudProvider());
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends ComputationalBase<T>> T newComputationalCreate(UserInfo userInfo,
                                                                            UserInstanceDTO userInstance,
                                                                            ComputationalCreateFormDTO form) {
        T computationalCreate;

        switch (cloudProvider()) {
            case AWS:
            case AZURE:
                AwsComputationalCreateForm awsForm = (AwsComputationalCreateForm) form;
                computationalCreate = (T) newResourceSysBaseDTO(userInfo, ComputationalCreateAws.class)
                        .withInstanceCount(awsForm.getInstanceCount())
                        .withMasterInstanceType(awsForm.getMasterInstanceType())
                        .withSlaveInstanceType(awsForm.getSlaveInstanceType())
                        .withSlaveInstanceSpot(awsForm.getSlaveInstanceSpot())
                        .withSlaveInstanceSpotPctPrice(awsForm.getSlaveInstanceSpotPctPrice())
                        .withVersion(awsForm.getVersion());
                break;
            default:
                throw new IllegalArgumentException(UNSUPPORTED_CLOUD_PROVIDER_MESSAGE + cloudProvider());
        }

        return computationalCreate
                .withExploratoryName(form.getNotebookName())
                .withComputationalName(form.getName())
                .withNotebookTemplateName(userInstance.getTemplateName())
                .withApplicationName(getApplicationNameFromImage(userInstance.getImageName()))
                .withNotebookInstanceName(userInstance.getExploratoryId());
    }

    @SuppressWarnings("unchecked")
    public static <T extends ComputationalBase<T>> T newComputationalCreate(UserInfo userInfo,
                                                                            UserInstanceDTO userInstance,
                                                                            SparkStandaloneClusterCreateForm form) {

        T computationalCreate;

        switch (cloudProvider()) {
            case AWS:
                computationalCreate = (T) newResourceSysBaseDTO(userInfo, SparkComputationalCreateAws.class)
                        .withDataEngineInstanceCount(form.getDataEngineInstanceCount())
                        .withDataEngineMasterShape(form.getDataEngineMaster())
                        .withDataEngineSlaveShape(form.getDataEngineSlave());
                break;
            case AZURE:
                computationalCreate = (T) newResourceSysBaseDTO(userInfo, SparkComputationalCreateAzure.class)
                        .withDataEngineInstanceCount(form.getDataEngineInstanceCount())
                        .withDataEngineMasterSize(form.getDataEngineMaster())
                        .withDataEngineSlaveSize(form.getDataEngineSlave());

                if (settingsDAO.isAzureDataLakeEnabled()) {
                    ((SparkComputationalCreateAzure) computationalCreate)
                            .withAzureClientId(settingsDAO.getAzureDataLakeClientId())
                            .withAzureUserRefreshToken(userInfo.getKeys().get(AZURE_REFRESH_TOKEN_KEY));
                }

                ((SparkComputationalCreateAzure) computationalCreate)
                        .withAzureDataLakeEnabled(Boolean.toString(settingsDAO.isAzureDataLakeEnabled()));

                break;
            case GCP:
                computationalCreate = (T) newResourceSysBaseDTO(userInfo, SparkComputationalCreateGcp.class)
                        .withDataEngineInstanceCount(form.getDataEngineInstanceCount())
                        .withDataEngineMasterSize(form.getDataEngineMaster())
                        .withDataEngineSlaveSize(form.getDataEngineSlave());
                break;
            default:
                throw new IllegalArgumentException(UNSUPPORTED_CLOUD_PROVIDER_MESSAGE + cloudProvider());
        }

        return computationalCreate
                .withExploratoryName(form.getNotebookName())
                .withComputationalName(form.getName())
                .withNotebookTemplateName(userInstance.getTemplateName())
                .withApplicationName(getApplicationNameFromImage(userInstance.getImageName()))
                .withNotebookInstanceName(userInstance.getExploratoryId());
    }

    @SuppressWarnings("unchecked")
    public static <T extends ComputationalBase<T>> T newComputationalTerminate(UserInfo userInfo,
                                                                               String exploratoryName,
                                                                               String exploratoryId,
                                                                               String computationalName,
                                                                               String computationalId,
                                                                               DataEngineType dataEngineType) {
        T computationalTerminate;

        switch (cloudProvider()) {
            case AWS:
                AwsComputationalTerminateDTO terminateDTO = newResourceSysBaseDTO(userInfo, AwsComputationalTerminateDTO.class);
                if (dataEngineType == DataEngineType.CLOUD_SERVICE) {
                    terminateDTO.setClusterName(computationalId);
                }
                computationalTerminate = (T) terminateDTO;
                break;
            case AZURE:
                computationalTerminate = (T) newResourceSysBaseDTO(userInfo, ComputationalTerminateDTO.class);
                break;
            case GCP:
                GcpComputationalTerminateDTO gcpTerminateDTO = newResourceSysBaseDTO(userInfo, GcpComputationalTerminateDTO.class);
                if (dataEngineType == DataEngineType.CLOUD_SERVICE) {
                    gcpTerminateDTO.setClusterName(computationalId);
                }
                computationalTerminate = (T) gcpTerminateDTO;
                break;

            default:
                throw new IllegalArgumentException(UNSUPPORTED_CLOUD_PROVIDER_MESSAGE + cloudProvider());
        }

        return computationalTerminate
                .withExploratoryName(exploratoryName)
                .withComputationalName(computationalName)
                .withNotebookInstanceName(exploratoryId);
    }

    private static CloudProvider cloudProvider() {
        return configuration.getCloudProvider();
    }

    /**
     * Returns application name basing on docker image
     *
     * @param imageName docker image name
     * @return application name
     */
    private static String getApplicationNameFromImage(String imageName) {
        if (imageName != null) {
            int pos = imageName.lastIndexOf('-');
            if (pos > 0) {
                return imageName.substring(pos + 1);
            }
        }
        return "";
    }
}



