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
import static com.epam.dlab.UserInstanceStatus.TERMINATED;
import static com.epam.dlab.UserInstanceStatus.TERMINATING;

import java.util.ArrayList;
import java.util.List;

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

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epam.dlab.UserInstanceStatus;
import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.core.UserInstanceDTO;
import com.epam.dlab.backendapi.dao.ComputationalDAO;
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.dao.ExploratoryLibDAO;
import com.epam.dlab.backendapi.dao.SettingsDAO;
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.resources.dto.ExploratoryActionFormDTO;
import com.epam.dlab.backendapi.resources.dto.ExploratoryCreateFormDTO;
import com.epam.dlab.backendapi.resources.dto.ExploratoryLibInstallFormDTO;
import com.epam.dlab.backendapi.roles.RoleType;
import com.epam.dlab.backendapi.roles.UserRoles;
import com.epam.dlab.backendapi.util.ResourceUtils;
import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.dto.StatusEnvBaseDTO;
import com.epam.dlab.dto.exploratory.ExploratoryActionDTO;
import com.epam.dlab.dto.exploratory.ExploratoryCreateDTO;
import com.epam.dlab.dto.exploratory.ExploratoryLibInstallDTO;
import com.epam.dlab.dto.exploratory.ExploratoryLibInstallStatusDTO;
import com.epam.dlab.dto.exploratory.ExploratoryStatusDTO;
import com.epam.dlab.dto.exploratory.LibInstallDTO;
import com.epam.dlab.dto.exploratory.LibStatus;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.client.RESTService;
import com.epam.dlab.rest.contracts.ApiCallbacks;
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
    private SettingsDAO settingsDAO;
    @Inject
    private ExploratoryDAO exploratoryDAO;
    @Inject
    private ComputationalDAO computationalDAO;
    @Inject
    private ExploratoryLibDAO libraryDAO;
    @Inject
    @Named(ServiceConsts.PROVISIONING_SERVICE_NAME)
    private RESTService provisioningService;

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

            ExploratoryCreateDTO dto = ResourceUtils.newResourceSysBaseDTO(userInfo, ExploratoryCreateDTO.class)
                    .withExploratoryName(formDTO.getName())
                    .withNotebookImage(formDTO.getImage())
                    .withApplicationName(ResourceUtils.getApplicationNameFromImage(formDTO.getImage()))
                    .withNotebookInstanceType(formDTO.getShape())
                    .withAwsSecurityGroupIds(settingsDAO.getAwsSecurityGroups());
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

    /** Changes the status of exploratory environment.
     * @param dto description of status.
     * @return 200 OK - if request success.
     * @exception DlabException
     */
    @POST
    @Path(ApiCallbacks.STATUS_URI)
    public Response status(ExploratoryStatusDTO dto) throws DlabException {
        LOGGER.debug("Updating status for exploratory environment {} for user {} to {}",
        		dto.getExploratoryName(), dto.getUser(), dto.getStatus());
        RequestId.checkAndRemove(dto.getRequestId());
        UserInstanceStatus currentStatus;
        
        try {
        	currentStatus = exploratoryDAO.fetchExploratoryStatus(dto.getUser(), dto.getExploratoryName());
        } catch (DlabException e) {
        	LOGGER.error("Could not get current status for exploratory environment {} for user {}",
        			dto.getExploratoryName(), dto.getUser(), e);
            throw new DlabException("Could not get current status for exploratory environment " + dto.getExploratoryName() +
            		" for user " + dto.getUser() + ": " + e.getLocalizedMessage(), e);
        }
        LOGGER.debug("Current status for exploratory environment {} for user {} is {}",
        		dto.getExploratoryName(), dto.getUser(), currentStatus);

        try {
            exploratoryDAO.updateExploratoryFields(dto);
            if (currentStatus == TERMINATING) {
            	updateComputationalStatuses(dto.getUser(), dto.getExploratoryName(), UserInstanceStatus.of(dto.getStatus()));
            } else if (currentStatus == STOPPING) {
            	updateComputationalStatuses(dto.getUser(), dto.getExploratoryName(), TERMINATED);
            }
        } catch (DlabException e) {
        	LOGGER.error("Could not update status for exploratory environment {} for user {} to {}",
        			dto.getExploratoryName(), dto.getUser(), dto.getStatus(), e);
        	throw new DlabException("Could not update status for exploratory environment " + dto.getExploratoryName() +
        			" for user " + dto.getUser() + " to " + dto.getStatus() + ": " + e.getLocalizedMessage(), e);
        }

    	return Response.ok().build();
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
        try {
        	return action(userInfo, formDTO.getNotebookInstanceName(), EXPLORATORY_START, STARTING);
        } catch (DlabException e) {
        	LOGGER.error("Could not start exploratory environment {} for user {}",
        			formDTO.getNotebookInstanceName(), userInfo.getName(), e);
        	throw new DlabException("Could not start exploratory environment " + formDTO.getNotebookInstanceName() +
        			" for user " + userInfo.getName() + ": " + e.getLocalizedMessage(), e);
        }
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
        System.out.println("stopping " + name);
        LOGGER.debug("Stopping exploratory environment {} for user {}", name, userInfo.getName());

        try {
        	updateExploratoryStatus(userInfo.getName(), name, STOPPING);
        	updateComputationalStatuses(userInfo.getName(), name, TERMINATING);
        } catch (DlabException e) {
        	LOGGER.error("Could not update status for exploratory environment {} for user {}:",
        			name, userInfo.getName(), e);
            throw new DlabException("Could not update status for exploratory environment " + name +
            		" for user " + userInfo.getName() + ": " + e.getLocalizedMessage(), e);
        }
        
        try {
            UserInstanceDTO userInstance = exploratoryDAO.fetchExploratoryFields(userInfo.getName(), name);
            ExploratoryActionDTO<?> dto = ResourceUtils.newResourceSysBaseDTO(userInfo, ExploratoryActionDTO.class);
            dto.withNotebookImage(userInstance.getImageName())
            	.withNotebookInstanceName(userInstance.getExploratoryId())
            	.withExploratoryName(name);

            String uuid = provisioningService.post(EXPLORATORY_STOP, userInfo.getAccessToken(), dto, String.class);
            RequestId.put(userInfo.getName(), uuid);
            return uuid;
        } catch (Throwable t) {
        	LOGGER.error("Could not stop exploratory environment {} for user {}",
                    name, userInfo.getName(), t);
        	updateExploratoryStatusSilent(userInfo.getName(), name, FAILED);
            throw new DlabException("Could not stop exploratory environment " + name + " for user " + userInfo.getName() + ": " + t.getLocalizedMessage(), t);
        }
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
        UserInstanceStatus status = TERMINATING;
        try {
            updateExploratoryStatus(userInfo.getName(), name, status);
            updateComputationalStatuses(userInfo.getName(), name, status);
        } catch (DlabException e) {
        	LOGGER.error("Could not update status for exploratory environment {} for user {}",
        			name, userInfo.getName(), e);
            throw new DlabException("Could not update status for exploratory environment " + name +
            		" for user " + userInfo.getName() + ": " + e.getLocalizedMessage(), e);
        }
        
        try {
        	return action(userInfo, name, EXPLORATORY_TERMINATE, status);
        } catch (DlabException e) {
        	LOGGER.error("Could not terminate exploratory environment {} for user {}",
                    name, userInfo.getName(), e);
           	throw new DlabException("Could not terminate exploratory environment " + name + " for user " + userInfo.getName() + ": " + e.getLocalizedMessage(), e);
        }
    }
    
    /** Returns the list of libraries groups for exploratory.
     * @param userInfo user info.
     * @param imageName name of exploratory image.
     */
    @POST
    @Path("/lib_list")
    public Iterable<Document> getLibList(@Auth UserInfo userInfo, @NotNull String exploratoryName) {
        LOGGER.debug("Loading list of libraries for user {} and exploratory {}", userInfo.getName(), exploratoryName);
        try {
        	return libraryDAO.findLibraries(userInfo.getName(), exploratoryName);
        } catch (Throwable t) {
        	LOGGER.error("Cannot load list of libraries for user {} and exploratory {}", userInfo.getName(), exploratoryName, t);
            throw new DlabException("Cannot load list of libraries: " + t.getLocalizedMessage(), t);
        }
    }

    /** Install the libraries to the exploratory environment.
     * @param userInfo user info.
     * @param formDTO description of libraries which will be installed to the exploratory environment.
     * @return Invocation response as JSON string.
     * @throws DlabException
     */
    @POST
    @Path("/lib_install")
    public Response libInstall(@Auth UserInfo userInfo, @Valid @NotNull ExploratoryLibInstallFormDTO formDTO) throws DlabException {
        LOGGER.debug("Installing libs to exploratory environment {} for user {}, libs {}",
        		formDTO.getNotebookName(), userInfo.getName(), formDTO);
        try {
        	UserInstanceDTO userInstance = exploratoryDAO.fetchExploratoryFields(userInfo.getName(), formDTO.getNotebookName());
        	if (UserInstanceStatus.RUNNING != UserInstanceStatus.of(userInstance.getStatus())) {
        		throw new DlabException("Exploratory " + formDTO.getNotebookName() + " is not running");
        	}
            List<LibInstallDTO> libs = new ArrayList<>();
        	ExploratoryLibInstallDTO dto = ResourceUtils.newResourceSysBaseDTO(userInfo, ExploratoryLibInstallDTO.class)
        			.withNotebookImage(userInstance.getImageName())
        			.withApplicationName(ResourceUtils.getApplicationNameFromImage(userInstance.getImageName()))
                	.withNotebookInstanceName(userInstance.getExploratoryId())
                	.withExploratoryName(formDTO.getNotebookName())
                	.withLibs(libs);
            
        	for (LibInstallDTO lib : formDTO.getLibs()) {
        		LibStatus status = libraryDAO.fetchLibraryStatus(userInfo.getName(), formDTO.getNotebookName(), lib.getGroup(), lib.getName());
        		if (status == LibStatus.INSTALLING) {
        			throw new DlabException("Library " + lib.getName() + " is already installing");
        		}
        		LibInstallDTO newLib = new LibInstallDTO()
        								.withGroup(lib.getGroup())
        								.withName(lib.getName());
				if (libs.contains(newLib)) {
					continue;
				}
        		libs.add(newLib);
        		lib.setStatus(LibStatus.INSTALLING.toString());
        		libraryDAO.addLibrary(userInfo.getName(), formDTO.getNotebookName(), lib);
        	}
        	
        	String uuid = provisioningService.post(EXPLORATORY_LIB_INSTALL, userInfo.getAccessToken(), dto, String.class);
            RequestId.put(userInfo.getName(), uuid);
            return Response.ok(uuid).build();
        } catch (DlabException e) {
        	LOGGER.error("Cannot install libs to exploratory environment {} for user {}: {}",
        			formDTO.getNotebookName(), userInfo.getName(), e.getLocalizedMessage(), e);
        	throw new DlabException("Cannot install libraries: " + e.getLocalizedMessage(), e);
        }
    }
    
    /** Changes the status of installed libraries for exploratory environment.
     * @param dto description of status.
     * @return 200 OK - if request success.
     * @exception DlabException
     */
    @POST
    @Path("/lib_status")
    public Response libInstallStatus(ExploratoryLibInstallStatusDTO dto) throws DlabException {
        LOGGER.debug("Updating status of libraries for exploratory environment {} for user {} to {}",
        		dto.getExploratoryName(), dto.getUser(), dto);
        RequestId.checkAndRemove(dto.getRequestId());
        try {
        	libraryDAO.updateLibraryFields(dto);
        } catch (DlabException e) {
        	LOGGER.error("Cannot update status of libraries for exploratory environment {} for user {} to {}",
        			dto.getExploratoryName(), dto.getUser(), dto, e);
        	throw new DlabException("Cannot update status of libaries for exploratory environment " + dto.getExploratoryName() +
        			" for user " + dto.getUser() + ": " + e.getLocalizedMessage(), e);
        }

    	return Response.ok().build();
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

            UserInstanceDTO userInstance = exploratoryDAO.fetchExploratoryFields(userInfo.getName(), exploratoryName);
            ExploratoryActionDTO<?> dto = ResourceUtils.newResourceSysBaseDTO(userInfo, ExploratoryActionDTO.class);
            dto.withNotebookImage(userInstance.getImageName())
            	.withNotebookInstanceName(userInstance.getExploratoryId())
            	.withExploratoryName(exploratoryName);

            String uuid = provisioningService.post(action, userInfo.getAccessToken(), dto, String.class);
            RequestId.put(userInfo.getName(), uuid);
            return uuid;
        } catch (Throwable t) {
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
