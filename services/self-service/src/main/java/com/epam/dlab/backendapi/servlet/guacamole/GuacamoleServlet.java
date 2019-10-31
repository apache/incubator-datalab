package com.epam.dlab.backendapi.servlet.guacamole;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.service.GuacamoleService;
import com.epam.dlab.exceptions.DlabException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import lombok.Data;
import org.apache.guacamole.net.GuacamoleTunnel;
import org.apache.guacamole.servlet.GuacamoleHTTPTunnelServlet;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class GuacamoleServlet extends GuacamoleHTTPTunnelServlet {
	static final String USER_ATTRIBUTE = "user";
	private final GuacamoleService guacamoleService;
	private final ObjectMapper mapper;

	@Inject
	public GuacamoleServlet(GuacamoleService guacamoleService, ObjectMapper mapper) {
		this.mapper = mapper;
		this.guacamoleService = guacamoleService;
	}

	@Override
	protected GuacamoleTunnel doConnect(HttpServletRequest request) {
		try {
			final UserInfo userInfo = (UserInfo) request.getAttribute(USER_ATTRIBUTE);
			final CreateTerminalDTO createTerminalDTO = mapper.readValue(request.getReader(), CreateTerminalDTO.class);
			return guacamoleService.getTunnel(userInfo, createTerminalDTO.getHost(), createTerminalDTO.getEndpoint());
		} catch (IOException e) {
			throw new DlabException("Can not read request body: " + e.getMessage(), e);
		}
	}

	@Data
	private static class CreateTerminalDTO {
		private String host;
		private String endpoint;
	}
}
