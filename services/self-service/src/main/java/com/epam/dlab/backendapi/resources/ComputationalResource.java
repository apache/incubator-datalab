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
import com.epam.dlab.backendapi.core.UserComputationalResourceDTO;
import com.epam.dlab.backendapi.dao.InfrastructureProvisionDAO;
import com.epam.dlab.backendapi.dao.SettingsDAO;
import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.backendapi.resources.dto.ComputationalCreateFormDTO;
import com.epam.dlab.dto.computational.ComputationalCreateDTO;
import com.epam.dlab.dto.computational.ComputationalStatusDTO;
import com.epam.dlab.dto.computational.ComputationalTerminateDTO;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.client.RESTService;
import com.epam.dlab.rest.contracts.ApiCallbacks;
import com.epam.dlab.rest.contracts.ComputationalAPI;
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

import static com.epam.dlab.UserInstanceStatus.*;

@Path("/infrastructure_provision/computational_resources")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ComputationalResource implements ComputationalAPI {
    private static final Logger LOGGER = LoggerFactory.getLogger(ComputationalResource.class);

    @Inject
    private SettingsDAO settingsDAO;
    @Inject
    private InfrastructureProvisionDAO infrastructureProvisionDAO;
    @Inject
    @Named(ServiceConsts.PROVISIONING_SERVICE_NAME)
    private RESTService provisioningService;

    @PUT
    public Response create(@Auth UserInfo userInfo, @Valid @NotNull ComputationalCreateFormDTO formDTO) {
        LOGGER.debug("creating computational resource {} for user {}", formDTO.getName(), userInfo.getName());
        boolean isAdded = infrastructureProvisionDAO.addComputational(userInfo.getName(), formDTO.getNotebookName(),
                new UserComputationalResourceDTO()
                        .withComputationalName(formDTO.getName())
                        .withStatus(CREATING.toString())
                        .withMasterShape(formDTO.getMasterInstanceType())
                        .withSlaveShape(formDTO.getSlaveInstanceType())
                        .withSlaveNumber(formDTO.getInstanceCount()));
        if (isAdded) {
            try {
                String exploratoryId = infrastructureProvisionDAO.fetchExploratoryId(userInfo.getName(), formDTO.getNotebookName());
                ComputationalCreateDTO dto = new ComputationalCreateDTO()
                        .withServiceBaseName(settingsDAO.getServiceBaseName())
                        .withExploratoryName(formDTO.getNotebookName())
                        .withComputationalName(formDTO.getName())
                        .withNotebookName(exploratoryId)
                        .withInstanceCount(formDTO.getInstanceCount())
                        .withMasterInstanceType(formDTO.getMasterInstanceType())
                        .withSlaveInstanceType(formDTO.getSlaveInstanceType())
                        .withVersion(formDTO.getVersion())
                        .withEdgeUserName(UsernameUtils.removeDomain(userInfo.getName()))
                        .withIamUserName(userInfo.getName())
                        .withRegion(settingsDAO.getCredsRegion());
                ;
                LOGGER.debug("created computational resource {} for user {}", formDTO.getName(), userInfo.getName());
                return Response
                        .ok(provisioningService.post(EMR_CREATE, dto, String.class))
                        .build();
            } catch (Throwable t) {
                updateComputationalStatus(userInfo.getName(), formDTO.getNotebookName(), formDTO.getName(), FAILED);
                throw new DlabException("Could not create computational resource " + formDTO.getName(), t);
            }
        } else {
            LOGGER.debug("used existing computational resource {} for user {}", formDTO.getName(), userInfo.getName());
            return Response.status(Response.Status.FOUND).build();
        }
    }

    @POST
    @Path(ApiCallbacks.STATUS_URI)
    public Response status(ComputationalStatusDTO dto) {
        LOGGER.debug("updating status for computational resource {} for user {}: {}", dto.getComputationalName(), dto.getUser(), dto.getStatus());
        infrastructureProvisionDAO.updateComputationalFields(dto);
        return Response.ok().build();
    }

    @DELETE
    @Path("/{exploratoryName}/{computationalName}/terminate")
    public String terminate(@Auth UserInfo userInfo, @PathParam("exploratoryName") String exploratoryName, @PathParam("computationalName") String computationalName) {
        LOGGER.debug("terminating computational resource {} for user {}", computationalName, userInfo.getName());
        updateComputationalStatus(userInfo.getName(), exploratoryName, computationalName, TERMINATING);
        try {
            String exploratoryId = infrastructureProvisionDAO.fetchExploratoryId(userInfo.getName(), exploratoryName);
            String computationalId = infrastructureProvisionDAO.fetchComputationalId(userInfo.getName(), exploratoryName, computationalName);
            ComputationalTerminateDTO dto = new ComputationalTerminateDTO()
                    .withServiceBaseName(settingsDAO.getServiceBaseName())
                    .withExploratoryName(exploratoryName)
                    .withComputationalName(computationalName)
                    .withNotebookInstanceName(exploratoryId)
                    .withClusterName(computationalId)
                    .withKeyDir(settingsDAO.getCredsKeyDir())
                    .withSshUser(settingsDAO.getExploratorySshUser())
                    .withEdgeUserName(UsernameUtils.removeDomain(userInfo.getName()))
                    .withIamUserName(userInfo.getName())
                    .withRegion(settingsDAO.getCredsRegion());
            return provisioningService.post(EMR_TERMINATE, dto, String.class);
        } catch (Throwable t) {
            updateComputationalStatus(userInfo.getName(), exploratoryName, computationalName, FAILED);
            throw new DlabException("Could not terminate computational resource " + computationalName, t);
        }
    }

    private void updateComputationalStatus(String user, String exploratoryName, String computationalName, UserInstanceStatus status) {
        ComputationalStatusDTO computationalStatus = new ComputationalStatusDTO()
                .withUser(user)
                .withExploratoryName(exploratoryName)
                .withComputationalName(computationalName)
                .withStatus(status);
        infrastructureProvisionDAO.updateComputationalStatus(computationalStatus);
    }

}
