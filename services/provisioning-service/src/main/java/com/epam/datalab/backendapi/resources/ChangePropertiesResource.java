package com.epam.datalab.backendapi.resources;

import com.epam.datalab.auth.UserInfo;
import com.epam.datalab.backendapi.core.response.folderlistener.FolderListener;
import com.epam.datalab.backendapi.core.response.folderlistener.WatchItem;
import com.epam.datalab.backendapi.core.response.folderlistener.WatchItemList;
import com.epam.datalab.properties.ChangePropertiesConst;
import com.epam.datalab.properties.ChangePropertiesService;
import com.epam.datalab.properties.RestartForm;
import com.epam.datalab.properties.YmlDTO;
import io.dropwizard.auth.Auth;
import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;

import static com.epam.datalab.backendapi.core.response.folderlistener.WatchItem.ItemStatus.INPROGRESS;
import static com.epam.datalab.backendapi.core.response.folderlistener.WatchItem.ItemStatus.WAIT_FOR_FILE;

@Path("/config")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Slf4j
public class ChangePropertiesResource implements ChangePropertiesConst {

    private final ChangePropertiesService changePropertiesService;
    private final List<WatchItem.ItemStatus> inProgressStatuses = Arrays.asList(INPROGRESS, WAIT_FOR_FILE);

    @Inject
    public ChangePropertiesResource(ChangePropertiesService changePropertiesService) {
        this.changePropertiesService = changePropertiesService;
    }

    @GET
    @Path("/provisioning-service")
    public Response getProvisioningServiceProperties(@Auth UserInfo userInfo) {
        return Response
                .ok(changePropertiesService.readFileAsString(PROVISIONING_SERVICE_PROP_PATH, PROVISIONING_SERVICE))
                .build();
    }

    @GET
    @Path("/billing")
    public Response getBillingServiceProperties(@Auth UserInfo userInfo) {
        return Response
                .ok(changePropertiesService.readFileAsString(BILLING_SERVICE_PROP_PATH, BILLING_SERVICE))
                .build();
    }

    @POST
    @Path("/provisioning-service")
    public Response overwriteProvisioningServiceProperties(@Auth UserInfo userInfo, YmlDTO ymlDTO) {
        changePropertiesService.writeFileFromString(PROVISIONING_SERVICE_PROP_PATH, PROVISIONING_SERVICE,
                ymlDTO.getYmlString());
        return Response.ok().build();
    }

    @POST
    @Path("/billing")
    public Response overwriteBillingServiceProperties(@Auth UserInfo userInfo, YmlDTO ymlDTO) {
        changePropertiesService.writeFileFromString(BILLING_SERVICE_PROP_PATH, BILLING_SERVICE, ymlDTO.getYmlString());
        return Response.ok().build();

    }

    @POST
    @Path("/restart")
    public Response restart(@Auth UserInfo userInfo, RestartForm restartForm) {
        checkResponseFiles(restartForm);
        changePropertiesService.restart(restartForm);
        return Response.ok().build();
    }

    private void checkResponseFiles(RestartForm restartForm) {
        List<WatchItem> watchItems = new ArrayList<>();
        //or check getFileHandlerCallback().getId()/uuid
        boolean isNoneFinishedRequests = FolderListener.getListeners().stream()
                .filter(FolderListener::isAlive)
                .filter(FolderListener::isListen)
                .map(FolderListener::getItemList)
                .anyMatch(findAnyInStatus(watchItems, inProgressStatuses));
        if (isNoneFinishedRequests) {
            log.info("Found unchecked response file from docker : {}." +
                    " Provisioning restart is denied", watchItems);
            restartForm.setProvserv(false);
        }
    }

    private Predicate<WatchItemList> findAnyInStatus(List<WatchItem> watchItems,
                                                     List<WatchItem.ItemStatus> statuses) {
        return watchItemList -> {
            for (int i = 0; i < watchItemList.size(); i++) {
                if (statuses.contains(watchItemList.get(i).getStatus())) {
                    watchItems.add(watchItemList.get(i));
                }
            }
            return !watchItems.isEmpty();
        };
    }
}
