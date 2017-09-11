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
import com.epam.dlab.backendapi.core.UserInstanceDTO;
import com.epam.dlab.backendapi.dao.SettingsDAO;
import com.epam.dlab.backendapi.resources.dto.ExploratoryCreateFormDTO;
import com.epam.dlab.cloud.CloudProvider;
import com.epam.dlab.dto.ResourceBaseDTO;
import com.epam.dlab.dto.ResourceSysBaseDTO;
import com.epam.dlab.dto.aws.AwsCloudSettings;
import com.epam.dlab.dto.aws.exploratory.ExploratoryCreateAws;
import com.epam.dlab.dto.azure.AzureCloudSettings;
import com.epam.dlab.dto.azure.exploratory.ExploratoryActionStopAzure;
import com.epam.dlab.dto.azure.exploratory.ExploratoryCreateAzure;
import com.epam.dlab.dto.base.CloudSettings;
import com.epam.dlab.dto.exploratory.ExploratoryActionDTO;
import com.epam.dlab.dto.exploratory.ExploratoryCreateDTO;
import com.epam.dlab.dto.exploratory.ExploratoryGitCreds;
import com.epam.dlab.dto.exploratory.ExploratoryGitCredsUpdateDTO;
import com.epam.dlab.exceptions.DlabException;
import com.google.inject.Inject;

import java.util.List;

public class RequestBuilder {
    @Inject
    private static SelfServiceApplicationConfiguration configuration;

    @Inject
    private static SettingsDAO settingsDAO;

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
                return AzureCloudSettings.builder().azureRegion(settingsDAO.getAzureRegion())
                        .azureResourceGroupName(settingsDAO.getAzureResourceGroupName())
                        .azureSecurityGroupName(settingsDAO.getAzureSecurityGroupName())
                        .azureSubnetName(settingsDAO.getAzureSubnetName())
                        .azureVpcName(settingsDAO.getAzureVpcName())
                        .azureVpcName(userInfo.getName()).build();
            default:
                throw new IllegalArgumentException("Unsupported cloud provider " + cloudProvider());
        }
    }

    @SuppressWarnings("unchecked")
    private static <T extends ResourceBaseDTO<?>> T newResourceBaseDTO(UserInfo userInfo, Class<T> resourceClass) {
        try {
            T resource = resourceClass.newInstance();
            switch (cloudProvider()) {
                case AWS:
                case AZURE:
                    return (T) resource
                            .withEdgeUserName(userInfo.getSimpleName())
                            .withCloudSettings(cloudSettings(userInfo));
                default:
                    throw new DlabException("Unknown cloud provider " + cloudProvider());
            }
        } catch (Exception e) {
            throw new DlabException("Cannot create instance of resource class " + resourceClass.getName() + ". " +
                    e.getLocalizedMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public static <T extends ResourceSysBaseDTO<?>> T newResourceSysBaseDTO(UserInfo userInfo, Class<T> resourceClass) {
        T resource = newResourceBaseDTO(userInfo, resourceClass);
        return (T) resource
                .withServiceBaseName(settingsDAO.getServiceBaseName())
                .withConfOsFamily(settingsDAO.getConfOsFamily());
    }


    @SuppressWarnings("unchecked")
    public static <T extends ExploratoryCreateDTO<T>> T newExploratoryCreate(ExploratoryCreateFormDTO formDTO, UserInfo userInfo,
                                                                             List<ExploratoryGitCreds> gitCreds) {

        T exploratoryCreate;

        switch (cloudProvider()) {
            case AWS:
                exploratoryCreate = (T) newResourceSysBaseDTO(userInfo, ExploratoryCreateAws.class)
                        .withNotebookInstanceType(formDTO.getShape());
                break;
            case AZURE:
                exploratoryCreate = (T) newResourceSysBaseDTO(userInfo, ExploratoryCreateAzure.class)
                        .withNotebookInstanceSize(formDTO.getShape());
                break;
            default:
                throw new IllegalArgumentException("Unsupported cloud provider " + cloudProvider());
        }

        return exploratoryCreate.withExploratoryName(formDTO.getName())
                .withNotebookImage(formDTO.getImage())
                .withApplicationName(ResourceUtils.getApplicationNameFromImage(formDTO.getImage()))
                .withGitCreds(gitCreds);
    }

    @SuppressWarnings("unchecked")
    public static <T extends ExploratoryGitCredsUpdateDTO> T newExploratoryStart(UserInfo userInfo) {

        T exploratoryStart;

        switch (cloudProvider()) {
            case AWS:
            case AZURE:
                exploratoryStart = (T) newResourceSysBaseDTO(userInfo, ExploratoryGitCredsUpdateDTO.class);
                break;
            default:
                throw new IllegalArgumentException("Unsupported cloud provider " + cloudProvider());
        }

        return exploratoryStart;
    }

    @SuppressWarnings("unchecked")
    public static <T extends ExploratoryActionDTO<T>> T newExploratoryStop(UserInfo userInfo, UserInstanceDTO userInstanceDTO) {

        T exploratoryStop;

        switch (cloudProvider()) {
            case AWS:
                exploratoryStop = (T) newResourceSysBaseDTO(userInfo, ExploratoryActionDTO.class);
                break;
            case AZURE:
                exploratoryStop = (T) newResourceSysBaseDTO(userInfo, ExploratoryActionStopAzure.class);
                break;
            default:
                throw new IllegalArgumentException("Unsupported cloud provider " + cloudProvider());
        }

        return exploratoryStop.withNotebookImage(userInstanceDTO.getImageName());
    }

    private static CloudProvider cloudProvider() {
        return configuration.getCloudProvider();


    }
}



