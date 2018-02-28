package com.epam.dlab.backendapi.service.aws;

import com.epam.dlab.backendapi.SelfServiceApplicationConfiguration;
import com.epam.dlab.backendapi.resources.dto.aws.AwsEmrConfiguration;
import com.epam.dlab.backendapi.service.impl.InfrastructureTemplateServiceBase;
import com.epam.dlab.dto.base.computational.FullComputationalTemplate;
import com.epam.dlab.dto.imagemetadata.ComputationalMetadataDTO;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.inject.Inject;

public class AwsInfrastructureTemplateService extends InfrastructureTemplateServiceBase {

    @Inject
    private SelfServiceApplicationConfiguration configuration;

    @Override
    protected FullComputationalTemplate getCloudFullComputationalTemplate(ComputationalMetadataDTO metadataDTO) {
        return new AwsFullComputationalTemplate(metadataDTO,
                AwsEmrConfiguration.builder()
                        .minEmrInstanceCount(configuration.getMinEmrInstanceCount())
                        .maxEmrInstanceCount(configuration.getMaxEmrInstanceCount())
                        .maxEmrSpotInstanceBidPct(configuration.getMaxEmrSpotInstanceBidPct())
                        .minEmrSpotInstanceBidPct(configuration.getMinEmrSpotInstanceBidPct())
                        .build());
    }

    private class AwsFullComputationalTemplate extends FullComputationalTemplate {
        @JsonProperty("limits")
        private AwsEmrConfiguration awsEmrConfiguration;

        AwsFullComputationalTemplate(ComputationalMetadataDTO metadataDTO,
                                            AwsEmrConfiguration awsEmrConfiguration) {
            super(metadataDTO);
            this.awsEmrConfiguration = awsEmrConfiguration;
        }
    }
}
