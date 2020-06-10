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

package com.epam.dlab.backendapi.util;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.conf.SelfServiceApplicationConfiguration;
import com.epam.dlab.backendapi.dao.SettingsDAO;
import com.epam.dlab.backendapi.domain.EndpointDTO;
import com.epam.dlab.backendapi.domain.ExploratoryLibCache;
import com.epam.dlab.backendapi.domain.ProjectDTO;
import com.epam.dlab.backendapi.resources.dto.BackupFormDTO;
import com.epam.dlab.backendapi.resources.dto.ComputationalCreateFormDTO;
import com.epam.dlab.backendapi.resources.dto.SparkStandaloneClusterCreateForm;
import com.epam.dlab.backendapi.resources.dto.aws.AwsComputationalCreateForm;
import com.epam.dlab.backendapi.resources.dto.gcp.GcpComputationalCreateForm;
import com.epam.dlab.cloud.CloudProvider;
import com.epam.dlab.dto.LibListComputationalDTO;
import com.epam.dlab.dto.ResourceBaseDTO;
import com.epam.dlab.dto.ResourceSysBaseDTO;
import com.epam.dlab.dto.UserInstanceDTO;
import com.epam.dlab.dto.aws.AwsCloudSettings;
import com.epam.dlab.dto.aws.computational.AwsComputationalTerminateDTO;
import com.epam.dlab.dto.aws.computational.ClusterConfig;
import com.epam.dlab.dto.aws.computational.ComputationalCreateAws;
import com.epam.dlab.dto.aws.computational.SparkComputationalCreateAws;
import com.epam.dlab.dto.aws.exploratory.ExploratoryCreateAws;
import com.epam.dlab.dto.azure.AzureCloudSettings;
import com.epam.dlab.dto.azure.computational.SparkComputationalCreateAzure;
import com.epam.dlab.dto.azure.exploratory.ExploratoryActionStartAzure;
import com.epam.dlab.dto.azure.exploratory.ExploratoryActionStopAzure;
import com.epam.dlab.dto.azure.exploratory.ExploratoryCreateAzure;
import com.epam.dlab.dto.backup.EnvBackupDTO;
import com.epam.dlab.dto.base.CloudSettings;
import com.epam.dlab.dto.base.DataEngineType;
import com.epam.dlab.dto.base.computational.ComputationalBase;
import com.epam.dlab.dto.computational.ComputationalCheckInactivityDTO;
import com.epam.dlab.dto.computational.ComputationalClusterConfigDTO;
import com.epam.dlab.dto.computational.ComputationalStartDTO;
import com.epam.dlab.dto.computational.ComputationalStopDTO;
import com.epam.dlab.dto.computational.ComputationalTerminateDTO;
import com.epam.dlab.dto.computational.UserComputationalResource;
import com.epam.dlab.dto.exploratory.ExploratoryActionDTO;
import com.epam.dlab.dto.exploratory.ExploratoryCheckInactivityAction;
import com.epam.dlab.dto.exploratory.ExploratoryCreateDTO;
import com.epam.dlab.dto.exploratory.ExploratoryGitCredsDTO;
import com.epam.dlab.dto.exploratory.ExploratoryGitCredsUpdateDTO;
import com.epam.dlab.dto.exploratory.ExploratoryImageDTO;
import com.epam.dlab.dto.exploratory.ExploratoryReconfigureSparkClusterActionDTO;
import com.epam.dlab.dto.exploratory.LibInstallDTO;
import com.epam.dlab.dto.exploratory.LibraryInstallDTO;
import com.epam.dlab.dto.gcp.GcpCloudSettings;
import com.epam.dlab.dto.gcp.computational.ComputationalCreateGcp;
import com.epam.dlab.dto.gcp.computational.GcpComputationalTerminateDTO;
import com.epam.dlab.dto.gcp.computational.SparkComputationalCreateGcp;
import com.epam.dlab.dto.gcp.exploratory.ExploratoryCreateGcp;
import com.epam.dlab.dto.project.ProjectActionDTO;
import com.epam.dlab.dto.project.ProjectCreateDTO;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.model.exploratory.Exploratory;
import com.epam.dlab.util.UsernameUtils;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.epam.dlab.cloud.CloudProvider.AWS;
import static com.epam.dlab.cloud.CloudProvider.AZURE;
import static com.epam.dlab.cloud.CloudProvider.GCP;

