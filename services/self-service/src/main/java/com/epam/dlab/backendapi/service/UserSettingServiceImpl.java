/*
 * Copyright (c) 2018, EPAM SYSTEMS INC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.epam.dlab.backendapi.service;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.UserSettingsDAO;
import com.epam.dlab.backendapi.resources.dto.UserDTO;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import java.util.List;

@Singleton
public class UserSettingServiceImpl implements UserSettingService {
	@Inject
	private UserSettingsDAO settingsDAO;

	@Override
	public void saveUISettings(UserInfo userInfo, String settings) {
		settingsDAO.setUISettings(userInfo, settings);
	}

	@Override
	public String getUISettings(UserInfo userInfo) {
		return settingsDAO.getUISettings(userInfo);
	}

	@Override
	public void updateUsersBudget(List<UserDTO> budgets) {
		budgets.forEach(settingsDAO::updateBudget);
	}
}
