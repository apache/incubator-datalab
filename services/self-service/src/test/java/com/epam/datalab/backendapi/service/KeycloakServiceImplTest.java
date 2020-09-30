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

import com.epam.datalab.backendapi.conf.SelfServiceApplicationConfiguration;
import com.epam.datalab.backendapi.dao.SecurityDAO;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.keycloak.representations.AccessTokenResponse;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class KeycloakServiceImplTest {
    private static final String ACCESS_TOKEN = "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICJNUC15QVpENFdJRzloa" +
            "np3R0RqQjdCeW9aNGpaV05QTjJ3X25uS1BkTnQ4In0.eyJqdGkiOiJlYTgzZTQ2OS0xNjFhLTQ1ZDUtYWI4YS1mZDUxYThmMzMwMzYiL" +
            "CJleHAiOjE1NzA0NDQ1NTQsIm5iZiI6MCwiaWF0IjoxNTcwNDQzMzU0LCJpc3MiOiJodHRwOi8vNTIuMTEuNDUuMTE6ODA4MC9hdXRoL" +
            "3JlYWxtcy9ETEFCX2JobGl2YSIsImF1ZCI6WyJwcm92aXNpb25pbmciLCJhY2NvdW50Il0sInN1YiI6ImRjNzczMThkLWYzN2UtNGNmO" +
            "S1iMDgwLTU2ZTRjMWUwNDVhNSIsInR5cCI6IkJlYXJlciIsImF6cCI6InNzcyIsImF1dGhfdGltZSI6MTU3MDQ0MzMyOSwic2Vzc2lvb" +
            "l9zdGF0ZSI6ImVkYTE3ODJjLTliZmItNDQ5Yy1hYWY1LWNhNzM5MDViMmI1NyIsImFjciI6IjEiLCJyZWFsbV9hY2Nlc3MiOnsicm9sZ" +
            "XMiOlsib2ZmbGluZV9hY2Nlc3MiLCJ1bWFfYXV0aG9yaXphdGlvbiJdfSwicmVzb3VyY2VfYWNjZXNzIjp7InNzcyI6eyJyb2xlcyI6W" +
            "yJ0ZXN0Il19LCJwcm92aXNpb25pbmciOnsicm9sZXMiOlsidGVzdCJdfSwiYWNjb3VudCI6eyJyb2xlcyI6WyJtYW5hZ2UtYWNjb3Vud" +
            "CIsIm1hbmFnZS1hY2NvdW50LWxpbmtzIiwidmlldy1wcm9maWxlIl19fSwic2NvcGUiOiJvcGVuaWQgZW1haWwgcHJvZmlsZSIsImVtY" +
            "WlsX3ZlcmlmaWVkIjpmYWxzZSwicHJlZmVycmVkX3VzZXJuYW1lIjoidGVzdCJ9.jXjqFP1xmG32NahYnw1spO7fhEyGiqeu0QdBYuo9" +
            "y6TI8xlAy5EFQ_5SrW6UuzpZUhgCjStH3Qkk_xgLlbZ9IqnxwOmx1i8arlurKf1mY_rm2_C5JBxHdHio8in8yEMls8t-wQOT46kMJNC7" +
            "4GUzuWWQxS1h6F1V6238rnT8_W27oFcG449ShOGOQ5Du4F9B4ur3Ns6j5oSduwUFlbY_rNpGurUmtFWXBaXM61CiezJPxXu5pHzWK6Xq" +
            "Z1IkuEUaDDJdBf1OGi13toq88V7C-Pr7YlnyWlbZ7bw2VXAs3NAgtn_30KlgYwR9YUJ_AB5TP3u8kK0jY0vLPosuBZgKeA";
    private static final String REFRESH_TOKEN = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCIsImtpZCI6ImUyZjk0YjFmLTBhMjMtNGJi" +
            "OS05NDUwLTdjNDQ4MTdjY2RkMCJ9.eyJqdGkiOiI0NmNiMzczMy1mM2IzLTRiYjItYmQyZC02N2FjNzg5N2VmNTUiLCJleHAiOjE1NzA" +
            "0NDMzNTQsIm5iZiI6MCwiaWF0IjoxNTcwNDQzMzU0LCJpc3MiOiJodHRwOi8vNTIuMTEuNDUuMTE6ODA4MC9hdXRoL3JlYWxtcy9ETEF" +
            "CX2JobGl2YSIsImF1ZCI6Imh0dHA6Ly81Mi4xMS40NS4xMTo4MDgwL2F1dGgvcmVhbG1zL0RMQUJfYmhsaXZhIiwic3ViIjoiZGM3NzM" +
            "xOGQtZjM3ZS00Y2Y5LWIwODAtNTZlNGMxZTA0NWE1IiwidHlwIjoiUmVmcmVzaCIsImF6cCI6InNzcyIsImF1dGhfdGltZSI6MCwic2V" +
            "zc2lvbl9zdGF0ZSI6ImVkYTE3ODJjLTliZmItNDQ5Yy1hYWY1LWNhNzM5MDViMmI1NyIsInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJ" +
            "vZmZsaW5lX2FjY2VzcyIsInVtYV9hdXRob3JpemF0aW9uIl19LCJyZXNvdXJjZV9hY2Nlc3MiOnsic3NzIjp7InJvbGVzIjpbInRlc3Q" +
            "iXX0sInByb3Zpc2lvbmluZyI6eyJyb2xlcyI6WyJ0ZXN0Il19LCJhY2NvdW50Ijp7InJvbGVzIjpbIm1hbmFnZS1hY2NvdW50IiwibWF" +
            "uYWdlLWFjY291bnQtbGlua3MiLCJ2aWV3LXByb2ZpbGUiXX19LCJzY29wZSI6Im9wZW5pZCBlbWFpbCBwcm9maWxlIn0.rzkrAprIt0-" +
            "jD0h9ex3krzfu8UDcgnrRdocNFJmYa30";
    @Mock
    private Client httpClient;
    @Mock
    private SecurityDAO securityDAO;
    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private SelfServiceApplicationConfiguration conf;

    private KeycloakServiceImpl keycloakService;

    @Before
    public void setUp() {
        keycloakService = new KeycloakServiceImpl(httpClient, conf, securityDAO);
    }

    @Test
    public void generateAccessToken() {
        WebTarget webTarget = mock(WebTarget.class);
        Invocation.Builder builder = mock(Invocation.Builder.class);
        Response response = mock(Response.class);
        Response.StatusType statusType = mock(Response.StatusType.class);
        AccessTokenResponse tokenResponse = mock(AccessTokenResponse.class);

        when(httpClient.target(anyString())).thenReturn(webTarget);
        when(webTarget.request()).thenReturn(builder);
        when(builder.header(any(), anyString())).thenReturn(builder);
        when(builder.post(any())).thenReturn(response);
        when(response.getStatusInfo()).thenReturn(statusType);
        when(response.readEntity(AccessTokenResponse.class)).thenReturn(tokenResponse);
        when(statusType.getFamily()).thenReturn(Response.Status.Family.SUCCESSFUL);
        when(tokenResponse.getToken()).thenReturn(ACCESS_TOKEN);
        doNothing().when(securityDAO).updateUser(anyString(), any(AccessTokenResponse.class));

        keycloakService.generateAccessToken(REFRESH_TOKEN);

        verify(httpClient).target(anyString());
        verify(securityDAO).updateUser("test", tokenResponse);
        verifyNoMoreInteractions(securityDAO, httpClient);
    }
}