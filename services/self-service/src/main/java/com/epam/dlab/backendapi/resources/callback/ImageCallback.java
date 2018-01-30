package com.epam.dlab.backendapi.resources.callback;

import com.epam.dlab.backendapi.service.ImageExploratoryService;
import com.epam.dlab.dto.exploratory.ImageCreateStatusDTO;
import com.epam.dlab.model.exloratory.Image;
import com.google.inject.Inject;

public class ImageCallback {

    @Inject
    protected ImageExploratoryService imageExploratoryService;

    protected Image getImage(ImageCreateStatusDTO dto) {
        return Image.builder()
                .name(dto.getName())
                .externalName(dto.getImageCreateDTO().getExternalName())
                .fullName(dto.getImageCreateDTO().getFullName())
                .status(dto.getImageCreateDTO().getStatus())
                .externalId(dto.getImageCreateDTO().getExternalId())
                .user(dto.getUser())
                .application(dto.getImageCreateDTO().getApplication())
                .build();
    }
}
