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
import com.epam.datalab.backendapi.conf.SelfServiceApplicationConfiguration;
import com.epam.datalab.backendapi.dao.GpuDAO;
import com.epam.datalab.backendapi.dao.ProjectDAO;
import com.epam.datalab.backendapi.dao.SettingsDAO;
import com.epam.datalab.backendapi.dao.UserGroupDAO;
import com.epam.datalab.backendapi.domain.EndpointDTO;
import com.epam.datalab.backendapi.resources.dto.SparkStandaloneConfiguration;
import com.epam.datalab.backendapi.resources.dto.aws.AwsEmrConfiguration;
import com.epam.datalab.backendapi.resources.dto.azure.AzureHDInsightConfiguration;
import com.epam.datalab.backendapi.resources.dto.gcp.GcpDataprocConfiguration;
import com.epam.datalab.backendapi.roles.RoleType;
import com.epam.datalab.backendapi.roles.UserRoles;
import com.epam.datalab.backendapi.service.EndpointService;
import com.epam.datalab.backendapi.service.InfrastructureTemplateService;
import com.epam.datalab.cloud.CloudProvider;
import com.epam.datalab.constants.ServiceConsts;
import com.epam.datalab.dto.base.DataEngineType;
import com.epam.datalab.dto.base.computational.FullComputationalTemplate;
import com.epam.datalab.dto.imagemetadata.ComputationalMetadataDTO;
import com.epam.datalab.dto.imagemetadata.ComputationalResourceShapeDto;
import com.epam.datalab.dto.imagemetadata.ExploratoryMetadataDTO;
import com.epam.datalab.dto.imagemetadata.ImageMetadataDTO;
import com.epam.datalab.exceptions.DatalabException;
import com.epam.datalab.rest.client.RESTService;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.epam.datalab.cloud.CloudProvider.AZURE;
import static com.epam.datalab.rest.contracts.DockerAPI.DOCKER_COMPUTATIONAL;
import static com.epam.datalab.rest.contracts.DockerAPI.DOCKER_EXPLORATORY;

@Slf4j
public class InfrastructureTemplateServiceImpl implements InfrastructureTemplateService {

    private final SelfServiceApplicationConfiguration configuration;
    private final SettingsDAO settingsDAO;
    private final UserGroupDAO userGroupDao;
    private final GpuDAO gpuDAO;
    private final EndpointService endpointService;
    private final RESTService provisioningService;

    @Inject
    public InfrastructureTemplateServiceImpl(SelfServiceApplicationConfiguration configuration, SettingsDAO settingsDAO,
                                             ProjectDAO projectDAO, EndpointService endpointService,
                                             UserGroupDAO userGroupDao, GpuDAO gpuDAO,
                                             @Named(ServiceConsts.PROVISIONING_SERVICE_NAME) RESTService provisioningService) {
        this.configuration = configuration;
        this.settingsDAO = settingsDAO;
        this.endpointService = endpointService;
        this.userGroupDao = userGroupDao;
        this.gpuDAO = gpuDAO;
        this.provisioningService = provisioningService;
    }

    @Override
    public List<ExploratoryMetadataDTO> getExploratoryTemplates(UserInfo user, String project, String endpoint) {
        log.debug("Loading list of exploratory templates for user {} for project {}", user.getName(), project);
        try {
            EndpointDTO endpointDTO = endpointService.get(endpoint);
            Set<String> roles = userGroupDao.getUserGroups(user.getName());
            return changeExploratoryMetadata(user, project, endpointDTO, roles);
        } catch (DatalabException e) {
            log.error("Could not load list of exploratory templates for user: {}", user.getName(), e);
            throw e;
        }
    }

