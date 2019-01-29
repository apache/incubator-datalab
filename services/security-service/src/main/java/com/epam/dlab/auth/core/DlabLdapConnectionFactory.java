/*
 *
 *  * Copyright (c) 2018, EPAM SYSTEMS INC
 *  *
 *  * Licensed under the Apache License, Version 2.0 (the "License");
 *  * you may not use this file except in compliance with the License.
 *  * You may obtain a copy of the License at
 *  *
 *  *     http://www.apache.org/licenses/LICENSE-2.0
 *  *
 *  * Unless required by applicable law or agreed to in writing, software
 *  * distributed under the License is distributed on an "AS IS" BASIS,
 *  * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  * See the License for the specific language governing permissions and
 *  * limitations under the License.
 *
 */

package com.epam.dlab.auth.core;

import com.epam.dlab.auth.SecurityServiceConfiguration;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapConnectionPool;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;
import org.apache.directory.ldap.client.api.ValidatingPoolableLdapConnectionFactory;

@Singleton
public class DlabLdapConnectionFactory {


	private final LdapConnectionConfig connConfig;
	private final LdapConnectionPool connectionPool;
	private final boolean usePool;

	@Inject
	public DlabLdapConnectionFactory(SecurityServiceConfiguration configuration) {
		this.connConfig = configuration.getLdapConnectionConfig();
		this.connectionPool = new LdapConnectionPool(new ValidatingPoolableLdapConnectionFactory(connConfig));
		this.usePool = configuration.isLdapUseConnectionPool();
	}

	public DlabLdapConnection newConnection() {
		return usePool ? new ReturnableConnection(connectionPool) :
				new SimpleConnection(new LdapNetworkConnection(connConfig));
	}
}
