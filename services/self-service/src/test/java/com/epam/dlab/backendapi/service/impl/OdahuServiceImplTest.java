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
import com.epam.dlab.rest.client.RESTService;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OdahuServiceImplTest {

    private static final String USER = "testUser";
    private static final String TOKEN = "testToken";

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

    @InjectMocks
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
        String project = "testProject";
        String endPointUrl = "https://localhsot:8080";
        String endPointName = "endpoint";
        String tag = "testTag";
        String uuid = "34dsr324";

        EndpointDTO endpointDTO = new EndpointDTO(endPointName, endPointName, "testAccount", tag, EndpointDTO.EndpointStatus.ACTIVE, CloudProvider.GCP);
        ProjectDTO projectDTO = new ProjectDTO(project,
                Collections.emptySet(),
                "ssh-testKey\n", tag, 200,
                singletonList(new ProjectEndpointDTO(endPointName, UserInstanceStatus.RUNNING, new EdgeInfo())),
                true);
        OdahuCreateDTO odahuCreateDTO = new OdahuCreateDTO("odahuTest", project, endPointUrl, tag);
        Map<String, String> tags = new HashMap<>();
        tags.put("custom_tag", odahuCreateDTO.getCustomTag());
        tags.put("project_tag", odahuCreateDTO.getProject());
        tags.put("endpoint_tag", odahuCreateDTO.getEndpoint());

        when(projectService.get(project)).thenReturn(projectDTO);
        when(odahuDAO.create(new OdahuDTO(odahuCreateDTO.getName(), odahuCreateDTO.getProject(),
                odahuCreateDTO.getEndpoint(), UserInstanceStatus.CREATING, tags))).thenReturn(true);
        when(endpointService.get(odahuCreateDTO.getEndpoint())).thenReturn(endpointDTO);
        when(requestId.put(userInfo.getName(), uuid)).thenReturn(uuid);

        odahuService.create(project, odahuCreateDTO, userInfo);
    }
}
