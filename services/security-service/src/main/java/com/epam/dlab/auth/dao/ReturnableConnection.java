/***************************************************************************

Copyright (c) 2016, EPAM SYSTEMS INC

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

****************************************************************************/

package com.epam.dlab.auth.dao;

import org.apache.directory.api.ldap.model.exception.LdapException;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapConnectionPool;

import java.io.Closeable;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ReturnableConnection implements Closeable {

	private final LdapConnectionPool pool;
	private LdapConnection con;
	private final Lock lock = new ReentrantLock();
	
	public ReturnableConnection(LdapConnectionPool pool) {
		Objects.requireNonNull(pool);
		this.pool = pool;
	}
	
	public LdapConnection getConnection() throws Exception {
		try {
			lock.lock(); //just protect from inproper use
			if(con == null) {
				con = pool.borrowObject();
			} else {
				throw new IllegalStateException("Cannot reuse connection. Create new ReturnableConnection");
			}
		} finally {
			lock.unlock();
		}
		return con;
	}
	
	@Override
	public void close() throws IOException {
		try {
			pool.releaseConnection(con);
		} catch (LdapException e) {
			throw new IOException("LDAP Release Connection error",e);
		}

	}

}
