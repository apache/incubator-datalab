package com.epam.datalab.backendapi.resources;

import com.epam.datalab.properties.ChangePropertiesConst;
import com.epam.datalab.properties.DynamicChangeProperties;
import com.epam.datalab.properties.RestartForm;
import com.epam.datalab.properties.YmlDTO;
import com.epam.datalab.auth.UserInfo;
import com.epam.datalab.backendapi.conf.SelfServiceApplicationConfiguration;
import com.epam.datalab.backendapi.dao.EndpointDAO;
import com.epam.datalab.backendapi.domain.EndpointDTO;
import com.epam.datalab.backendapi.roles.UserRoles;
import com.epam.datalab.exceptions.ResourceNotFoundException;
import io.dropwizard.auth.Auth;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

@Path("config")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ChangePropertiesResource implements ChangePropertiesConst {

    private final EndpointDAO endpointDAO;
    private final DynamicChangeProperties dynamicChangeProperties;
    private final String deployedOn;

    @Inject
    public ChangePropertiesResource(EndpointDAO endpointDAO, DynamicChangeProperties dynamicChangeProperties,
                                    SelfServiceApplicationConfiguration selfServiceApplicationConfiguration) {
        this.endpointDAO = endpointDAO;
        this.dynamicChangeProperties = dynamicChangeProperties;
        deployedOn = selfServiceApplicationConfiguration.getDeployed();
    }

    @GET
    @Path("/multiple")
    public Response getAllPropertiesForEndpoint(@Auth UserInfo userInfo, @QueryParam("endpoint") String endpoint) {
        if (UserRoles.isAdmin(userInfo)) {
            String url = findEndpointDTO(endpoint).getUrl() + ChangePropertiesConst.BASE_CONFIG_URL;
            return Response
                    .ok(dynamicChangeProperties.getPropertiesWithExternal(endpoint, userInfo, url))
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
            if (deployedOn.equals("GKE")) {
                dynamicChangeProperties.overwritePropertiesWithExternal(GKE_SELF_SERVICE_PATH, GKE_SELF_SERVICE,
                        ymlDTO, userInfo, null);
            } else {
                dynamicChangeProperties.overwritePropertiesWithExternal(SELF_SERVICE_PROP_PATH, SELF_SERVICE,
                        ymlDTO, userInfo, null);
            }
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
            String url = findEndpointDTO(ymlDTO.getEndpointName()).getUrl() + ChangePropertiesConst.BASE_MULTIPLE_CONFIG_URL;
            dynamicChangeProperties.overwritePropertiesWithExternal(PROVISIONING_SERVICE_PROP_PATH, PROVISIONING_SERVICE,
                    ymlDTO, userInfo, url);
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
            String url = findEndpointDTO(ymlDTO.getEndpointName()).getUrl() + ChangePropertiesConst.BASE_MULTIPLE_CONFIG_URL;
            dynamicChangeProperties.overwritePropertiesWithExternal(BILLING_SERVICE_PROP_PATH, BILLING_SERVICE,
                    ymlDTO, userInfo, url);
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
            if (deployedOn.equals("GKE")) {
                dynamicChangeProperties.restartForExternalForGKE(userInfo, restartForm);
            } else {
                String url = findEndpointDTO(restartForm.getEndpoint()).getUrl() + ChangePropertiesConst.RESTART_URL;
                dynamicChangeProperties.restartForExternal(restartForm, userInfo, url);
            }
            return Response.ok().build();
        } else {
            return Response
                    .status(Response.Status.FORBIDDEN)
                    .build();
        }
    }

    private EndpointDTO findEndpointDTO(String endpointName) {
        return endpointDAO.get(endpointName)
                .orElseThrow(() -> new ResourceNotFoundException("Endpoint with name " + endpointName
                        + " not found"));
    }
}