@Singleton
public class RequestBuilder {
	private static final String UNSUPPORTED_CLOUD_PROVIDER_MESSAGE = "Unsupported cloud provider ";
	private static final String AZURE_REFRESH_TOKEN_KEY = "refresh_token";

	@Inject
	private SelfServiceApplicationConfiguration configuration;
	@Inject
	private SettingsDAO settingsDAO;

	private CloudSettings cloudSettings(String user, CloudProvider cloudProvider) {
		switch (cloudProvider) {
			case AWS:
				return AwsCloudSettings.builder()
						.awsIamUser(user)
						.build();
			case AZURE:
				return AzureCloudSettings.builder()
						.azureIamUser(user).build();
			case GCP:
				return GcpCloudSettings.builder()
						.gcpIamUser(user).build();
			default:
				throw new IllegalArgumentException(UNSUPPORTED_CLOUD_PROVIDER_MESSAGE + cloudProvider);
		}
	}

	@SuppressWarnings("unchecked")
	private <T extends ResourceBaseDTO<?>> T newResourceBaseDTO(String user, CloudProvider cloudProvider,
																Class<T> resourceClass) {
		try {
			return (T) resourceClass.newInstance()
					.withEdgeUserName(getEdgeUserName(user, cloudProvider))
					.withCloudSettings(cloudSettings(user, cloudProvider));
		} catch (Exception e) {
			throw new DlabException("Cannot create instance of resource class " + resourceClass.getName() + ". " +
					e.getLocalizedMessage(), e);
		}
	}

	private String getEdgeUserName(String user, CloudProvider cloudProvider) {
		String edgeUser = UsernameUtils.removeDomain(user);
		switch (cloudProvider) {
			case GCP:
				return adjustUserName(configuration.getMaxUserNameLength(), edgeUser);
			case AWS:
			case AZURE:
				return edgeUser;
			default:
				throw new DlabException(UNSUPPORTED_CLOUD_PROVIDER_MESSAGE + cloudProvider);
		}
	}

	private String adjustUserName(int maxLength, String userName) {
		return userName.length() > maxLength ?
				UUID.nameUUIDFromBytes(userName.getBytes()).toString().substring(0, maxLength) : userName;
	}

	@SuppressWarnings("unchecked")
	private <T extends ResourceSysBaseDTO<?>> T newResourceSysBaseDTO(String user, CloudProvider cloudProvider,
																	  Class<T> resourceClass) {
		return newResourceBaseDTO(user, cloudProvider, resourceClass);
	}

	@SuppressWarnings("unchecked")
	public <T extends ExploratoryCreateDTO<T>> T newExploratoryCreate(ProjectDTO projectDTO, EndpointDTO endpointDTO, Exploratory exploratory,
																	  UserInfo userInfo,
																	  ExploratoryGitCredsDTO exploratoryGitCredsDTO,
																	  Map<String, String> tags) {

		T exploratoryCreate;
		CloudProvider cloudProvider = endpointDTO.getCloudProvider();
		switch (cloudProvider) {
			case AWS:
				exploratoryCreate = (T) newResourceSysBaseDTO(userInfo.getName(), cloudProvider, ExploratoryCreateAws.class)
						.withNotebookInstanceType(exploratory.getShape());
				break;
			case AZURE:
				exploratoryCreate = (T) newResourceSysBaseDTO(userInfo.getName(), cloudProvider, ExploratoryCreateAzure.class)
						.withNotebookInstanceSize(exploratory.getShape());
				if (settingsDAO.isAzureDataLakeEnabled()) {
					((ExploratoryCreateAzure) exploratoryCreate)
							.withAzureUserRefreshToken(userInfo.getKeys().get(AZURE_REFRESH_TOKEN_KEY));
				}

				((ExploratoryCreateAzure) exploratoryCreate)
						.withAzureDataLakeEnabled(Boolean.toString(settingsDAO.isAzureDataLakeEnabled()));
				break;
			case GCP:
				exploratoryCreate = (T) newResourceSysBaseDTO(userInfo.getName(), cloudProvider, ExploratoryCreateGcp.class)
						.withNotebookInstanceType(exploratory.getShape());
				break;
			default:
				throw new IllegalArgumentException(UNSUPPORTED_CLOUD_PROVIDER_MESSAGE + cloudProvider);
		}

		return exploratoryCreate.withExploratoryName(exploratory.getName())
				.withNotebookImage(exploratory.getDockerImage())
				.withApplicationName(getApplicationNameFromImage(exploratory.getDockerImage()))
				.withGitCreds(exploratoryGitCredsDTO.getGitCreds())
				.withImageName(exploratory.getImageName())
				.withClusterConfig(exploratory.getClusterConfig())
				.withProject(exploratory.getProject())
				.withEndpoint(exploratory.getEndpoint())
				.withSharedImageEnabled(String.valueOf(projectDTO.isSharedImageEnabled()))
				.withTags(tags);
	}

