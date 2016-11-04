/******************************************************************************************************

 Copyright (c) 2016 EPAM Systems Inc.

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 *****************************************************************************************************/

package com.epam.dlab.auth.ldap;

import org.apache.commons.pool.PoolableObjectFactory;
import org.apache.directory.api.ldap.model.cursor.SearchCursor;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchRequestImpl;
import org.apache.directory.api.ldap.model.message.SearchResultEntry;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.ldap.client.api.LdapConnection;
import org.apache.directory.ldap.client.api.LdapConnectionConfig;
import org.apache.directory.ldap.client.api.LdapConnectionPool;
import org.apache.directory.ldap.client.api.ValidatingPoolableLdapConnectionFactory;
//import org.apache.directory.ldap.client.api.PoolableLdapConnectionFactory;

public class BasicTest {

	public static void main(String[] args) throws Exception {
		System.out.println("Basic test");
		LdapConnectionConfig config = new LdapConnectionConfig();
		config.setLdapHost( "localhost" );
		config.setLdapPort( 3890 );
		config.setName( "cn=admin,dc=example,dc=com" );
		config.setCredentials( "ldap" );
		PoolableObjectFactory<LdapConnection> poolFactory = new ValidatingPoolableLdapConnectionFactory( config );
		LdapConnectionPool pool = new LdapConnectionPool( poolFactory );
		pool.setTestOnBorrow( true );
		LdapConnection con = pool.borrowObject();
		
		SearchRequest sr = new SearchRequestImpl();
	    sr.setScope(SearchScope.SUBTREE);
	    sr.addAttributes("*");
	    sr.setTimeLimit(0);
	    sr.setBase(new Dn("dc=example,dc=com"));
	    sr.setFilter("(cn=Mike Teplitskiy)");
	    sr.setMessageId(1);
		
//		EntryCursor cursor = con.search( "dc=example,dc=com", "(objectclass=*)", SearchScope.SUBTREE );
		SearchCursor cursor = con.search( sr );
//
//		cursor.forEach(entry->{
//	    	System.out.println( "---- DN "+entry.getDn() );
//		    entry.forEach(attr->{
//		    	System.out.println( "---- ATTR "+attr );
//		    });
//			
//		});
		
		cursor.forEach(response->{
		    if ( response instanceof SearchResultEntry )
		    {
		        Entry resultEntry = ( ( SearchResultEntry ) response ).getEntry();
		        System.out.println( "---- DN "+resultEntry.getDn() );
		        resultEntry.forEach(attr->{
		        	System.out.println( "---- ATTR "+attr );
		        });
		    }
		});
		cursor.close();
		
	    sr.setFilter("(cn=John Doe)");
	    sr.setMessageId(1);
	    cursor = con.search( sr );
		cursor.forEach(response->{
		    if ( response instanceof SearchResultEntry )
		    {
		        Entry resultEntry = ( ( SearchResultEntry ) response ).getEntry();
		        System.out.println( "---- DN "+resultEntry.getDn() );
		        resultEntry.forEach(attr->{
		        	System.out.println( "---- ATTR "+attr );
		        });
		    }
		});
		cursor.close();
	    

		
		con.unBind();
		
		con.bind("uid=mike,ou=People,dc=example,dc=com","test");
	    sr.setFilter("(uid=mike)");
	    sr.setMessageId(2);
	    cursor = con.search( sr );
		cursor.forEach(response->{
		    if ( response instanceof SearchResultEntry )
		    {
		        Entry resultEntry = ( ( SearchResultEntry ) response ).getEntry();
		        System.out.println( "---- DN "+resultEntry.getDn() );
		        resultEntry.forEach(attr->{
		        	System.out.println( "---- ATTR "+attr );
		        });
		    }
		});
		cursor.close();
		
		System.out.println("Press ENTER");
		pool.releaseConnection(con);
		pool.close();
		
	}

}
