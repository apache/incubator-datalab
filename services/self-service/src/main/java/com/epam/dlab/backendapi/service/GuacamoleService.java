package com.epam.dlab.backendapi.service;

import com.epam.dlab.auth.UserInfo;
import org.apache.guacamole.net.GuacamoleTunnel;

public interface GuacamoleService {

	GuacamoleTunnel getTunnel(UserInfo userInfo, String host, String endpoint);

}