	@SuppressWarnings("unchecked")
	public <T extends ExploratoryGitCredsUpdateDTO> T newExploratoryStart(UserInfo userInfo,
																		  UserInstanceDTO userInstance,
																		  EndpointDTO endpointDTO,
																		  ExploratoryGitCredsDTO
																				  exploratoryGitCredsDTO) {
		CloudProvider cloudProvider = endpointDTO.getCloudProvider();
		switch (cloudProvider) {
			case AWS:
			case GCP:
				return (T) newResourceSysBaseDTO(userInfo.getName(), cloudProvider, ExploratoryGitCredsUpdateDTO.class)
						.withNotebookInstanceName(userInstance.getExploratoryId())
						.withGitCreds(exploratoryGitCredsDTO.getGitCreds())
						.withNotebookImage(userInstance.getImageName())
						.withExploratoryName(userInstance.getExploratoryName())
						.withReuploadKeyRequired(userInstance.isReuploadKeyRequired())
						.withProject(userInstance.getProject())
						.withEndpoint(userInstance.getEndpoint());
			case AZURE:
				T exploratoryStart = (T) newResourceSysBaseDTO(userInfo.getName(), cloudProvider, ExploratoryActionStartAzure.class)
						.withNotebookInstanceName(userInstance.getExploratoryId())
						.withGitCreds(exploratoryGitCredsDTO.getGitCreds())
						.withNotebookImage(userInstance.getImageName())
						.withExploratoryName(userInstance.getExploratoryName())
						.withReuploadKeyRequired(userInstance.isReuploadKeyRequired())
						.withProject(userInstance.getProject())
						.withEndpoint(userInstance.getEndpoint());

				if (settingsDAO.isAzureDataLakeEnabled()) {
					((ExploratoryActionStartAzure) exploratoryStart)
							.withAzureUserRefreshToken(userInfo.getKeys().get(AZURE_REFRESH_TOKEN_KEY));
				}

				((ExploratoryActionStartAzure) exploratoryStart)
						.withAzureDataLakeEnabled(Boolean.toString(settingsDAO.isAzureDataLakeEnabled()));

				return exploratoryStart;
			default:
				throw new IllegalArgumentException(UNSUPPORTED_CLOUD_PROVIDER_MESSAGE + cloudProvider);
		}
	}

	@SuppressWarnings("unchecked")
	public <T extends ExploratoryActionDTO<T>> T newExploratoryStop(String user, UserInstanceDTO userInstance, EndpointDTO endpointDTO) {

		T exploratoryStop;
		CloudProvider cloudProvider = endpointDTO.getCloudProvider();

		switch (cloudProvider) {
			case AWS:
			case GCP:
				exploratoryStop = (T) newResourceSysBaseDTO(user, cloudProvider, ExploratoryActionDTO.class);
				break;
			case AZURE:
				exploratoryStop = (T) newResourceSysBaseDTO(user, cloudProvider, ExploratoryActionStopAzure.class);
				break;
			default:
				throw new IllegalArgumentException(UNSUPPORTED_CLOUD_PROVIDER_MESSAGE + cloudProvider);
		}

		return exploratoryStop
				.withNotebookInstanceName(userInstance.getExploratoryId())
				.withNotebookImage(userInstance.getImageName())
				.withExploratoryName(userInstance.getExploratoryName())
				.withNotebookImage(userInstance.getImageName())
				.withReuploadKeyRequired(userInstance.isReuploadKeyRequired())
				.withProject(userInstance.getProject())
				.withEndpoint(userInstance.getEndpoint());
	}

