/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.epam.dlab.backendapi.servlet.guacamole;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.SecurityDAO;
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

	private final SecurityDAO securityDAO;

	@Inject
	public GuacamoleSecurityFilter(SecurityDAO securityDAO) {
		this.securityDAO = securityDAO;
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
		final Optional<UserInfo> user = securityDAO.getUser(credentials);
		if (user.isPresent()) {
			request.setAttribute(GuacamoleServlet.USER_ATTRIBUTE, user.get());
			filterChain.doFilter(servletRequest, servletResponse);
		} else {
			response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
		}

	}

	@Override
	public void destroy() {

	}
}
