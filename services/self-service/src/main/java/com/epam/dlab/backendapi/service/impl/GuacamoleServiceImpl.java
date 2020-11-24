package com.epam.dlab.backendapi.service.impl;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.conf.SelfServiceApplicationConfiguration;
import com.epam.dlab.backendapi.service.EndpointService;
import com.epam.dlab.backendapi.service.GuacamoleService;
import com.epam.dlab.constants.ServiceConsts;
import com.epam.dlab.exceptions.DlabException;
import com.epam.dlab.rest.client.RESTService;
import com.epam.dlab.rest.contracts.KeyAPI;
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
			throw new DlabException("Can not create guacamole tunnel due to: " + e.getMessage(), e);
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