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
import com.epam.dlab.backendapi.resources.dto.BackupFormDTO;
import com.epam.dlab.backendapi.resources.dto.ComputationalCreateFormDTO;
import com.epam.dlab.backendapi.resources.dto.SparkStandaloneClusterCreateForm;
import com.epam.dlab.backendapi.resources.dto.aws.AwsComputationalCreateForm;
import com.epam.dlab.backendapi.resources.dto.gcp.GcpComputationalCreateForm;
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
import com.epam.dlab.dto.backup.EnvBackupDTO;
import com.epam.dlab.dto.base.CloudSettings;
import com.epam.dlab.dto.base.DataEngineType;
import com.epam.dlab.dto.base.computational.ComputationalBase;
import com.epam.dlab.dto.base.keyload.ReuploadFile;
import com.epam.dlab.dto.base.keyload.UploadFile;
import com.epam.dlab.dto.computational.ComputationalStartDTO;
import com.epam.dlab.dto.computational.ComputationalStopDTO;
import com.epam.dlab.dto.computational.ComputationalTerminateDTO;
import com.epam.dlab.dto.computational.UserComputationalResource;
import com.epam.dlab.dto.exploratory.*;
import com.epam.dlab.dto.gcp.GcpCloudSettings;
import com.epam.dlab.dto.gcp.computational.ComputationalCreateGcp;
import com.epam.dlab.dto.gcp.computational.GcpComputationalTerminateDTO;
import com.epam.dlab.dto.gcp.computational.SparkComputationalCreateGcp;
import com.epam.dlab.dto.gcp.edge.EdgeCreateGcp;
import com.epam.dlab.dto.gcp.exploratory.ExploratoryCreateGcp;
import com.epam.dlab.dto.gcp.keyload.UploadFileGcp;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.model.exloratory.Exploratory;
import com.epam.dlab.utils.UsernameUtils;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.ArrayList;
import java.util.UUID;

import static com.epam.dlab.cloud.CloudProvider.*;

@Singleton
public class RequestBuilder {
	private static final String UNSUPPORTED_CLOUD_PROVIDER_MESSAGE = "Unsupported cloud provider ";
	private static final String AZURE_REFRESH_TOKEN_KEY = "refresh_token";

	@Inject
	private SelfServiceApplicationConfiguration configuration;
	@Inject
	private SettingsDAO settingsDAO;

