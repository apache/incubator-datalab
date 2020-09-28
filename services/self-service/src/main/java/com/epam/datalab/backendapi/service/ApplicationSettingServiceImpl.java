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
import com.google.inject.Inject;

import java.util.Map;

public class ApplicationSettingServiceImpl implements ApplicationSettingService {
    @Inject
    private SettingsDAO settingsDAO;

    @Override
    public void removeMaxBudget() {
        settingsDAO.removeSetting(MongoSetting.CONF_MAX_BUDGET);
    }

    @Override
    public void setMaxBudget(Long maxBudget) {
        settingsDAO.setMaxBudget(maxBudget);
    }

    @Override
    public Map<String, Object> getSettings() {
        return settingsDAO.getSettings();
    }
}
