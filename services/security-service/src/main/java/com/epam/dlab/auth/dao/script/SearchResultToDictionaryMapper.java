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

package com.epam.dlab.auth.dao.script;

import com.epam.dlab.auth.dao.filter.SearchResultMapper;
import org.apache.commons.lang3.StringUtils;
import org.apache.directory.api.ldap.model.cursor.SearchCursor;
import org.apache.directory.api.ldap.model.entry.Attribute;
import org.apache.directory.api.ldap.model.entry.Entry;
import org.apache.directory.api.ldap.model.message.Response;
import org.apache.directory.api.ldap.model.message.SearchResultEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class SearchResultToDictionaryMapper implements SearchResultMapper<Map<String, Object>> {

	private static final Logger LOG = LoggerFactory.getLogger(SearchResultToDictionaryMapper.class);
	private static final String DISTINGUISH_NAME = "dn";

	private final DeepMap root;
	private final DeepMap reqBranch;
	private final String name;

	public SearchResultToDictionaryMapper(String name) {
		this.name = name;
		this.root = new DeepMap();
		reqBranch = root.getBranch(name);
	}

	public SearchResultToDictionaryMapper(String name, Map<String, Object> context) {
		this.name = name;
		this.root = new DeepMap(context);
		reqBranch = root.getBranch(name);
	}

	@Override
	public Map<String, Object> transformSearchResult(SearchCursor cursor) throws IOException {
		LOG.debug(name);
		cursor.forEach(this::processResponse);
		return reqBranch.getRoot();
	}

	private void processResponse(Response response) {
		if (response instanceof SearchResultEntry) {
			Entry resultEntry = ((SearchResultEntry) response).getEntry();
			String dn = resultEntry.getDn().toString();
			LOG.debug("\tEntryDN {}", dn);
			DeepMap dnBranch = reqBranch.getBranch(dn.toLowerCase());
			dnBranch.put(DISTINGUISH_NAME, dn);
			resultEntry.forEach(attr -> addAttributes(dnBranch, attr));
		}
	}

	private void addAttributes(DeepMap dnBranch, Attribute attr) {
		// Since there might be multiple attributes with the same name, it is required to collect all their values (i.e. memberUid in group)
		if (attr.size() > 1) {

			List<Object> list = new ArrayList<>();
			attr.iterator().forEachRemaining(list::add);

			String join = StringUtils.join(list, ",");
			dnBranch.put(attr.getId() + "", join);
			LOG.debug("\t\tAttr {} : {} ", attr.getId(), join);
		} else {
			dnBranch.put(attr.getId() + "", attr.get() + "");
			LOG.debug("\t\tAttr {}", attr);
		}
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
