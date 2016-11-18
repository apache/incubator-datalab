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

package com.epam.dlab.backendapi.resources;

import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.client.rest.DockerAPI;
import com.epam.dlab.backendapi.dao.InfrastructureProvisionDAO;
import com.epam.dlab.backendapi.dao.KeyDAO;
import com.epam.dlab.client.restclient.RESTService;
import com.epam.dlab.dto.imagemetadata.ComputationalMetadataDTO;
import com.epam.dlab.dto.imagemetadata.ExploratoryMetadataDTO;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.dropwizard.auth.Auth;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.epam.dlab.backendapi.SelfServiceApplicationConfiguration.PROVISIONING_SERVICE;
import io.dropwizard.auth.Auth;

@Path("/infrastructure_provision")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class InfrastructureProvisionResource implements DockerAPI {
    public static final String EDGE_IP = "edge_node_ip";
    private static final Logger LOGGER = LoggerFactory.getLogger(KeyUploaderResource.class);

    @Inject
    private InfrastructureProvisionDAO dao;
    @Inject
    private KeyDAO keyDAO;
    @Inject
    @Named(PROVISIONING_SERVICE)
    private RESTService provisioningService;

    @GET
    @Path("/provisioned_user_resources")
    public Iterable<Document> getList(@Auth UserInfo userInfo) {
        LOGGER.debug("loading notebooks for user {}", userInfo.getName());
        String ip = keyDAO.getUserEdgeIP(userInfo.getName());
        return appendEdgeIp(dao.find(userInfo.getName()), ip);
    }

    private List<Document> appendEdgeIp(Iterable<Document> documents, String ip) {
        return StreamSupport.stream(documents.spliterator(), false)
                .map(document -> document.append(EDGE_IP, ip))
                .collect(Collectors.toList());
    }

    @GET
    @Path("/computational_resources_shapes")
    public Iterable<Document> getShapes(@Auth UserInfo userInfo) {
        LOGGER.debug("loading shapes for user {}", userInfo.getName());
        return dao.findShapes();
    }

    @GET
    @Path("/computational_resources_templates")
    public Iterable<ComputationalMetadataDTO> getComputationalTemplates(@Auth UserInfo userInfo) {
        LOGGER.debug("loading computational templates for user {}", userInfo.getName());
        return getComputationalTemplates();
    }

    @GET
    @Path("/exploratory_environment_templates")
    public Iterable<ExploratoryMetadataDTO> getExploratoryTemplates(@Auth UserInfo userInfo) {
        LOGGER.debug("loading exploratory templates for user {}", userInfo.getName());
        return getExploratoryTemplates();
    }

    private Iterable<ExploratoryMetadataDTO> getExploratoryTemplates() {
        return Stream.of(provisioningService.get(DOCKER_EXPLORATORY, ExploratoryMetadataDTO[].class))
                .collect(Collectors.toSet());
    }

    private Iterable<ComputationalMetadataDTO> getComputationalTemplates() {
        return Stream.of(provisioningService.get(DOCKER_COMPUTATIONAL, ComputationalMetadataDTO[].class))
                .collect(Collectors.toSet());
    }
}
