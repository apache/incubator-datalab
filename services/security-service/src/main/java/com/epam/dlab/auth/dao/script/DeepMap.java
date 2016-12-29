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

import java.util.HashMap;
import java.util.Map;

public class DeepMap {
	
	private final Map<String, Object> root;
	
	public DeepMap(Map<String, Object> parent) {
		super();
		this.root = parent;
	}

	public DeepMap() {
		super();
		this.root = new HashMap<>();
	}

	public Map<String, Object> getRoot() {
		return root;
	}
	
	public DeepMap getBranch(String branchName) {
		Map<String, Object> branch = (Map<String, Object>) root.get(branchName);
		if( branch == null ) {
			branch = new HashMap<>();
			root.put(branchName, branch);
		}
		return new DeepMap(branch);
	}
	
	public void put(String key,Object val) {
		root.put(key, val);
	}
	
}