	private CloudSettings cloudSettings(UserInfo userInfo) {
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
				return GcpCloudSettings.builder().gcpIamUser(userInfo.getName()).build();
			default:
				throw new IllegalArgumentException(UNSUPPORTED_CLOUD_PROVIDER_MESSAGE + cloudProvider());
		}
	}

	@SuppressWarnings("unchecked")
	private <T extends ResourceBaseDTO<?>> T newResourceBaseDTO(UserInfo userInfo, Class<T> resourceClass) {
		try {
			return (T) resourceClass.newInstance()
					.withEdgeUserName(getEdgeUserName(userInfo))
					.withCloudSettings(cloudSettings(userInfo));
		} catch (Exception e) {
			throw new DlabException("Cannot create instance of resource class " + resourceClass.getName() + ". " +
					e.getLocalizedMessage(), e);
		}
	}

	private String getEdgeUserName(UserInfo userInfo) {
		String edgeUser = UsernameUtils.replaceWhitespaces(userInfo.getSimpleName());
		switch (cloudProvider()) {
			case GCP:
				return adjustUserName(configuration.getMaxUserNameLength(), edgeUser);
			case AWS:
			case AZURE:
				return edgeUser;
			default:
				throw new DlabException(UNSUPPORTED_CLOUD_PROVIDER_MESSAGE + cloudProvider());
		}
	}

	private String adjustUserName(int maxLength, String userName) {
		return userName.length() > maxLength ?
				UUID.nameUUIDFromBytes(userName.getBytes()).toString().substring(0, maxLength) : userName;
	}

	@SuppressWarnings("unchecked")
	private <T extends ResourceSysBaseDTO<?>> T newResourceSysBaseDTO(UserInfo userInfo, Class<T> resourceClass) {
		T resource = newResourceBaseDTO(userInfo, resourceClass);
		return (T) resource
				.withServiceBaseName(settingsDAO.getServiceBaseName())
				.withConfOsFamily(settingsDAO.getConfOsFamily());
	}

	@SuppressWarnings("unchecked")
	public UploadFile newEdgeKeyUpload(UserInfo userInfo, String content) {

		switch (cloudProvider()) {
			case AWS:
				EdgeCreateAws edgeCreateAws = newResourceSysBaseDTO(userInfo, EdgeCreateAws.class);
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

	public UploadFile newKeyReupload(UserInfo userInfo, String content) {
		ReuploadFile reuploadFile = new ReuploadFile();
		reuploadFile.setContent(content);
		reuploadFile.setEdgeUserName(getEdgeUserName(userInfo));
		return reuploadFile;
	}

	@SuppressWarnings("unchecked")
	public <T extends ResourceSysBaseDTO<?>> T newEdgeAction(UserInfo userInfo) {
		checkInappropriateCloudProviderOrElseThrowException();
		return (T) newResourceSysBaseDTO(userInfo, ResourceSysBaseDTO.class);
	}

	public UserEnvironmentResources newUserEnvironmentStatus(UserInfo userInfo) {
		checkInappropriateCloudProviderOrElseThrowException();
		return newResourceSysBaseDTO(userInfo, UserEnvironmentResources.class);
	}

	@SuppressWarnings("unchecked")
	public <T extends ExploratoryCreateDTO<T>> T newExploratoryCreate(Exploratory exploratory, UserInfo userInfo,
																	  ExploratoryGitCredsDTO exploratoryGitCredsDTO) {

		T exploratoryCreate;

		switch (cloudProvider()) {
			case AWS:
				exploratoryCreate = (T) newResourceSysBaseDTO(userInfo, ExploratoryCreateAws.class)
						.withNotebookInstanceType(exploratory.getShape());
				break;
			case AZURE:
				exploratoryCreate = (T) newResourceSysBaseDTO(userInfo, ExploratoryCreateAzure.class)
						.withNotebookInstanceSize(exploratory.getShape());
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
						.withNotebookInstanceType(exploratory.getShape());
				break;

			default:
				throw new IllegalArgumentException(UNSUPPORTED_CLOUD_PROVIDER_MESSAGE + cloudProvider());
		}

		return exploratoryCreate.withExploratoryName(exploratory.getName())
				.withNotebookImage(exploratory.getDockerImage())
				.withApplicationName(getApplicationNameFromImage(exploratory.getDockerImage()))
				.withGitCreds(exploratoryGitCredsDTO.getGitCreds())
				.withImageName(exploratory.getImageName());
	}

	@SuppressWarnings("unchecked")
	public <T extends ExploratoryGitCredsUpdateDTO> T newExploratoryStart(UserInfo userInfo,
																		  UserInstanceDTO userInstance,
																		  ExploratoryGitCredsDTO
																				  exploratoryGitCredsDTO) {

		switch (cloudProvider()) {
			case AWS:
			case GCP:
				return (T) newResourceSysBaseDTO(userInfo, ExploratoryGitCredsUpdateDTO.class)
						.withNotebookInstanceName(userInstance.getExploratoryId())
						.withGitCreds(exploratoryGitCredsDTO.getGitCreds())
						.withNotebookImage(userInstance.getImageName())
						.withExploratoryName(userInstance.getExploratoryName())
						.withReuploadKeyRequired(userInstance.isReuploadKeyRequired());
			case AZURE:
				T exploratoryStart = (T) newResourceSysBaseDTO(userInfo, ExploratoryActionStartAzure.class)
						.withNotebookInstanceName(userInstance.getExploratoryId())
						.withGitCreds(exploratoryGitCredsDTO.getGitCreds())
						.withNotebookImage(userInstance.getImageName())
						.withExploratoryName(userInstance.getExploratoryName())
						.withReuploadKeyRequired(userInstance.isReuploadKeyRequired());

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
	public <T extends ExploratoryActionDTO<T>> T newExploratoryStop(UserInfo userInfo, UserInstanceDTO userInstance) {

		T exploratoryStop;

		switch (cloudProvider()) {
			case AWS:
			case GCP:
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
				.withNotebookImage(userInstance.getImageName())
				.withReuploadKeyRequired(userInstance.isReuploadKeyRequired());
	}

	public ExploratoryGitCredsUpdateDTO newGitCredentialsUpdate(UserInfo userInfo, UserInstanceDTO instanceDTO,
																ExploratoryGitCredsDTO exploratoryGitCredsDTO) {
		checkInappropriateCloudProviderOrElseThrowException();
		return newResourceSysBaseDTO(userInfo, ExploratoryGitCredsUpdateDTO.class)
				.withNotebookImage(instanceDTO.getImageName())
				.withApplicationName(getApplicationNameFromImage(instanceDTO.getImageName()))
				.withNotebookInstanceName(instanceDTO.getExploratoryId())
				.withExploratoryName(instanceDTO.getExploratoryName())
				.withGitCreds(exploratoryGitCredsDTO.getGitCreds());
	}

	public LibraryInstallDTO newLibInstall(UserInfo userInfo, UserInstanceDTO userInstance) {
		checkInappropriateCloudProviderOrElseThrowException();
		return newResourceSysBaseDTO(userInfo, LibraryInstallDTO.class)
				.withNotebookImage(userInstance.getImageName())
				.withApplicationName(getApplicationNameFromImage(userInstance.getImageName()))
				.withNotebookInstanceName(userInstance.getExploratoryId())
				.withExploratoryName(userInstance.getExploratoryName())
				.withLibs(new ArrayList<>());
	}

	@SuppressWarnings("unchecked")
	public <T extends ExploratoryActionDTO<T>> T newLibExploratoryList(UserInfo userInfo,
																	   UserInstanceDTO userInstance) {
		checkInappropriateCloudProviderOrElseThrowException();
		return (T) newResourceSysBaseDTO(userInfo, ExploratoryActionDTO.class)
				.withNotebookInstanceName(userInstance.getExploratoryId())
				.withNotebookImage(userInstance.getImageName())
				.withApplicationName(getApplicationNameFromImage(userInstance.getImageName()))
				.withExploratoryName(userInstance.getExploratoryName());
	}

	@SuppressWarnings("unchecked")
	public <T extends LibraryInstallDTO> T newLibInstall(UserInfo userInfo, UserInstanceDTO userInstance,
														 UserComputationalResource computationalResource) {
		checkInappropriateCloudProviderOrElseThrowException();
		return (T) newResourceSysBaseDTO(userInfo, LibraryInstallDTO.class)
				.withComputationalId(computationalResource.getComputationalId())
				.withComputationalName(computationalResource.getComputationalName())
				.withExploratoryName(userInstance.getExploratoryName())
				.withComputationalImage(computationalResource.getImageName())
				.withApplicationName(getApplicationNameFromImage(userInstance.getImageName()))
				.withLibs(new ArrayList<>());
	}

	@SuppressWarnings("unchecked")
	public <T extends LibListComputationalDTO> T newLibComputationalList(UserInfo userInfo,
																		 UserInstanceDTO userInstance,
																		 UserComputationalResource
																					 computationalResource) {

		checkInappropriateCloudProviderOrElseThrowException();
		return (T) newResourceSysBaseDTO(userInfo, LibListComputationalDTO.class)
				.withComputationalId(computationalResource.getComputationalId())
				.withComputationalImage(computationalResource.getImageName())
				.withLibCacheKey(ExploratoryLibCache.libraryCacheKey(userInstance))
				.withApplicationName(getApplicationNameFromImage(userInstance.getImageName()));
	}

	@SuppressWarnings("unchecked")
	public <T extends ComputationalBase<T>> T newComputationalCreate(UserInfo userInfo,
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
			case GCP:
				GcpComputationalCreateForm gcpForm = (GcpComputationalCreateForm) form;
				computationalCreate = (T) newResourceSysBaseDTO(userInfo, ComputationalCreateGcp.class)
						.withMasterInstanceCount(gcpForm.getMasterInstanceCount())
						.withSlaveInstanceCount(gcpForm.getSlaveInstanceCount())
						.withPreemptibleCount(gcpForm.getPreemptibleCount())
						.withMasterInstanceType(gcpForm.getMasterInstanceType())
						.withSlaveInstanceType(gcpForm.getSlaveInstanceType())
						.withVersion(gcpForm.getVersion());
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
	public <T extends ComputationalBase<T>> T newComputationalCreate(UserInfo userInfo,
																	 UserInstanceDTO userInstance,
																	 SparkStandaloneClusterCreateForm form) {

		T computationalCreate;

		switch (cloudProvider()) {
			case AWS:
				computationalCreate = (T) newResourceSysBaseDTO(userInfo, SparkComputationalCreateAws.class)
						.withDataEngineInstanceCount(form.getDataEngineInstanceCount())
						.withDataEngineMasterShape(form.getDataEngineInstanceShape())
						.withDataEngineSlaveShape(form.getDataEngineInstanceShape());
				break;
			case AZURE:
				computationalCreate = (T) newResourceSysBaseDTO(userInfo, SparkComputationalCreateAzure.class)
						.withDataEngineInstanceCount(form.getDataEngineInstanceCount())
						.withDataEngineMasterSize(form.getDataEngineInstanceShape())
						.withDataEngineSlaveSize(form.getDataEngineInstanceShape());

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
						.withDataEngineMasterSize(form.getDataEngineInstanceShape())
						.withDataEngineSlaveSize(form.getDataEngineInstanceShape());
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
	public <T extends ComputationalBase<T>> T newComputationalTerminate(UserInfo userInfo,
																		String exploratoryName,
																		String exploratoryId,
																		String computationalName,
																		String computationalId,
																		DataEngineType dataEngineType) {
		T computationalTerminate;

		switch (cloudProvider()) {
			case AWS:
				AwsComputationalTerminateDTO terminateDTO = newResourceSysBaseDTO(userInfo,
						AwsComputationalTerminateDTO.class);
				if (dataEngineType == DataEngineType.CLOUD_SERVICE) {
					terminateDTO.setClusterName(computationalId);
				}
				computationalTerminate = (T) terminateDTO;
				break;
			case AZURE:
				computationalTerminate = (T) newResourceSysBaseDTO(userInfo, ComputationalTerminateDTO.class);
				break;
			case GCP:
				GcpComputationalTerminateDTO gcpTerminateDTO = newResourceSysBaseDTO(userInfo,
						GcpComputationalTerminateDTO.class);
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

	@SuppressWarnings("unchecked")
	public <T extends ComputationalBase<T>> T newComputationalStop(UserInfo userInfo,
																   String exploratoryName,
																   String exploratoryId,
																   String computationalName) {
		return (T) newResourceSysBaseDTO(userInfo, ComputationalStopDTO.class)
				.withExploratoryName(exploratoryName)
				.withComputationalName(computationalName)
				.withNotebookInstanceName(exploratoryId);
	}

	@SuppressWarnings("unchecked")
	public <T extends ComputationalBase<T>> T newComputationalStart(UserInfo userInfo, String exploratoryName,
																	String exploratoryId, String computationalName) {
		return (T) newResourceSysBaseDTO(userInfo, ComputationalStartDTO.class)
				.withExploratoryName(exploratoryName)
				.withComputationalName(computationalName)
				.withNotebookInstanceName(exploratoryId);
	}

	@SuppressWarnings("unchecked")
	public <T extends ExploratoryImageDTO> T newExploratoryImageCreate(UserInfo userInfo, UserInstanceDTO userInstance,
																	   String imageName) {
		checkInappropriateCloudProviderOrElseThrowException();
		return (T) newResourceSysBaseDTO(userInfo, ExploratoryImageDTO.class)
				.withNotebookInstanceName(userInstance.getExploratoryId())
				.withExploratoryName(userInstance.getExploratoryName())
				.withApplicationName(getApplicationNameFromImage(userInstance.getImageName()))
				.withNotebookImage(userInstance.getImageName())
				.withImageName(imageName);
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

	private CloudProvider cloudProvider() {
		return configuration.getCloudProvider();
	}

	/**
	 * Returns application name basing on docker image
	 *
	 * @param imageName docker image name
	 * @return application name
	 */
	private String getApplicationNameFromImage(String imageName) {
		if (imageName != null) {
			int pos = imageName.lastIndexOf('-');
			if (pos > 0) {
				return imageName.substring(pos + 1);
			}
		}
		return "";
	}

	private void checkInappropriateCloudProviderOrElseThrowException() {
		CloudProvider provider = cloudProvider();
		if (provider != AWS && provider != AZURE && provider != GCP) {
			throw new IllegalArgumentException(UNSUPPORTED_CLOUD_PROVIDER_MESSAGE + provider);
		}
	}
}



