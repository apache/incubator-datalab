package com.epam.dlab.backendapi.servlet.guacamole;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.auth.UserInfoDAO;
import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.util.Optional;

@Slf4j
public class GuacamoleSecurityFilter implements Filter {
	private static final String AUTH_HEADER_PREFIX = "Bearer ";

	private final UserInfoDAO userInfoDAO;

	@Inject
	public GuacamoleSecurityFilter(UserInfoDAO userInfoDAO) {
		this.userInfoDAO = userInfoDAO;
	}

	@Override
	public void init(FilterConfig filterConfig) {

	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) servletRequest;
		HttpServletResponse response = (HttpServletResponse) servletResponse;
		final String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
		final String credentials = StringUtils.substringAfter(authorization, AUTH_HEADER_PREFIX);
		final Optional<UserInfo> user = userInfoDAO.getUserInfoByAccessToken(credentials);
		if (user.isPresent()) {
			request.setAttribute(GuacamoleServlet.USER_ATTRIBUTE, user.get());
			filterChain.doFilter(servletRequest, servletResponse);
		} else {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
		}
		filterChain.doFilter(servletRequest, servletResponse);

	}

	@Override
	public void destroy() {

	}
}
