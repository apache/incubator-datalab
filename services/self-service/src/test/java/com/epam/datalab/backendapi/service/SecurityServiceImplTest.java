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
import com.epam.datalab.exceptions.DatalabException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.keycloak.representations.AccessTokenResponse;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Optional;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class SecurityServiceImplTest {

    private static final String CODE = "code";
    private static final String TOKEN = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJNUC15QVpENFdJRzloanp3R0RqQjdCeW9aNGpaV05QTjJ3X25uS1BkTnQ4In0.eyJqdGkiOiJkN2U0MDk3Yi1hNjdlLTQxYWUtYjBiNC05MzE1YWFmOGZkMDciLCJleHAiOjE1OTkwNDI1ODgsIm5iZiI6MCwiaWF0IjoxNTk5MDM4OTg4LCJpc3MiOiJodHRwOi8vNTIuMTEuNDUuMTE6ODA4MC9hdXRoL3JlYWxtcy9ETEFCX2JobGl2YSIsImF1ZCI6WyJwcm92aXNpb25pbmciLCJhY2NvdW50Il0sInN1YiI6ImRjNzczMThkLWYzN2UtNGNmOS1iMDgwLTU2ZTRjMWUwNDVhNSIsInR5cCI6IkJlYXJlciIsImF6cCI6InNzcyIsImF1dGhfdGltZSI6MTU5OTAzODk4Nywic2Vzc2lvbl9zdGF0ZSI6Ijg2Njg3NzQ3LTNlOGUtNDhhYy1iMTE1LWM4ZDRmNzQ5M2M1ZSIsImFjciI6IjEiLCJyZWFsbV9hY2Nlc3MiOnsicm9sZXMiOlsib2ZmbGluZV9hY2Nlc3MiLCJ1bWFfYXV0aG9yaXphdGlvbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7InNzcyI6eyJyb2xlcyI6WyJ0ZXN0Il19LCJwcm92aXNpb25pbmciOnsicm9sZXMiOlsidGVzdCJdfSwiYWNjb3VudCI6eyJyb2xlcyI6WyJtYW5hZ2UtYWNjb3VudCIsIm1hbmFnZS1hY2NvdW50LWxpbmtzIiwidmlldy1wcm9maWxlIl19fSwic2NvcGUiOiJvcGVuaWQgZW1haWwgcHJvZmlsZSIsImVtYWlsX3ZlcmlmaWVkIjpmYWxzZSwicHJlZmVycmVkX3VzZXJuYW1lIjoidGVzdCIsImVtYWlsIjoidGVzdEBlcGFtLmNvbSJ9.cYXGXjHGOg2XKADNGuQSykb6aUc-_CC4cUaTJ9qVml0IZDPYcUF9H_iTB-vL67k5MpuM4mVpZWHYT9oJYuFKZoR5lPUmiHG3A8LWxZq4ALeTEqWzOdOn2hUsoPSVoaTG8nLqgOQFHAV8G66dD-0zy-8QheGwZznXZ2_lRvSIg-IYRj_vhv91AItNQQ2eXho7zAic_QiLQHo5vUIEOGcVQdRkiVXe-CZ9pe8lHw8tlKwLlHJj7ldv1YVi0m8f9Tztn2ylSoETwSXdmk09pLxzoFG1EP-EU5cDE5l_MDJIVdhGFJR8eLd8dyzFjThupSMwgWEr_iDmbZ4Q116sv8g4jw";
    private static final String REFRESH_TOKEN = "refreshToken";
    private static final String USERNAME = "test";

    @Mock
    private KeycloakService keycloakService;
    @Mock
    private SecurityDAO securityDAO;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private SecurityServiceImpl securityService;

    @Test
    public void testGetUserInfo() {
        AccessTokenResponse tokenResponse = mock(AccessTokenResponse.class);
        when(tokenResponse.getToken()).thenReturn(TOKEN);
        when(tokenResponse.getRefreshToken()).thenReturn(REFRESH_TOKEN);
        when(keycloakService.getToken(anyString())).thenReturn(tokenResponse);

        UserInfo actualUserInfo = securityService.getUserInfo(CODE);

        assertEquals("UserInfo should be equal", getUserInfoWithRefreshToken(), actualUserInfo);
        verify(keycloakService).getToken(CODE);
        verify(securityDAO).saveUser(USERNAME, tokenResponse);
        verify(auditService).save(getAuditDTO());
        verifyNoMoreInteractions(keycloakService, securityDAO, auditService);
    }

    @Test
    public void getUserInfoOffline() {
        AccessTokenResponse tokenResponseFromDB = mock(AccessTokenResponse.class);
        when(tokenResponseFromDB.getRefreshToken()).thenReturn(REFRESH_TOKEN);
        when(securityDAO.getTokenResponse(anyString())).thenReturn(Optional.of(tokenResponseFromDB));
        AccessTokenResponse tokenResponse = mock(AccessTokenResponse.class);
        when(tokenResponse.getToken()).thenReturn(TOKEN);
        when(keycloakService.refreshToken(anyString())).thenReturn(tokenResponse);

        UserInfo actualUserInfoOffline = securityService.getUserInfoOffline(USERNAME);

        assertEquals("UserInfo should be equal", getUserInfo(), actualUserInfoOffline);
        verify(securityDAO).getTokenResponse(USERNAME);
        verify(keycloakService).refreshToken(REFRESH_TOKEN);
        verifyNoMoreInteractions(securityDAO, keycloakService);
    }

    @Test(expected = DatalabException.class)
    public void getUserInfoOfflineWithException() {
        when(securityDAO.getTokenResponse(anyString())).thenReturn(Optional.empty());

        securityService.getUserInfoOffline(USERNAME);

        verify(securityDAO).getTokenResponse(USERNAME);
        verifyNoMoreInteractions(securityDAO, keycloakService);
    }

    @Test
    public void getServiceAccountInfo() {
        AccessTokenResponse tokenResponse = mock(AccessTokenResponse.class);
        when(tokenResponse.getToken()).thenReturn(TOKEN);
        when(keycloakService.generateServiceAccountToken()).thenReturn(tokenResponse);

        UserInfo actualUserInfo = securityService.getServiceAccountInfo(USERNAME);

        assertEquals("UserInfo should be equal", getUserInfo(), actualUserInfo);
        verify(keycloakService).generateServiceAccountToken();
        verifyNoMoreInteractions(keycloakService);
    }


    private UserInfo getUserInfo() {
        return new UserInfo(USERNAME, TOKEN);
    }

    private UserInfo getUserInfoWithRefreshToken() {
        UserInfo userInfo = new UserInfo(USERNAME, TOKEN);
        userInfo.setRefreshToken(REFRESH_TOKEN);
        return userInfo;
    }

    private AuditDTO getAuditDTO() {
        return AuditDTO.builder()
                .user(USERNAME)
                .action(AuditActionEnum.LOG_IN)
                .build();
    }
}