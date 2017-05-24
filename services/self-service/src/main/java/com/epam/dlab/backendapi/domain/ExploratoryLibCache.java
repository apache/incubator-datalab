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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.util.ResourceUtils;
import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.dto.exploratory.ExploratoryBaseDTO;
import com.epam.dlab.rest.client.RESTService;
import com.epam.dlab.rest.contracts.DockerAPI;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import io.dropwizard.lifecycle.Managed;

/** Cache of libraries for exploratory.
 */
@Singleton
public class ExploratoryLibCache implements Managed {
	private static final Logger LOGGER = LoggerFactory.getLogger(ExploratoryLibCache.class);
	
    @Inject
    @Named(ServiceConsts.PROVISIONING_SERVICE_NAME)
    private RESTService provisioningService;
	
    /** Instance of cache.
     */
	private static ExploratoryLibCache libCache;
	
	/** List of libraries.
	 */
	private Map<String, ExploratoryLibList> cache = new HashMap<>();

	/** Return the list of libraries.
	 */
	public static ExploratoryLibCache getCache() {
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
		cache.clear();
	}
	
	/** Return the list of libraries groups from cache.
	 * @param userInfo the user info.
	 * @param imageName the name of docker image.
	 * @return
	 */
	public List<String> getLibGroupList(UserInfo userInfo, String imageName) {
		ExploratoryLibList libs = getLibs(userInfo, imageName);
		return libs.getGroupList();
	}

	/** Return the list of libraries for docker image and group start with prefix from cache.
	 * @param userInfo the user info.
	 * @param imageName the name of image.
	 * @param group the name of group.
	 * @param startWith the prefix for library name.
	 */
	public List<String> getLibList(UserInfo userInfo, String imageName, String group, String startWith) {
		ExploratoryLibList libs = getLibs(userInfo, imageName);
		return libs.getLibs(group, startWith);
	}
	
	/** Return the list of libraries for docker image from cache.
	 * @param userInfo the user info.
	 * @param imageName the name of image.
	 */
	public ExploratoryLibList getLibs(UserInfo userInfo, String imageName) {
		ExploratoryLibList libs;
		synchronized (cache) {
			libs = cache.get(imageName);
			if (libs == null) {
				libs = new ExploratoryLibList(imageName, null);
				cache.put(imageName, libs);
			}
			if (libs.isUpdateNeeded() && !libs.isUpdating()) {
				libs.setUpdating();
				requestLibList(userInfo, imageName);
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
	 * @param imageName the name of image.
	 */
	private void requestLibList(UserInfo userInfo, String imageName) {
		try {
			LOGGER.debug("Ask docker for the list of libraries for user {} and image {}", userInfo.getName(), imageName);
			ExploratoryBaseDTO<?> dto = ResourceUtils.newResourceSysBaseDTO(userInfo, ExploratoryBaseDTO.class);
			dto.withNotebookImage(imageName)
				.withApplicationName(ResourceUtils.getApplicationNameFromImage(imageName));

			String uuid = provisioningService.post(
					DockerAPI.DOCKER_LIB_LIST,
					userInfo.getAccessToken(),
					dto,
					String.class);
            RequestId.put(userInfo.getName(), uuid);
		} catch (Exception e) {
			LOGGER.warn("Ask docker for the status of resources for user {} and image {} fails: {}", userInfo.getName(), imageName, e.getLocalizedMessage(), e);
		}
	}
	
}
