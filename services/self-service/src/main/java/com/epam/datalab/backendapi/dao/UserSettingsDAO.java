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

package com.epam.datalab.backendapi.dao;

import com.epam.datalab.auth.UserInfo;
import com.epam.datalab.backendapi.resources.dto.ImageFilter;
import com.epam.datalab.backendapi.resources.dto.UserDTO;
import io.dropwizard.auth.Auth;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;


import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static com.epam.datalab.backendapi.dao.MongoCollections.USER_SETTINGS;
import static com.mongodb.client.model.Filters.*;
import static com.mongodb.client.model.Projections.*;
import static com.mongodb.client.model.Updates.set;

/**
 * DAO for the user preferences.
 */
public class UserSettingsDAO extends BaseDAO {
    private static final String USER_UI_SETTINGS_FIELD = "userUISettings";
    private static final String USER_ALLOWED_BUDGET = "allowedBudget";

    private static final String USER_IMAGE_FILTER = "imageFilter";

    /**
     * Returns the user preferences of UI dashboard.
     *
     * @param userInfo user info.
     * @return JSON content.
     */
    public String getUISettings(@Auth UserInfo userInfo) {
        return findOne(USER_SETTINGS, eq(ID, userInfo.getName()))
                .map(d -> d.getString(USER_UI_SETTINGS_FIELD))
                .orElse(StringUtils.EMPTY);
    }

    /**
     * Store the user preferences of UI dashboard.
     *
     * @param userInfo user info.
     * @param settings user preferences in JSON format.
     */
    public void setUISettings(UserInfo userInfo, String settings) {
        updateOne(USER_SETTINGS,
                eq(ID, userInfo.getName()),
                set(USER_UI_SETTINGS_FIELD, settings),
                true);
    }

    public void updateBudget(UserDTO allowedBudgetDTO) {
        updateOne(USER_SETTINGS,
                eq(ID, allowedBudgetDTO.getName()),
                set(USER_ALLOWED_BUDGET, allowedBudgetDTO.getBudget()),
                true);
    }

    public Optional<Integer> getAllowedBudget(String user) {
        return findOne(USER_SETTINGS, eq(ID, user))
                .flatMap(d -> Optional.ofNullable(d.getInteger(USER_ALLOWED_BUDGET)));
    }

    public Optional<ImageFilter> getImageFilter(String user){
         return findOne(USER_SETTINGS, and(eq(ID, user), notNull(USER_IMAGE_FILTER)),
                fields(include(USER_IMAGE_FILTER), excludeId()))
                 .map(d -> convertFromDocument((Document) d.get(USER_IMAGE_FILTER), ImageFilter.class));
    }


    public void setUserImageFilter(String userName, ImageFilter imageFilter) {
        updateOne(USER_SETTINGS,
                eq(ID, userName),
                set(USER_IMAGE_FILTER, convertToBson(imageFilter)),
                true);
    }

    public Set<String> getUserNames(String name){
        return stream(find(USER_SETTINGS, regex(ID, name,"i")))
                .map(document -> document.getString(ID))
                .collect(Collectors.toSet());
    }

}