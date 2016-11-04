package com.epam.dlab.auth;

public interface UserInfoDAO {
	public UserInfo getUserInfoByAccessToken(String accessToken);
	public void updateUserInfoTTL(String accessToken, UserInfo ui);
	public void deleteUserInfo(String accessToken);
	public void saveUserInfo(UserInfo ui);
}
