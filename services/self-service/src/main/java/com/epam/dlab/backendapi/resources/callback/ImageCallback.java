package com.epam.dlab.backendapi.resources.callback;

import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.service.ImageExploratoryService;
import com.epam.dlab.dto.exploratory.ImageCreateStatusDTO;
import com.epam.dlab.model.exloratory.Image;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/infrastructure_provision/image")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class ImageCallback {

    @Inject
    private ImageExploratoryService imageExploratoryService;


    @POST
    @Path("/image_status")
    public Response imageCreateStatus(ImageCreateStatusDTO dto) {
        log.debug("Updating status of image {} for user {} to {}", dto.getName(), dto.getUser(), dto);
        RequestId.remove(dto.getRequestId());
        imageExploratoryService.finishImageCreate(getImage(dto), dto.getExploratoryName(), dto.getImageCreateDTO().getIp());
        return Response.status(Response.Status.CREATED).build();
    }


    private Image getImage(ImageCreateStatusDTO dto) {
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
