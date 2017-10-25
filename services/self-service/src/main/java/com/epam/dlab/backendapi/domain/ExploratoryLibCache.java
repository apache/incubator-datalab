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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.epam.dlab.backendapi.util.RequestBuilder;
import com.epam.dlab.rest.contracts.ExploratoryAPI;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.dto.UserInstanceDTO;
import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.dto.exploratory.ExploratoryActionDTO;
import com.epam.dlab.rest.client.RESTService;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.dropwizard.lifecycle.Managed;

/** Cache of libraries for exploratory.
 */
@Singleton
public class ExploratoryLibCache implements Managed, Runnable {
	private static final Logger LOGGER = LoggerFactory.getLogger(ExploratoryLibCache.class);
	
    @Inject
    @Named(ServiceConsts.PROVISIONING_SERVICE_NAME)
    private RESTService provisioningService;
	
    /** Instance of cache.
     */
	private static ExploratoryLibCache libCache;
	
	/** Thread of the cache. */
	private Thread thread;
	
	/** List of libraries.
	 */
	private Map<String, ExploratoryLibList> cache = new HashMap<>();

	/** Return the list of libraries.
	 */
	public static ExploratoryLibCache getCache() {
		synchronized (libCache) {
			if (libCache.thread == null) {
				LOGGER.debug("Library cache thread not running and will be started ...");
				libCache.thread = new Thread(libCache, libCache.getClass().getSimpleName());
				libCache.thread.start();
			}
		}
		return libCache;
	}

	@Override
	public void start() throws Exception {
		if (libCache == null) {
			libCache = this;
		}
	}

	@Override
	public void stop() throws Exception {
		if (libCache != null) {
			synchronized (libCache) {
				if (libCache.thread != null) {
					LOGGER.debug("Library cache thread will be stopped ...");
					libCache.thread.interrupt();
					libCache.thread = null;
					LOGGER.debug("Library cache thread has been stopped");
				}
				libCache.cache.clear();
			}
		}
	}
	
	/** Return the list of libraries groups from cache.
	 * @param userInfo the user info.
	 * @param userInstance the notebook info.
	 * @return
	 */
	public List<String> getLibGroupList(UserInfo userInfo, UserInstanceDTO userInstance) {
		ExploratoryLibList libs = getLibs(userInfo, userInstance);
		return libs.getGroupList();
	}

	/** Return the list of libraries for docker image and group start with prefix from cache.
	 * @param userInfo the user info.
	 * @param userInstance the notebook info.
	 * @param group the name of group.
	 * @param startWith the prefix for library name.
	 */
	public Map<String, String> getLibList(UserInfo userInfo, UserInstanceDTO userInstance, String group, String startWith) {
		ExploratoryLibList libs = getLibs(userInfo, userInstance);
		return libs.getLibs(group, startWith);
	}
	
	/** Return the list of libraries for docker image from cache.
	 * @param userInfo the user info.
	 * @param userInstance the notebook info.
	 */
	public ExploratoryLibList getLibs(UserInfo userInfo, UserInstanceDTO userInstance) {
		ExploratoryLibList libs;
		synchronized (cache) {
			libs = cache.get(userInstance.getImageName());
			if (libs == null) {
				libs = new ExploratoryLibList(userInstance.getImageName(), null);
				cache.put(userInstance.getImageName(), libs);
			}
			if (libs.isUpdateNeeded() && !libs.isUpdating()) {
				libs.setUpdating();
				requestLibList(userInfo, userInstance);
			}
		}
		
		return libs;
	}

	/** Update the list of libraries for docker image in cache.
	 * @param imageName the name of image.
	 * @param content the content of libraries list.
	 */
	public void updateLibList(String imageName, String content) {
		synchronized (cache) {
			cache.remove(imageName);
			cache.put(imageName,
					new ExploratoryLibList(imageName, content));
		}
	}

	/** Remove the list of libraries for docker image from cache.
	 * @param imageName
	 */
	public void removeLibList(String imageName) {
		synchronized (cache) {
			cache.remove(imageName);
		}
	}

	/** Send request to provisioning service for the list of libraries.
	 * @param userInfo the user info.
	 * @param userInstance the notebook info.
	 */
	private void requestLibList(UserInfo userInfo, UserInstanceDTO userInstance) {
		try {
			LOGGER.debug("Ask docker for the list of libraries for user {} and exploratory {}", userInfo.getName(), userInstance.getExploratoryId());
			ExploratoryActionDTO<?> dto = RequestBuilder.newLibExploratoryList(userInfo, userInstance);
			String uuid = provisioningService.post(ExploratoryAPI.EXPLORATORY_LIB_LIST, userInfo.getAccessToken(), dto, String.class);
            RequestId.put(userInfo.getName(), uuid);
		} catch (Exception e) {
			LOGGER.warn("Ask docker for the status of resources for user {} and exploratory {} fails: {}", userInfo.getName(), userInstance.getExploratoryName(), e.getLocalizedMessage(), e);
		}
	}
	
	
	@Override
	public void run() {
		while (true) {
			try {
				Thread.sleep(ExploratoryLibList.UPDATE_REQUEST_TIMEOUT_MILLIS);

				synchronized (cache) {
					cache.entrySet().removeIf(e -> e.getValue().isExpired());
				}

				// TODO race conditions
				if (cache.size() == 0) {
					synchronized (libCache) {
						thread = null;
						LOGGER.debug("Library cache thread have no data and will be finished");
						return;
					}
				}
			} catch (InterruptedException e) {
				LOGGER.trace("Library cache thread has been interrupted");
				break;
			} catch (Exception e) {
				LOGGER.warn("Library cache thread unhandled error: {}", e.getLocalizedMessage(), e);
			}
		}
	}
}
