package com.epam.dlab.backendapi.resources;

import com.epam.dlab.backendapi.conf.SelfServiceApplicationConfiguration;
import com.epam.dlab.backendapi.dao.SecurityDAO;
import com.epam.dlab.backendapi.service.KeycloakService;
import com.epam.dlab.backendapi.service.SecurityService;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.apache.http.HttpStatus;
import org.junit.Rule;
import org.junit.Test;
import org.keycloak.representations.AccessTokenResponse;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;

public class KeycloakResourceTest extends TestBase {
    private SecurityService securityService = mock(SecurityService.class);
    private SelfServiceApplicationConfiguration configuration = mock(SelfServiceApplicationConfiguration.class, RETURNS_DEEP_STUBS);
    private SecurityDAO securityDAO = mock(SecurityDAO.class);
    private KeycloakService keycloakService = mock(KeycloakService.class);

    @Rule
    public final ResourceTestRule resources = getResourceTestRuleInstance(
            new KeycloakResource(securityService, configuration, securityDAO, keycloakService));

    @Test
    public void refreshAccessToken() {
        when(keycloakService.generateAccessToken(anyString())).thenReturn(mock(AccessTokenResponse.class));

        final Response response = resources.getJerseyTest()
                .target("oauth/refresh/" + "refresh_token")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .post(Entity.json(""));

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));
        verify(keycloakService).generateAccessToken(anyString());
        verifyNoMoreInteractions(keycloakService);
    }
}