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

package com.epam.dlab.auth.ldap.core;

import java.util.List;
import java.util.Map;

import org.apache.directory.api.ldap.model.message.SearchRequest;
import org.apache.directory.api.ldap.model.message.SearchRequestImpl;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;

import com.epam.dlab.auth.ldap.core.filter.SearchResultProcessor;

public class Request {
/*  
-   request: 
    scope: SUBTREE
    attributes: 
      - "*"
    timeLimit: 0
    base: dc=example,dc=com
    filter: 
 * */
	
	private String name;
	private String scope;
	private List<String> attributes;
	private int timeLimit = 0;
	private String base;
	private String filter = "";
	private SearchResultProcessor searchResultProcessor;
	private boolean cache;
	private long expirationTimeMsec;
	public String getScope() {
		return scope;
	}
	public String[] getAttributes() {
		return attributes.toArray(new String[]{});
	}
	public int getTimeLimit() {
		return timeLimit;
	}
	public String getBase() {
		return base;
	}
	public String getFilter() {
		return filter;
	}
	
	public SearchResultProcessor getSearchResultProcessor() {
		return searchResultProcessor;
	}
	public void setSearchResultProcessor(SearchResultProcessor searchResultProcessor) {
		this.searchResultProcessor = searchResultProcessor;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public boolean isCache() {
		return cache;
	}
	public void setCache(boolean cache) {
		this.cache = cache;
	}
	public long getExpirationTimeMsec() {
		return expirationTimeMsec;
	}
	public void setExpirationTimeMsec(long expirationTimeMsec) {
		this.expirationTimeMsec = expirationTimeMsec;
	}
	public SearchRequest buildSearchRequest(Map<String,Object> replace) {
		SearchRequest sr = new SearchRequestImpl();
		try {
			sr.setBase(new Dn(this.base));
			sr.addAttributes(this.getAttributes());
			if(this.filter != null && ! "".equals(this.filter ) ){
				String f = filter;
				for(String key:replace.keySet()) {
					f = f.replaceAll(key, replace.get(key).toString());
				}
				sr.setFilter(f);				
			}
			sr.setScope(SearchScope.valueOf(this.scope));
			sr.setTimeLimit(this.timeLimit);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return sr;
	}
	
	@Override
	public String toString() {
		return "RequestConfig [scope=" + scope + ", attributes=" + attributes + ", timeLimit=" + timeLimit + ", base=" + base
				+ ", filter=" + filter + "]";
	}


	
}
