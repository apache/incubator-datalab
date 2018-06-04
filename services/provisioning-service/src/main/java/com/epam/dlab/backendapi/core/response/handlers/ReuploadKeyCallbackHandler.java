package com.epam.dlab.backendapi.core.response.handlers;

import com.epam.dlab.backendapi.core.FileHandlerCallback;
import com.epam.dlab.dto.reuploadkey.ReuploadKeyCallbackDTO;
import com.epam.dlab.dto.reuploadkey.ReuploadKeyStatus;
import com.epam.dlab.dto.reuploadkey.ReuploadKeyStatusDTO;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.client.RESTService;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import javax.ws.rs.core.Response;

@Slf4j
public class ReuploadKeyCallbackHandler implements FileHandlerCallback {
	private static final ObjectMapper MAPPER = new ObjectMapper()
			.configure(JsonParser.Feature.AUTO_CLOSE_SOURCE, true);
	private static final String STATUS_FIELD = "status";
	private static final String ERROR_MESSAGE_FIELD = "error_message";
	private final String uuid;
	private final ReuploadKeyCallbackDTO dto;
	private final RESTService selfService;
	private final String callbackUrl;
	private final String user;

	public ReuploadKeyCallbackHandler(RESTService selfService, String callbackUrl, String user,
									  ReuploadKeyCallbackDTO dto) {
		this.selfService = selfService;
		this.uuid = dto.getId();
		this.callbackUrl = callbackUrl;
		this.user = user;
		this.dto = dto;
	}

	@Override
	public String getUUID() {
		return uuid;
	}

	@Override
	public boolean checkUUID(String uuid) {
		return this.uuid.equals(uuid);
	}

	@Override
	public boolean handle(String fileName, byte[] content) throws Exception {
		final String fileContent = new String(content);
		log.debug("Got file {} while waiting for UUID {}, reupload key response: {}", fileName, uuid, fileContent);

		final JsonNode jsonNode = MAPPER.readTree(fileContent);
		final String status = jsonNode.get(STATUS_FIELD).textValue();
		ReuploadKeyStatusDTO reuploadKeyStatusDTO;
		if ("ok".equals(status)) {
			reuploadKeyStatusDTO = buildReuploadKeyStatusDto(ReuploadKeyStatus.COMPLETED);
		} else {
			reuploadKeyStatusDTO = buildReuploadKeyStatusDto(ReuploadKeyStatus.FAILED)
					.withErrorMessage(jsonNode.get(ERROR_MESSAGE_FIELD).textValue());
		}
		selfServicePost(reuploadKeyStatusDTO);
		return "ok".equals(status);
	}

	private void selfServicePost(ReuploadKeyStatusDTO statusDTO) {
		log.debug("Send post request to self service for UUID {}, object is {}", uuid, statusDTO);
		try {
			selfService.post(callbackUrl, statusDTO, Response.class);
		} catch (Exception e) {
			log.error("Send request or response error for UUID {}: {}", uuid, e.getLocalizedMessage(), e);
			throw new DlabException("Send request or response error for UUID " + uuid + ": "
					+ e.getLocalizedMessage(), e);
		}
	}

	@Override
	public void handleError(String errorMessage) {
		buildReuploadKeyStatusDto(ReuploadKeyStatus.FAILED)
				.withErrorMessage(errorMessage);
	}

	private ReuploadKeyStatusDTO buildReuploadKeyStatusDto(ReuploadKeyStatus status) {
		return new ReuploadKeyStatusDTO()
				.withRequestId(uuid)
				.withReuploadKeyCallbackDto(dto)
				.withReuploadKeyStatus(status)
				.withUser(user);
	}

}

