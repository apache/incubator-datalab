package com.epam.dlab.backendapi.service.impl;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.OdahuDAO;
import com.epam.dlab.backendapi.domain.EndpointDTO;
import com.epam.dlab.backendapi.domain.OdahuCreateDTO;
import com.epam.dlab.backendapi.domain.OdahuDTO;
import com.epam.dlab.backendapi.domain.ProjectDTO;
import com.epam.dlab.backendapi.domain.ProjectEndpointDTO;
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.service.EndpointService;
import com.epam.dlab.backendapi.service.ProjectService;
import com.epam.dlab.backendapi.util.RequestBuilder;
import com.epam.dlab.cloud.CloudProvider;
import com.epam.dlab.dto.UserInstanceStatus;
import com.epam.dlab.dto.base.edge.EdgeInfo;
import com.epam.dlab.exceptions.ResourceConflictException;
import com.epam.dlab.rest.client.RESTService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.*;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OdahuServiceImplTest {

    private static final String USER = "testUser";
    private static final String TOKEN = "testToken";
    private static final String PROJECT = "testProject";
    private static final String END_POINT_URL = "https://localhsot:8080";
    private static final String END_POINT_NAME = "endpoint";
    private static final String tag = "testTag";
    private static final String uuid = "34dsr324";
    private static final String ODAHU_NAME = "odahuTest";

    private UserInfo userInfo;

    @Mock
    private OdahuDAO odahuDAO;

    @Mock
    private ProjectService projectService;

    @Mock
    private EndpointService endpointService;

    @Mock
    private RequestId requestId;

    @Mock
    private RESTService provisioningService;

    @Mock
    private RequestBuilder requestBuilder;

    @Mock
    private OdahuServiceImpl odahuService;

    @Before
    public void setUp(){
        userInfo = new UserInfo(USER, TOKEN);
    }

    @Test
    public void findOdahuTest() {
        List<OdahuDTO> odahuDTOList = odahuService.findOdahu();
        assertNotNull(odahuDTOList);
    }

    @Test
    public void createTest() {

        EndpointDTO endpointDTO = new EndpointDTO(END_POINT_NAME, END_POINT_NAME, "testAccount", tag, EndpointDTO.EndpointStatus.ACTIVE, CloudProvider.GCP);
        ProjectDTO projectDTO = new ProjectDTO(PROJECT,
                Collections.emptySet(),
                "ssh-testKey\n", tag, 200,
                singletonList(new ProjectEndpointDTO(END_POINT_NAME, UserInstanceStatus.RUNNING, new EdgeInfo())),
                true);
        OdahuCreateDTO odahuCreateDTO = getOdahuCreateDTO();

        when(projectService.get(PROJECT)).thenReturn(projectDTO);
        when(odahuDAO.create(getOdahuDTO(UserInstanceStatus.CREATING))).thenReturn(true);
        when(endpointService.get(odahuCreateDTO.getEndpoint())).thenReturn(endpointDTO);
        when(requestId.put(userInfo.getName(), uuid)).thenReturn(uuid);

        odahuService.create(PROJECT, odahuCreateDTO, userInfo);
    }

    @Test
    public void createIfClusterActive() {

        OdahuCreateDTO odahuCreateDTO = getOdahuCreateDTO();
        OdahuDTO failedOdahu = getOdahuDTO(UserInstanceStatus.RUNNING);
        List<OdahuDTO> failedOdahuList = singletonList(failedOdahu);
        when(odahuDAO.findOdahuClusters(odahuCreateDTO.getProject(), odahuCreateDTO.getEndpoint())).thenReturn(failedOdahuList);

        doThrow(new ResourceConflictException(String.format("Odahu cluster already exist in system for project %s " +
                "and endpoint %s", odahuCreateDTO.getProject(),
                odahuCreateDTO.getEndpoint()))).when(odahuService).create(PROJECT, odahuCreateDTO, userInfo);
    }

    private Map<String, String> getTags() {
        Map<String, String> tags = new HashMap<>();
        tags.put("custom_tag", getOdahuCreateDTO().getCustomTag());
        tags.put("project_tag", getOdahuCreateDTO().getProject());
        tags.put("endpoint_tag", getOdahuCreateDTO().getEndpoint());
        return tags;
    }

    private OdahuDTO getOdahuDTO(UserInstanceStatus instanceStatus) {
        return new OdahuDTO(ODAHU_NAME, PROJECT, END_POINT_NAME, instanceStatus, getTags());
    }

    private OdahuCreateDTO getOdahuCreateDTO() {
        return new OdahuCreateDTO(ODAHU_NAME, PROJECT, END_POINT_URL, tag);
    }
}
