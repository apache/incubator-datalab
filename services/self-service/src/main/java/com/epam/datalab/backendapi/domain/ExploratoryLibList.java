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

import com.epam.datalab.backendapi.resources.dto.LibraryAutoCompleteDTO;
import com.epam.datalab.backendapi.resources.dto.LibraryDTO;
import com.epam.datalab.exceptions.DatalabException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.MoreObjects;
import io.dropwizard.util.Duration;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Class to store the info about libraries.
 */
@Slf4j
public class ExploratoryLibList {

    /**
     * Timeout in milliseconds when the info is out of date.
     */
    private static final long EXPIRED_TIMEOUT_MILLIS = Duration.hours(2).toMilliseconds();

    /**
     * Timeout in milliseconds until the is out of date.
     */
    private static final long UPDATE_TIMEOUT_MILLIS = Duration.minutes(30).toMilliseconds();

    /**
     * Timeout in milliseconds for request to update lib.
     */
    protected static final long UPDATE_REQUEST_TIMEOUT_MILLIS = Duration.minutes(15).toMilliseconds();

    /**
     * Group name.
     */
    private String group;

    /**
     * List of libraries group:libraries:version.
     */
    private Map<String, Map<String, String>> libs = new HashMap<>();

    /**
     * Time in milliseconds when the info is out of date.
     */
    private long expiredTimeMillis = 0;

    /**
     * Last access time in milliseconds to the info.
     */
    private long accessTimeMillis = 0;

    /**
     * Update start time in milliseconds.
     */
    private long updateStartTimeMillis = 0;

    /**
     * Update in progress.
     */
    private boolean updating = false;


    /**
     * Instantiate the list of libraries.
     *
     * @param group   the name of docker's image.
     * @param content JSON string.
     */
    ExploratoryLibList(String group, String content) {
        this.group = group;
        if (content != null) {
            setLibs(content);
        }
    }

    /**
     * Return the list of all groups.
     */
    public List<String> getGroupList() {
        List<String> list = new ArrayList<>(libs.keySet());
        Collections.sort(list);
        return list;
    }

    /**
     * Return the name of docker image;
     */
    public String getGroup() {
        return group;
    }

    /**
     * Return the full list of libraries for group.
     *
     * @param group the name of group.
     */
    public Map<String, String> getLibs(String group) {
        return libs.get(group);
    }

    /**
     * Return the full list of libraries for group.
     *
     * @param content JSON string.
     */
    private void setLibs(String content) {
        ObjectMapper mapper = new ObjectMapper();
        try {
            synchronized (this) {
                @SuppressWarnings("unchecked")
                Map<String, Map<String, String>> map = mapper.readValue(content, Map.class);
                for (Map.Entry<String, Map<String, String>> entry : map.entrySet()) {
                    Map<String, String> group = entry.getValue();
                    String groupName = entry.getKey();
                    libs.remove(groupName);
                    log.info("Update {} group with lib group {} with {} libraries", this.group, groupName, (group != null) ? group.size() : null);
                    libs.put(groupName, new TreeMap<>(group));
                }
                setExpiredTime();
                updating = false;
            }
        } catch (IOException e) {
            throw new DatalabException("Cannot deserialize the list of libraries. " + e.getLocalizedMessage(), e);
        }
    }

    public void setExpiredTime() {
        expiredTimeMillis = System.currentTimeMillis() + EXPIRED_TIMEOUT_MILLIS;
        accessTimeMillis = System.currentTimeMillis();
    }

    /**
     * Search and return the list of libraries for name's prefix <b>startWith</b>.
     *
     * @param group     the name of group.
     * @param startWith the prefix for library name.
     * @return LibraryAutoCompleteDTO dto
     */
    public LibraryAutoCompleteDTO getLibs(String group, String startWith) {
        final String startsWithLower = startWith.toLowerCase();
        Map<String, String> libMap = getLibs(group);
        if (libMap == null) {
            return LibraryAutoCompleteDTO.builder()
                    .autoComplete(isUpdating() ? AutoCompleteEnum.UPDATING : AutoCompleteEnum.NONE)
                    .libraries(Collections.emptyList())
                    .build();
        }
        List<LibraryDTO> libraries = libMap.entrySet()
                .stream()
                .filter(e -> e.getKey().toLowerCase().startsWith(startsWithLower))
                .map(e -> new LibraryDTO(e.getKey(), e.getValue()))
                .collect(Collectors.toList());

        return LibraryAutoCompleteDTO.builder()
                .autoComplete(AutoCompleteEnum.ENABLED)
                .libraries(libraries)
                .build();
    }

    /**
     * Set last access time.
     */
    private void touch() {
        accessTimeMillis = System.currentTimeMillis();
    }

    /**
     * Return <b>true</b> if the info is out of date.
     */
    public boolean isExpired() {
        touch();
        return (expiredTimeMillis < System.currentTimeMillis());
    }

    /**
     * Return <b>true</b> if the info needs to update.
     */
    public boolean isUpdateNeeded() {
        touch();
        return (accessTimeMillis > expiredTimeMillis - UPDATE_TIMEOUT_MILLIS);
    }

    /**
     * Set updating in progress.
     */
    public void setUpdating() {
        updateStartTimeMillis = System.currentTimeMillis();
        updating = true;
    }

    /**
     * Set updating to false.
     */
    public void setNotUpdating() {
        updating = Boolean.FALSE;
    }

    /**
     * Return <b>true</b> if the update in progress.
     */
    public boolean isUpdating() {
        if (updating &&
                updateStartTimeMillis + UPDATE_REQUEST_TIMEOUT_MILLIS < System.currentTimeMillis()) {
            updating = false;
        }
        return updating;
    }


    @Override
    public String toString() {
        return MoreObjects.toStringHelper(this)
                .add("group", group)
                .add("expiredTimeMillis", expiredTimeMillis)
                .add("accessTimeMillis", accessTimeMillis)
                .add("updateStartTimeMillis", updateStartTimeMillis)
                .add("isUpdating", updating)
                .add("libs", (libs == null ? "null" : "..."))
                .toString();
    }
}
