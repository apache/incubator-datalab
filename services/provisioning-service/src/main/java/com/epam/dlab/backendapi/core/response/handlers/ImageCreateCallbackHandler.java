package com.epam.dlab.backendapi.core.response.handlers;

import com.epam.dlab.UserInstanceStatus;
import com.epam.dlab.backendapi.core.commands.DockerAction;
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

    public ImageCreateCallbackHandler(RESTService selfService, String user, String uuid, DockerAction action, String imageName) {
        super(selfService, user, uuid, action);
        this.imageName = imageName;
    }

    @Override
    protected String getCallbackURI() {
        return ApiCallbacks.IMAGE_STATUS_URI;
    }

    @Override
    protected ImageCreateStatusDTO parseOutResponse(JsonNode document, ImageCreateStatusDTO statusDTO) {
        if (UserInstanceStatus.FAILED == UserInstanceStatus.of(statusDTO.getStatus())) {
            final ImageCreateStatusDTO.ImageCreateDTO imageCreateDTO =
                    new ImageCreateStatusDTO.ImageCreateDTO(imageName, getUser(), ImageStatus.valueOf(statusDTO.getStatus()), statusDTO.getErrorMessage());
            statusDTO.setImageCreateDTO(imageCreateDTO);
        } else {
            final String content = document.toString();
            try {
                statusDTO.setImageCreateDTO(mapper.readValue(content, new TypeReference<ImageCreateStatusDTO.ImageCreateDTO>() {
                }));
            } catch (IOException e) {
                log.error("Can't parse create image response  with content {} for uuid {}", content, getUUID());
                throw new DlabException(String.format("Can't parse create image response  with content %s for uuid %s", content, getUUID()), e);
            }
        }

        return statusDTO;
    }
}
