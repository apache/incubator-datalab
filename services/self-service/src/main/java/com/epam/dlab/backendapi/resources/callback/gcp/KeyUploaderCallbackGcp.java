package com.epam.dlab.backendapi.resources.callback.gcp;

import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.resources.callback.base.KeyUploaderCallback;
import com.epam.dlab.dto.base.keyload.UploadFileResult;
import com.epam.dlab.dto.gcp.edge.EdgeInfoGcp;
import com.epam.dlab.exceptions.DlabException;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("/user/access_key")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Slf4j
public class KeyUploaderCallbackGcp {

    @Inject
    private KeyUploaderCallback keyUploaderCallback;

    public KeyUploaderCallbackGcp() {
        log.info("{} is initialized", getClass().getSimpleName());
    }

    /**
     * Stores the result of the upload the user key.
     *
     * @param dto result of the upload the user key.
     * @return 200 OK
     */
    @POST
    @Path("/callback")
    public Response loadKeyResponse(UploadFileResult<EdgeInfoGcp> dto) throws DlabException {
        log.debug("Upload the key result and EDGE node info for user {}: {}", dto.getUser(), dto);
        RequestId.checkAndRemove(dto.getRequestId());
        keyUploaderCallback.handleCallback(dto.getStatus(), dto.getUser(), dto.getEdgeInfo());

        return Response.ok().build();

    }
}
