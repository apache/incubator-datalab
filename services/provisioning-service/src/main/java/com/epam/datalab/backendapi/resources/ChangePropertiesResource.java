package com.epam.datalab.backendapi.resources;

import com.epam.datalab.auth.UserInfo;
import com.epam.datalab.backendapi.ProvisioningServiceApplicationConfiguration;
import com.epam.datalab.backendapi.core.FileHandlerCallback;
import com.epam.datalab.backendapi.core.response.folderlistener.FolderListenerExecutor;
import com.epam.datalab.backendapi.core.response.handlers.dao.CallbackHandlerDao;
import com.epam.datalab.properties.ChangePropertiesConst;
import com.epam.datalab.properties.DynamicChangeProperties;
import com.epam.datalab.properties.RestartForm;
import com.epam.datalab.properties.YmlDTO;
import com.google.common.base.Strings;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("config")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Slf4j
public class ChangePropertiesResource implements ChangePropertiesConst {

    private final DynamicChangeProperties dynamicChangeProperties;
    private final FolderListenerExecutor folderListenerExecutor;
    private final FileHandlerCallback fileHandlerCallback;
    private final CallbackHandlerDao handlerDao;


    @Inject
    public ChangePropertiesResource(DynamicChangeProperties dynamicChangeProperties,
                                    ProvisioningServiceApplicationConfiguration conf, FolderListenerExecutor folderListenerExecutor, FileHandlerCallback fileHandlerCallback, CallbackHandlerDao handlerDao) {
        this.dynamicChangeProperties = dynamicChangeProperties;
        this.folderListenerExecutor = folderListenerExecutor;
        this.fileHandlerCallback = fileHandlerCallback;
        this.handlerDao = handlerDao;
    }

    @GET
    @Path("/provisioning-service")
    public Response getProvisioningServiceProperties(@Auth UserInfo userInfo) {
        return Response
                .ok(dynamicChangeProperties.getProperties(PROVISIONING_SERVICE_PROP_PATH, PROVISIONING_SERVICE))
                .build();
    }

    @GET
    @Path("/billing")
    public Response getBillingServiceProperties(@Auth UserInfo userInfo) {
        return Response
                .ok(dynamicChangeProperties.getProperties(BILLING_SERVICE_PROP_PATH, BILLING_SERVICE))
                .build();
    }

    @POST
    @Path("/provisioning-service")
    public Response overwriteProvisioningServiceProperties(@Auth UserInfo userInfo, YmlDTO ymlDTO) {
        dynamicChangeProperties.overwriteProperties(PROVISIONING_SERVICE_PROP_PATH, PROVISIONING_SERVICE,
                ymlDTO.getYmlString());
        return Response.ok().build();
    }

    @POST
    @Path("/billing")
    public Response overwriteBillingServiceProperties(@Auth UserInfo userInfo, YmlDTO ymlDTO) {
        dynamicChangeProperties.overwriteProperties(BILLING_SERVICE_PROP_PATH, BILLING_SERVICE, ymlDTO.getYmlString());
        return Response.ok().build();

    }

    @POST
    @Path("/restart")
    public Response restart(@Auth UserInfo userInfo, RestartForm restartForm) {
        checkResponseFiles(restartForm);
        dynamicChangeProperties.restart(restartForm);
        return Response.ok().build();
    }

    private void checkResponseFiles(RestartForm restartForm) {
        boolean isNoneFinishedRequests = handlerDao.findAll().stream()
                .anyMatch(x -> Strings.isNullOrEmpty(x.getHandler().getUUID()));
        if (isNoneFinishedRequests) {
            log.info("Found unchecked response file from docker. Provisioning restart is denied");
            restartForm.setProvserv(false);
        }
    }
}
