package com.epam.dlab.backendapi.core.response.handlers;

import com.epam.dlab.backendapi.core.commands.DockerAction;
import com.epam.dlab.dto.exploratory.ExploratoryImage;
import com.epam.dlab.dto.exploratory.ImageCreateStatusDTO;
import com.epam.dlab.rest.client.RESTService;
import com.epam.dlab.rest.contracts.ApiCallbacks;
import com.fasterxml.jackson.databind.JsonNode;

public class ImageCreateCallbackHandler extends ResourceCallbackHandler<ImageCreateStatusDTO> {
    public ImageCreateCallbackHandler(RESTService selfService, String user, String uuid, DockerAction action, ExploratoryImage image) {
        super(selfService, user, uuid, action);
    }

    @Override
    protected String getCallbackURI() {
        return ApiCallbacks.IMAGE_STATUS_URI;
    }

    @Override
    protected ImageCreateStatusDTO parseOutResponse(JsonNode document, ImageCreateStatusDTO statusDTO) {
        return new ImageCreateStatusDTO("image1", "some description", "CREATED", null);
    }
}
