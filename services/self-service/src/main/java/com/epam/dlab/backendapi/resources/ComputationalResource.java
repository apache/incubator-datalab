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
import com.epam.dlab.backendapi.SelfServiceApplicationConfiguration;
import com.epam.dlab.backendapi.core.UserComputationalResourceDTO;
import com.epam.dlab.backendapi.core.UserInstanceDTO;
import com.epam.dlab.backendapi.dao.ComputationalDAO;
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.dao.SettingsDAO;
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.resources.dto.ComputationalCreateFormDTO;
import com.epam.dlab.backendapi.resources.dto.UIConfigurationDTO;
import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.dto.computational.ComputationalCreateDTO;
import com.epam.dlab.dto.computational.ComputationalStatusDTO;
import com.epam.dlab.dto.computational.ComputationalTerminateDTO;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.client.RESTService;
import com.epam.dlab.rest.contracts.ApiCallbacks;
import com.epam.dlab.rest.contracts.ComputationalAPI;
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

/** Provides the REST API for the computational resource.
 */
@Path("/infrastructure_provision/computational_resources")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class ComputationalResource implements ComputationalAPI {
    private static final Logger LOGGER = LoggerFactory.getLogger(ComputationalResource.class);

    @Inject
    private SettingsDAO settingsDAO;
    @Inject
    private ExploratoryDAO infExpDAO;
    @Inject
    private ComputationalDAO infCompDAO;
    @Inject
    @Named(ServiceConsts.PROVISIONING_SERVICE_NAME)
    private RESTService provisioningService;
    @Inject
    private SelfServiceApplicationConfiguration configuration;


    /** Returns the limits for creation the computational resources.
     * @param userInfo user info.
     */
    @GET
    @Path("/configuration")
    public UIConfigurationDTO getUIConfiguration(@Auth UserInfo userInfo) {
    	UIConfigurationDTO limits = new UIConfigurationDTO()
    			.withMinEmrInstanceCount(configuration.getMinEmrInstanceCount())
    			.withMaxEmrInstanceCount(configuration.getMaxEmrInstanceCount())
    			.withMaxEmrSpotInstanceBidPct(configuration.getMaxEmrSpotInstanceBidPct())
    			.withMinEmrSpotInstanceBidPct(configuration.getMinEmrSpotInstanceBidPct());
    	LOGGER.debug("Returns limits for user {}: {}", userInfo.getName(), limits.toString());
        return limits;
    }
    
    /** Sends request to provisioning service for creation the computational resource for user.
     * @param userInfo user info.
     * @param formDTO DTO info about creation of the computational resource.
     * @return 200 OK - if request success, 302 Found - for duplicates, otherwise throws exception.
     * @exception DlabException
     */
    @PUT
    public Response create(@Auth UserInfo userInfo, @Valid @NotNull ComputationalCreateFormDTO formDTO) throws DlabException {
    	LOGGER.debug("Send request for creation the computational resource {} for user {}", formDTO.getName(), userInfo.getName());

        int slaveInstanceCount = Integer.parseInt(formDTO.getInstanceCount());
        if (slaveInstanceCount < configuration.getMinEmrInstanceCount() || slaveInstanceCount > configuration.getMaxEmrInstanceCount()) {
            LOGGER.debug("Creating computational resource {} for user {} fail: Limit exceeded to creation slave instances. Minimum is {}, maximum is {}",
            		formDTO.getName(), userInfo.getName(), configuration.getMinEmrInstanceCount(), configuration.getMaxEmrInstanceCount());
            throw new DlabException("Limit exceeded to creation slave instances. Minimum is " + configuration.getMinEmrInstanceCount() +
            		", maximum is " + configuration.getMaxEmrInstanceCount() + ".");
        }

        int slaveSpotInstanceBidPct = formDTO.getSlaveInstanceSpotPctPrice();
        if (formDTO.getSlaveInstanceSpot() && (slaveSpotInstanceBidPct < configuration.getMinEmrSpotInstanceBidPct() || slaveSpotInstanceBidPct > configuration.getMaxEmrSpotInstanceBidPct())) {
            LOGGER.debug("Creating computational resource {} for user {} fail: Spot instances bidding percentage value out of the boundaries. Minimum is {}, maximum is {}",
                    formDTO.getName(), userInfo.getName(), configuration.getMinEmrSpotInstanceBidPct(), configuration.getMaxEmrSpotInstanceBidPct());
            throw new DlabException("Spot instances bidding percentage value out of the boundaries. Minimum is " + configuration.getMinEmrSpotInstanceBidPct() +
                    ", maximum is " + configuration.getMaxEmrSpotInstanceBidPct() + ".");
        }

        boolean isAdded = infCompDAO.addComputational(userInfo.getName(), formDTO.getNotebookName(),
                new UserComputationalResourceDTO()
                        .withComputationalName(formDTO.getName())
                        .withStatus(CREATING.toString())
                        .withMasterShape(formDTO.getMasterInstanceType())
                        .withSlaveShape(formDTO.getSlaveInstanceType())
                        .withSlaveSpot(formDTO.getSlaveInstanceSpot())
                        .withSlaveSpotPctPrice(formDTO.getSlaveInstanceSpotPctPrice())
                        .withSlaveNumber(formDTO.getInstanceCount())
                        .withVersion(formDTO.getVersion()));
        if (isAdded) {
            try {
            	UserInstanceDTO instance = infExpDAO.fetchExploratoryFields(userInfo.getName(), formDTO.getNotebookName());
                ComputationalCreateDTO dto = new ComputationalCreateDTO()
                        .withServiceBaseName(settingsDAO.getServiceBaseName())
                        .withExploratoryName(formDTO.getNotebookName())
                        .withNotebookTemplateName(instance.getTemplateName())
                        .withApplicationName(getApplicationName(instance.getImageName()))
                        .withComputationalName(formDTO.getName())
                        .withNotebookInstanceName(instance.getExploratoryId())
                        .withInstanceCount(formDTO.getInstanceCount())
                        .withMasterInstanceType(formDTO.getMasterInstanceType())
                        .withSlaveInstanceType(formDTO.getSlaveInstanceType())
                        .withSlaveInstanceSpot(formDTO.getSlaveInstanceSpot())
                        .withSlaveInstanceSpotPctPrice(formDTO.getSlaveInstanceSpotPctPrice())
                        .withVersion(formDTO.getVersion())
                        .withEdgeUserName(userInfo.getSimpleName())
                        .withAwsIamUser(userInfo.getName())
                        .withAwsRegion(settingsDAO.getAwsRegion())
                        .withConfOsUser(settingsDAO.getConfOsUser())
                        .withConfOsFamily(settingsDAO.getConfOsFamily());
                String uuid = provisioningService.post(EMR_CREATE, userInfo.getAccessToken(), dto, String.class);
                RequestId.put(userInfo.getName(), uuid);
                return Response.ok(uuid).build();
            } catch (Throwable t) {
            	try {
            		updateComputationalStatus(userInfo.getName(), formDTO.getNotebookName(), formDTO.getName(), FAILED);
            	} catch (DlabException e) {
            		LOGGER.error("Could not update the status of computational resource {} for user {}", formDTO.getName(), userInfo.getName(), e);
            	}
                throw new DlabException("Could not send request for creation the computational resource " + formDTO.getName() + ": " + t.getLocalizedMessage(), t);
            }
        } else {
            LOGGER.debug("Used existing computational resource {} for user {}", formDTO.getName(), userInfo.getName());
            return Response.status(Response.Status.FOUND).build();
        }
    }

    /** Updates the status of the computational resource for user.
     * @param dto DTO info about the status of the computational resource.
     * @return 200 OK - if request success otherwise throws exception.
     * @exception DlabException if update the status fails.
     */
    @POST
    @Path(ApiCallbacks.STATUS_URI)
    public Response status(ComputationalStatusDTO dto) throws DlabException {
        LOGGER.debug("Updating status for computational resource {} for user {}: {}", dto.getComputationalName(), dto.getUser(), dto);
        String uuid = dto.getRequestId();
        RequestId.checkAndRemove(uuid);

        try {
        	infCompDAO.updateComputationalFields(dto);
        } catch (DlabException e) {
        	LOGGER.error("Could not update status for computational resource {} for user {} to {}: {}",
        			dto.getComputationalName(), dto.getUser(), dto.getStatus(), e.getLocalizedMessage(), e);
        	throw new DlabException("Could not update status for computational resource " + dto.getComputationalName() +
        			" for user " + dto.getUser() + " to " + dto.getStatus() + ": " + e.getLocalizedMessage(), e);
        }
        if (UserInstanceStatus.CONFIGURING == UserInstanceStatus.of(dto.getStatus())) {
            LOGGER.debug("Waiting for configuration of the computational resource {} for user {}", dto.getComputationalName(), dto.getUser());
            RequestId.put(dto.getUser(), uuid);
        }
        return Response.ok().build();
    }

    /** Sends request to provisioning service for termination the computational resource for user.
     * @param userInfo user info.
     * @param exploratoryName name of exploratory.
     * @param computationalName name of computational resource.
     * @return JSON formatted string representation of {@link ComputationalTerminateDTO}, otherwise fails.
     * @exception DlabException
     */
    @DELETE
    @Path("/{exploratoryName}/{computationalName}/terminate")
    public String terminate(@Auth UserInfo userInfo,
    		@PathParam("exploratoryName") String exploratoryName,
    		@PathParam("computationalName") String computationalName) throws DlabException {
        LOGGER.debug("Terminating computational resource {} for user {}", computationalName, userInfo.getName());
        try {
    		updateComputationalStatus(userInfo.getName(), exploratoryName, computationalName, TERMINATING);
    	} catch (DlabException e) {
    		LOGGER.error("Could not update the status of computational resource {} for user {}", computationalName, userInfo.getName(), e);
    		throw new DlabException("Could not terminate computational resource " + computationalName + ": " + e.getLocalizedMessage(), e);
    	}
        
        try {
            String exploratoryId = infExpDAO.fetchExploratoryId(userInfo.getName(), exploratoryName);
            String computationalId = infCompDAO.fetchComputationalId(userInfo.getName(), exploratoryName, computationalName);
            ComputationalTerminateDTO dto = new ComputationalTerminateDTO()
                    .withServiceBaseName(settingsDAO.getServiceBaseName())
                    .withExploratoryName(exploratoryName)
                    .withComputationalName(computationalName)
                    .withNotebookInstanceName(exploratoryId)
                    .withClusterName(computationalId)
                    .withConfKeyDir(settingsDAO.getConfKeyDir())
                    .withConfOsUser(settingsDAO.getConfOsUser())
                    .withEdgeUserName(userInfo.getSimpleName())
                    .withAwsIamUser(userInfo.getName())
                    .withAwsRegion(settingsDAO.getAwsRegion());

            String uuid = provisioningService.post(EMR_TERMINATE, userInfo.getAccessToken(), dto, String.class);
            RequestId.put(userInfo.getName(), uuid);
            return uuid;
        } catch (Throwable t) {
        	try {
        		updateComputationalStatus(userInfo.getName(), exploratoryName, computationalName, FAILED);
        	} catch (DlabException e) {
        		LOGGER.error("Could not update the status of computational resource {} for user {}", computationalName, userInfo.getName(), e);
        	}
            throw new DlabException("Could not terminate computational resource " + computationalName + ": " + t.getLocalizedMessage(), t);
        }
    }

    /** Updates the status of computational resource in database.
     * @param user user name.
     * @param exploratoryName name of exploratory.
     * @param computationalName name of computational resource.
     * @param status status
     * @throws DlabException if the update fails.
     */
    private void updateComputationalStatus(String user, String exploratoryName, String computationalName, UserInstanceStatus status) throws DlabException {
        ComputationalStatusDTO computationalStatus = new ComputationalStatusDTO()
                .withUser(user)
                .withExploratoryName(exploratoryName)
                .withComputationalName(computationalName)
                .withStatus(status);
        infCompDAO.updateComputationalStatus(computationalStatus);
    }


    /** Returns the name of application for notebook: jupiter, rstudio, etc. */
    private String getApplicationName(String imageName) {
    	if (imageName != null) {
    		int pos = imageName.lastIndexOf('-');
    		if (pos > 0) {
    			return imageName.substring(pos + 1);
    		}
    	}
    	return "";
    }

}
