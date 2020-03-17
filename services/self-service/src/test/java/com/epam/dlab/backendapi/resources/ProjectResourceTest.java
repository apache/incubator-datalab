package com.epam.dlab.backendapi.resources;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.resources.dto.KeysDTO;
import com.epam.dlab.backendapi.resources.dto.ProjectActionFormDTO;
import com.epam.dlab.backendapi.service.AccessKeyService;
import com.epam.dlab.backendapi.service.ProjectService;
import com.epam.dlab.exceptions.DlabException;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.testing.junit.ResourceTestRule;
import org.apache.http.HttpStatus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.Collections;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verifyNoMoreInteractions;


public class ProjectResourceTest extends TestBase {
    private ProjectService projectService = mock(ProjectService.class);
    private AccessKeyService keyService = mock(AccessKeyService.class);

    @Rule
    public final ResourceTestRule resources = getResourceTestRuleInstance(
            new ProjectResource(projectService, keyService));

    @Before
    public void setup() throws AuthenticationException {
        authSetup();
    }

    @Test
    public void getProjectsForManaging() {
        final Response response = resources.getJerseyTest()
                .target("project/managing")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .get();

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));
        verify(projectService, times(1)).getProjectsForManaging();
        verifyNoMoreInteractions(projectService);
    }

    @Test
    public void stopProject() {
        final Response response = resources.getJerseyTest()
                .target("project/stop")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .post(Entity.json(getProjectActionDTO()));

        assertEquals(HttpStatus.SC_ACCEPTED, response.getStatus());
        verify(projectService).stopWithResources(any(UserInfo.class), anyList(), anyString());
        verifyNoMoreInteractions(projectService);
    }

    @Test
    public void startProject() {
        final Response response = resources.getJerseyTest()
                .target("project/start")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .post(Entity.json(getProjectActionDTO()));

        assertEquals(HttpStatus.SC_ACCEPTED, response.getStatus());
        verify(projectService).start(any(UserInfo.class), anyList(), anyString());
        verifyNoMoreInteractions(projectService);
    }

    @Test
    public void generate() {
        when(keyService.generateKeys(any(UserInfo.class))).thenReturn(new KeysDTO("somePublicKey", "somePrivateKey",
                "user"));

        final Response response = resources.getJerseyTest()
                .target("/project/keys")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .post(Entity.json(""));

        assertEquals(HttpStatus.SC_OK, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(keyService).generateKeys(getUserInfo());
        verifyNoMoreInteractions(keyService);
    }

    @Test
    public void generateKeysWithException() {
        doThrow(new DlabException("Can not generate private/public key pair due to"))
                .when(keyService).generateKeys(any(UserInfo.class));

        final Response response = resources.getJerseyTest()
                .target("/project/keys")
                .request()
                .header("Authorization", "Bearer " + TOKEN)
                .post(Entity.json(""));

        assertEquals(HttpStatus.SC_INTERNAL_SERVER_ERROR, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON, response.getHeaderString(HttpHeaders.CONTENT_TYPE));

        verify(keyService).generateKeys(getUserInfo());
        verifyNoMoreInteractions(keyService);
    }

    private ProjectActionFormDTO getProjectActionDTO() {
        return new ProjectActionFormDTO("DLAB", Collections.singletonList("https://localhost:8083/"));
    }
}
