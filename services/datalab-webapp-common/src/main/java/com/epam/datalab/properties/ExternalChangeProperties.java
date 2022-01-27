/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.epam.datalab.properties;

import com.epam.datalab.auth.UserInfo;
import com.epam.datalab.constants.ServiceConsts;
import com.epam.datalab.rest.client.RESTService;
import lombok.extern.slf4j.Slf4j;
import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import javax.inject.Inject;
import javax.inject.Named;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class ExternalChangeProperties implements ChangePropertiesConst {
    private final RESTService provService;
    private final ChangePropertiesService changePropertiesService;

    @Inject
    public ExternalChangeProperties(@Named(ServiceConsts.PROVISIONING_SERVICE_NAME) RESTService provService,
                                    ChangePropertiesService changePropertiesService) {
        this.provService = provService;
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
        properties.put(SELF_SERVICE, getProperties(SELF_SERVICE_PROP_PATH, SELF_SERVICE));
        properties.put(PROVISIONING_SERVICE, getProperties(PROVISIONING_SERVICE_PROP_PATH, PROVISIONING_SERVICE));
        properties.put(BILLING_SERVICE, getProperties(BILLING_SERVICE_PROP_PATH, BILLING_SERVICE));
        if (!endpoint.equals(LOCAL_ENDPOINT_NAME)) {
            log.info("Trying to read properties, for external endpoint : {} , for user: {}",
                    endpoint, userInfo.getSimpleName());
            String provPath = url + "/provisioning-service";
            String billPath = url + "/billing";
            properties.put(PROVISIONING_SERVICE, provService.get(provPath, userInfo.getAccessToken(), String.class));
            properties.put(BILLING_SERVICE, provService.get(billPath, userInfo.getAccessToken(), String.class));
        }
        return properties;
    }

    public void overwritePropertiesWithExternal(String path, String name, YmlDTO ymlDTO,
                                                UserInfo userInfo, String url) {
        log.info("Trying to write {}, for external endpoint : {} , for user: {}",
                name, ymlDTO.getEndpointName(), userInfo.getSimpleName());
        if (ymlDTO.getEndpointName().equals(LOCAL_ENDPOINT_NAME)
                || ymlDTO.getEndpointName().isEmpty()
                || name.equals(SELF_SERVICE)
                || name.equals(GKE_SELF_SERVICE)) {
            changePropertiesService.writeFileFromString(ymlDTO.getYmlString(), name, path);
        } else {
            url += findMethodName(name);
            provService.post(url, userInfo.getAccessToken(), ymlDTO, Void.class);
        }
    }


    public RestartAnswer restartForExternal(RestartForm restartForm, UserInfo userInfo, String url) {
        if (restartForm.getEndpoint().equals(LOCAL_ENDPOINT_NAME) || restartForm.getEndpoint().isEmpty()) {
            return changePropertiesService.restart(restartForm);
        } else {
            log.info("External request for endpoint {}, for user {}", restartForm.getEndpoint(), userInfo.getSimpleName());
            return provService.post(url, userInfo.getAccessToken(), restartForm, RestartAnswer.class);
        }
    }

    private String findMethodName(String name) {
        switch (name) {
            case "provisioning.yml":
            case "provisioning": {
                return "/provisioning-service";
            }
            case "billing.yml":
            case "billing": {
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
