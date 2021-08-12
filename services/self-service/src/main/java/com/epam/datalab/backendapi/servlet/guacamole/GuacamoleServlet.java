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

package com.epam.datalab.backendapi.servlet.guacamole;

import com.epam.datalab.auth.UserInfo;
import com.epam.datalab.backendapi.dao.SecurityDAO;
import com.epam.datalab.backendapi.service.GuacamoleService;
import com.epam.datalab.exceptions.DatalabAuthenticationException;
import com.epam.datalab.exceptions.DatalabException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.guacamole.net.GuacamoleTunnel;
import org.apache.guacamole.servlet.GuacamoleHTTPTunnelServlet;
import org.apache.http.HttpStatus;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import java.io.IOException;

@Slf4j
public class GuacamoleServlet extends GuacamoleHTTPTunnelServlet {
    private static final String UNAUTHORIZED_MSG = "User is not authenticated";
    private static final String DATALAB_PREFIX = "DataLab-";
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
            final String authorization = request.getHeader(DATALAB_PREFIX + HttpHeaders.AUTHORIZATION);
            final String credentials = StringUtils.substringAfter(authorization, AUTH_HEADER_PREFIX);
            final UserInfo userInfo = getUserInfo(credentials);
            final CreateTerminalDTO createTerminalDTO = mapper.readValue(request.getReader(), CreateTerminalDTO.class);
            return guacamoleService.getTunnel(userInfo, createTerminalDTO.getHost(), createTerminalDTO.getEndpoint());
        } catch (IOException e) {
            log.error("Cannot read request body. Reason {}", e.getMessage(), e);
            throw new DatalabException("Can not read request body: " + e.getMessage(), e);
        }
    }

    @Override
    protected void handleTunnelRequest(HttpServletRequest request, HttpServletResponse response) throws ServletException {
        try {
            super.handleTunnelRequest(request, response);
        } catch (DatalabAuthenticationException e) {
            log.error(UNAUTHORIZED_MSG, e);
            sendError(response, HttpStatus.SC_UNAUTHORIZED, HttpStatus.SC_UNAUTHORIZED, UNAUTHORIZED_MSG);
        }
    }

    private UserInfo getUserInfo(String credentials) {
        try {
            return securityDAO.getUser(credentials)
                    .orElseThrow(() -> new DatalabAuthenticationException(UNAUTHORIZED_MSG));
        } catch (DatalabAuthenticationException e) {
            log.error(UNAUTHORIZED_MSG, e);
            throw new DatalabException(UNAUTHORIZED_MSG);
        }
    }

    @Data
    private static class CreateTerminalDTO {
        private String host;
        private String endpoint;
    }
}
