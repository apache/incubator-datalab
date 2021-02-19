package com.epam.datalab.backendapi.resources.admin;

import com.epam.datalab.auth.UserInfo;
import com.epam.datalab.backendapi.modules.ChangePropertiesConst;
import com.epam.datalab.backendapi.modules.RestartForm;
import com.epam.datalab.backendapi.resources.dto.YmlDTO;
import com.epam.datalab.backendapi.roles.UserRoles;
import com.epam.datalab.backendapi.service.impl.DynamicChangeProperties;
import io.dropwizard.auth.Auth;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("config")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ChangePropertiesResource implements ChangePropertiesConst {

    private final DynamicChangeProperties dynamicChangeProperties;

    @Inject
    public ChangePropertiesResource(DynamicChangeProperties dynamicChangeProperties) {
        this.dynamicChangeProperties = dynamicChangeProperties;
    }

    @GET
    @Path("/self-service")
    public Response getSelfServiceProperties(@Auth UserInfo userInfo) {
        if (UserRoles.isAdmin(userInfo)) {
            return Response
                    .ok(dynamicChangeProperties.getProperties(SELF_SERVICE_PROP_PATH, SELF_SERVICE))
                    .build();
        } else {
            return Response
                    .status(Response.Status.FORBIDDEN)
                    .build();
        }
    }

    @GET
    @Path("/provisioning-service")
    public Response getProvisioningServiceProperties(@Auth UserInfo userInfo) {
        if (UserRoles.isAdmin(userInfo)) {
            return Response
                    .ok(dynamicChangeProperties.getProperties(PROVISIONING_SERVICE_PROP_PATH, PROVISIONING_SERVICE))
                    .build();
        } else {
            return Response
                    .status(Response.Status.FORBIDDEN)
                    .build();
        }
    }

    @GET
    @Path("/billing")
    public Response getBillingServiceProperties(@Auth UserInfo userInfo) {
        if (UserRoles.isAdmin(userInfo)) {
            return Response
                    .ok(dynamicChangeProperties.getProperties(BILLING_SERVICE_PROP_PATH, BILLING_SERVICE))
                    .build();
        } else {
            return Response
                    .status(Response.Status.FORBIDDEN)
                    .build();
        }
    }

    @POST
    @Path("/self-service")
    public Response overwriteSelfServiceProperties(@Auth UserInfo userInfo, YmlDTO ymlDTO) {
        if (UserRoles.isAdmin(userInfo)) {
            dynamicChangeProperties.overwriteProperties(SELF_SERVICE_PROP_PATH, SELF_SERVICE, ymlDTO.getYmlString());
            return Response.ok().build();
        } else {
            return Response
                    .status(Response.Status.FORBIDDEN)
                    .build();
        }
    }

    @POST
    @Path("/provisioning-service")
    public Response overwriteProvisioningServiceProperties(@Auth UserInfo userInfo, YmlDTO ymlDTO) {
        if (UserRoles.isAdmin(userInfo)) {
            dynamicChangeProperties.overwriteProperties(PROVISIONING_SERVICE_PROP_PATH, PROVISIONING_SERVICE,
                    ymlDTO.getYmlString());
            return Response.ok().build();
        } else {
            return Response
                    .status(Response.Status.FORBIDDEN)
                    .build();
        }
    }

    @POST
    @Path("/billing")
    public Response overwriteBillingServiceProperties(@Auth UserInfo userInfo, YmlDTO ymlDTO) {
        if (UserRoles.isAdmin(userInfo)) {
            dynamicChangeProperties.overwriteProperties(BILLING_SERVICE_PROP_PATH, BILLING_SERVICE, ymlDTO.getYmlString());
            return Response.ok().build();
        } else {
            return Response
                    .status(Response.Status.FORBIDDEN)
                    .build();
        }
    }

    @POST
    @Path("restart")
    public Response restart(@Auth UserInfo userInfo, RestartForm restartForm) {
        if (UserRoles.isAdmin(userInfo)) {
            dynamicChangeProperties.restart(restartForm);
            return Response.ok().build();
        } else {
            return Response
                    .status(Response.Status.FORBIDDEN)
                    .build();
        }
    }

    @GET
    @Path("/multiple")
    public Response getAllPropertiesForEndpoint(@Auth UserInfo userInfo, @QueryParam("endpoint") String endpoint) {
        if (UserRoles.isAdmin(userInfo)) {
            return Response
                    .ok(dynamicChangeProperties.getPropertiesWithExternal(endpoint, userInfo))
                    .build();
        } else {
            return Response
                    .status(Response.Status.FORBIDDEN)
                    .build();
        }
    }

    @POST
    @Path("/multiple/self-service")
    public Response overwriteExternalSelfServiceProperties(@Auth UserInfo userInfo, YmlDTO ymlDTO) {
        if (UserRoles.isAdmin(userInfo)) {
            dynamicChangeProperties.overwritePropertiesWithExternal(SELF_SERVICE_PROP_PATH, SELF_SERVICE,
                    ymlDTO, userInfo);
            return Response.status(Response.Status.OK).build();
        } else {
            return Response
                    .status(Response.Status.FORBIDDEN)
                    .build();
        }
    }

    @POST
    @Path("/multiple/provisioning-service")
    public Response overwriteExternalProvisioningServiceProperties(@Auth UserInfo userInfo, YmlDTO ymlDTO) {
        if (UserRoles.isAdmin(userInfo)) {
            dynamicChangeProperties.overwritePropertiesWithExternal(PROVISIONING_SERVICE_PROP_PATH, PROVISIONING_SERVICE,
                    ymlDTO, userInfo);
            return Response.status(Response.Status.OK).build();
        } else {
            return Response
                    .status(Response.Status.FORBIDDEN)
                    .build();
        }
    }

    @POST
    @Path("/multiple/billing")
    public Response overwriteExternalBillingProperties(@Auth UserInfo userInfo, YmlDTO ymlDTO) {
        if (UserRoles.isAdmin(userInfo)) {
            dynamicChangeProperties.overwritePropertiesWithExternal(BILLING_SERVICE_PROP_PATH, BILLING_SERVICE,
                    ymlDTO, userInfo);
            return Response.status(Response.Status.OK).build();
        } else {
            return Response
                    .status(Response.Status.FORBIDDEN)
                    .build();
        }
    }


    @POST
    @Path("/multiple/restart")
    public Response restartWithExternal(@Auth UserInfo userInfo, RestartForm restartForm) {
        if (UserRoles.isAdmin(userInfo)) {
            dynamicChangeProperties.restartForExternal(restartForm, userInfo);
            return Response.ok().build();
        } else {
            return Response
                    .status(Response.Status.FORBIDDEN)
                    .build();
        }
    }
}