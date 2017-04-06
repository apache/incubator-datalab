package com.epam.dlab.backendapi.domain;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epam.dlab.backendapi.resources.KeyUploaderResource;
import com.epam.dlab.exceptions.DlabException;

/** Stores and checks the id of requests for Provisioning Service.
 */
public class RequestId {
    private static final Logger LOGGER = LoggerFactory.getLogger(KeyUploaderResource.class);

	private static final Map<String, String> uuids = new HashMap<String, String>();
	
	/** Add the request id for user.
	 * @param username the name of user.
	 * @param uuid UUID.
	 */
	public static String put(String username, String uuid) {
		LOGGER.trace("Register request id {} for user {}", uuid, username);
		uuids.put(uuid, username);
		return uuid;
	}
	
	/** Generate, add and return new UUID.
	 * @param username the name of user.
	 * @return
	 */
	public static String get(String username) {
		String uuid = UUID.randomUUID().toString();
		LOGGER.trace("Register request id {} for user {}", uuid, username);
		uuids.put(uuid, username);
		return uuid;
	}
	
	/** Remove UUID if it exist. 
	 * @param uuid UUID.
	 * @throws DlabException
	 */
	public static void remove(String uuid) throws DlabException {
		String username = RequestId.get(uuid);
		LOGGER.trace("Unregister request id {} for user {}", uuid, username);
		uuids.remove(uuid);
	}

	/** Check and remove UUID, if it not exists throw exception.
	 * @param uuid UUID.
	 * @return
	 * @throws DlabException
	 */
	public static String checkAndRemove(String uuid) throws DlabException {
		String username = uuids.get(uuid);
		if (username == null) {
			throw new DlabException("Unknown request id " + uuid);
		}
		LOGGER.trace("Unregister request id {} for user {}", uuid, username);
		uuids.remove(uuid);
		return username;
	}
}
