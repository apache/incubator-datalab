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

import static com.epam.dlab.UserInstanceStatus.FAILED;
import static com.epam.dlab.UserInstanceStatus.TERMINATED;
import static com.epam.dlab.constants.ServiceConsts.PROVISIONING_SERVICE_NAME;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.stream.Collectors;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epam.dlab.UserInstanceStatus;
import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.dao.KeyDAO;
import com.epam.dlab.backendapi.dao.SettingsDAO;
import com.epam.dlab.backendapi.domain.RequestId;
import com.epam.dlab.backendapi.util.ResourceUtils;
import com.epam.dlab.dto.edge.EdgeCreateDTO;
import com.epam.dlab.dto.edge.EdgeInfoDTO;
import com.epam.dlab.dto.keyload.KeyLoadStatus;
import com.epam.dlab.dto.keyload.UploadFileDTO;
import com.epam.dlab.dto.keyload.UploadFileResultDTO;
import com.epam.dlab.dto.keyload.UserKeyDTO;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.client.RESTService;
import com.epam.dlab.rest.contracts.EdgeAPI;
import com.google.inject.Inject;
import com.google.inject.name.Named;

import io.dropwizard.auth.Auth;

/** Provides the REST API for upload the user key.
 */
@Path("/user/access_key")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class KeyUploaderResource implements EdgeAPI {
    private static final Logger LOGGER = LoggerFactory.getLogger(KeyUploaderResource.class);

    @Inject
    private KeyDAO keyDAO;
    @Inject
    private SettingsDAO settingsDAO;
    @Inject
    @Named(PROVISIONING_SERVICE_NAME)
    private RESTService provisioningService;

    /** Finds and returns the status of the user key.
     * @param userInfo user info.
     * @return <pre>
     * {@link Status#NOT_FOUND} the user key has been not found.
     * {@link Status#ACCEPTED} the user key is uploading now.
     * {@link Status#OK} the user key is valid.
     * {@link Status#INTERNAL_SERVER_ERROR} the check of the status is failed.
     * </pre>
     */
    @GET
    public Response checkKey(@Auth UserInfo userInfo) {
    	LOGGER.debug("Check the status of the user key for {}", userInfo.getName());
    	KeyLoadStatus status;
    	try {
    		status = keyDAO.findKeyStatus(userInfo.getName());
        	LOGGER.debug("The status of the user key for {} is {}", userInfo.getName(), status.name());
    	} catch (DlabException e) {
    		LOGGER.error("Check the status of the user key for {} fails", userInfo.getName(), e);
    		status = KeyLoadStatus.ERROR;
    	}
		return Response.status(status.getHttpStatus()).build();
    }

    /** Uploads the user key to server. Stores the user key to database and calls the post method
     * of the provisioning service for the upload of the key to the provisioning service and creation
     * of the EDGE notebook for user.
     * @param userInfo user info.
     * @param uploadedInputStream content of the user key.
     * @param fileDetail
     * @return 200 OK
     * @throws IOException, DlabException
     */
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response uploadKey(@Auth UserInfo userInfo,
                         @FormDataParam("file") InputStream uploadedInputStream,
                         @FormDataParam("file") FormDataContentDisposition fileDetail) throws IOException, DlabException {
        LOGGER.debug("Upload the key for user {}", userInfo.getName());
        String content;
        try (BufferedReader buffer = new BufferedReader(new InputStreamReader(uploadedInputStream))) {
            content = buffer.lines().collect(Collectors.joining("\n"));
        } catch (IOException|DlabException e) {
    		LOGGER.error("Could not upload the key for user {}", userInfo.getName(), e);
    		throw new DlabException("Could not upload the key for user " + userInfo.getName() + ": " + e.getLocalizedMessage(), e);
    	}
        startKeyUpload(userInfo, content, null);
        return Response.ok().build();
    }
    
    /** Creates the EDGE node and upload the user key  for user.
     * @param userInfo user info.
     * @return {@link Response.Status#OK} request for provisioning service has been accepted.<br>
     * @throws DlabException
     */
    @POST
    @Path("/recover")
    public Response recover(@Auth UserInfo userInfo) throws DlabException {
        LOGGER.debug("Recreating edge node for user {}", userInfo.getName());

        EdgeInfoDTO edgeInfo = keyDAO.getEdgeInfo(userInfo.getName());
        UserInstanceStatus status = UserInstanceStatus.of(edgeInfo.getEdgeStatus());
    	if (status == null || !status.in(FAILED, TERMINATED)) {
        	LOGGER.error("Could not create EDGE node for user {} because the status of instance is {}", userInfo.getName(), status);
            throw new DlabException("Could not create EDGE node because the status of instance is " + status);
        }

    	UserKeyDTO key = keyDAO.fetchKey(userInfo.getName());
    	KeyLoadStatus keyStatus = KeyLoadStatus.findByStatus(key.getStatus());
    	if (keyStatus == null || keyStatus != KeyLoadStatus.SUCCESS) {
        	LOGGER.error("Could not create EDGE node for user {} because the status of user key is {}", userInfo.getName(), keyStatus);
            throw new DlabException("Could not create EDGE node because the status of user key is " + keyStatus);
        }
    	
        try {
        	edgeInfo.withInstanceId(null)
        			.withEdgeStatus(UserInstanceStatus.CREATING.toString());
        	keyDAO.updateEdgeInfo(userInfo.getName(), edgeInfo);
        } catch (DlabException e) {
        	LOGGER.error("Could not update the status of EDGE node for user {}", userInfo.getName(), e);
            throw new DlabException("Could not create EDGE node: " + e.getLocalizedMessage(), e);
        }
        
        try {
			Thread.sleep(10000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
        
        try {
            String uuid = startKeyUpload(userInfo,
            		key.getContent(),
            		edgeInfo.getPublicIp());
            return Response.ok(uuid).build();
        } catch (Throwable e) {
            LOGGER.error("Could not create the EDGE node for user {}", userInfo.getName(), e);
            keyDAO.updateEdgeStatus(userInfo.getName(), UserInstanceStatus.FAILED.toString());
            throw new DlabException("Could not create EDGE node: " + e.getLocalizedMessage(), e);
        }
    }

    /** Store the user key into database and file system, create EDGE node.
     * @param userInfo user info.
     * @param keyContent content of file key
     * @throws DlabException
     */
    private String startKeyUpload(UserInfo userInfo, String keyContent, String publicIp) throws DlabException {
		LOGGER.debug("The upload of the user key and creation EDGE node will be started for user {}", userInfo.getName());
    	if (publicIp == null) {
    		keyDAO.insertKey(userInfo.getName(), keyContent);
    	}
    	
        try {
            EdgeCreateDTO edge = ResourceUtils.newResourceSysBaseDTO(userInfo, EdgeCreateDTO.class)
                    .withAwsSecurityGroupIds(settingsDAO.getAwsSecurityGroups())
                    .withAwsVpcId(settingsDAO.getAwsVpcId())
                    .withAwsSubnetId(settingsDAO.getAwsSubnetId())
		            .withEdgeElasticIp(publicIp);
            
            UploadFileDTO dto = new UploadFileDTO()
                    .withEdge(edge)
                    .withContent(keyContent);
            String uuid = provisioningService.post(EDGE_CREATE, userInfo.getAccessToken(), dto, String.class);
            RequestId.put(userInfo.getName(), uuid);
            return uuid;
        } catch (Exception e) {
        	LOGGER.error("The upload of the user key and create EDGE node for user {} fails", userInfo.getName(), e);
            keyDAO.deleteKey(userInfo.getName());
            throw new DlabException("Could not upload the key and create EDGE node: " + e.getLocalizedMessage(), e);
        }
    }
    
    /** Stores the result of the upload the user key.
     * @param dto result of the upload the user key.
     * @return 200 OK
     */
    @POST
    @Path("/callback")
    public Response loadKeyResponse(UploadFileResultDTO dto) throws DlabException {
        LOGGER.debug("Upload the key result and EDGE node info for user {}: {}", dto.getUser(), dto);
        RequestId.checkAndRemove(dto.getRequestId());
        boolean isSuccess = UserInstanceStatus.of(dto.getStatus()) == UserInstanceStatus.RUNNING;
        try {
            keyDAO.updateKey(dto.getUser(), KeyLoadStatus.getStatus(isSuccess));
            if (isSuccess) {
            	keyDAO.updateEdgeInfo(dto.getUser(), dto.getEdgeInfo());
            } else {
            	UserInstanceStatus status = UserInstanceStatus.of(keyDAO.getEdgeStatus(dto.getUser()));
            	if (status == null) {
            		// Upload the key first time
            		LOGGER.debug("Delete the key for user {}", dto.getUser());
            		keyDAO.deleteKey(dto.getUser());
            	}
            }
        } catch (DlabException e) {
    		LOGGER.error("Could not upload the key result and create EDGE node for user {}", dto.getUser(), e);
    		throw new DlabException("Could not upload the key result and create EDGE node for user " + dto.getUser() + ": " + e.getLocalizedMessage(), e);
    	}
        return Response.ok().build();
    }
}