	public ExploratoryGitCredsUpdateDTO newGitCredentialsUpdate(UserInfo userInfo, UserInstanceDTO instanceDTO,
																EndpointDTO endpointDTO,
																ExploratoryGitCredsDTO exploratoryGitCredsDTO) {
		checkInappropriateCloudProviderOrElseThrowException(endpointDTO.getCloudProvider());
		return newResourceSysBaseDTO(userInfo.getName(), endpointDTO.getCloudProvider(), ExploratoryGitCredsUpdateDTO.class)
				.withNotebookImage(instanceDTO.getImageName())
				.withApplicationName(getApplicationNameFromImage(instanceDTO.getImageName()))
				.withProject(instanceDTO.getProject())
				.withEndpoint(instanceDTO.getEndpoint())
				.withNotebookInstanceName(instanceDTO.getExploratoryId())
				.withExploratoryName(instanceDTO.getExploratoryName())
				.withGitCreds(exploratoryGitCredsDTO.getGitCreds());
	}

	public LibraryInstallDTO newLibInstall(UserInfo userInfo, UserInstanceDTO userInstance,
										   EndpointDTO endpointDTO, List<LibInstallDTO> libs) {
		checkInappropriateCloudProviderOrElseThrowException(endpointDTO.getCloudProvider());
		return newResourceSysBaseDTO(userInfo.getName(), endpointDTO.getCloudProvider(), LibraryInstallDTO.class)
				.withNotebookImage(userInstance.getImageName())
				.withApplicationName(getApplicationNameFromImage(userInstance.getImageName()))
				.withNotebookInstanceName(userInstance.getExploratoryId())
				.withExploratoryName(userInstance.getExploratoryName())
				.withProject(userInstance.getProject())
				.withEndpoint(endpointDTO.getName())
				.withLibs(libs);
	}

	@SuppressWarnings("unchecked")
	public <T extends ExploratoryActionDTO<T>> T newLibExploratoryList(UserInfo userInfo,
																	   UserInstanceDTO userInstance,
																	   EndpointDTO endpointDTO) {
		checkInappropriateCloudProviderOrElseThrowException(endpointDTO.getCloudProvider());
		return (T) newResourceSysBaseDTO(userInfo.getName(), endpointDTO.getCloudProvider(), ExploratoryActionDTO.class)
				.withNotebookInstanceName(userInstance.getExploratoryId())
				.withProject(userInstance.getProject())
				.withEndpoint(endpointDTO.getName())
				.withNotebookImage(userInstance.getImageName())
				.withApplicationName(getApplicationNameFromImage(userInstance.getImageName()))
				.withExploratoryName(userInstance.getExploratoryName());
	}

	@SuppressWarnings("unchecked")
	public <T extends LibraryInstallDTO> T newLibInstall(UserInfo userInfo, UserInstanceDTO userInstance,
														 UserComputationalResource computationalResource,
														 List<LibInstallDTO> libs, EndpointDTO endpointDTO) {
		checkInappropriateCloudProviderOrElseThrowException(endpointDTO.getCloudProvider());
		return (T) newResourceSysBaseDTO(userInfo.getName(), endpointDTO.getCloudProvider(), LibraryInstallDTO.class)
				.withComputationalId(computationalResource.getComputationalId())
				.withComputationalName(computationalResource.getComputationalName())
				.withExploratoryName(userInstance.getExploratoryName())
				.withProject(userInstance.getProject())
				.withEndpoint(endpointDTO.getName())
				.withComputationalImage(computationalResource.getImageName())
				.withApplicationName(getApplicationNameFromImage(userInstance.getImageName()))
				.withLibs(libs);
	}

	@SuppressWarnings("unchecked")
	public <T extends LibListComputationalDTO> T newLibComputationalList(UserInfo userInfo,
																		 UserInstanceDTO userInstance,
																		 UserComputationalResource
																				 computationalResource,
																		 EndpointDTO endpointDTO) {

		checkInappropriateCloudProviderOrElseThrowException(endpointDTO.getCloudProvider());
		return (T) newResourceSysBaseDTO(userInfo.getName(), endpointDTO.getCloudProvider(), LibListComputationalDTO.class)
				.withComputationalId(computationalResource.getComputationalId())
				.withProject(userInstance.getProject())
				.withEndpoint(endpointDTO.getName())
				.withComputationalImage(computationalResource.getImageName())
				.withLibCacheKey(ExploratoryLibCache.libraryCacheKey(userInstance))
				.withApplicationName(getApplicationNameFromImage(userInstance.getImageName()));
	}

