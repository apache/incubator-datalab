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

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.util.RequestBuilder;
import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.dto.LibListComputationalDTO;
import com.epam.dlab.dto.UserInstanceDTO;
import com.epam.dlab.dto.base.DataEngineType;
import com.epam.dlab.dto.computational.UserComputationalResource;
import com.epam.dlab.dto.exploratory.ExploratoryActionDTO;
import com.epam.dlab.rest.client.RESTService;
import com.epam.dlab.rest.contracts.ComputationalAPI;
import com.epam.dlab.rest.contracts.ExploratoryAPI;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.dropwizard.lifecycle.Managed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Cache of libraries for exploratory.
 */
@Singleton
public class ExploratoryLibCache implements Managed, Runnable {
	private static final Logger LOGGER = LoggerFactory.getLogger(ExploratoryLibCache.class);

    @Inject
    @Named(ServiceConsts.PROVISIONING_SERVICE_NAME)
    private RESTService provisioningService;

	@Inject
	private RequestBuilder requestBuilder;

	@Inject
	private RequestId requestId;

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
		String cacheKey = libraryCacheKey(userInstance);
		synchronized (cache) {
			libs = cache.get(cacheKey);
			if (libs == null) {
				libs = new ExploratoryLibList(cacheKey, null);
				cache.put(cacheKey, libs);
			}
			if (libs.isUpdateNeeded() && !libs.isUpdating()) {
				libs.setUpdating();
				requestLibList(userInfo, userInstance);
			}
		}

		return libs;
	}

	public static String libraryCacheKey(UserInstanceDTO instanceDTO) {
		if (instanceDTO.getResources() != null && !instanceDTO.getResources().isEmpty()) {
			if (instanceDTO.getResources().size() > 1) {
				throw new IllegalStateException("Several clusters in userInstance");
			}

			UserComputationalResource userComputationalResource = instanceDTO.getResources().get(0);
			return (DataEngineType.fromDockerImageName(userComputationalResource.getImageName()) == DataEngineType.SPARK_STANDALONE)
                    ? instanceDTO.getImageName()
					: libraryCacheKey(instanceDTO.getImageName(), userComputationalResource.getImageName());

		} else {
			return instanceDTO.getImageName();
		}
	}

	private static String libraryCacheKey(String exploratoryImage, String computationalImage) {
		return exploratoryImage + "/" + computationalImage;
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

			LOGGER.debug("Ask docker for the list of libraries for user {} and exploratory {} computational {}",
					userInfo.getName(), userInstance.getExploratoryId(),
					userInstance.getResources());

            String uuid;
			if (userInstance.getResources() != null && !userInstance.getResources().isEmpty()) {
				UserComputationalResource userComputationalResource = userInstance.getResources().get(0);
				LibListComputationalDTO dto = requestBuilder.newLibComputationalList(userInfo, userInstance,
						userComputationalResource);
                uuid = provisioningService.post(ComputationalAPI.COMPUTATIONAL_LIB_LIST, userInfo.getAccessToken(), dto, String.class);
			} else {
				ExploratoryActionDTO<?> dto = requestBuilder.newLibExploratoryList(userInfo, userInstance);
				uuid = provisioningService.post(ExploratoryAPI.EXPLORATORY_LIB_LIST, userInfo.getAccessToken(), dto, String.class);
			}

			requestId.put(userInfo.getName(), uuid);

		} catch (Exception e) {
			LOGGER.warn("Ask docker for the status of resources for user {} and exploratory {} fails: {}",
					userInfo.getName(), userInstance, e.getLocalizedMessage(), e);
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

				if (cache.size() == 0) {
					synchronized (libCache) {
						thread = null;
						LOGGER.debug("Library cache thread have no data and will be finished");
						return;
					}
				}
			} catch (InterruptedException e) {
				LOGGER.trace("Library cache thread has been interrupted");
				Thread.currentThread().interrupt();
				break;
			} catch (Exception e) {
				LOGGER.warn("Library cache thread unhandled error: {}", e.getLocalizedMessage(), e);
			}
		}
	}
}
