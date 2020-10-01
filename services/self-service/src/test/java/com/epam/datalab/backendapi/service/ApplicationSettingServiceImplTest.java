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

import com.epam.datalab.backendapi.dao.MongoSetting;
import com.epam.datalab.backendapi.dao.SettingsDAO;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ApplicationSettingServiceImplTest {

    private static final long MAX_BUDGET = 10L;
    @Mock
    private SettingsDAO settingsDAO;
    @InjectMocks
    private ApplicationSettingServiceImpl applicationSettingService;

    @Test
    public void setMaxBudget() {

        applicationSettingService.setMaxBudget(MAX_BUDGET);

        verify(settingsDAO).setMaxBudget(MAX_BUDGET);
        verifyNoMoreInteractions(settingsDAO);
    }

    @Test
    public void getSettings() {
        when(settingsDAO.getSettings()).thenReturn(Collections.singletonMap("key", "value"));
        final Map<String, Object> settings = applicationSettingService.getSettings();
        assertEquals(1, settings.size());
        assertEquals("value", settings.get("key"));

        verify(settingsDAO).getSettings();
        verifyNoMoreInteractions(settingsDAO);
    }

    @Test
    public void removeMaxBudget() {

        applicationSettingService.removeMaxBudget();

        verify(settingsDAO).removeSetting(MongoSetting.CONF_MAX_BUDGET);
        verifyNoMoreInteractions(settingsDAO);
    }
}