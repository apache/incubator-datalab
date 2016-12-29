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

import com.epam.dlab.UserInstanceStatus;
import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.core.UserInstanceDTO;
import com.epam.dlab.backendapi.dao.InfrastructureProvisionDAO;
import com.epam.dlab.backendapi.dao.SettingsDAO;
import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.backendapi.resources.dto.ExploratoryActionFormDTO;
import com.epam.dlab.backendapi.resources.dto.ExploratoryCreateFormDTO;
import com.epam.dlab.dto.StatusBaseDTO;
import com.epam.dlab.dto.exploratory.ExploratoryActionDTO;
import com.epam.dlab.dto.exploratory.ExploratoryCreateDTO;
import com.epam.dlab.dto.exploratory.ExploratoryStatusDTO;
import com.epam.dlab.dto.exploratory.ExploratoryStopDTO;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.client.RESTService;
import com.epam.dlab.rest.contracts.ApiCallbacks;
import com.epam.dlab.rest.contracts.ExploratoryAPI;
import com.epam.dlab.utils.UsernameUtils;
import com.google.inject.Inject;
import com.google.inject.name.Named;
import io.dropwizard.auth.Auth;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import java.util.Optional;

import static com.epam.dlab.UserInstanceStatus.*;

@Path("/infrastructure_provision/exploratory_environment")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ExploratoryResource implements ExploratoryAPI {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExploratoryResource.class);

    @Inject
    private SettingsDAO settingsDAO;
    @Inject
    private InfrastructureProvisionDAO infrastructureProvisionDAO;
    @Inject
    @Named(ServiceConsts.PROVISIONING_SERVICE_NAME)
    private RESTService provisioningService;

    @PUT
    public Response create(@Auth UserInfo userInfo, @Valid @NotNull ExploratoryCreateFormDTO formDTO) {
        LOGGER.debug("creating exploratory environment {} with name {} for user {}",
                formDTO.getImage(), formDTO.getName(), userInfo.getName());
        boolean isAdded = infrastructureProvisionDAO.insertExploratory(new UserInstanceDTO()
                .withUser(userInfo.getName())
                .withExploratoryName(formDTO.getName())
                .withStatus(CREATING.toString())
                .withImageName(formDTO.getImage())
                .withImageVersion(formDTO.getVersion())
                .withShape(formDTO.getShape()));
        if (isAdded) {
            try {
                ExploratoryCreateDTO dto = new ExploratoryCreateDTO()
                        .withServiceBaseName(settingsDAO.getServiceBaseName())
                        .withExploratoryName(formDTO.getName())
                        .withNotebookUserName(UsernameUtils.removeDomain(userInfo.getName()))
                        .withIamUserName(userInfo.getName())
                        .withNotebookImage(formDTO.getImage())
                        .withNotebookInstanceType(formDTO.getShape())
                        .withRegion(settingsDAO.getCredsRegion())
                        .withSecurityGroupIds(settingsDAO.getSecurityGroups());
                LOGGER.debug("created exploratory environment {} for user {}", formDTO.getName(), userInfo.getName());
                return Response
                        .ok(provisioningService.post(EXPLORATORY_CREATE, dto, String.class))
                        .build();
            } catch (Throwable t) {
                infrastructureProvisionDAO.updateExploratoryStatus(createStatusDTO(userInfo.getName(), formDTO.getName(), FAILED));
                throw new DlabException("Could not create exploratory environment " + formDTO.getName(), t);
            }
        } else {
            LOGGER.debug("used existing exploratory environment {} for user {}", formDTO.getName(), userInfo.getName());
            return Response.status(Response.Status.FOUND).build();
        }
    }

    @POST
    @Path(ApiCallbacks.STATUS_URI)
    public Response status(ExploratoryStatusDTO dto) {
        UserInstanceStatus currentStatus = infrastructureProvisionDAO.fetchExploratoryStatus(dto.getUser(), dto.getExploratoryName());
        LOGGER.debug("updating status for exploratory environment {} for user {}: was {}, now {}", dto.getExploratoryName(), dto.getUser(), currentStatus, dto.getStatus());
        infrastructureProvisionDAO.updateExploratoryFields(dto);
        if (currentStatus == TERMINATING) {
            updateComputationalStatuses(dto.getUser(), dto.getExploratoryName(), UserInstanceStatus.of(dto.getStatus()));
        } else if (currentStatus == STOPPING) {
            updateComputationalStatuses(dto.getUser(), dto.getExploratoryName(), TERMINATED);
        }
        return Response.ok().build();
    }

    @POST
    public String start(@Auth UserInfo userInfo, @Valid @NotNull ExploratoryActionFormDTO formDTO) {
        LOGGER.debug("starting exploratory environment {} for user {}", formDTO.getNotebookInstanceName(), userInfo.getName());
        return action(userInfo, formDTO.getNotebookInstanceName(), EXPLORATORY_START, STARTING);
    }

    @DELETE
    @Path("/{name}/stop")
    public String stop(@Auth UserInfo userInfo, @PathParam("name") String name) {
        System.out.println("stopping " + name);
        LOGGER.debug("stopping exploratory environment {} for user {}", name, userInfo.getName());
        UserInstanceStatus exploratoryStatus = STOPPING;
        UserInstanceStatus computationalStatus = TERMINATING;
        updateExploratoryStatus(userInfo.getName(), name, exploratoryStatus);
        updateComputationalStatuses(userInfo.getName(), name, computationalStatus);
        try {
            Optional<UserInstanceDTO> opt =
                    infrastructureProvisionDAO.fetchExploratoryFields(userInfo.getName(), name);
            if(!opt.isPresent()) {
                throw new DlabException(String.format("Exploratory instance with name {} not found.", name));
            }

            UserInstanceDTO userInstance = opt.get();
            ExploratoryStopDTO dto = new ExploratoryStopDTO()
                    .withServiceBaseName(settingsDAO.getServiceBaseName())
                    .withNotebookImage(userInstance.getImageName())
                    .withExploratoryName(name)
                    .withNotebookUserName(UsernameUtils.removeDomain(userInfo.getName()))
                    .withIamUserName(userInfo.getName())
                    .withNotebookInstanceName(userInstance.getExploratoryId())
                    .withKeyDir(settingsDAO.getCredsKeyDir())
                    .withSshUser(settingsDAO.getExploratorySshUser())
                    .withRegion(settingsDAO.getCredsRegion());
            return provisioningService.post(EXPLORATORY_STOP, dto, String.class);
        } catch (Throwable t) {
            updateExploratoryStatus(userInfo.getName(), name, FAILED);
            throw new DlabException("Could not stop exploratory environment " + name, t);
        }
    }

    @DELETE
    @Path("/{name}/terminate")
    public String terminate(@Auth UserInfo userInfo, @PathParam("name") String name) {
        LOGGER.debug("terminating exploratory environment {} for user {}", name, userInfo.getName());
        UserInstanceStatus status = TERMINATING;
        updateExploratoryStatus(userInfo.getName(), name, status);
        updateComputationalStatuses(userInfo.getName(), name, status);
        return action(userInfo, name, EXPLORATORY_TERMINATE, status);
    }

    private String action(UserInfo userInfo, String name, String action, UserInstanceStatus status) {
        try {
            updateExploratoryStatus(userInfo.getName(), name, status);
            Optional<UserInstanceDTO> opt =
                    infrastructureProvisionDAO.fetchExploratoryFields(userInfo.getName(), name);
            if(!opt.isPresent())
                throw new DlabException(String.format("Exploratory instance with name {} not found.", name));

            UserInstanceDTO userInstance = opt.get();
            ExploratoryActionDTO dto = new ExploratoryActionDTO<>()
                    .withServiceBaseName(settingsDAO.getServiceBaseName())
                    .withNotebookImage(userInstance.getImageName())
                    .withExploratoryName(name)
                    .withNotebookUserName(UsernameUtils.removeDomain(userInfo.getName()))
                    .withIamUserName(userInfo.getName())
                    .withNotebookInstanceName(userInstance.getExploratoryId())
                    .withRegion(settingsDAO.getCredsRegion());
            return provisioningService.post(action, dto, String.class);
        } catch (Throwable t) {
            updateExploratoryStatus(userInfo.getName(), name, FAILED);
            throw new DlabException("Could not " + action + " exploratory environment " + name, t);
        }
    }

    private StatusBaseDTO createStatusDTO(String user, String name, UserInstanceStatus status) {
        return new ExploratoryStatusDTO()
                .withUser(user)
                .withExploratoryName(name)
                .withStatus(status);
    }

    private void updateComputationalStatuses(String user, String exploratoryName, UserInstanceStatus status) {
        LOGGER.debug("updating status for all computational resources of {} for user {}: {}", exploratoryName, user, status);
        StatusBaseDTO exploratoryStatus = createStatusDTO(user, exploratoryName, status);
        infrastructureProvisionDAO.updateComputationalStatusesForExploratory(exploratoryStatus);
    }

    private void updateExploratoryStatus(String user, String exploratoryName, UserInstanceStatus status) {
        StatusBaseDTO exploratoryStatus = createStatusDTO(user, exploratoryName, status);
        infrastructureProvisionDAO.updateExploratoryStatus(exploratoryStatus);
    }

}
