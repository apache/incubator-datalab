package com.epam.dlab.backendapi.servlet.guacamole;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.auth.SelfServiceSecurityAuthenticator;
import com.google.inject.Inject;
import io.dropwizard.auth.AuthenticationException;
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
	private final SelfServiceSecurityAuthenticator authenticator;

	@Inject
	public GuacamoleSecurityFilter(SelfServiceSecurityAuthenticator authenticator) {
		this.authenticator = authenticator;
	}

	@Override
	public void init(FilterConfig filterConfig) {

	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
		HttpServletRequest request = (HttpServletRequest) servletRequest;
		HttpServletResponse response = (HttpServletResponse) servletResponse;
		final String authorization = request.getHeader(HttpHeaders.AUTHORIZATION);
		try {
			final String credentials = StringUtils.substringAfter(authorization, AUTH_HEADER_PREFIX);
			final Optional<UserInfo> user = authenticator.authenticate(credentials);
			if (user.isPresent()) {
				request.setAttribute(GuacamoleServlet.USER_ATTRIBUTE, user.get());
				filterChain.doFilter(servletRequest, servletResponse);
			} else {
				response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
			}
		} catch (AuthenticationException e) {
			log.error("Authentication error occurred: {}", e.getMessage());
		}

	}

	@Override
	public void destroy() {

	}
}
