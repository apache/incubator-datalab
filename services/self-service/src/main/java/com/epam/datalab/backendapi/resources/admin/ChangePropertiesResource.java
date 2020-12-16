package com.epam.datalab.backendapi.resources.admin;

import com.epam.datalab.auth.UserInfo;
import com.epam.datalab.backendapi.resources.dto.YmlDTO;
import com.epam.datalab.backendapi.roles.UserRoles;
import com.epam.datalab.backendapi.service.impl.DynamicChangeProperties;
import io.dropwizard.auth.Auth;
import lombok.NoArgsConstructor;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("admin")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@NoArgsConstructor
public class ChangePropertiesResource {

    @GET
    @Path("/self-service")
    public Response getSelfServiceProperties(@Auth UserInfo userInfo) {
        if (UserRoles.isAdmin(userInfo)) {
            return Response
                    .ok(DynamicChangeProperties.getSelfServiceProperties())
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
                    .ok(DynamicChangeProperties.getProvisioningServiceProperties())
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
                    .ok(DynamicChangeProperties.getBillingServiceProperties())
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
            DynamicChangeProperties.overwriteSelfServiceProperties(ymlDTO.getYmlString());
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
            DynamicChangeProperties.overwriteProvisioningServiceProperties(ymlDTO.getYmlString());
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
            DynamicChangeProperties.overwriteBillingServiceProperties(ymlDTO.getYmlString());
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
                            @QueryParam("ui") boolean ui) {
        if (UserRoles.isAdmin(userInfo)) {
        DynamicChangeProperties.restart(billing, provserv, ui);
        return Response.ok().build();
        } else {
            return Response
                    .status(Response.Status.FORBIDDEN)
                    .build();
        }
    }
}
