/*
 * Copyright (c) 2018, EPAM SYSTEMS INC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.epam.dlab.auth.core;

import com.epam.dlab.exceptions.DlabException;
import lombok.extern.slf4j.Slf4j;
import org.apache.directory.ldap.client.api.LdapConnection;

import java.io.Closeable;

@Slf4j
public abstract class DlabLdapConnection implements Closeable {

	abstract LdapConnection getConnection() throws Exception;

	public LdapConnection connect() throws Exception {
		final LdapConnection connection = getConnection();
		if (!connection.connect()) {
			log.error("Cannot establish a connection to LDAP server");
			throw new DlabException("Login user failed. LDAP server is not available");
		}
		connection.bind();
		return connection;
	}
}
