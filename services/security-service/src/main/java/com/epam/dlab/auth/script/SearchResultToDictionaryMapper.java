/******************************************************************************************************

 Copyright (c) 2016 EPAM Systems Inc.

 Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

 *****************************************************************************************************/

package com.epam.dlab.auth.script;

import java.io.IOException;
import java.util.Map;

import org.apache.directory.api.ldap.model.cursor.SearchCursor;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.message.SearchResultEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.epam.dlab.auth.ldap.core.filter.SearchResultMapper;

public class SearchResultToDictionaryMapper implements SearchResultMapper<Map<String,Object>> {
	
	private final static Logger LOG = LoggerFactory.getLogger(SearchResultToDictionaryMapper.class);
	
	private final DeepMap root;
	private final DeepMap reqBranch;
	private final String name;
	
	public SearchResultToDictionaryMapper(String name) {
		this.name = name;
		this.root = new DeepMap();
		reqBranch = root.getBranch(name);
	}
	
	public SearchResultToDictionaryMapper(String name, Map<String,Object> context) {
		this.name = name;
		this.root = new DeepMap(context);
		reqBranch = root.getBranch(name);
	}
	
	@Override
	public Map<String, Object> transformSearchResult(SearchCursor cursor) throws IOException {
		LOG.debug(name);
		cursor.forEach(response -> {
			if (response instanceof SearchResultEntry) {
				Entry resultEntry = ((SearchResultEntry) response).getEntry();
				String dn = resultEntry.getDn().toString();
				LOG.debug("\tEntryDN {}",dn);
				DeepMap dnBranch = reqBranch.getBranch(dn);
				resultEntry.forEach(attr -> {
					dnBranch.put(attr.getId() + "", attr.get() + "");
					LOG.debug("\t\tAttr {}",attr);
				});
			}
		});
		cursor.close();
		return reqBranch.getRoot();
	}

	@Override
	public Map<String, Object> getBranch() {
		return reqBranch.getRoot();
	}

	@Override
	public String toString() {
		return "SearchResultToDictionaryMapper [name=" + name + ", parent=" + root + ", branch=" + reqBranch + "]";
	}

	
}
