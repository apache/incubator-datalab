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

package com.epam.datalab.backendapi.service;

import com.epam.datalab.auth.UserInfo;
import com.epam.datalab.backendapi.dao.SecurityDAO;
import com.epam.datalab.backendapi.domain.AuditActionEnum;
import com.epam.datalab.backendapi.domain.AuditDTO;
import com.epam.datalab.backendapi.util.KeycloakUtil;
import com.epam.datalab.exceptions.DatalabException;
import com.google.inject.Inject;
import org.keycloak.representations.AccessTokenResponse;

public class SecurityServiceImpl implements SecurityService {
    private final KeycloakService keycloakService;
    private final SecurityDAO securityDAO;
    private final AuditService auditService;

    @Inject
    public SecurityServiceImpl(KeycloakService keycloakService, SecurityDAO securityDAO, AuditService auditService) {
        this.keycloakService = keycloakService;
        this.securityDAO = securityDAO;
        this.auditService = auditService;
    }

    @Override
    public UserInfo getUserInfo(String code) {
        final AccessTokenResponse token = keycloakService.getToken(code);
        final String username = KeycloakUtil.parseToken(token.getToken()).getPreferredUsername();
        securityDAO.saveUser(username, token);
        UserInfo userInfo = new UserInfo(username, token.getToken());
        userInfo.setRefreshToken(token.getRefreshToken());
        saveLogInAudit(username);
        return userInfo;
    }

    @Override
    public UserInfo getUserInfoOffline(String username) {
        return securityDAO.getTokenResponse(username)
                .map(AccessTokenResponse::getRefreshToken)
                .map(keycloakService::refreshToken)
                .map(accessTokenResponse -> new UserInfo(KeycloakUtil.parseToken(accessTokenResponse.getToken()).getPreferredUsername(),
                        accessTokenResponse.getToken()))
                .orElseThrow(() -> new DatalabException("Can not find token for user " + username));
    }

    @Override
    public UserInfo getServiceAccountInfo(String username) {
        AccessTokenResponse accessTokenResponse = keycloakService.generateServiceAccountToken();
        return new UserInfo(username, accessTokenResponse.getToken());
    }

    private void saveLogInAudit(String username) {
        AuditDTO auditDTO = AuditDTO.builder()
                .user(username)
                .action(AuditActionEnum.LOG_IN)
                .build();
        auditService.save(auditDTO);
    }
}
