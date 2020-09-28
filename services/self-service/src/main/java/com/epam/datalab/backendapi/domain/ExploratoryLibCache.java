/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */


package com.epam.datalab.backendapi.domain;

import com.epam.datalab.auth.UserInfo;
import com.epam.datalab.backendapi.resources.dto.LibraryAutoCompleteDTO;
import com.epam.datalab.backendapi.service.EndpointService;
import com.epam.datalab.backendapi.util.RequestBuilder;
import com.epam.datalab.constants.ServiceConsts;
import com.epam.datalab.dto.LibListComputationalDTO;
import com.epam.datalab.dto.LibListExploratoryDTO;
import com.epam.datalab.dto.UserInstanceDTO;
import com.epam.datalab.dto.computational.UserComputationalResource;
import com.epam.datalab.rest.client.RESTService;
import com.epam.datalab.rest.contracts.ComputationalAPI;
import com.epam.datalab.rest.contracts.ExploratoryAPI;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import io.dropwizard.lifecycle.Managed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Cache of libraries for exploratory.
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
    @Inject
    private EndpointService endpointService;

    /**
     * Instance of cache.
     */
    private static ExploratoryLibCache libCache;

    /**
     * Thread of the cache.
     */
    private Thread thread;

    /**
     * List of libraries.
     */
    private Map<String, ExploratoryLibList> cache = new HashMap<>();

    /**
     * Return the list of libraries.
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
    public void start() {
        if (libCache == null) {
            libCache = this;
        }
    }

    @Override
    public void stop() {
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

    /**
     * Return the list of libraries for docker image and group start with prefix from cache.
     *
     * @param userInfo  the user info.
     * @param group     the name of group.
     * @param startWith the prefix for library name.
     * @return LibraryAutoCompleteDTO dto
     */
    public LibraryAutoCompleteDTO getLibList(UserInfo userInfo, UserInstanceDTO userInstance, String group, String startWith) {
        ExploratoryLibList libs = getLibs(userInfo, userInstance, group);
        return libs.getLibs(group, startWith);
    }

    /**
     * Return the list of libraries for docker image from cache.
     *
     * @param userInfo     the user info.
     * @param userInstance userInstance
     * @param cacheKey     the group of library
     */
    private ExploratoryLibList getLibs(UserInfo userInfo, UserInstanceDTO userInstance, String cacheKey) {
        ExploratoryLibList libs;
        synchronized (cache) {
            cache.computeIfAbsent(cacheKey, libraries -> new ExploratoryLibList(cacheKey, null));
            libs = cache.get(cacheKey);
            if (libs.isUpdateNeeded() && !libs.isUpdating()) {
                libs.setUpdating();
                libs.setExpiredTime();
                requestLibList(userInfo, userInstance, cacheKey);
            }
        }

        return libs;
    }

    /**
     * Update the list of libraries for docker image in cache.
     *
     * @param group   the name of image.
     * @param content the content of libraries list.
     */
    public void updateLibList(String group, String content) {
        synchronized (cache) {
            cache.remove(group);
            cache.put(group, new ExploratoryLibList(group, content));
        }
    }

    /**
     * Set updating library list to false
     *
     * @param groupName group name
     */
    public void updateLibListStatus(String groupName) {
        synchronized (cache) {
            ExploratoryLibList exploratoryLibList = cache.get(groupName);
            exploratoryLibList.setNotUpdating();
        }
    }

    /**
     * Send request to provisioning service for the list of libraries.
     *
     * @param userInfo     the user info.
     * @param userInstance the notebook info.
     * @param group        the library group
     */
    private void requestLibList(UserInfo userInfo, UserInstanceDTO userInstance, String group) {
        try {

            LOGGER.info("Ask docker for the list of libraries for user {} and exploratory {} computational {}",
                    userInfo.getName(), userInstance.getExploratoryId(),
                    userInstance.getResources());

            String uuid;
            if (userInstance.getResources() != null && !userInstance.getResources().isEmpty()) {
                UserComputationalResource userComputationalResource = userInstance.getResources().get(0);
                EndpointDTO endpointDTO = endpointService.get(userInstance.getEndpoint());
                LibListComputationalDTO dto = requestBuilder.newLibComputationalList(userInfo, userInstance,
                        userComputationalResource, endpointDTO, group);

                uuid = provisioningService.post(endpointDTO.getUrl() + ComputationalAPI.COMPUTATIONAL_LIB_LIST,
                        userInfo.getAccessToken(),
                        dto, String.class);
            } else {
                EndpointDTO endpointDTO = endpointService.get(userInstance.getEndpoint());
                LibListExploratoryDTO dto = requestBuilder.newLibExploratoryList(userInfo, userInstance, endpointDTO, group);
                uuid = provisioningService.post(endpointDTO.getUrl() + ExploratoryAPI.EXPLORATORY_LIB_LIST,
                        userInfo.getAccessToken(), dto,
                        String.class);
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
