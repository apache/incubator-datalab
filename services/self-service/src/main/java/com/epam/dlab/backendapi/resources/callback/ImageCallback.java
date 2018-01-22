package com.epam.dlab.backendapi.resources.callback;

import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.dto.exploratory.ImageCreateStatusDTO;
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


    @POST
    @Path("/image_status")
    public Response imageCreateStatus(ImageCreateStatusDTO dto) {
        RequestId.remove(dto.getRequestId());
        return Response.status(Response.Status.CREATED).build();
    }
}
