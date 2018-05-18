package com.epam.dlab.backendapi.resources.callback;

import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.service.ReuploadKeyService;
import com.epam.dlab.dto.reuploadkey.ReuploadKeyStatusDTO;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

@Path("infrastructure/reupload_key")
@Consumes(MediaType.APPLICATION_JSON)
@Slf4j
public class ReuploadKeyCallback {

	@Inject
	private RequestId requestId;
	@Inject
	private ReuploadKeyService reuploadKeyService;

	@Context
	private UriInfo uriInfo;

	@POST
	@Path("/callback")
	public Response reuploadKeyResponse(ReuploadKeyStatusDTO dto) {
		requestId.checkAndRemove(dto.getRequestId());
		reuploadKeyService.processReuploadKeyResponse(dto);
		return Response.ok(uriInfo.getRequestUri()).build();
	}
}
