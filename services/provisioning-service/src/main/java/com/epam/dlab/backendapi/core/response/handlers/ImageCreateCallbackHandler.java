package com.epam.dlab.backendapi.core.response.handlers;

import com.epam.dlab.backendapi.core.commands.DockerAction;
import com.epam.dlab.dto.exploratory.ExploratoryImageDTO;
import com.epam.dlab.dto.exploratory.ImageCreateStatusDTO;
import com.epam.dlab.dto.exploratory.ImageStatus;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.client.RESTService;
import com.epam.dlab.rest.contracts.ApiCallbacks;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;

@Slf4j
public class ImageCreateCallbackHandler extends ResourceCallbackHandler<ImageCreateStatusDTO> {
    private final String imageName;
    private final String exploratoryName;

    public ImageCreateCallbackHandler(RESTService selfService, String uuid, DockerAction action, ExploratoryImageDTO image) {
        super(selfService, image.getCloudSettings().getIamUser(), uuid, action);
        this.imageName = image.getImageName();
        this.exploratoryName = image.getExploratoryName();
    }

    @Override
    protected String getCallbackURI() {
        return ApiCallbacks.IMAGE_STATUS_URI;
    }

    @Override
    protected ImageCreateStatusDTO parseOutResponse(JsonNode document, ImageCreateStatusDTO statusDTO) {
        statusDTO.setName(imageName);
        statusDTO.setExploratoryName(exploratoryName);
        statusDTO.setImageCreateDTO(new ImageCreateStatusDTO.ImageCreateDTO());
        final ImageStatus imageStatus = ImageStatus.fromValue(statusDTO.getStatus());
        return imageStatus != ImageStatus.FAILED ?
                statusDTO.withImageCreateDto(toImageCreateDto(document.toString())) : statusDTO.withoutImageCreateDto();
    }

    private ImageCreateStatusDTO.ImageCreateDTO toImageCreateDto(String content) {
        try {
            return mapper.readValue(content, ImageCreateStatusDTO.ImageCreateDTO.class);
        } catch (IOException e) {
            log.error("Can't parse create image response with content {} for uuid {}", content, getUUID());
            throw new DlabException(String.format("Can't parse create image response with content %s for uuid %s", content, getUUID()), e);
        }
    }
}
