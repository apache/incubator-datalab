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
import com.epam.datalab.backendapi.dao.UserSettingsDAO;
import com.epam.datalab.backendapi.resources.dto.UserDTO;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class UserSettingServiceImplTest {

    private static final String SETTINGS = "settings";
    private static final String TOKEN = "Token";
    private static final String USER = "user";
    private static final Integer BUDGET = 10;
    @Mock
    private UserSettingsDAO settingsDAO;
    @InjectMocks
    private UserSettingServiceImpl userSettingService;

    @Test
    public void saveUISettings() {
        userSettingService.saveUISettings(getUserInfo(), SETTINGS);

        verify(settingsDAO).setUISettings(refEq(getUserInfo()), eq(SETTINGS));
        verifyNoMoreInteractions(settingsDAO);
    }

    @Test
    public void getUISettings() {
        when(settingsDAO.getUISettings(any(UserInfo.class))).thenReturn(SETTINGS);

        final String uiSettings = userSettingService.getUISettings(getUserInfo());

        assertEquals(SETTINGS, uiSettings);
        verify(settingsDAO).getUISettings(refEq(getUserInfo()));
    }

    @Test
    public void updateUsersBudget() {

        userSettingService.updateUsersBudget(Collections.singletonList(getBudgetDTO()));

        verify(settingsDAO).updateBudget(refEq(getBudgetDTO()));
        verifyNoMoreInteractions(settingsDAO);
    }

    private UserDTO getBudgetDTO() {
        return new UserDTO(USER, BUDGET, UserDTO.Status.ACTIVE);
    }


    private UserInfo getUserInfo() {
        return new UserInfo(USER, TOKEN);
    }

}