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

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.ExploratoryDAO;
import com.epam.dlab.backendapi.dao.KeyDAO;
import com.epam.dlab.backendapi.roles.RoleType;
import com.epam.dlab.backendapi.roles.UserRoles;
import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.dto.edge.EdgeInfoDTO;
import com.epam.dlab.dto.imagemetadata.ComputationalMetadataDTO;
import com.epam.dlab.dto.imagemetadata.ExploratoryMetadataDTO;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.client.RESTService;
import com.epam.dlab.rest.contracts.DockerAPI;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.dropwizard.auth.Auth;

/** Provides the REST API for the information about provisioning infrastructure.
 */
@Path("/infrastructure_provision")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class InfrastructureProvisionResource implements DockerAPI {
    public static final String EDGE_IP = "edge_node_ip";
    private static final Logger LOGGER = LoggerFactory.getLogger(InfrastructureProvisionResource.class);

    @Inject
    private ExploratoryDAO expDAO;
    @Inject
    private KeyDAO keyDAO;
    @Inject
    @Named(ServiceConsts.PROVISIONING_SERVICE_NAME)
    private RESTService provisioningService;

    /** Returns the list of the provisioned user resources.
     * @param userInfo user info.
     */
    @GET
    @Path("/provisioned_user_resources")
    public Iterable<Document> getUserResources(@Auth UserInfo userInfo) throws DlabException {
        LOGGER.debug("Loading list of provisioned resources for user {}", userInfo.getName());
        try {
        	Iterable<Document> documents = expDAO.findExploratory(userInfo.getName());
        	EdgeInfoDTO edgeInfo = keyDAO.getEdgeInfo(userInfo.getName());
        	List<Document> notebooks = new ArrayList<Document>();
        	
        	int i = 0;
    		for (Document d : documents) {
        		d.append(EDGE_IP, edgeInfo.getPublicIp())
    			 .append(EdgeInfoDTO.USER_OWN_BUCKET_NAME, edgeInfo.getUserOwnBucketName());
        		notebooks.add(d);
        		LOGGER.debug("Notebook[{}]: {}", ++i, d);
        	}
    		return notebooks;
        } catch (Throwable t) {
        	LOGGER.error("Could not load list of provisioned resources for user: {}", userInfo.getName(), t);
            throw new DlabException("Could not load list of provisioned resources for user " + userInfo.getName() + ": " + t.getLocalizedMessage(), t);
        }
    }

    /** Returns the list of the computational resources templates for user.
     * @param userInfo user info.
     */
    @GET
    @Path("/computational_resources_templates")
    public Iterable<ComputationalMetadataDTO> getComputationalTemplates(@Auth UserInfo userInfo) {
        LOGGER.debug("Loading list of computational templates for user {}", userInfo.getName());
        try {
        	ComputationalMetadataDTO [] array = provisioningService.get(DOCKER_COMPUTATIONAL, userInfo.getAccessToken(), ComputationalMetadataDTO[].class);
        	List<ComputationalMetadataDTO> list = new ArrayList<>();
        	for (int i = 0; i < array.length; i++) {
        		array[i].setImage(getSimpleImageName(array[i].getImage()));
	            if (UserRoles.checkAccess(userInfo, RoleType.COMPUTATIONAL, array[i].getImage())) {
        			list.add(array[i]);
        		}
        	}
	        return list;
        } catch (Throwable t) {
        	LOGGER.error("Could not load list of computational templates for user: {}", userInfo.getName(), t);
            throw new DlabException("Could not load list of computational templates for user " + userInfo.getName() + ": " + t.getLocalizedMessage(), t);
        }
    }

    /** Returns the list of the exploratory environment templates for user.
     * @param userInfo user info.
     */
    @GET
    @Path("/exploratory_environment_templates")
    public Iterable<ExploratoryMetadataDTO> getExploratoryTemplates(@Auth UserInfo userInfo) {
        LOGGER.debug("Loading list of exploratory templates for user {}", userInfo.getName());
        try {
        	ExploratoryMetadataDTO [] array = provisioningService.get(DOCKER_EXPLORATORY, userInfo.getAccessToken(), ExploratoryMetadataDTO[].class);
        	List<ExploratoryMetadataDTO> list = new ArrayList<>();
        	for (int i = 0; i < array.length; i++) {
    			array[i].setImage(getSimpleImageName(array[i].getImage()));
	            if (UserRoles.checkAccess(userInfo, RoleType.EXPLORATORY, array[i].getImage())) {
        			list.add(array[i]);
        		}
        	}
	        return list;
        } catch (Throwable t) {
        	LOGGER.error("Could not load list of exploratory templates for user: {}", userInfo.getName(), t);
            throw new DlabException("Could not load list of exploratory templates for user " + userInfo.getName() + ": " + t.getLocalizedMessage(), t);
        }
    }
    
    /** Return the image name without suffix version.
     * @param imageName the name of image.
     */
    private String getSimpleImageName(String imageName) {
    	int separatorIndex = imageName.indexOf(":");
        return (separatorIndex > 0 ? imageName.substring(0, separatorIndex) : imageName);
    }
}

