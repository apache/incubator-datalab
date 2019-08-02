package com.epam.dlab.backendapi.servlet.guacamole;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.service.GuacamoleService;
import com.epam.dlab.exceptions.DlabException;
import com.google.inject.Inject;
import org.apache.guacamole.net.GuacamoleTunnel;
import org.apache.guacamole.servlet.GuacamoleHTTPTunnelServlet;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public class GuacamoleServlet extends GuacamoleHTTPTunnelServlet {
	static final String USER_ATTRIBUTE = "user";
	private final GuacamoleService guacamoleService;

	@Inject
	public GuacamoleServlet(GuacamoleService guacamoleService) {
		this.guacamoleService = guacamoleService;
	}

	@Override
	protected GuacamoleTunnel doConnect(HttpServletRequest request) {
		try {
			final UserInfo userInfo = (UserInfo) request.getAttribute(USER_ATTRIBUTE);
			final String host = request.getReader().readLine();
			return guacamoleService.getTunnel(userInfo, host);
		} catch (IOException e) {
			throw new DlabException("Can not read request body: " + e.getMessage(), e);
		}
	}
}
