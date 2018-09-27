package com.epam.dlab.auth.service;

import com.epam.dlab.auth.UserInfo;

public interface DexOauthService {

	String getDexOauthUrl();

	UserInfo getUserInfo(String authorizationCode);
}
