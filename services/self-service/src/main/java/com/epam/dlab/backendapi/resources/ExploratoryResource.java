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

import static com.epam.dlab.UserInstanceStatus.CREATING;
import static com.epam.dlab.UserInstanceStatus.FAILED;
import static com.epam.dlab.UserInstanceStatus.STARTING;
import static com.epam.dlab.UserInstanceStatus.STOPPING;
import static com.epam.dlab.UserInstanceStatus.TERMINATING;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.epam.dlab.backendapi.SelfServiceApplicationConfiguration;
import com.epam.dlab.backendapi.util.RequestBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epam.dlab.UserInstanceStatus;
import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.core.UserInstanceDTO;
import com.epam.dlab.backendapi.dao.ComputationalDAO;
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.dao.ExploratoryLibDAO;
import com.epam.dlab.backendapi.dao.GitCredsDAO;
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.resources.dto.ExploratoryActionFormDTO;
import com.epam.dlab.backendapi.resources.dto.ExploratoryCreateFormDTO;
import com.epam.dlab.backendapi.roles.RoleType;
import com.epam.dlab.backendapi.roles.UserRoles;
import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.dto.StatusEnvBaseDTO;
import com.epam.dlab.dto.exploratory.ExploratoryActionDTO;
import com.epam.dlab.dto.exploratory.ExploratoryCreateDTO;
import com.epam.dlab.dto.exploratory.ExploratoryGitCredsDTO;
import com.epam.dlab.dto.exploratory.ExploratoryStatusDTO;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.client.RESTService;
import com.epam.dlab.rest.contracts.ExploratoryAPI;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.dropwizard.auth.Auth;

/** Provides the REST API for the exploratory.
 */
