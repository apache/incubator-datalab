package com.epam.dlab.backendapi.resources.callback;

import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.service.AccessKeyService;
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

@Path("infrastructure/reupload_key/status")
@Consumes(MediaType.APPLICATION_JSON)
@Slf4j
public class ReuploadKeyCallback {

	@Inject
	private RequestId requestId;
	@Inject
	private AccessKeyService accessKeyService;

	@Context
	private UriInfo uriInfo;

	@POST
	public Response status(ReuploadKeyStatusDTO dto) {
		requestId.remove(dto.getRequestId());
		accessKeyService.processReuploadKeyResponse(dto);
		return Response.created(uriInfo.getRequestUri()).build();
	}
}
