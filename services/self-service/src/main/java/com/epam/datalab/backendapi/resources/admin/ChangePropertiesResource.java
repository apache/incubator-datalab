package com.epam.datalab.backendapi.resources.admin;

import com.epam.datalab.auth.UserInfo;
import com.epam.datalab.backendapi.resources.dto.YmlDTO;
import com.epam.datalab.backendapi.roles.UserRoles;
import com.epam.datalab.backendapi.service.impl.DynamicChangeProperties;
import io.dropwizard.auth.Auth;
import lombok.NoArgsConstructor;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.List;
import java.util.Map;

@Path("config")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ChangePropertiesResource {

    private static final String SELF_SERVICE = "self-service.yml";
    //services/self-service/self-service.yml";
    private static final String SELF_SERVICE_PROP_PATH = "/opt/datalab/conf/self-service.yml";
    private static final String PROVISIONING_SERVICE = "provisioning.yml";
    //"services/provisioning-service/provisioning.yml";
    private static final String PROVISIONING_SERVICE_PROP_PATH = "/opt/datalab/conf/provisioning.yml";
    private static final String BILLING_SERVICE = "billing.yml";
    //"services/billing-aws/billing.yml";
    //"services/billing-azure/billing.yml";
    //"services/billing-gcp/billing.yml";
    private static final String BILLING_SERVICE_PROP_PATH = "/opt/datalab/conf/billing.yml";

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

    @GET
    @Path("/multiple/self-service")
    public Response getAllSelfServiceProperties(@Auth UserInfo userInfo) {
        if (UserRoles.isAdmin(userInfo)) {
            return Response
                    .ok(dynamicChangeProperties.getPropertiesWithExternal(SELF_SERVICE_PROP_PATH, SELF_SERVICE, userInfo))
                    .build();
        } else {
            return Response
                    .status(Response.Status.FORBIDDEN)
                    .build();
        }
    }

    @GET
    @Path("/multiple/provisioning-service")
    public Response getAllProvisioningServiceProperties(@Auth UserInfo userInfo) {
        if (UserRoles.isAdmin(userInfo)) {
            return Response
                    .ok(dynamicChangeProperties.getPropertiesWithExternal(PROVISIONING_SERVICE_PROP_PATH, PROVISIONING_SERVICE, userInfo))
                    .build();
        } else {
            return Response
                    .status(Response.Status.FORBIDDEN)
                    .build();
        }
    }

    @GET
    @Path("/multiple/billing")
    public Response getAllBillingServiceProperties(@Auth UserInfo userInfo) {
        if (UserRoles.isAdmin(userInfo)) {
            return Response
                    .ok(dynamicChangeProperties.getPropertiesWithExternal(BILLING_SERVICE_PROP_PATH, BILLING_SERVICE, userInfo))
                    .build();
        } else {
            return Response
                    .status(Response.Status.FORBIDDEN)
                    .build();
        }
    }

    @POST
    @Path("/multiple/self-service")
    public Response overwriteAllSelfServiceProperties(@Auth UserInfo userInfo, Map<String, YmlDTO> ymlDTO) {
        if (UserRoles.isAdmin(userInfo)) {
            dynamicChangeProperties.overwritePropertiesWithExternal(SELF_SERVICE_PROP_PATH, SELF_SERVICE, ymlDTO, userInfo);
            return Response.ok().build();
        } else {
            return Response
                    .status(Response.Status.FORBIDDEN)
                    .build();
        }
    }

    @POST
    @Path("/multiple/provisioning-service")
    public Response overwriteAllProvisioningServiceProperties(@Auth UserInfo userInfo, Map<String, YmlDTO> ymlDTO) {
        if (UserRoles.isAdmin(userInfo)) {
            dynamicChangeProperties.overwritePropertiesWithExternal(PROVISIONING_SERVICE_PROP_PATH, PROVISIONING_SERVICE,
                    ymlDTO, userInfo);
            return Response.ok().build();
        } else {
            return Response
                    .status(Response.Status.FORBIDDEN)
                    .build();
        }
    }

    @POST
    @Path("/multiple/billing")
    public Response overwriteAllBillingServiceProperties(@Auth UserInfo userInfo, Map<String, YmlDTO> ymlDTO) {
        if (UserRoles.isAdmin(userInfo)) {
            dynamicChangeProperties.overwritePropertiesWithExternal(BILLING_SERVICE_PROP_PATH, BILLING_SERVICE, ymlDTO,
                    userInfo);
            return Response.ok().build();
        } else {
            return Response
                    .status(Response.Status.FORBIDDEN)
                    .build();
        }
    }

    @POST
    @Path("/restart")
    public Response restart(@Auth UserInfo userInfo,
                            @QueryParam("billing") boolean billing,
                            @QueryParam("provserv") boolean provserv,
                            @QueryParam("ui") boolean ui,
                            @QueryParam("endpoints") List<String> endpoints) {
        if (UserRoles.isAdmin(userInfo)) {
            dynamicChangeProperties.restart(billing, provserv, ui);
            return Response.ok().build();
        } else {
            return Response
                    .status(Response.Status.FORBIDDEN)
                    .build();
        }
    }
}
