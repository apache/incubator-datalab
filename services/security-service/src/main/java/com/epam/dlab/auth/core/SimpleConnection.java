package com.epam.dlab.auth.core;

import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapNetworkConnection;

import java.io.IOException;

public class SimpleConnection extends DlabLdapConnection {

	private final LdapNetworkConnection connection;

	public SimpleConnection(LdapNetworkConnection connection) {
		this.connection = connection;
	}

	@Override
	public LdapConnection getConnection() {
		return this.connection;
	}

	@Override
	public void close() throws IOException {
		if (connection != null) {
			connection.close();
		}

	}
}
