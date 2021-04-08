package com.epam.datalab.properties;

import com.epam.datalab.auth.UserInfo;
import com.epam.datalab.constants.ServiceConsts;
import com.epam.datalab.rest.client.RESTService;
import com.google.inject.name.Named;
import lombok.extern.slf4j.Slf4j;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class ExternalChangeProperties implements ChangePropertiesConst {
    private final RESTService externalSelfService;
    private final ChangePropertiesService changePropertiesService;

    @Inject
    public ExternalChangeProperties(@Named(ServiceConsts.PROVISIONING_SERVICE_NAME)
                                           RESTService externalSelfService, ChangePropertiesService changePropertiesService) {
        this.externalSelfService = externalSelfService;
        this.changePropertiesService = changePropertiesService;
    }

    public String getProperties(String path, String name) {
        return changePropertiesService.readFileAsString(path, name);
    }

    public void overwriteProperties(String path, String name, String ymlString) {
        changePropertiesService.writeFileFromString(ymlString, name, path);
    }

    public Map<String, String> getPropertiesWithExternal(String endpoint, UserInfo userInfo, String url) {
        Map<String, String> properties = new HashMap<>();
        if (endpoint.equals(ChangePropertiesConst.LOCAL_ENDPOINT_NAME)) {
            properties.put(ChangePropertiesConst.SELF_SERVICE, getProperties(ChangePropertiesConst.SELF_SERVICE_PROP_PATH, ChangePropertiesConst.SELF_SERVICE));
            properties.put(ChangePropertiesConst.PROVISIONING_SERVICE, getProperties(ChangePropertiesConst.PROVISIONING_SERVICE_PROP_PATH, ChangePropertiesConst.PROVISIONING_SERVICE));
            properties.put(ChangePropertiesConst.BILLING_SERVICE, getProperties(ChangePropertiesConst.BILLING_SERVICE_PROP_PATH, ChangePropertiesConst.BILLING_SERVICE));
        } else {
            log.info("Trying to read properties, for external endpoint : {} , for user: {}",
                    endpoint, userInfo.getSimpleName());
            properties.put(ChangePropertiesConst.PROVISIONING_SERVICE,
                    externalSelfService.get(url + "/provisioning-service", userInfo.getAccessToken(), String.class));
            properties.put(ChangePropertiesConst.BILLING_SERVICE,
                    externalSelfService.get(url + "/billing", userInfo.getAccessToken(), String.class));
        }
        return properties;
    }

    public void overwritePropertiesWithExternal(String path, String name, YmlDTO ymlDTO, UserInfo userInfo,
                                                String url) {
        log.info("Trying to write {}, for external endpoint : {} , for user: {}",
                name, ymlDTO.getEndpointName(), userInfo.getSimpleName());
        if (ymlDTO.getEndpointName().equals(ChangePropertiesConst.LOCAL_ENDPOINT_NAME)
                || name.equals(SELF_SERVICE)
                || name.equals(GKE_SELF_SERVICE)) {
            changePropertiesService.writeFileFromString(ymlDTO.getYmlString(), name, path);
        } else {
            url += findMethodName(name);
            externalSelfService.post(url, ymlDTO.getYmlString(), userInfo.getAccessToken(), String.class);
        }
    }


    public void restartForExternal(RestartForm restartForm, UserInfo userInfo, String url) {
        if (restartForm.getEndpoint().equals(LOCAL_ENDPOINT_NAME)) {
            changePropertiesService.restart(restartForm);
        } else {
            log.info("External request for endpoint {}, for user {}", restartForm.getEndpoint(), userInfo.getSimpleName());
            externalSelfService.post(url, userInfo.getAccessToken(), restartForm, Void.class);
        }
    }

    private String findMethodName(String name) {
        switch (name) {
            case "provisioning.yml": {
                return "/provisioning-service";
            }
            case "billing.yml": {
                return "/billing";
            }
            default:
                return "";
        }
    }

    public void restartForExternalForGKE(UserInfo userInfo, RestartForm restartForm) {
        throw new NotImplementedException();
    }
}
