package com.epam.dlab.dto.base.computational;

import com.epam.dlab.dto.imagemetadata.ComputationalMetadataDTO;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

public class FullComputationalTemplate {
    @JsonUnwrapped
    private ComputationalMetadataDTO computationalMetadataDTO;


    public FullComputationalTemplate(ComputationalMetadataDTO metadataDTO) {
        this.computationalMetadataDTO = metadataDTO;
    }
}
