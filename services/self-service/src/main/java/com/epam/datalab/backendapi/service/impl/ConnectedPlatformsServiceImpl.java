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

package com.epam.datalab.backendapi.service.impl;

import com.epam.datalab.auth.UserInfo;
import com.epam.datalab.backendapi.dao.ConnectedPlatformsDAO;
import com.epam.datalab.backendapi.resources.dto.ConnectedPlatformDTO;
import com.epam.datalab.backendapi.resources.dto.ConnectedPlatformType;
import com.epam.datalab.backendapi.resources.dto.ConnectedPlatformsInfo;
import com.epam.datalab.backendapi.service.ConnectedPlatformsService;
import com.epam.datalab.exceptions.ResourceAlreadyExistException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Singleton
@Slf4j
public class ConnectedPlatformsServiceImpl implements ConnectedPlatformsService {

    private static final String CONNECTED_PLATFORM_NAME_EXIST = "Connected platform with name %s already exist";
    private final ConnectedPlatformsDAO connectedPlatformsDAO;


    @Inject
    public ConnectedPlatformsServiceImpl(ConnectedPlatformsDAO connectedPlatformsDAO) {
        this.connectedPlatformsDAO = connectedPlatformsDAO;
    }


    @Override
    public ConnectedPlatformsInfo getUserPlatforms(String userName) {
        List<String> platformNames = getAll().stream().map(ConnectedPlatformDTO::getName).collect(Collectors.toList());
        List<String> platformTypes = Stream.of(ConnectedPlatformType.values()).map(ConnectedPlatformType::getName).collect(Collectors.toList());
        return ConnectedPlatformsInfo.builder()
                .userPlatforms(connectedPlatformsDAO.getUserPlatforms(userName))
                .types(platformTypes)
                .platformNames(platformNames)
                .build();
    }

    @Override
    public List<ConnectedPlatformDTO> getAll() {
        return connectedPlatformsDAO.getAll();
    }

    @Override
    public void addPlatform(UserInfo user, String name, ConnectedPlatformType type, String url) {
        if(connectedPlatformsDAO.exist(name)){
            log.error(String.format(CONNECTED_PLATFORM_NAME_EXIST,name));
            throw new ResourceAlreadyExistException(String.format(CONNECTED_PLATFORM_NAME_EXIST,name));
        }

        connectedPlatformsDAO.addPlatform(ConnectedPlatformDTO.builder()
                .name(name)
                .url(url)
                .user(user.getName())
                .type(type)
                .build());
    }

    @Override
    public void disconnect(UserInfo user, String name) {
        connectedPlatformsDAO.delete(user.getName(), name);
    }
}