@Path("/infrastructure_provision/exploratory_environment")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ExploratoryResource implements ExploratoryAPI {
    private static final Logger LOGGER = LoggerFactory.getLogger(ExploratoryResource.class);

    @Inject
    private ExploratoryDAO exploratoryDAO;
    @Inject
    private ComputationalDAO computationalDAO;
    @Inject
    private ExploratoryLibDAO libraryDAO;
    @Inject
    private GitCredsDAO gitCredsDAO;
    @Inject
    @Named(ServiceConsts.PROVISIONING_SERVICE_NAME)
    private RESTService provisioningService;
    @Inject
    private SelfServiceApplicationConfiguration configuration;

    /**
     * Creates the exploratory environment for user.
     *
     * @param userInfo user info.
     * @param formDTO  description for the exploratory environment.
     * @return {@link Response.Status#OK} request for provisioning service has been accepted.<br>
     * {@link Response.Status#FOUND} request for provisioning service has been duplicated.
     * @throws DlabException
     */
    @PUT
    public Response create(@Auth UserInfo userInfo, @Valid @NotNull ExploratoryCreateFormDTO formDTO) throws DlabException {
        LOGGER.debug("Creating exploratory environment {} with name {} for user {}",
                formDTO.getImage(), formDTO.getName(), userInfo.getName());
		if (!UserRoles.checkAccess(userInfo, RoleType.EXPLORATORY, formDTO.getImage())) {
			LOGGER.warn("Unauthorized attempt to create a {} by user {}", formDTO.getImage(), userInfo.getName());
			throw new DlabException("You do not have the privileges to create a " + formDTO.getTemplateName());
		}
        boolean isAdded = false;
        try {
            exploratoryDAO.insertExploratory(new UserInstanceDTO()
                    .withUser(userInfo.getName())
                    .withExploratoryName(formDTO.getName())
                    .withStatus(CREATING.toString())
                    .withImageName(formDTO.getImage())
                    .withImageVersion(formDTO.getVersion())
                    .withTemplateName(formDTO.getTemplateName())
                    .withShape(formDTO.getShape()));

            isAdded = true;
            ExploratoryGitCredsDTO gitCreds = gitCredsDAO.findGitCreds(userInfo.getName());
            ExploratoryCreateDTO<?> dto = RequestBuilder.newExploratoryCreate(formDTO, userInfo, gitCreds.getGitCreds());
            LOGGER.debug("Created exploratory environment {} for user {}", formDTO.getName(), userInfo.getName());
            String uuid = provisioningService.post(EXPLORATORY_CREATE, userInfo.getAccessToken(), dto, String.class);
            RequestId.put(userInfo.getName(), uuid);
            return Response.ok(uuid).build();
        } catch (Throwable t) {
            LOGGER.error("Could not update the status of exploratory environment {} with name {} for user {}",
                    formDTO.getImage(), formDTO.getName(), userInfo.getName(), t);
            if (isAdded) {
                updateExploratoryStatusSilent(userInfo.getName(), formDTO.getName(), FAILED);
            }
            throw new DlabException("Could not create exploratory environment " + formDTO.getName() + " for user " + userInfo.getName() + ": " + t.getLocalizedMessage(), t);
        }
    }


    /** Starts exploratory environment for user.
     * @param userInfo user info.
     * @param formDTO description of exploratory action.
     * @return Invocation response as JSON string.
     * @throws DlabException
     */
    @POST
    public String start(@Auth UserInfo userInfo, @Valid @NotNull ExploratoryActionFormDTO formDTO) throws DlabException {
        LOGGER.debug("Starting exploratory environment {} for user {}", formDTO.getNotebookInstanceName(), userInfo.getName());
        return action(userInfo, formDTO.getNotebookInstanceName(), EXPLORATORY_START, STARTING);
    }

    /** Stops exploratory environment for user.
     * @param userInfo user info.
     * @param name name of exploratory environment.
     * @return Invocation response as JSON string.
     * @throws DlabException
     */
    @DELETE
    @Path("/{name}/stop")
    public String stop(@Auth UserInfo userInfo, @PathParam("name") String name) throws DlabException {
        LOGGER.debug("Stopping exploratory environment {} for user {}", name, userInfo.getName());
        return action(userInfo, name, EXPLORATORY_STOP, STOPPING);
    }

    /** Terminates exploratory environment for user.
     * @param userInfo user info.
     * @param name name of exploratory environment.
     * @return Invocation response as JSON string.
     * @throws DlabException
     */
    @DELETE
    @Path("/{name}/terminate")
    public String terminate(@Auth UserInfo userInfo, @PathParam("name") String name) throws DlabException {
        LOGGER.debug("Terminating exploratory environment {} for user {}", name, userInfo.getName());
        return action(userInfo, name, EXPLORATORY_TERMINATE, TERMINATING);
    }

    /** Sends the post request to the provisioning service and update the status of exploratory environment.
     * @param userInfo user info.
     * @param exploratoryName name of exploratory environment.
     * @param action action for exploratory environment.
     * @param status status for exploratory environment.
     * @return Invocation request as JSON string.
     * @throws DlabException
     */
    private String action(UserInfo userInfo, String exploratoryName, String action, UserInstanceStatus status) throws DlabException {
        try {
            updateExploratoryStatus(userInfo.getName(), exploratoryName, status);

            if (status == STOPPING || status == TERMINATING) {
            	updateComputationalStatuses(userInfo.getName(), exploratoryName, TERMINATING);
            }

            UserInstanceDTO userInstance = exploratoryDAO.fetchExploratoryFields(userInfo.getName(), exploratoryName);
            ExploratoryActionDTO<?> dto;
            switch (status) {
                case STARTING:
                    dto = RequestBuilder.newExploratoryStart(userInfo)
                            .withNotebookInstanceName(userInstance.getExploratoryId())
                            .withGitCreds(gitCredsDAO.findGitCreds(userInfo.getName()).getGitCreds())
                            .withNotebookImage(userInstance.getImageName())
                            .withExploratoryName(exploratoryName);
                    break;
                default:
                    dto = RequestBuilder.newExploratoryStop(userInfo, userInstance)
                            .withNotebookInstanceName(userInstance.getExploratoryId())
                            .withNotebookImage(userInstance.getImageName())
                            .withExploratoryName(exploratoryName);
                    break;
            }

            String uuid = provisioningService.post(action, userInfo.getAccessToken(), dto, String.class);
            RequestId.put(userInfo.getName(), uuid);
            return uuid;
        } catch (Throwable t) {
        	LOGGER.error("Could not " + action + " exploratory environment {} for user {}", exploratoryName, userInfo.getName(), t);
        	updateExploratoryStatusSilent(userInfo.getName(), exploratoryName, FAILED);
            throw new DlabException("Could not " + action + " exploratory environment " + exploratoryName + ": " + t.getLocalizedMessage(), t);
        }
    }

    /** Instantiates and returns the descriptor of exploratory environment status.
     * @param user user name
     * @param exploratoryName name of exploratory environment.
     * @param status status for exploratory environment.
     */
    private StatusEnvBaseDTO<?> createStatusDTO(String user, String exploratoryName, UserInstanceStatus status) {
        return new ExploratoryStatusDTO()
                .withUser(user)
                .withExploratoryName(exploratoryName)
                .withStatus(status);
    }

    /** Updates the computational status of exploratory environment.
     * @param user user name
     * @param exploratoryName name of exploratory environment.
     * @param status status for exploratory environment.
     * @throws DlabException
     */
    private void updateComputationalStatuses(String user, String exploratoryName, UserInstanceStatus status) throws DlabException {
        LOGGER.debug("updating status for all computational resources of {} for user {}: {}", exploratoryName, user, status);
        StatusEnvBaseDTO<?> exploratoryStatus = createStatusDTO(user, exploratoryName, status);
        computationalDAO.updateComputationalStatusesForExploratory(exploratoryStatus);
    }

    /** Updates the status of exploratory environment.
     * @param user user name
     * @param exploratoryName name of exploratory environment.
     * @param status status for exploratory environment.
     * @throws DlabException
     */
    private void updateExploratoryStatus(String user, String exploratoryName, UserInstanceStatus status) throws DlabException {
        StatusEnvBaseDTO<?> exploratoryStatus = createStatusDTO(user, exploratoryName, status);
        exploratoryDAO.updateExploratoryStatus(exploratoryStatus);
    }

    /** Updates the status of exploratory environment without exceptions. If exception occurred then logging it.
     * @param user user name
     * @param exploratoryName name of exploratory environment.
     * @param status status for exploratory environment.
     */
    private void updateExploratoryStatusSilent(String user, String exploratoryName, UserInstanceStatus status) {
    	try {
       		updateExploratoryStatus(user, exploratoryName, status);
       	} catch (DlabException e) {
            LOGGER.error("Could not update the status of exploratory environment {} for user {} to {}",
            		exploratoryName, user, status, e);
       	}
    }
}
