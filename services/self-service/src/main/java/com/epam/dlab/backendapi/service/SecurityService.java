package com.epam.dlab.backendapi.service;

import com.epam.dlab.auth.UserInfo;

public interface SecurityService {
	UserInfo getUserInfo(String code);
	UserInfo getUserInfoOffline(String username);
}
