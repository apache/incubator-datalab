package com.epam.dlab.backendapi.resources.callback.gcp;

import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.resources.callback.base.EdgeCallback;
import com.epam.dlab.dto.base.keyload.UploadFileResult;
import com.epam.dlab.dto.gcp.edge.EdgeInfoGcp;
import com.epam.dlab.rest.contracts.ApiCallbacks;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/infrastructure/edge")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class EdgeCallbackGcp extends EdgeCallback {

    public EdgeCallbackGcp() {
        log.info("{} is initialized", getClass().getSimpleName());
    }

    /**
     * Stores the result of the upload the user key.
     *
     * @param dto result of the upload the user key.
     * @return 200 OK
     */
    @POST
    @Path(ApiCallbacks.STATUS_URI)
    public Response status(UploadFileResult<EdgeInfoGcp> dto) {
        RequestId.checkAndRemove(dto.getRequestId());
        handleEdgeCallback(dto.getUser(), dto.getStatus());
        return Response.ok().build();
    }
}