    @Override
    public List<FullComputationalTemplate> getComputationalTemplates(UserInfo user, String project, String endpoint) {

        log.debug("Loading list of computational templates for user {}", user.getName());
        try {
            EndpointDTO endpointDTO = endpointService.get(endpoint);
            ComputationalMetadataDTO[] array =
                    provisioningService.get(endpointDTO.getUrl() + DOCKER_COMPUTATIONAL,
                            user.getAccessToken(), ComputationalMetadataDTO[]
                                    .class);
            log.info("loaded: {}", (Object) array);

            final Set<String> roles = userGroupDao.getUserGroups(user.getName());

            return Arrays.stream(array)
                    .peek(e -> e.setImage(getSimpleImageName(e.getImage())))
                    .peek(e -> filterShapes(user, e.getComputationResourceShapes(), RoleType.COMPUTATIONAL_SHAPES, roles))
                    .filter(e -> UserRoles.checkAccess(user, RoleType.COMPUTATIONAL, e.getImage(), roles))
                    .map(comp -> fullComputationalTemplate(comp, endpointDTO.getCloudProvider(), project))
                    .collect(Collectors.toList());

        } catch (DatalabException e) {
            log.error("Could not load list of computational templates for user: {}", user.getName(), e);
            throw e;
        }
    }

    protected FullComputationalTemplate getCloudFullComputationalTemplate(ComputationalMetadataDTO metadataDTO,
                                                                          CloudProvider cloudProvider) {

        switch (cloudProvider) {
            case AWS:
                return new AwsFullComputationalTemplate(metadataDTO,
                        AwsEmrConfiguration.builder()
                                .minEmrInstanceCount(configuration.getMinEmrInstanceCount())
                                .maxEmrInstanceCount(configuration.getMaxEmrInstanceCount())
                                .maxEmrSpotInstanceBidPct(configuration.getMaxEmrSpotInstanceBidPct())
                                .minEmrSpotInstanceBidPct(configuration.getMinEmrSpotInstanceBidPct())
                                .build());
            case GCP:
                return new GcpFullComputationalTemplate(metadataDTO,
                        GcpDataprocConfiguration.builder()
                                .minInstanceCount(configuration.getMinInstanceCount())
                                .maxInstanceCount(configuration.getMaxInstanceCount())
                                .minDataprocPreemptibleInstanceCount(configuration.getMinDataprocPreemptibleCount())
                                .build());
            case AZURE:
                return new AzureFullComputationalTemplate(metadataDTO,
                        AzureHDInsightConfiguration.builder()
                                .minHdinsightInstanceCount(configuration.getMinHDInsightInstanceCount())
                                .maxHdinsightInstanceCount(configuration.getMaxHDInsightInstanceCount())
                                .build());
            default:
                throw new UnsupportedOperationException("Dataengine service is not supported currently for " + cloudProvider);
        }
    }

    private List<ExploratoryMetadataDTO> changeExploratoryMetadata(UserInfo user, String project, EndpointDTO endpointDTO, Set<String> roles) {
        return getExploratoryMetadata(user, endpointDTO).stream()
                .peek(e -> e.setImage(getSimpleImageName(e.getImage())))
                .filter(e -> exploratoryGpuIssuesAzureFilter(e, endpointDTO.getCloudProvider()) &&
                        UserRoles.checkAccess(user, RoleType.EXPLORATORY, e.getImage(), roles))
                .peek(e -> filterShapes(user, e.getExploratoryEnvironmentShapes(), RoleType.EXPLORATORY_SHAPES, roles))
                .peek(e -> addGpu(e, project))
                .collect(Collectors.toList());
    }

    private List<ExploratoryMetadataDTO> getExploratoryMetadata(UserInfo user, EndpointDTO endpointDTO) {
        return Arrays.asList(provisioningService.get(endpointDTO.getUrl() + DOCKER_EXPLORATORY,
                user.getAccessToken(),
                ExploratoryMetadataDTO[].class));
    }


    /**
     * Removes shapes for which user does not have an access
     *
     * @param user              user
     * @param environmentShapes shape types
     * @param roleType
     * @param roles
     */
    private void filterShapes(UserInfo user, Map<String, List<ComputationalResourceShapeDto>> environmentShapes,
                              RoleType roleType, Set<String> roles) {
        environmentShapes.forEach((k, v) -> v.removeIf(compResShapeDto ->
                !UserRoles.checkAccess(user, roleType, compResShapeDto.getType(), roles))
        );
    }

