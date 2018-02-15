package com.epam.dlab.auth;

import java.util.Optional;

/**
 * Interface is used to manage system users
 * The purpose of this interface is to create user to allow
 * to make call from self services to provisioning services e.g.
 * when have no client full client credential information. E.g. need to make
 * call from cron job
 */
public interface SystemUserInfoService {

	/**
	 * Return system user if it was registered in system
	 * and remove current user from persistent storage if
	 * it exist
	 *
	 * @param token access token
	 * @return user info
	 */
	Optional<UserInfo> getUser(String token);

	/**
	 * Creates new system user with specified name
	 *
	 * @param name user name
	 * @return user info
	 */
	UserInfo create(String name);
}
