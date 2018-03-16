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
