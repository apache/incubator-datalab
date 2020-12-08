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

package com.epam.datalab.backendapi.dropwizard.listeners;

import com.epam.datalab.backendapi.conf.SelfServiceApplicationConfiguration;
import com.epam.datalab.backendapi.dao.EndpointDAO;
import com.epam.datalab.backendapi.dao.SettingsDAO;
import com.epam.datalab.backendapi.dao.UserRoleDAO;
import com.epam.datalab.backendapi.domain.EndpointDTO;
import com.epam.datalab.backendapi.resources.dto.UserRoleDTO;
import com.epam.datalab.cloud.CloudProvider;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.Inject;
import io.dropwizard.lifecycle.ServerLifecycleListener;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.jetty.server.Server;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toCollection;

@Slf4j
public class MongoStartupListener implements ServerLifecycleListener {

    private static final String PATH_TO_ROLES = "/mongo/%s/mongo_roles.json";
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private final UserRoleDAO userRoleDao;
    private final SelfServiceApplicationConfiguration configuration;
    private final SettingsDAO settingsDAO;
    private final EndpointDAO endpointDAO;

    @Inject
    public MongoStartupListener(UserRoleDAO userRoleDao, SelfServiceApplicationConfiguration configuration,
                                SettingsDAO settingsDAO, EndpointDAO endpointDAO) {
        this.userRoleDao = userRoleDao;
        this.configuration = configuration;
        this.settingsDAO = settingsDAO;
        this.endpointDAO = endpointDAO;
    }

    @Override
    public void serverStarted(Server server) {
        settingsDAO.setServiceBaseName(configuration.getServiceBaseName());
        settingsDAO.setConfOsFamily(configuration.getOs());
        settingsDAO.setSsnInstanceSize(configuration.getSsnInstanceSize());
        List<EndpointDTO> endpointDTOs = endpointDAO.getEndpoints();
        if (userRoleDao.findAll().isEmpty()) {
            log.info("Populating DataLab default roles into database");
            List<UserRoleDTO> cloudRoles = getRoles(CloudProvider.GENERAL);
            List<CloudProvider> connectedCloudProviders = getConnectedProviders(endpointDTOs);
            log.info("Check for connected endpoints:\n connected endpoints: {} \n connected clouds: {}",
                    endpointDTOs.size(), connectedCloudProviders);
            connectedCloudProviders.forEach(provider -> cloudRoles.addAll(getRoles(provider)));
            userRoleDao.insert(cloudRoles);
        } else {
            log.info("Roles already populated. Do nothing ...");
        }
    }

    private List<CloudProvider> getConnectedProviders(List<EndpointDTO> endpointDTOs) {
        return endpointDTOs.stream()
                .map(EndpointDTO::getCloudProvider)
                .collect(Collectors.toList());
    }

    private List<UserRoleDTO> getRoles(CloudProvider cloudProvider) {
        Set<UserRoleDTO> userRoles = new HashSet<>(getUserRoleFromFile(cloudProvider));
        return userRoles.stream()
                .collect(collectingAndThen(toCollection(() -> new TreeSet<>(comparing(UserRoleDTO::getId))),
                        ArrayList::new));
    }

    private List<UserRoleDTO> getUserRoleFromFile(CloudProvider cloudProvider) {
        try (InputStream is = getClass().getResourceAsStream(format(PATH_TO_ROLES, cloudProvider.getName()))) {
            return MAPPER.readValue(is, new TypeReference<List<UserRoleDTO>>() {
            });
        } catch (IOException e) {
            log.error("Can not marshall datalab roles due to: {}", e.getMessage(), e);
            throw new IllegalStateException("Can not marshall datalab roles due to: " + e.getMessage());
        }
    }
}