	@SuppressWarnings("unchecked")
	public <T extends ComputationalBase<T>> T newComputationalCreate(UserInfo userInfo, ProjectDTO projectDTO,
																	 UserInstanceDTO userInstance,
																	 ComputationalCreateFormDTO form,
																	 EndpointDTO endpointDTO) {
		T computationalCreate;
		CloudProvider cloudProvider = endpointDTO.getCloudProvider();
		switch (cloudProvider) {
			case AZURE:
				throw new UnsupportedOperationException("Creating dataengine service is not supported yet");
			case AWS:
				AwsComputationalCreateForm awsForm = (AwsComputationalCreateForm) form;
				computationalCreate = (T) newResourceSysBaseDTO(userInfo.getName(), cloudProvider, ComputationalCreateAws.class)
						.withInstanceCount(awsForm.getInstanceCount())
						.withMasterInstanceType(awsForm.getMasterInstanceType())
						.withSlaveInstanceType(awsForm.getSlaveInstanceType())
						.withSlaveInstanceSpot(awsForm.getSlaveInstanceSpot())
						.withSlaveInstanceSpotPctPrice(awsForm.getSlaveInstanceSpotPctPrice())
						.withVersion(awsForm.getVersion())
						.withConfig((awsForm.getConfig()))
						.withSharedImageEnabled(String.valueOf(projectDTO.isSharedImageEnabled()));
				break;
			case GCP:
				GcpComputationalCreateForm gcpForm = (GcpComputationalCreateForm) form;
				computationalCreate = (T) newResourceSysBaseDTO(userInfo.getName(), cloudProvider, ComputationalCreateGcp.class)
						.withMasterInstanceCount(gcpForm.getMasterInstanceCount())
						.withSlaveInstanceCount(gcpForm.getSlaveInstanceCount())
						.withPreemptibleCount(gcpForm.getPreemptibleCount())
						.withMasterInstanceType(gcpForm.getMasterInstanceType())
						.withSlaveInstanceType(gcpForm.getSlaveInstanceType())
						.withVersion(gcpForm.getVersion())
						.withSharedImageEnabled(String.valueOf(projectDTO.isSharedImageEnabled()));
				break;

			default:
				throw new IllegalArgumentException(UNSUPPORTED_CLOUD_PROVIDER_MESSAGE + cloudProvider);
		}

		return computationalCreate
				.withExploratoryName(form.getNotebookName())
				.withComputationalName(form.getName())
				.withNotebookTemplateName(userInstance.getTemplateName())
				.withApplicationName(getApplicationNameFromImage(userInstance.getImageName()))
				.withNotebookInstanceName(userInstance.getExploratoryId())
				.withProject(userInstance.getProject())
				.withTags(userInstance.getTags())
				.withEndpoint(userInstance.getEndpoint());
	}

