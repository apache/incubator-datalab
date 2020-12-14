package com.epam.datalab.backendapi.resources.admin;

import com.epam.datalab.auth.UserInfo;
import com.epam.datalab.backendapi.roles.UserRoles;
import com.epam.datalab.backendapi.service.impl.DynamicChangeProperties;
import io.dropwizard.auth.Auth;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("admin")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ChangePropertiesResource {

    private final DynamicChangeProperties dynamic;

    @Inject
    public ChangePropertiesResource(DynamicChangeProperties dynamic) {
        this.dynamic = dynamic;
    }

    @GET
    @Path("/self-service")
    public Response getSelfServiceProperties(@Auth UserInfo userInfo) {
        if (UserRoles.isAdmin(userInfo)) {
            return Response
                    .ok(dynamic.getSelfServiceProperties())
                    .build();
        }
        return Response
                .status(Response.Status.FORBIDDEN)
                .build();
    }

    @GET
    @Path("/provisioning-service")
    public Response getProvisioningServiceProperties(@Auth UserInfo userInfo) {
        if (UserRoles.isAdmin(userInfo)) {
            return Response
                    .ok(dynamic.getProvisioningServiceProperties())
                    .build();
        } else {
            return Response
                    .status(Response.Status.FORBIDDEN)
                    .build();
        }
    }

    @POST
    @Path("/self-service")
    public Response overwriteSelfServiceProperties(@Auth UserInfo userInfo, String ymlString) {
        if (UserRoles.isAdmin(userInfo)) {
            dynamic.overwriteSelfServiceProperties(ymlString);
            return Response.ok().build();
        }
        return Response
                .status(Response.Status.FORBIDDEN)
                .build();
    }

    @POST
    @Path("/provisioning-service")
    public Response overwriteProvisioningServiceProperties(@Auth UserInfo userInfo, String ymlString) {
        if (UserRoles.isAdmin(userInfo)) {
            dynamic.overwriteProvisioningServiceProperties(ymlString);
            return Response.ok().build();
        } else {
            return Response
                    .status(Response.Status.FORBIDDEN)
                    .build();
        }
    }

//    @POST
//    @Path("/restart")
//    public Response restart(@Auth UserInfo userInfo, boolean restartSelfService, boolean restartProvisioning) {
//        if (UserRoles.isAdmin(userInfo)) {
//            dynamic.restart(restartSelfService,restartProvisioning);
//            return Response.ok().build();
//        } else {
//            return Response
//                    .status(Response.Status.FORBIDDEN)
//                    .build();
//        }
//    }
}
