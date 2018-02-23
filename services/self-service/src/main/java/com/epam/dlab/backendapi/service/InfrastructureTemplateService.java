package com.epam.dlab.backendapi.service;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.dto.base.computational.FullComputationalTemplate;
import com.epam.dlab.dto.imagemetadata.ExploratoryMetadataDTO;

import java.util.List;

public interface InfrastructureTemplateService {
    List<ExploratoryMetadataDTO> getExploratoryTemplates(UserInfo user);

    List<FullComputationalTemplate> getComputationalTemplates(UserInfo user);
}
