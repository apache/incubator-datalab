/***************************************************************************

Copyright (c) 2016, EPAM SYSTEMS INC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

****************************************************************************/

package com.epam.dlab.auth.dao;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.auth.UserInfoDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UserInfoDAODumbImpl implements UserInfoDAO {
	
	private static final Logger LOG = LoggerFactory.getLogger(UserInfoDAODumbImpl.class);

	@Override
	public UserInfo getUserInfoByAccessToken(String accessToken) {
		LOG.debug("UserInfo persistence find unavailable: {}",accessToken);
		return null;
	}

	@Override
	public void updateUserInfoTTL(String accessToken, UserInfo ui) {
		LOG.debug("UserInfo persistence update unavailable: {} {}",accessToken,ui);
	}

	@Override
	public void deleteUserInfo(String accessToken) {
		LOG.debug("UserInfo persistence delete unavailable: {}",accessToken);
	}

	@Override
	public void saveUserInfo(UserInfo ui) {
		LOG.debug("UserInfo persistence save unavailable: {}",ui);
	}

}
