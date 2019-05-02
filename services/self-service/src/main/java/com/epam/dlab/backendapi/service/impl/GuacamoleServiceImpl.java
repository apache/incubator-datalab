package com.epam.dlab.backendapi.service.impl;

import com.epam.dlab.auth.UserInfo;
import com.epam.dlab.backendapi.SelfServiceApplicationConfiguration;
import com.epam.dlab.backendapi.service.GuacamoleService;
import com.epam.dlab.exceptions.DlabException;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import org.apache.guacamole.net.GuacamoleTunnel;
import org.apache.guacamole.net.InetGuacamoleSocket;
import org.apache.guacamole.net.SimpleGuacamoleTunnel;
import org.apache.guacamole.protocol.ConfiguredGuacamoleSocket;
import org.apache.guacamole.protocol.GuacamoleConfiguration;

import java.util.Map;

@Slf4j
@Singleton
public class GuacamoleServiceImpl implements GuacamoleService {

	private static final String PRIVATE_KEY_PARAM_NAME = "private-key";
	private static final String HOSTNAME_PARAM = "hostname";
	private static final String CONNECTION_PROTOCOL_PARAM = "connectionProtocol";
	private final SelfServiceApplicationConfiguration conf;

	@Inject
	public GuacamoleServiceImpl(SelfServiceApplicationConfiguration conf) {
		this.conf = conf;
	}

	@Override
	public GuacamoleTunnel getTunnel(UserInfo userInfo, String host) {
		try {
			final String privateKeyContent = "";// TODO figure out from which place private key should be taken
			final InetGuacamoleSocket socket = new InetGuacamoleSocket(conf.getGuacamoleHost(),
					conf.getGuacamolePort());
			final GuacamoleConfiguration guacamoleConfig = getGuacamoleConfig(privateKeyContent, conf.getGuacamole(),
					host);
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
