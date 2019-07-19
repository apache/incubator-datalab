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
import com.epam.dlab.dto.aws.computational.ClusterConfig;
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
import com.epam.dlab.dto.base.keyload.UploadFile;
import com.epam.dlab.dto.computational.*;
import com.epam.dlab.dto.exploratory.*;
import com.epam.dlab.dto.gcp.GcpCloudSettings;
import com.epam.dlab.dto.gcp.computational.ComputationalCreateGcp;
import com.epam.dlab.dto.gcp.computational.GcpComputationalTerminateDTO;
import com.epam.dlab.dto.gcp.computational.SparkComputationalCreateGcp;
import com.epam.dlab.dto.gcp.edge.EdgeCreateGcp;
import com.epam.dlab.dto.gcp.exploratory.ExploratoryCreateGcp;
import com.epam.dlab.dto.gcp.keyload.UploadFileGcp;
import com.epam.dlab.dto.reuploadkey.ReuploadKeyDTO;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.model.ResourceData;
import com.epam.dlab.model.exploratory.Exploratory;
import com.epam.dlab.util.UsernameUtils;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.List;
import java.util.Map;
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
						.awsNotebookSubnetId(settingsDAO.getAwsNotebookSubnetId())
						.awsNotebookVpcId(settingsDAO.getAwsNotebookVpcId())
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

	public ReuploadKeyDTO newKeyReupload(UserInfo userInfo, String id, String content, List<ResourceData> resources) {
		return newResourceSysBaseDTO(userInfo, ReuploadKeyDTO.class)
				.withId(id)
				.withContent(content)
				.withResources(resources);
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
																	  ExploratoryGitCredsDTO exploratoryGitCredsDTO,
																	  Map<String, String> tags) {

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
				.withImageName(exploratory.getImageName())
				.withClusterConfig(exploratory.getClusterConfig())
				.withProject(exploratory.getProject())
				.withEndpoint(exploratory.getEndpoint())
				.withTags(tags);
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
						.withReuploadKeyRequired(userInstance.isReuploadKeyRequired())
						.withProject(userInstance.getProject());
			case AZURE:
				T exploratoryStart = (T) newResourceSysBaseDTO(userInfo, ExploratoryActionStartAzure.class)
						.withNotebookInstanceName(userInstance.getExploratoryId())
						.withGitCreds(exploratoryGitCredsDTO.getGitCreds())
						.withNotebookImage(userInstance.getImageName())
						.withExploratoryName(userInstance.getExploratoryName())
						.withReuploadKeyRequired(userInstance.isReuploadKeyRequired())
						.withProject(userInstance.getProject());

				if (settingsDAO.isAzureDataLakeEnabled()) {
					((ExploratoryActionStartAzure) exploratoryStart)
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
				.withReuploadKeyRequired(userInstance.isReuploadKeyRequired())
				.withProject(userInstance.getProject());
	}

	public ExploratoryGitCredsUpdateDTO newGitCredentialsUpdate(UserInfo userInfo, UserInstanceDTO instanceDTO,
																ExploratoryGitCredsDTO exploratoryGitCredsDTO) {
		checkInappropriateCloudProviderOrElseThrowException();
		return newResourceSysBaseDTO(userInfo, ExploratoryGitCredsUpdateDTO.class)
				.withNotebookImage(instanceDTO.getImageName())
				.withApplicationName(getApplicationNameFromImage(instanceDTO.getImageName()))
				.withProject(instanceDTO.getProject())
				.withNotebookInstanceName(instanceDTO.getExploratoryId())
				.withExploratoryName(instanceDTO.getExploratoryName())
				.withGitCreds(exploratoryGitCredsDTO.getGitCreds());
	}

	public LibraryInstallDTO newLibInstall(UserInfo userInfo, UserInstanceDTO userInstance,
										   List<LibInstallDTO> libs) {
		checkInappropriateCloudProviderOrElseThrowException();
		return newResourceSysBaseDTO(userInfo, LibraryInstallDTO.class)
				.withNotebookImage(userInstance.getImageName())
				.withApplicationName(getApplicationNameFromImage(userInstance.getImageName()))
				.withNotebookInstanceName(userInstance.getExploratoryId())
				.withExploratoryName(userInstance.getExploratoryName())
				.withProject(userInstance.getProject())
				.withLibs(libs);
	}

	@SuppressWarnings("unchecked")
	public <T extends ExploratoryActionDTO<T>> T newLibExploratoryList(UserInfo userInfo,
																	   UserInstanceDTO userInstance) {
		checkInappropriateCloudProviderOrElseThrowException();
		return (T) newResourceSysBaseDTO(userInfo, ExploratoryActionDTO.class)
				.withNotebookInstanceName(userInstance.getExploratoryId())
				.withProject(userInstance.getProject())
				.withNotebookImage(userInstance.getImageName())
				.withApplicationName(getApplicationNameFromImage(userInstance.getImageName()))
				.withExploratoryName(userInstance.getExploratoryName());
	}

	@SuppressWarnings("unchecked")
	public <T extends LibraryInstallDTO> T newLibInstall(UserInfo userInfo, UserInstanceDTO userInstance,
														 UserComputationalResource computationalResource,
														 List<LibInstallDTO> libs) {
		checkInappropriateCloudProviderOrElseThrowException();
		return (T) newResourceSysBaseDTO(userInfo, LibraryInstallDTO.class)
				.withComputationalId(computationalResource.getComputationalId())
				.withComputationalName(computationalResource.getComputationalName())
				.withExploratoryName(userInstance.getExploratoryName())
				.withProject(userInstance.getProject())
				.withComputationalImage(computationalResource.getImageName())
				.withApplicationName(getApplicationNameFromImage(userInstance.getImageName()))
				.withLibs(libs);
	}

	@SuppressWarnings("unchecked")
	public <T extends LibListComputationalDTO> T newLibComputationalList(UserInfo userInfo,
																		 UserInstanceDTO userInstance,
																		 UserComputationalResource
																				 computationalResource) {

		checkInappropriateCloudProviderOrElseThrowException();
		return (T) newResourceSysBaseDTO(userInfo, LibListComputationalDTO.class)
				.withComputationalId(computationalResource.getComputationalId())
				.withProject(userInstance.getProject())
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
			case AZURE:
				throw new UnsupportedOperationException("Creating dataengine service is not supported yet");
			case AWS:
				AwsComputationalCreateForm awsForm = (AwsComputationalCreateForm) form;
				computationalCreate = (T) newResourceSysBaseDTO(userInfo, ComputationalCreateAws.class)
						.withInstanceCount(awsForm.getInstanceCount())
						.withMasterInstanceType(awsForm.getMasterInstanceType())
						.withSlaveInstanceType(awsForm.getSlaveInstanceType())
						.withSlaveInstanceSpot(awsForm.getSlaveInstanceSpot())
						.withSlaveInstanceSpotPctPrice(awsForm.getSlaveInstanceSpotPctPrice())
						.withVersion(awsForm.getVersion())
						.withConfig((awsForm.getConfig()));
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
				.withNotebookInstanceName(userInstance.getExploratoryId())
				.withProject(userInstance.getProject());
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
						.withDataEngineSlaveShape(form.getDataEngineInstanceShape())
						.withConfig(form.getConfig());
				break;
			case AZURE:
				computationalCreate = (T) newResourceSysBaseDTO(userInfo, SparkComputationalCreateAzure.class)
						.withDataEngineInstanceCount(form.getDataEngineInstanceCount())
						.withDataEngineMasterSize(form.getDataEngineInstanceShape())
						.withDataEngineSlaveSize(form.getDataEngineInstanceShape())
						.withConfig(form.getConfig());
				if (settingsDAO.isAzureDataLakeEnabled()) {
					((SparkComputationalCreateAzure) computationalCreate)
							.withAzureUserRefreshToken(userInfo.getKeys().get(AZURE_REFRESH_TOKEN_KEY));
				}

				((SparkComputationalCreateAzure) computationalCreate)
						.withAzureDataLakeEnabled(Boolean.toString(settingsDAO.isAzureDataLakeEnabled()));

				break;
			case GCP:
				computationalCreate = (T) newResourceSysBaseDTO(userInfo, SparkComputationalCreateGcp.class)
						.withDataEngineInstanceCount(form.getDataEngineInstanceCount())
						.withDataEngineMasterSize(form.getDataEngineInstanceShape())
						.withDataEngineSlaveSize(form.getDataEngineInstanceShape())
						.withConfig(form.getConfig());
				break;
			default:
				throw new IllegalArgumentException(UNSUPPORTED_CLOUD_PROVIDER_MESSAGE + cloudProvider());
		}

		return computationalCreate
				.withExploratoryName(form.getNotebookName())
				.withComputationalName(form.getName())
				.withNotebookTemplateName(userInstance.getTemplateName())
				.withApplicationName(getApplicationNameFromImage(userInstance.getImageName()))
				.withNotebookInstanceName(userInstance.getExploratoryId())
				.withProject(userInstance.getProject());
	}

	@SuppressWarnings("unchecked")
	public <T extends ComputationalBase<T>> T newComputationalTerminate(UserInfo userInfo,
																		String exploratoryName,
																		String exploratoryId,
																		String computationalName,
																		String computationalId,
																		DataEngineType dataEngineType,
																		String project) {
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
				.withNotebookInstanceName(exploratoryId)
				.withProject(project);
	}

	@SuppressWarnings("unchecked")
	public <T extends ComputationalBase<T>> T newComputationalStop(UserInfo userInfo,
																   UserInstanceDTO exploratory,
																   String computationalName) {
		return (T) newResourceSysBaseDTO(userInfo, ComputationalStopDTO.class)
				.withExploratoryName(exploratory.getExploratoryName())
				.withComputationalName(computationalName)
				.withNotebookInstanceName(exploratory.getExploratoryId())
				.withApplicationName(getApplicationNameFromImage(exploratory.getImageName()))
				.withProject(exploratory.getProject());
	}

	@SuppressWarnings("unchecked")
	public <T extends ComputationalBase<T>> T newComputationalStart(UserInfo userInfo, UserInstanceDTO exploratory,
																	String computationalName) {
		return (T) newResourceSysBaseDTO(userInfo, ComputationalStartDTO.class)
				.withExploratoryName(exploratory.getExploratoryName())
				.withComputationalName(computationalName)
				.withNotebookInstanceName(exploratory.getExploratoryId())
				.withApplicationName(getApplicationNameFromImage(exploratory.getImageName()))
				.withProject(exploratory.getProject());
	}

	@SuppressWarnings("unchecked")
	public <T extends ExploratoryImageDTO> T newExploratoryImageCreate(UserInfo userInfo, UserInstanceDTO userInstance,
																	   String imageName) {
		checkInappropriateCloudProviderOrElseThrowException();
		return (T) newResourceSysBaseDTO(userInfo, ExploratoryImageDTO.class)
				.withProject(userInstance.getProject())
				.withNotebookInstanceName(userInstance.getExploratoryId())
				.withExploratoryName(userInstance.getExploratoryName())
				.withApplicationName(getApplicationNameFromImage(userInstance.getImageName()))
				.withNotebookImage(userInstance.getImageName())
				.withImageName(imageName);
	}

	@SuppressWarnings("unchecked")
	public <T extends ComputationalBase<T>> T newComputationalCheckInactivity(UserInfo userInfo,
																			  UserInstanceDTO exploratory,
																			  UserComputationalResource cr) {
		return (T) newResourceSysBaseDTO(userInfo, ComputationalCheckInactivityDTO.class)
				.withExploratoryName(exploratory.getExploratoryName())
				.withComputationalName(cr.getComputationalName())
				.withNotebookInstanceName(exploratory.getExploratoryId())
				.withApplicationName(getApplicationNameFromImage(exploratory.getImageName()))
				.withNotebookImageName(exploratory.getImageName())
				.withImage(cr.getImageName())
				.withComputationalId(cr.getComputationalId())
				.withProject(exploratory.getProject());
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
																List<ClusterConfig> config) {
		final ComputationalClusterConfigDTO clusterConfigDTO = newResourceSysBaseDTO(userInfo,
				ComputationalClusterConfigDTO.class)
				.withExploratoryName(userInstanceDTO.getExploratoryName())
				.withNotebookInstanceName(userInstanceDTO.getExploratoryId())
				.withComputationalName(compRes.getComputationalName())
				.withApplicationName(compRes.getImageName());
		clusterConfigDTO.setCopmutationalId(compRes.getComputationalId());
		clusterConfigDTO.setConfig(config);
		if (cloudProvider() == AZURE && settingsDAO.isAzureDataLakeEnabled()) {
			clusterConfigDTO.setAzureUserRefreshToken(userInfo.getKeys().get(AZURE_REFRESH_TOKEN_KEY));
		}

		return clusterConfigDTO;
	}

	public ExploratoryReconfigureSparkClusterActionDTO newClusterConfigUpdate(UserInfo userInfo,
																			  UserInstanceDTO userInstance,
																			  List<ClusterConfig> config) {

		final ExploratoryReconfigureSparkClusterActionDTO dto =
				newResourceSysBaseDTO(userInfo, ExploratoryReconfigureSparkClusterActionDTO.class)
						.withNotebookInstanceName(userInstance.getExploratoryId())
						.withExploratoryName(userInstance.getExploratoryName())
						.withApplicationName(getApplicationNameFromImage(userInstance.getImageName()))
						.withNotebookImage(userInstance.getImageName())
						.withConfig(config);
		if (cloudProvider() == AZURE && settingsDAO.isAzureDataLakeEnabled()) {
			dto.withAzureUserRefreshToken(userInfo.getKeys().get(AZURE_REFRESH_TOKEN_KEY));
		}

		return dto;


	}

	public ExploratoryCheckInactivityAction newExploratoryCheckInactivityAction(UserInfo userInfo,
																				UserInstanceDTO userInstance) {
		final ExploratoryCheckInactivityAction dto = newResourceSysBaseDTO(userInfo,
				ExploratoryCheckInactivityAction.class);
		dto.withNotebookInstanceName(userInstance.getExploratoryId())
				.withNotebookImage(userInstance.getImageName())
				.withExploratoryName(userInstance.getExploratoryName())
				.withReuploadKeyRequired(userInstance.isReuploadKeyRequired())
				.withProject(userInstance.getProject());
		return dto;
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
			int pos = imageName.indexOf('-');
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



