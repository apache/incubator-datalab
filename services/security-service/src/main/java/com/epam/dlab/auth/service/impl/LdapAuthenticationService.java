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

package com.epam.dlab.auth.service.impl;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.auth.UserInfoDAO;
import com.epam.dlab.auth.UserVerificationService;
import com.epam.dlab.auth.dao.LdapUserDAO;
import com.epam.dlab.auth.dto.UserCredentialDTO;
import com.epam.dlab.auth.service.AuthenticationService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.Optional;

import static com.epam.dlab.auth.rest.AbstractAuthenticationService.getRandomToken;

@Singleton
@Slf4j
public class LdapAuthenticationService implements AuthenticationService {
	private final UserInfoDAO userInfoDAO;
	private final LdapUserDAO ldapUserDAO;
	private final UserVerificationService verificationService;

	@Inject
	public LdapAuthenticationService(UserInfoDAO userInfoDAO, LdapUserDAO ldapUserDAO,
									 UserVerificationService verificationService) {
		this.userInfoDAO = userInfoDAO;
		this.ldapUserDAO = ldapUserDAO;
		this.verificationService = verificationService;
	}

	@Override
	public Optional<UserInfo> getUserInfo(String token) {
		return userInfoDAO.getUserInfoByAccessToken(token)
				.map(userInfo -> touchedUser(token, userInfo));
	}

	@Override
	public Optional<UserInfo> login(UserCredentialDTO credentialDTO) {
		final String token = credentialDTO.getAccessToken();
		return StringUtils.isNoneBlank(token) ? getUserInfo(token) : getLdapUserInfo(credentialDTO);
	}

	@Override
	public void logout(String token) {
		userInfoDAO.deleteUserInfo(token);
	}

	private Optional<UserInfo> getLdapUserInfo(UserCredentialDTO credentialDTO) {
		final UserInfo user = ldapUserDAO.getUserInfo(credentialDTO.getUsername(), credentialDTO.getPassword());
		user.addRoles(ldapUserDAO.getUserGroups(user));
		verificationService.verify(user);
		final String token = getRandomToken();
		final UserInfo userWithToken = user.withToken(token);
		userInfoDAO.saveUserInfo(userWithToken);
		return Optional.of(userWithToken);
	}

	private UserInfo touchedUser(String token, UserInfo userInfo) {
		userInfoDAO.updateUserInfoTTL(token, userInfo);
		return userInfo.withToken(token);
	}
}
