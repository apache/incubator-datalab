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
