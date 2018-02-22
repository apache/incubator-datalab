package com.epam.dlab.backendapi.service.azure;

import com.epam.dlab.backendapi.service.InfrastructureTemplatesServiceBase;
import com.epam.dlab.dto.base.computational.FullComputationalTemplate;
import com.epam.dlab.dto.imagemetadata.ComputationalMetadataDTO;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Singleton
@Slf4j
public class AzureInfrastructureTemplatesService extends InfrastructureTemplatesServiceBase {

    @Override
    protected FullComputationalTemplate getCloudFullComputationalTemplate(ComputationalMetadataDTO metadataDTO) {
        log.error("Operation is not supported currently");
        throw new UnsupportedOperationException("Operation is not supported currently");
    }
}