	@SuppressWarnings("unchecked")
	public <T extends ComputationalBase<T>> T newComputationalCreate(UserInfo userInfo, ProjectDTO projectDTO,
																	 UserInstanceDTO userInstance,
																	 SparkStandaloneClusterCreateForm form,
																	 EndpointDTO endpointDTO) {

		T computationalCreate;
		CloudProvider cloudProvider = endpointDTO.getCloudProvider();
		switch (cloudProvider) {
			case AWS:
				computationalCreate = (T) newResourceSysBaseDTO(userInfo.getName(), cloudProvider, SparkComputationalCreateAws.class)
						.withDataEngineInstanceCount(form.getDataEngineInstanceCount())
						.withDataEngineMasterShape(form.getDataEngineInstanceShape())
						.withDataEngineSlaveShape(form.getDataEngineInstanceShape())
						.withConfig(form.getConfig())
						.withSharedImageEnabled(String.valueOf(projectDTO.isSharedImageEnabled()));
				break;
			case AZURE:
				computationalCreate = (T) newResourceSysBaseDTO(userInfo.getName(), cloudProvider, SparkComputationalCreateAzure.class)
						.withDataEngineInstanceCount(form.getDataEngineInstanceCount())
						.withDataEngineMasterSize(form.getDataEngineInstanceShape())
						.withDataEngineSlaveSize(form.getDataEngineInstanceShape())
						.withConfig(form.getConfig())
						.withSharedImageEnabled(String.valueOf(projectDTO.isSharedImageEnabled()));
				if (settingsDAO.isAzureDataLakeEnabled()) {
					((SparkComputationalCreateAzure) computationalCreate)
							.withAzureUserRefreshToken(userInfo.getKeys().get(AZURE_REFRESH_TOKEN_KEY));
				}

				((SparkComputationalCreateAzure) computationalCreate)
						.withAzureDataLakeEnabled(Boolean.toString(settingsDAO.isAzureDataLakeEnabled()));

				break;
			case GCP:
				computationalCreate = (T) newResourceSysBaseDTO(userInfo.getName(), cloudProvider, SparkComputationalCreateGcp.class)
						.withDataEngineInstanceCount(form.getDataEngineInstanceCount())
						.withDataEngineMasterSize(form.getDataEngineInstanceShape())
						.withDataEngineSlaveSize(form.getDataEngineInstanceShape())
						.withConfig(form.getConfig())
						.withSharedImageEnabled(String.valueOf(projectDTO.isSharedImageEnabled()));
				break;
			default:
				throw new IllegalArgumentException(UNSUPPORTED_CLOUD_PROVIDER_MESSAGE + cloudProvider);
		}

		return computationalCreate
				.withExploratoryName(form.getNotebookName())
				.withComputationalName(form.getName())
				.withNotebookTemplateName(userInstance.getTemplateName())
				.withApplicationName(getApplicationNameFromImage(userInstance.getImageName()))
				.withNotebookInstanceName(userInstance.getExploratoryId())
				.withProject(userInstance.getProject())
				.withTags(userInstance.getTags())
				.withEndpoint(userInstance.getEndpoint());
	}

	@SuppressWarnings("unchecked")
    public <T extends ComputationalBase<T>> T newComputationalTerminate(String resourceCreator,
                                                                        UserInstanceDTO userInstanceDTO,
                                                                        UserComputationalResource computationalResource,
                                                                        EndpointDTO endpointDTO) {
        T computationalTerminate;
        CloudProvider cloudProvider = endpointDTO.getCloudProvider();
        switch (cloudProvider) {
            case AWS:
                AwsComputationalTerminateDTO terminateDTO = newResourceSysBaseDTO(resourceCreator, cloudProvider,
                        AwsComputationalTerminateDTO.class);
                if (computationalResource.getDataEngineType() == DataEngineType.CLOUD_SERVICE) {
                    terminateDTO.setClusterName(computationalResource.getComputationalId());
                }
                computationalTerminate = (T) terminateDTO;
                break;
            case AZURE:
                computationalTerminate = (T) newResourceSysBaseDTO(resourceCreator, cloudProvider, ComputationalTerminateDTO.class);
                break;
            case GCP:
                GcpComputationalTerminateDTO gcpTerminateDTO = newResourceSysBaseDTO(resourceCreator, cloudProvider,
                        GcpComputationalTerminateDTO.class);
                if (computationalResource.getDataEngineType() == DataEngineType.CLOUD_SERVICE) {
                    gcpTerminateDTO.setClusterName(computationalResource.getComputationalId());
                }
                computationalTerminate = (T) gcpTerminateDTO;
                break;

            default:
                throw new IllegalArgumentException(UNSUPPORTED_CLOUD_PROVIDER_MESSAGE + cloudProvider);
        }

		return computationalTerminate
				.withExploratoryName(userInstanceDTO.getExploratoryName())
				.withComputationalName(computationalResource.getComputationalName())
				.withNotebookInstanceName(userInstanceDTO.getExploratoryId())
				.withProject(userInstanceDTO.getProject())
				.withEndpoint(userInstanceDTO.getEndpoint());
	}

