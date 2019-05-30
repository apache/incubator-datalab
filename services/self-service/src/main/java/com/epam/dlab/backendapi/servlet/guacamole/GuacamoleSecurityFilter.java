package com.epam.dlab.backendapi.servlet.guacamole;

import com.google.inject.Inject;
import lombok.extern.slf4j.Slf4j;

import javax.servlet.*;
import java.io.IOException;

@Slf4j
public class GuacamoleSecurityFilter implements Filter {
	private static final String AUTH_HEADER_PREFIX = "Bearer ";

	@Inject
	public GuacamoleSecurityFilter() {

	}

	@Override
	public void init(FilterConfig filterConfig) {

	}

	@Override
	public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
		/*HttpServletRequest request = (HttpServletRequest) servletRequest;
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
		}*/
		filterChain.doFilter(servletRequest, servletResponse);

	}

	@Override
	public void destroy() {

	}
}
