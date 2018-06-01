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

package com.epam.dlab.auth;

import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.rest.client.RESTService;
import com.epam.dlab.auth.contract.SecurityAPI;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.dropwizard.auth.AuthenticationException;
import io.dropwizard.auth.Authenticator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class SecurityRestAuthenticator implements Authenticator<String, UserInfo> {
	private static final Logger LOGGER = LoggerFactory.getLogger(SecurityRestAuthenticator.class);

	@Inject
	@Named(ServiceConsts.SECURITY_SERVICE_NAME)
	private RESTService securityService;

	@Inject
	private SystemUserInfoService systemUserInfoService;

	@Override
	public Optional<UserInfo> authenticate(String credentials) throws AuthenticationException {
		LOGGER.debug("authenticate token {}", credentials);

		return Optional.ofNullable(systemUserInfoService.getUser(credentials).orElseGet(
				() -> securityService.post(SecurityAPI.GET_USER_INFO, credentials, UserInfo.class)));
	}
}