	@SuppressWarnings("unchecked")
	public <T extends ComputationalBase<T>> T newComputationalStop(UserInfo userInfo, UserInstanceDTO exploratory,
																   String computationalName, EndpointDTO endpointDTO) {
		return (T) newResourceSysBaseDTO(userInfo.getName(), endpointDTO.getCloudProvider(), ComputationalStopDTO.class)
				.withExploratoryName(exploratory.getExploratoryName())
				.withComputationalName(computationalName)
				.withNotebookInstanceName(exploratory.getExploratoryId())
				.withApplicationName(getApplicationNameFromImage(exploratory.getImageName()))
				.withProject(exploratory.getProject())
				.withEndpoint(endpointDTO.getName());
	}

	@SuppressWarnings("unchecked")
	public <T extends ComputationalBase<T>> T newComputationalStart(UserInfo userInfo, UserInstanceDTO exploratory,
																	String computationalName, EndpointDTO endpointDTO) {
		return (T) newResourceSysBaseDTO(userInfo.getName(), endpointDTO.getCloudProvider(), ComputationalStartDTO.class)
				.withExploratoryName(exploratory.getExploratoryName())
				.withComputationalName(computationalName)
				.withNotebookInstanceName(exploratory.getExploratoryId())
				.withApplicationName(getApplicationNameFromImage(exploratory.getImageName()))
				.withProject(exploratory.getProject())
				.withEndpoint(endpointDTO.getName());
	}

	@SuppressWarnings("unchecked")
	public <T extends ExploratoryImageDTO> T newExploratoryImageCreate(UserInfo userInfo, UserInstanceDTO userInstance,
																	   String imageName, EndpointDTO endpointDTO, ProjectDTO projectDTO) {
		checkInappropriateCloudProviderOrElseThrowException(endpointDTO.getCloudProvider());
		return (T) newResourceSysBaseDTO(userInfo.getName(), endpointDTO.getCloudProvider(), ExploratoryImageDTO.class)
				.withProject(userInstance.getProject())
				.withNotebookInstanceName(userInstance.getExploratoryId())
				.withExploratoryName(userInstance.getExploratoryName())
				.withApplicationName(getApplicationNameFromImage(userInstance.getImageName()))
				.withNotebookImage(userInstance.getImageName())
				.withImageName(imageName)
				.withEndpoint(userInstance.getEndpoint())
				.withTags(userInstance.getTags())
				.withSharedImageEnabled(String.valueOf(projectDTO.isSharedImageEnabled()));
	}

	@SuppressWarnings("unchecked")
	public <T extends ComputationalBase<T>> T newComputationalCheckInactivity(UserInfo userInfo,
																			  UserInstanceDTO exploratory,
																			  UserComputationalResource cr, EndpointDTO endpointDTO) {
		return (T) newResourceSysBaseDTO(userInfo.getName(), endpointDTO.getCloudProvider(), ComputationalCheckInactivityDTO.class)
				.withExploratoryName(exploratory.getExploratoryName())
				.withComputationalName(cr.getComputationalName())
				.withNotebookInstanceName(exploratory.getExploratoryId())
				.withApplicationName(getApplicationNameFromImage(exploratory.getImageName()))
				.withNotebookImageName(exploratory.getImageName())
				.withImage(cr.getImageName())
				.withComputationalId(cr.getComputationalId())
				.withProject(exploratory.getProject())
				.withEndpoint(endpointDTO.getName());
	}


	@SuppressWarnings("unchecked")
	public <T extends EnvBackupDTO> T newBackupCreate(BackupFormDTO backupFormDTO, String id) {

		return (T) EnvBackupDTO.builder()
				.configFiles(backupFormDTO.getConfigFiles())
				.certificates(backupFormDTO.getCertificates())
				.keys(backupFormDTO.getKeys())
				.jars(backupFormDTO.getJars())
				.databaseBackup(backupFormDTO.isDatabaseBackup())
				.logsBackup(backupFormDTO.isLogsBackup())
				.id(id)
				.build();
	}

