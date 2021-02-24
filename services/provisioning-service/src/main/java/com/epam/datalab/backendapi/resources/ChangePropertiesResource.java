package com.epam.datalab.backendapi.resources;

import com.epam.datalab.auth.UserInfo;
import com.epam.datalab.backendapi.ProvisioningServiceApplicationConfiguration;
import com.epam.datalab.properties.ChangePropertiesConst;
import com.epam.datalab.properties.DynamicChangeProperties;
import com.epam.datalab.properties.RestartForm;
import com.epam.datalab.properties.YmlDTO;
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
    public ChangePropertiesResource(DynamicChangeProperties dynamicChangeProperties,
                                    ProvisioningServiceApplicationConfiguration conf) {
        this.dynamicChangeProperties = dynamicChangeProperties;
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
        dynamicChangeProperties.restart(restartForm);
        return Response.ok().build();
    }
}
