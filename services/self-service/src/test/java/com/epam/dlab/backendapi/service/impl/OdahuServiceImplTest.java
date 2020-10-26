package com.epam.dlab.backendapi.service.impl;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.annotation.Project;
import com.epam.dlab.backendapi.dao.OdahuDAO;
import com.epam.dlab.backendapi.dao.OdahuDAOImpl;
import com.epam.dlab.backendapi.domain.OdahuCreateDTO;
import com.epam.dlab.backendapi.domain.OdahuDTO;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Incubating;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class OdahuServiceImplTest {

    private static final String USER = "testUser";
    private static final String TOKEN = "testToken";

    private UserInfo userInfo;

    @Mock
    private OdahuDAO odahuDAO;

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
/*
    @Test
    public void createTest() {
        String prject = "testProject";
        OdahuCreateDTO odahuCreateDTO = new OdahuCreateDTO("odahuTest", prject, "https://localhsot:8080", "testTag");
        odahuService.create(prject, odahuCreateDTO, userInfo);
        //verifyNoMoreInteractions(odahuService);
    }*/
}
