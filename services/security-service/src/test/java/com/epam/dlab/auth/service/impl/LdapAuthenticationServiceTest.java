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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Matchers.refEq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class LdapAuthenticationServiceTest {

	private static final String TOKEN = "token123";
	private static final String USER = "user";
	private static final String PASSWORD = "password";
	@Mock
	private LdapUserDAO ldapUserDAO;
	@Mock
	private UserInfoDAO userInfoDAO;
	@Mock
	private UserVerificationService verificationService;
	@InjectMocks
	private LdapAuthenticationService ldapAuthenticationService;

	@Test
	public void getUserInfo() {

		when(userInfoDAO.getUserInfoByAccessToken(anyString())).thenReturn(Optional.of(userInfo()));
		final Optional<UserInfo> userInfo = ldapAuthenticationService.getUserInfo(TOKEN);

		assertTrue(userInfo.isPresent());
		assertEquals(USER.toLowerCase(), userInfo.get().getName());
		assertEquals(TOKEN, userInfo.get().getAccessToken());

		verify(userInfoDAO).getUserInfoByAccessToken(TOKEN);
		verify(userInfoDAO).updateUserInfoTTL(eq(TOKEN), refEq(userInfo()));
		verifyNoMoreInteractions(userInfoDAO);
	}

	@Test
	public void getUserInfoWhenUserNotFound() {

		when(userInfoDAO.getUserInfoByAccessToken(anyString())).thenReturn(Optional.empty());
		final Optional<UserInfo> userInfo = ldapAuthenticationService.getUserInfo(TOKEN);

		assertFalse(userInfo.isPresent());

		verify(userInfoDAO).getUserInfoByAccessToken(TOKEN);
		verifyNoMoreInteractions(userInfoDAO);
	}

	@Test
	public void loginWithoutAccessToken() {

		when(ldapUserDAO.getUserInfo(anyString(), anyString())).thenReturn(userInfo());
		final Optional<UserInfo> userInfo = ldapAuthenticationService.login(getCredentialDTO());

		assertTrue(userInfo.isPresent());
		assertEquals(USER, userInfo.get().getName());
		assertNotNull(userInfo.get().getAccessToken());

		verify(verificationService).verify(refEq(userInfo()));
		verify(ldapUserDAO).getUserInfo(USER, PASSWORD);
		verify(ldapUserDAO).getUserGroups(refEq(userInfo()));
		verify(userInfoDAO).saveUserInfo(refEq(userInfo().withToken(TOKEN), "accessToken"));
		verifyNoMoreInteractions(ldapUserDAO, userInfoDAO);
	}

	@Test
	public void loginWithAccessToken() {

		when(userInfoDAO.getUserInfoByAccessToken(anyString())).thenReturn(Optional.of(userInfo()));
		final UserCredentialDTO credentialDTO = getCredentialDTO();
		credentialDTO.setAccessToken(TOKEN);
		final Optional<UserInfo> userInfo = ldapAuthenticationService.login(credentialDTO);

		assertTrue(userInfo.isPresent());
		assertEquals(USER, userInfo.get().getName());
		assertNotNull(userInfo.get().getAccessToken());

		verify(userInfoDAO).getUserInfoByAccessToken(TOKEN);
		verify(userInfoDAO).updateUserInfoTTL(eq(TOKEN), refEq(userInfo()));
		verifyNoMoreInteractions(userInfoDAO);
		verifyZeroInteractions(ldapUserDAO, verificationService);
	}

	@Test
	public void logout() {

		ldapAuthenticationService.logout(TOKEN);

		verify(userInfoDAO).deleteUserInfo(TOKEN);
		verifyNoMoreInteractions(userInfoDAO);
		verifyZeroInteractions(ldapUserDAO);
	}

	private UserInfo userInfo() {
		return new UserInfo(USER, null);
	}

	private UserCredentialDTO getCredentialDTO() {
		final UserCredentialDTO dto = new UserCredentialDTO();
		dto.setUsername(USER);
		dto.setPassword(PASSWORD);
		return dto;
	}

}