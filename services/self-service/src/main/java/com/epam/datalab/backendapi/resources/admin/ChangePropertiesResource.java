package com.epam.datalab.backendapi.resources.admin;

import com.epam.datalab.auth.UserInfo;
import com.epam.datalab.backendapi.resources.dto.BillingFilter;
import com.epam.datalab.backendapi.roles.UserRoles;
import io.dropwizard.auth.Auth;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("admin")
public class ChangePropertiesResource {

    @GET
    @Path("/self-service")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getSelfServiceProperties(@Auth UserInfo userInfo) {
        if (UserRoles.isAdmin(userInfo))
            return Response.ok(DinamycChangeProperties.getSelfServiceProperties());
        else return Response.status(Response.Status.FORBIDDEN).build();
    }

    @GET
    @Path("/provisioning-service")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getProvisioningServiceProperties(@Auth UserInfo userInfo, @Valid @NotNull BillingFilter filter) {
        if (UserRoles.isAdmin(userInfo))
            return Response.ok(DinamycChangeProperties.getProvisioningServiceProperties());
        else return Response.status(Response.Status.FORBIDDEN).build();
    }

    @POST
    @Path("/self-service")
    @Produces(MediaType.APPLICATION_JSON)
    public Response setSelfServiceProperties(@Auth UserInfo userInfo) {
        if (UserRoles.isAdmin(userInfo))
            return Response.ok(DinamycChangeProperties.setSelfServiceProperties());
        else return Response.status(Response.Status.FORBIDDEN).build();
    }

    @POST
    @Path("/provisioning-service")
    @Produces(MediaType.APPLICATION_JSON)
    public Response setProvisioningServiceProperties(@Auth UserInfo userInfo) {
        if (UserRoles.isAdmin(userInfo))
            return Response.ok(DinamycChangeProperties.setProvisioningServiceProperties());
        else return Response.status(Response.Status.FORBIDDEN).build();
    }

    @POST
    @Path("/restart")
    @Produces(MediaType.APPLICATION_JSON)
    public Response restart(@Auth UserInfo userInfo, boolean restartSelfService, boolean restartProvisioning) {
        if (UserRoles.isAdmin(userInfo))
            return Response.ok(DinamycChangeProperties.restart());
        else return Response.status(Response.Status.FORBIDDEN).build();
    }
}
