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

package com.epam.dlab.backendapi.domain;

import java.util.Date;
import java.util.UUID;

import com.google.inject.Inject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epam.dlab.backendapi.dao.RequestIdDAO;
import com.epam.dlab.exceptions.DlabException;

import io.dropwizard.util.Duration;

/** Stores and checks the id of requests for Provisioning Service.
 */
public class RequestId {
    private static final Logger LOGGER = LoggerFactory.getLogger(RequestId.class);

	/**	Timeout in milliseconds when the request id is out of date. */
	private static final long EXPIRED_TIMEOUT_MILLIS = Duration.hours(12).toMilliseconds();

	@Inject
	private static RequestIdDAO dao;
	
	/** Add the request id for user.
	 * @param username the name of user.
	 * @param uuid UUID.
	 */
	public static String put(String username, String uuid) {
		LOGGER.trace("Register request id {} for user {}", uuid, username);
		dao.put(new RequestIdDTO()
				.withId(uuid)
				.withUser(username)
				.withRequestTime(new Date())
				.withExpirationTime(new Date(System.currentTimeMillis() + EXPIRED_TIMEOUT_MILLIS)));
		return uuid;
	}
	
	/** Generate, add and return new UUID.
	 * @param username the name of user.
	 * @return
	 */
	public static String get(String username) {
		return put(UUID.randomUUID().toString(), username);
	}
	
	/** Remove UUID if it exist. 
	 * @param uuid UUID.
	 * @throws DlabException
	 */
	public static void remove(String uuid) throws DlabException {
		LOGGER.trace("Unregister request id {}", uuid);
		dao.delete(uuid);
	}

	/** Check and remove UUID, if it not exists throw exception.
	 * @param uuid UUID.
	 * @return
	 * @throws DlabException
	 */
	public static String checkAndRemove(String uuid) throws DlabException {
		String username = dao.get(uuid).getUser();
		LOGGER.trace("Unregister request id {} for user {}", uuid, username);
		dao.delete(uuid);
		return username;
	}
}