	public ComputationalClusterConfigDTO newClusterConfigUpdate(UserInfo userInfo, UserInstanceDTO userInstanceDTO,
																UserComputationalResource compRes,
																List<ClusterConfig> config, EndpointDTO endpointDTO) {
		CloudProvider cloudProvider = endpointDTO.getCloudProvider();
		final ComputationalClusterConfigDTO clusterConfigDTO = newResourceSysBaseDTO(userInfo.getName(), cloudProvider,
				ComputationalClusterConfigDTO.class)
				.withExploratoryName(userInstanceDTO.getExploratoryName())
				.withNotebookInstanceName(userInstanceDTO.getExploratoryId())
				.withComputationalName(compRes.getComputationalName())
				.withApplicationName(compRes.getImageName())
				.withProject(userInstanceDTO.getProject())
				.withEndpoint(userInstanceDTO.getEndpoint());
		clusterConfigDTO.setCopmutationalId(compRes.getComputationalId());
		clusterConfigDTO.setConfig(config);
		if (cloudProvider == AZURE && settingsDAO.isAzureDataLakeEnabled()) {
			clusterConfigDTO.setAzureUserRefreshToken(userInfo.getKeys().get(AZURE_REFRESH_TOKEN_KEY));
		}

		return clusterConfigDTO;
	}

	public ExploratoryReconfigureSparkClusterActionDTO newClusterConfigUpdate(UserInfo userInfo,
																			  UserInstanceDTO userInstance,
																			  List<ClusterConfig> config,
																			  EndpointDTO endpointDTO) {

		CloudProvider cloudProvider = endpointDTO.getCloudProvider();
		final ExploratoryReconfigureSparkClusterActionDTO dto =
				newResourceSysBaseDTO(userInfo.getName(), cloudProvider, ExploratoryReconfigureSparkClusterActionDTO.class)
						.withNotebookInstanceName(userInstance.getExploratoryId())
						.withExploratoryName(userInstance.getExploratoryName())
						.withApplicationName(getApplicationNameFromImage(userInstance.getImageName()))
						.withNotebookImage(userInstance.getImageName())
						.withConfig(config)
						.withProject(userInstance.getProject())
						.withEndpoint(userInstance.getEndpoint());
		if (cloudProvider == AZURE && settingsDAO.isAzureDataLakeEnabled()) {
			dto.withAzureUserRefreshToken(userInfo.getKeys().get(AZURE_REFRESH_TOKEN_KEY));
		}

		return dto;
	}

	public ExploratoryCheckInactivityAction newExploratoryCheckInactivityAction(UserInfo userInfo,
																				UserInstanceDTO userInstance,
																				EndpointDTO endpointDTO) {
		final ExploratoryCheckInactivityAction dto = newResourceSysBaseDTO(userInfo.getName(), endpointDTO.getCloudProvider(),
				ExploratoryCheckInactivityAction.class);
		dto.withNotebookInstanceName(userInstance.getExploratoryId())
				.withNotebookImage(userInstance.getImageName())
				.withExploratoryName(userInstance.getExploratoryName())
				.withReuploadKeyRequired(userInstance.isReuploadKeyRequired())
				.withProject(userInstance.getProject())
				.withEndpoint(endpointDTO.getName());
		return dto;
	}

	public ProjectCreateDTO newProjectCreate(UserInfo userInfo, ProjectDTO projectDTO, EndpointDTO endpointDTO) {
		return ProjectCreateDTO.builder()
				.key(projectDTO.getKey().replace("\n", ""))
				.name(projectDTO.getName())
				.tag(projectDTO.getTag())
				.endpoint(endpointDTO.getName())
				.build()
				.withCloudSettings(cloudSettings(userInfo.getName(), endpointDTO.getCloudProvider()));
	}

	public ProjectActionDTO newProjectAction(UserInfo userInfo, String project, EndpointDTO endpointDTO) {
		return new ProjectActionDTO(project, endpointDTO.getName())
				.withCloudSettings(cloudSettings(userInfo.getName(), endpointDTO.getCloudProvider()));
	}

	/**
	 * Returns application name basing on docker image
	 *
	 * @param imageName docker image name
	 * @return application name
	 */
	private String getApplicationNameFromImage(String imageName) {
		if (imageName != null) {
			int pos = imageName.indexOf('-');
			if (pos > 0) {
				return imageName.substring(pos + 1);
			}
		}
		return "";
	}

	private void checkInappropriateCloudProviderOrElseThrowException(CloudProvider provider) {
		if (provider != AWS && provider != AZURE && provider != GCP) {
			throw new IllegalArgumentException(UNSUPPORTED_CLOUD_PROVIDER_MESSAGE + provider);
		}
	}
}



