/******************************************************************************************************

 Copyright (c) 2016 EPAM Systems Inc.

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 *****************************************************************************************************/

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