    /**
     * Temporary filter for creation of exploratory env due to Azure issues
     */
    private boolean exploratoryGpuIssuesAzureFilter(ExploratoryMetadataDTO e, CloudProvider cloudProvider) {
        return (!"redhat".equals(settingsDAO.getConfOsFamily()) || cloudProvider != AZURE) ||
                !(e.getImage().endsWith("deeplearning") || e.getImage().endsWith("tensor"));
    }

    /**
     * Return the image name without suffix version.
     *
     * @param imageName the name of image.
     */
    private String getSimpleImageName(String imageName) {
        int separatorIndex = imageName.indexOf(':');
        return (separatorIndex > 0 ? imageName.substring(0, separatorIndex) : imageName);
    }

    /**
     * Wraps metadata with limits
     *
     * @param metadataDTO   metadata
     * @param cloudProvider cloudProvider
     * @return wrapped object
     */

    private FullComputationalTemplate fullComputationalTemplate(ComputationalMetadataDTO metadataDTO,
                                                                CloudProvider cloudProvider, String projectName) {

        DataEngineType dataEngineType = DataEngineType.fromDockerImageName(metadataDTO.getImage());
        addGpu(metadataDTO, projectName);
        if (dataEngineType == DataEngineType.CLOUD_SERVICE) {
            return getCloudFullComputationalTemplate(metadataDTO, cloudProvider);
        } else if (dataEngineType == DataEngineType.SPARK_STANDALONE) {
            return new SparkFullComputationalTemplate(metadataDTO,
                    SparkStandaloneConfiguration.builder()
                            .maxSparkInstanceCount(configuration.getMaxSparkInstanceCount())
                            .minSparkInstanceCount(configuration.getMinSparkInstanceCount())
                            .build());
        } else {
            throw new IllegalArgumentException("Unknown data engine " + dataEngineType);
        }
    }

    private void addGpu(ImageMetadataDTO e, String projectName) {
        log.info("Trying to read GPU from DB for {}", projectName);
        try {
            if (e instanceof ExploratoryMetadataDTO) {
                gpuDAO.getGPUByProjectName(projectName).ifPresent(edgeGPU ->
                        ((ExploratoryMetadataDTO) e).setComputationGPU(edgeGPU.getGpus()));
            } else
                gpuDAO.getGPUByProjectName(projectName).ifPresent(edgeGPU ->
                        ((ComputationalMetadataDTO) e).setComputationGPU(edgeGPU.getGpus()));
        } catch (Exception ex) {
            log.info(ex.getMessage());
        }
    }


    private static class AwsFullComputationalTemplate extends FullComputationalTemplate {
        @JsonProperty("limits")
        private AwsEmrConfiguration awsEmrConfiguration;

        AwsFullComputationalTemplate(ComputationalMetadataDTO metadataDTO,
                                     AwsEmrConfiguration awsEmrConfiguration) {
            super(metadataDTO);
            this.awsEmrConfiguration = awsEmrConfiguration;
        }
    }

    private static class AzureFullComputationalTemplate extends FullComputationalTemplate {
        @JsonProperty("limits")
        private AzureHDInsightConfiguration azureHDInsightConfiguration;

        AzureFullComputationalTemplate(ComputationalMetadataDTO metadataDTO,
                                       AzureHDInsightConfiguration azureHDInsightConfiguration){
            super(metadataDTO);
            this.azureHDInsightConfiguration = azureHDInsightConfiguration;
        }
    }

    private static class GcpFullComputationalTemplate extends FullComputationalTemplate {
        @JsonProperty("limits")
        private GcpDataprocConfiguration gcpDataprocConfiguration;

        GcpFullComputationalTemplate(ComputationalMetadataDTO metadataDTO,
                                     GcpDataprocConfiguration gcpDataprocConfiguration) {
            super(metadataDTO);
            this.gcpDataprocConfiguration = gcpDataprocConfiguration;
        }
    }

    private static class SparkFullComputationalTemplate extends FullComputationalTemplate {
        @JsonProperty("limits")
        private SparkStandaloneConfiguration sparkStandaloneConfiguration;

        SparkFullComputationalTemplate(ComputationalMetadataDTO metadataDTO,
                                       SparkStandaloneConfiguration sparkStandaloneConfiguration) {
            super(metadataDTO);
            this.sparkStandaloneConfiguration = sparkStandaloneConfiguration;
        }
    }

}
