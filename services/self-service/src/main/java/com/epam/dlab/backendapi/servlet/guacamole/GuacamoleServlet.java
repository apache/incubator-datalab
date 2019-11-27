package com.epam.dlab.backendapi.servlet.guacamole;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.SecurityDAO;
import com.epam.dlab.backendapi.service.GuacamoleService;
import com.epam.dlab.exceptions.DlabAuthenticationException;
import com.epam.dlab.exceptions.DlabException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.apache.guacamole.net.GuacamoleTunnel;
import org.apache.guacamole.servlet.GuacamoleHTTPTunnelServlet;
import org.apache.http.HttpStatus;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import java.io.IOException;

public class GuacamoleServlet extends GuacamoleHTTPTunnelServlet {
	private static final String UNAUTHORIZED_MSG = "User is not authenticated";
	private static final String DLAB_PREFIX = "DLab-";
	private final GuacamoleService guacamoleService;
	private final ObjectMapper mapper;
	private final SecurityDAO securityDAO;
	private static final String AUTH_HEADER_PREFIX = "Bearer ";

	@Inject
	public GuacamoleServlet(GuacamoleService guacamoleService, ObjectMapper mapper, SecurityDAO securityDAO) {
		this.mapper = mapper;
		this.guacamoleService = guacamoleService;
		this.securityDAO = securityDAO;
	}

	@Override
	protected GuacamoleTunnel doConnect(HttpServletRequest request) {
		try {
			final String authorization = request.getHeader(DLAB_PREFIX + HttpHeaders.AUTHORIZATION);
			final String credentials = StringUtils.substringAfter(authorization, AUTH_HEADER_PREFIX);
			final UserInfo userInfo =
					securityDAO.getUser(credentials)
							.orElseThrow(() -> new DlabAuthenticationException(UNAUTHORIZED_MSG));
			final CreateTerminalDTO createTerminalDTO = mapper.readValue(request.getReader(), CreateTerminalDTO.class);
			return guacamoleService.getTunnel(userInfo, createTerminalDTO.getHost(), createTerminalDTO.getEndpoint());
		} catch (IOException e) {
			throw new DlabException("Can not read request body: " + e.getMessage(), e);
		}
	}

	@Override
	protected void handleTunnelRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException {
		try {
			super.handleTunnelRequest(request, response);
		} catch (DlabAuthenticationException e) {
			sendError(response, HttpStatus.SC_UNAUTHORIZED, HttpStatus.SC_UNAUTHORIZED, UNAUTHORIZED_MSG);
		}
	}

	@Data
	private static class CreateTerminalDTO {
		private String host;
		private String endpoint;
	}
}
