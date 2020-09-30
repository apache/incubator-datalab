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

package com.epam.datalab.backendapi.service.impl;

import com.epam.datalab.auth.UserInfo;
import com.epam.datalab.backendapi.conf.SelfServiceApplicationConfiguration;
import com.epam.datalab.backendapi.service.EndpointService;
import com.epam.datalab.backendapi.service.GuacamoleService;
import com.epam.datalab.constants.ServiceConsts;
import com.epam.datalab.exceptions.DatalabException;
import com.epam.datalab.rest.client.RESTService;
import com.epam.datalab.rest.contracts.KeyAPI;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.apache.guacamole.net.GuacamoleTunnel;
import org.apache.guacamole.net.InetGuacamoleSocket;
import org.apache.guacamole.net.SimpleGuacamoleTunnel;
import org.apache.guacamole.protocol.ConfiguredGuacamoleSocket;
import org.apache.guacamole.protocol.GuacamoleConfiguration;

import javax.inject.Named;
import java.net.URI;
import java.util.Map;

@Slf4j
@Singleton
public class GuacamoleServiceImpl implements GuacamoleService {

    private static final String PRIVATE_KEY_PARAM_NAME = "private-key";
    private static final String HOSTNAME_PARAM = "hostname";
    private static final String CONNECTION_PROTOCOL_PARAM = "connectionProtocol";
    private static final String SERVER_HOST_PARAM = "serverHost";
    private final SelfServiceApplicationConfiguration conf;
    private final RESTService provisioningService;
    private final EndpointService endpointService;

    @Inject
    public GuacamoleServiceImpl(SelfServiceApplicationConfiguration conf,
                                @Named(ServiceConsts.PROVISIONING_SERVICE_NAME) RESTService provisioningService,
                                EndpointService endpointService) {
        this.conf = conf;
        this.provisioningService = provisioningService;
        this.endpointService = endpointService;
    }

    @Override
    public GuacamoleTunnel getTunnel(UserInfo userInfo, String host, String endpoint) {
        try {
            final String url = endpointService.get(endpoint).getUrl();
            String key = provisioningService.get(url + KeyAPI.GET_ADMIN_KEY,
                    userInfo.getAccessToken(), String.class);
            final String guacamoleServerHost = new URI(url).getHost();
            InetGuacamoleSocket socket = new InetGuacamoleSocket(guacamoleServerHost, conf.getGuacamolePort());
            final Map<String, String> guacamoleConf = conf.getGuacamole();
            guacamoleConf.put(SERVER_HOST_PARAM, guacamoleServerHost);
            GuacamoleConfiguration guacamoleConfig = getGuacamoleConfig(key, guacamoleConf, host);
            return new SimpleGuacamoleTunnel(new ConfiguredGuacamoleSocket(socket, guacamoleConfig));
        } catch (Exception e) {
            log.error("Can not create guacamole tunnel due to: " + e.getMessage());
            throw new DatalabException("Can not create guacamole tunnel due to: " + e.getMessage(), e);
        }
    }

    private GuacamoleConfiguration getGuacamoleConfig(String privateKeyContent, Map<String, String> guacamoleParams,
                                                      String host) {
        GuacamoleConfiguration guacamoleConfiguration = new GuacamoleConfiguration();
        guacamoleConfiguration.setProtocol(guacamoleParams.get(CONNECTION_PROTOCOL_PARAM));
        guacamoleConfiguration.setParameters(guacamoleParams);
        guacamoleConfiguration.setParameter(HOSTNAME_PARAM, host);
        guacamoleConfiguration.setParameter(PRIVATE_KEY_PARAM_NAME, privateKeyContent);
        return guacamoleConfiguration;
    }
}
