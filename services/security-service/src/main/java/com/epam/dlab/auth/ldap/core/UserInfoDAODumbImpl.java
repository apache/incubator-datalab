package com.epam.dlab.auth.ldap.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.auth.UserInfoDAO;

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
